(ns minweb.utils.oss-ctyun
  "天翼云对象存储(ZOS,基于自研 S3 兼容 API)provider —— AWS Signature V4 + path-style URL。
   端点格式:https://<endpoint>/<bucket>/<key>(path-style,**非** virtual-hosted)
   实际部署示例:https://insp-task-photos.hangzhou7.zos.ctyun.cn/...
   Region 形如 hangzhou7 / jiangsu-10 / yunnan-2。

   签名流程(AWS Sig V4):
     1. canonical request = method + uri + '' + sorted-headers-block + signed-headers + payload-hash
     2. string-to-sign = 'AWS4-HMAC-SHA256' + amz-date + credential-scope + sha256(canonical-request)
     3. signing key 链:HMAC-SHA256 chain(date → region → 's3' → 'aws4_request')
     4. signature = HMAC-SHA256(signing-key, string-to-sign)
     5. Authorization: 'AWS4-HMAC-SHA256 Credential=<ak>/<scope>, SignedHeaders=<...>, Signature=<sig>'

   URL style(实测 + Java SDK 手册确认):
     Java SDK 使用 .enablePathStyleAccess(),即 https://<endpoint>/<bucket>/<key>
     Host header 不带 bucket 前缀。

   公开读(public-read)设置(踩过的坑,2026-07-08):
     重要:ctyun ZOS 的「桶读写权限 = 公共读」Canned ACL **不会让对象可匿名访问**。
     实测:配了 Canned ACL PublicRead + 桶 ACL 显式给 `*` 读取,匿名 HEAD/GET 对象
     仍 403 AccessDenied。ctyun ZOS 的对象级访问必须显式加 bucket policy:
       Version: 2012-10-17
       Statement:
         - Effect: Allow
           Principal: '*'
           Action:    zos:GetObject              # ctyun 用 zos: 前缀,不是 s3:
           Resource:  zos:<region>:<bucket>/*    # 不是 arn:aws:s3:::<bucket>/*
     控制台 → 桶 → 权限管理 → 桶策略 tab(注意不是 桶ACL 那个)。若控制台拒绝 `zos:`
     前缀,试 `s3:GetObject` + `arn:aws:s3:::<bucket>/*`(ctyun 内部可能会自动转换)。

     put-object 不带 ACL header(per-object x-amz-acl 在 ZOS 无效),公开读只能
     靠 bucket policy + 桶 CannedACL(后者只对桶列表生效)。

   Key 命名 quirk(踩过的坑,2026-07-08):
     AWS S3 允许 key 含 `:`(冒号),但 ctyun ZOS 的网关代理对 `:` 过敏 —— 任何
     带冒号的 key 都 403 AccessDenied,即使签名是合法的。冒号在 URL path 上可能被
     代理误解(port separator / scheme separator),ctyun 没正确处理。规避:key 里
     不用 `:`(时间戳用 `T08-37-40` 而不是 `T08:37:40`)。

   参考:
   - https://docs.aws.amazon.com/IAM/latest/UserGuide/create-signed-request.html
   - https://www.ctyun.cn/document/10026735/10110276
   - 《ZOS 对象存储 Java SDK 使用手册》(本地 PDF)"
  (:require
   [babashka.http-client :as http]
   [cheshire.core :as json]
   [clojure.string :as str])
  (:import
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.net URLEncoder]
   [java.security MessageDigest]
   [java.time ZonedDateTime ZoneOffset]
   [java.time.format DateTimeFormatter]))

;; ----------------------------------------------------------------------------
;; 时间格式:ISO 8601 basic(无分隔符)
;; ----------------------------------------------------------------------------

(def ^:private amz-date-fmt
  (DateTimeFormatter/ofPattern "yyyyMMdd'T'HHmmss'Z'"))

(def ^:private date-stamp-fmt
  (DateTimeFormatter/ofPattern "yyyyMMdd"))

(defn- now-amz-date []
  (let [now (ZonedDateTime/now ZoneOffset/UTC)]
    [(.format amz-date-fmt now) (.format date-stamp-fmt now)]))

;; ----------------------------------------------------------------------------
;; 编码 / 哈希
;; ----------------------------------------------------------------------------

(defn- sha256-hex [^bytes data]
  (let [md (MessageDigest/getInstance "SHA-256")
        digest (.digest md data)]
    (format "%064x" (BigInteger. 1 digest))))

(defn- hmac-sha256 [^bytes key ^String data]
  (let [mac (Mac/getInstance "HmacSHA256")
        spec (SecretKeySpec. key "HmacSHA256")
        _ (.init mac spec)]
    (.doFinal mac (.getBytes data "UTF-8"))))

(defn- hmac-sha256-hex [^bytes key ^String data]
  (format "%064x" (BigInteger. 1 (hmac-sha256 key data))))

(defn- to-bytes [^String s] (.getBytes s "UTF-8"))

;; ----------------------------------------------------------------------------
;; 签名
;; ----------------------------------------------------------------------------

(def ^:private service "s3")

(defn- canonical-request
  "构造 canonical request。signed-headers-name 按字典序排序(小写)。
   path-style URL,Host 不带 bucket 前缀。
   公开读由 bucket 侧保证(CannedAccessControlList.PublicRead on create,
   或 bucket policy),不签 x-amz-acl——per-object ACL header 在 ZOS 上无效。

   query-params: {String-key value-or-values},URI-encode 排序后写进 canonical-query-string。
   形如  PutBucketPolicy 的 `?policy=`(单值)。
   不传 = 空 query string。"
  [{:keys [method canonical-uri query-params amz-date payload-hash host content-length content-type]}]
  (let [signed-headers ["content-length"
                        "content-type"
                        "host"
                        "x-amz-content-sha256"
                        "x-amz-date"]
        headers-block (str "content-length:" content-length "\n"
                           "content-type:" content-type "\n"
                           "host:" host "\n"
                           "x-amz-content-sha256:" payload-hash "\n"
                           "x-amz-date:" amz-date "\n")
        signed-headers-str (str/join ";" signed-headers)
        canonical-query (if (seq query-params)
                          ;; 单值直接 encode,多值按字典序罗列(Sig V4 规范)
                          (->> query-params
                               (mapcat (fn [[k v]]
                                         (let [ek (URLEncoder/encode (name k) "UTF-8")]
                                           (if (sequential? v)
                                             (->> v sort (map #(str ek "=" (URLEncoder/encode (str %) "UTF-8"))))
                                             [(str ek "=" (URLEncoder/encode (str v) "UTF-8"))]))))
                               (sort)
                               (str/join "&"))
                          "")]
    {:canonical-request (str/join "\n"
                                  [method
                                   canonical-uri
                                   canonical-query
                                   headers-block
                                   signed-headers-str
                                   payload-hash])
     :signed-headers signed-headers-str}))

(defn- string-to-sign [amz-date credential-scope creq]
  (str/join "\n"
            ["AWS4-HMAC-SHA256"
             amz-date
             credential-scope
             (sha256-hex (to-bytes creq))]))

(defn- signing-key
  "kDate → kRegion → kService → kSigning 的 4 级 HMAC-SHA256 链"
  [secret date-stamp region]
  (-> (to-bytes (str "AWS4" secret))
      (hmac-sha256 date-stamp)
      (hmac-sha256 region)
      (hmac-sha256 service)
      (hmac-sha256 "aws4_request")))

(defn sign-headers
  "对 PUT/PUT-?policy= 等构造 V4 签名 + Authorization 头。
   creds: {:ak ... :sk ... :endpoint ... :bucket ... :region ...}
   opts:
     :method        \"PUT\"
     :key           对象路径(不带 /);形如 \"photos/2026/.../x.jpg\";桶级调用传 nil
     :content-type  MIME(\"image/jpeg\" / \"application/json\" / ...)
     :data          字节 payload
     :query-params  {String-key value-or-values};缺省 {} → canonical-query-string 为空
   返回 {:headers {:x-amz-content-sha256 :x-amz-date :Authorization :Content-Type ...}
          :canonical-uri :string-to-sign :signature :signed-headers}"
  [{:keys [ak sk endpoint bucket region]}
   {:keys [method key content-type data query-params]}]
  (let [[amz-date date-stamp] (now-amz-date)
        host (str endpoint)
        payload-hash (sha256-hex data)
        canonical-uri (if key (str "/" bucket "/" key) (str "/" bucket))
        content-length (str (alength data))
        build-creq canonical-request
        creq-resp (build-creq {:method (or method "PUT")
                               :canonical-uri canonical-uri
                               :query-params (or query-params {})
                               :amz-date amz-date
                               :payload-hash payload-hash
                               :host host
                               :content-length content-length
                               :content-type content-type})
        creq-str (:canonical-request creq-resp)
        signed-headers (:signed-headers creq-resp)
        credential-scope (str date-stamp "/" region "/" service "/aws4_request")
        k-signing (signing-key sk date-stamp region)
        sig (hmac-sha256-hex k-signing (string-to-sign amz-date credential-scope creq-str))
        authorization (str "AWS4-HMAC-SHA256 "
                           "Credential=" ak "/" credential-scope ", "
                           "SignedHeaders=" signed-headers ", "
                           "Signature=" sig)]
    {:headers {"x-amz-content-sha256" payload-hash
               "x-amz-date" amz-date
               "Authorization" authorization
               "Content-Type" content-type}
     :canonical-uri canonical-uri
     :canonical-request creq-str
     :string-to-sign (string-to-sign amz-date credential-scope creq-str)
     :signature sig
     :signed-headers signed-headers}))

;; ----------------------------------------------------------------------------
;; URL / put-object
;; ----------------------------------------------------------------------------

(defn- object-url [{:keys [bucket endpoint]} key]
  (format "https://%s/%s/%s" endpoint bucket key))

(defn- bucket-url [{:keys [endpoint bucket]} query-string]
  (format "https://%s/%s?%s" endpoint bucket query-string))

(defn public-read-policy
  "返回 {Allow 匿名 GetObject / GetObjectVersion} 的 bucket policy 字符串(JSON)。
   后端 PutBucketPolicy 调这个,客户控制台找不着入口时一条命令搞定。

   Resource 用 `arn:aws:s3:::<bucket>/*`(而不是 `zos:hangzhou7:<bucket>/*`)
   —— ctyun 官方文档示例就是 aws ARN 格式,实测 `zos:` 前缀也能存,
   但 aws ARN 更通用(也在跟 AWS S3 SDK 兼容层对话),减少踩坑面。"
  [bucket]
  (json/generate-string
   {"Version" "2012-10-17"
    "Statement" [{"Effect" "Allow"
                  "Principal" {"AWS" "*"}
                  "Action" ["s3:GetObject"]
                  "Resource" [(str "arn:aws:s3:::" bucket "/*")]}]}
   {:pretty true}))

(defn put-bucket-policy!
  "调 ctyun ZOS PutBucketPolicy API 配 bucket policy。
   creds: 同 put-object
   policy-json: 完整 policy 字符串(调 public-read-policy 拿默认)
   返回 {:status 200} 或 {:error ...}"
  [{:keys [endpoint bucket] :as creds} ^String policy-json]
  (when-not (every? seq [(:ak creds) (:sk creds) endpoint bucket (:region creds)])
    (throw (ex-info "ctyun ZOS not configured"
                    {:has-ak? (boolean (seq (:ak creds)))
                     :has-sk? (boolean (seq (:sk creds)))
                     :has-endpoint? (boolean (seq endpoint))
                     :has-bucket? (boolean (seq bucket))
                     :has-region? (boolean (seq (:region creds)))})))
  (let [data (.getBytes policy-json "UTF-8")
        {:keys [headers]} (sign-headers creds
                                        {:method "PUT"
                                         :key nil
                                         :content-type "application/json"
                                         :data data
                                         :query-params {"policy" ""}})
        url (bucket-url creds "policy=")
        resp (http/put url {:headers headers :body data :throw false})]
    (if (= 200 (:status resp))
      {:status 200}
      {:error (str "ctyun PutBucketPolicy failed " (:status resp) ": "
                   (let [b (:body resp)]
                     (cond
                       (nil? b) ""
                       (> (count b) 200) (str (subs b 0 200) "…")
                       :else b)))
       :status (:status resp)
       :body (:body resp)})))

(defn put-object
  "把字节数组写到天翼云对象存储。
   creds: {:ak ... :sk ... :endpoint ... :bucket ... :region ...}
   key: 对象路径(不带前导 /)
   data: byte-array
   content-type: 'image/jpeg' 等
   返回 {:url ... :status 200} 或 {:error ... :status ... :body ...}"
  [{:keys [ak sk endpoint bucket region] :as creds}
   ^String key ^bytes data ^String content-type]
  (when-not (every? seq [ak sk endpoint bucket region])
    (throw (ex-info "ctyun ZOS not configured"
                    {:has-ak? (boolean (seq ak))
                     :has-sk? (boolean (seq sk))
                     :has-endpoint? (boolean (seq endpoint))
                     :has-bucket? (boolean (seq bucket))
                     :has-region? (boolean (seq region))})))
  (let [{:keys [headers]} (sign-headers creds {:key key :content-type content-type :data data})
        url (object-url creds key)
        resp (http/put url {:headers headers :body data :throw false})]
    (if (= 200 (:status resp))
      {:url url :status 200}
      {:error (str "ctyun PUT failed " (:status resp) ": "
                   (let [b (:body resp)]
                     (cond
                       (nil? b) ""
                       (> (count b) 200) (str (subs b 0 200) "…")
                       :else b)))
       :status (:status resp)
       :body (:body resp)})))

;; ----------------------------------------------------------------------------
;; GET —— backend photo-proxy 等场景用
;;
;; 浏览器侧 img.src 直连 + fetch no-cors 都不可靠:
;; - ctyun 防盗链返 200 + Content-Disposition: attachment,浏览器把 img
;;   请求当下载不渲染
;; - 钉钉 WebView 拦 fetch(security/network whitelist),no-cors 抛 TypeError
;; 后端 GET 拿字节 → inline 返回 Content-Type,绕开这两条。
;;
;; **为什么不签 SigV4**:ctyun ZOS 的鉴权逻辑跟 AWS S3 不同 —— 对「带 Authorization 头的
;;   请求」走 AK-IAM 鉴权(查 ak 有没有 s3:GetObject 权限),**不**评估 bucket policy;
;;   对「不带 Authorization 头的请求」走 bucket policy(`Principal: *` 直接命中)。
;;   客户的桶 AK 是 ZOS 控制台建的桶级 AK,没显式 s3:GetObject,所以签名 GET 一律
;;   403 AccessDenied;curl/浏览器 unsigned GET 200 OK + Content-Disposition: attachment
;;   (防盗链)就能下到文件。PUT 必须签名(写操作 ctyun 强制 owner auth)所以
;;   put-object 保持 SigV4 签名不变;只有 GET 走 unsigned path 模仿 curl/browser。
;;
;; **响应里有 Content-Disposition: attachment**:防盗链触发后的副作用,photo-proxy 不会
;;   透传这个头,会显式 set `Content-Type: image/<mime>` 让浏览器 inline 渲染。
;; creds: 同 put-object
;; key: 对象路径(不带前导 /)
;; 返回 {:body bytes :content-type mime :status 200} 或 {:error ... :status ... :body ...}"
(defn get-object
  "从 ctyun ZOS 取对象字节(不签名,见 ns docstring)。"
  [{:keys [endpoint bucket] :as creds} ^String key]
  (when-not (every? seq [(:ak creds) (:sk creds) endpoint bucket (:region creds)])
    (throw (ex-info "ctyun ZOS not configured"
                    {:has-ak? (boolean (seq (:ak creds)))
                     :has-sk? (boolean (seq (:sk creds)))
                     :has-endpoint? (boolean (seq endpoint))
                     :has-bucket? (boolean (seq bucket))
                     :has-region? (boolean (seq (:region creds)))})))
  (let [url (object-url creds key)
        ;; 强制 :as :bytes —— 2026-07-11 现场八踩坑:ctyun 返 image/png,
        ;; babashka.http-client 默认 auto-detect 把 body 返成 String
        ;; (3.4 MB 二进制当 String),导致下游 detect-image-content-type
        ;; 的 (aget data 0) 抛 \"Argument is not an array\"。:as :bytes
        ;; 不依赖 Content-Type 嗅探,直接拿原始字节,稳。
        resp (http/get url {:throw false :as :bytes})]
    (if (= 200 (:status resp))
      {:body (:body resp)
       :content-type (get-in resp [:headers "Content-Type"] "application/octet-stream")
       :status 200}
      {:error (str "ctyun GET failed " (:status resp) ": "
                   (let [b (:body resp)]
                     (cond
                       (nil? b) ""
                       (> (count b) 200) (str (subs b 0 200) "…")
                       :else b)))
       :status (:status resp)
       :body (:body resp)})))
