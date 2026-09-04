(ns minweb.utils.oss-aliyun
  "阿里云 OSS provider —— 自实现 PUT/HEAD/GET,不依赖官方 SDK。
   签名:`OSS AccessKeyId:Signature`,Signature = Base64(HmacSHA1(sk, StringToSign))
   StringToSign 见: https://help.aliyun.com/document_detail/31951.html

   由项目侧 facade(minweb 风格下不内置)在 OSS_PROVIDER=aliyun 时调用。
   本 ns 只做纯算法,凭证由调用方传入,不读 config / 不依赖 Datalevin。"
  (:require
   [babashka.http-client :as http]
   [clojure.string :as str])
  (:import
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.util Base64]
   [java.time ZonedDateTime ZoneOffset]
   [java.time.format DateTimeFormatter]))

(def ^:private gmt-fmt
  (-> (DateTimeFormatter/ofPattern "EEE, dd MMM yyyy HH:mm:ss 'GMT'")
      (.withLocale java.util.Locale/US)
      (.withZone ZoneOffset/UTC)))

(defn- now-gmt []
  (.format gmt-fmt (ZonedDateTime/now ZoneOffset/UTC)))

(defn- hmac-sha1-b64 [^String secret ^String data]
  (let [mac (Mac/getInstance "HmacSHA1")
        spec (SecretKeySpec. (.getBytes secret "UTF-8") "HmacSHA1")
        _ (.init mac spec)
        digest (.doFinal mac (.getBytes data "UTF-8"))]
    (.encodeToString (Base64/getEncoder) digest)))

(defn- string-to-sign
  [{:keys [http-verb content-md5 content-type date canonical-headers canonical-resource]}]
  (str/join "\n"
            [http-verb
             (or content-md5 "")
             (or content-type "")
             date
             (or canonical-headers "")
             (or canonical-resource "")]))

;; 上传时把对象 ACL 设成 public-read,这样浏览器拿 URL 直接 GET 就能拿到图片
;; (否则 bucket 默认私有,GET 返 403,前端 img 显示 broken 图标)。
;; 同时必须放进 CanonicalizedOSSHeaders 让签名算上,否则 SignatureDoesNotMatch。
;; 注:每个 canonical header 结尾的 \n 是 OSS 规范要求的(per-header),
;; 但它跟后面的 CanonicalizedResource 之间不再加分隔符,
;; 所以这里不加尾巴 \n —— str/join "\n" 已经会加。
(def ^:private object-acl "public-read")
(def ^:private canonical-headers
  (str "x-oss-object-acl:" object-acl))

(defn- sign-headers
  [{:keys [ak sk bucket]} {:keys [http-verb key content-type content-md5]}]
  (let [date (now-gmt)
        resource (str "/" bucket "/" key)
        sts (string-to-sign {:http-verb http-verb
                             :content-md5 content-md5
                             :content-type content-type
                             :date date
                             :canonical-headers canonical-headers
                             :canonical-resource resource})
        sig (hmac-sha1-b64 sk sts)]
    {"Date" date
     "Authorization" (str "OSS " ak ":" sig)}))

(defn- object-url [{:keys [bucket endpoint]} key]
  (format "https://%s.%s/%s" bucket endpoint key))

(defn put-object
  "把字节数组写到阿里云 OSS。
   creds: {:ak ... :sk ... :endpoint ... :bucket ...}
   key: 对象路径(不带前导 /)
   data: byte-array
   content-type: 'image/jpeg' 等
   返回 {:url ... :status 200} 或 {:error ... :status ... :body ...}"
  [{:keys [ak sk endpoint bucket]} ^String key ^bytes data ^String content-type]
  ;; seq 检查同时挡掉 nil 和空字符串(env defaults 给 \"\" 也不能漏到这)
  (when-not (every? seq [ak sk endpoint bucket])
    (throw (ex-info "Aliyun OSS not configured"
                    {:has-ak? (boolean (seq ak))
                     :has-sk? (boolean (seq sk))
                     :has-endpoint? (boolean (seq endpoint))
                     :has-bucket? (boolean (seq bucket))})))
  ;; Content-Length 是 JDK HttpURLConnection restricted header(11+),不能手动设,
  ;; 它会从 body 算。Content-Type 不在 restricted 列表里,放 :headers 是最稳的:
  ;; babashka.http-client 的 :content-type option 在某些情况下不会把头真发出去
  ;; (会让 OSS 签名计算的 StringToSign 第 3 行空着 → SignatureDoesNotMatch)。
  (let [headers (-> (sign-headers {:ak ak :sk sk :bucket bucket}
                                  {:http-verb "PUT"
                                   :key key
                                   :content-type content-type})
                    (assoc "Content-Type" content-type)
                    (assoc "x-oss-object-acl" object-acl))
        url (object-url {:bucket bucket :endpoint endpoint} key)
        resp (http/put url {:headers headers :body data :throw false})]
    (if (= 200 (:status resp))
      {:url url :status 200}
      {:error (str "Aliyun OSS PUT failed " (:status resp) ": "
                   (let [b (:body resp)]
                     (cond
                       (nil? b) ""
                       (> (count b) 200) (str (subs b 0 200) "…")
                       :else b)))
       :status (:status resp)
       :body (:body resp)})))

;; ----------------------------------------------------------------------------
;; GET —— backend photo-proxy 等场景用
;; 与 PUT 不同:不发 x-oss-object-acl(GET 不需要),CanonicalizedOSSHeaders 空
;; ----------------------------------------------------------------------------

(defn get-object
  "从阿里云 OSS 取对象字节。
   creds: 同 put-object
   key: 对象路径(不带前导 /)
   返回 {:body bytes :content-type mime :status 200} 或 {:error ... :status ... :body ...}"
  [{:keys [ak sk endpoint bucket]} ^String key]
  (when-not (every? seq [ak sk endpoint bucket])
    (throw (ex-info "Aliyun OSS not configured"
                    {:has-ak? (boolean (seq ak))
                     :has-sk? (boolean (seq sk))
                     :has-endpoint? (boolean (seq endpoint))
                     :has-bucket? (boolean (seq bucket))})))
  (let [date (now-gmt)
        resource (str "/" bucket "/" key)
        ;; StringToSign:VERB\nContent-MD5\nContent-Type\nDate\n
        ;;             CanonicalizedOSSHeaders\nCanonicalizedResource
        ;; GET 不发 x-oss-object-acl → canonical-headers 空
        sts (str "GET\n\n\n" date "\n\n" resource)
        sig (hmac-sha1-b64 sk sts)
        headers {"Date" date
                 "Authorization" (str "OSS " ak ":" sig)}
        url (object-url {:bucket bucket :endpoint endpoint} key)
        ;; 强制 :as :bytes —— 同 ctyun 现场八踩坑:
        ;; babashka.http-client 默认 auto-detect 对 image/png 响应把 body
        ;; 返成 String(3.4MB 二进制当 String),下游 byte 操作会挂。
        resp (http/get url {:headers headers :throw false :as :bytes})]
    (if (= 200 (:status resp))
      {:body (:body resp)
       :content-type (get-in resp [:headers "Content-Type"] "application/octet-stream")
       :status 200}
      {:error (str "Aliyun GET failed " (:status resp) ": "
                   (let [b (:body resp)]
                     (cond
                       (nil? b) ""
                       (> (count b) 200) (str (subs b 0 200) "…")
                       :else b)))
       :status (:status resp)
       :body (:body resp)})))
