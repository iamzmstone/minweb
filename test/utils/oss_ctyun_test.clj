(ns utils.oss-ctyun-test
  "minweb.utils.oss-ctyun 单测 —— 覆盖 path-style URL、V4 签名、Host header 不带 bucket。
   纯函数测试,凭证直接传 creds map,无 Datalevin/cprop 依赖。"
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]
   [cheshire.core :as json]
   [babashka.http-client :as http]
   [minweb.utils.oss-ctyun :as oss-ctyun])
  (:import
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.security MessageDigest]))

(def ^:private creds
  {:ak "AKID-TEST"
   :sk "SECRET-TEST"
   :endpoint "hangzhou7.zos.ctyun.cn"
   :bucket "insp-task-photos"
   :region "hangzhou7"})

;; ============================================================================
;; stub http/put; capture the (url opts) seen by the provider
;; ============================================================================

(defn- with-captured-put [body-fn]
  (let [out (atom nil)]
    (with-redefs [http/put (fn [u opts]
                             (reset! out {:url u :opts opts})
                             {:status 200 :body ""})]
      (body-fn out))
    out))

;; ============================================================================
;; V4 签名重算
;; ============================================================================

(defn- sha256-hex [^bytes data]
  (let [md (MessageDigest/getInstance "SHA-256")
        digest (.digest md data)]
    (format "%064x" (BigInteger. 1 digest))))

(defn- hmac-sha256 [^bytes key ^String data]
  (let [mac (Mac/getInstance "HmacSHA256")
        spec (SecretKeySpec. key "HmacSHA256")
        _ (.init mac spec)]
    (.doFinal mac (.getBytes data "UTF-8"))))

(defn- v4-sign [{:keys [secret region date-stamp amz-date canonical-request]}]
  (let [service "s3"
        k-signing (reduce (fn [k x] (hmac-sha256 k x))
                          (.getBytes (str "AWS4" secret) "UTF-8")
                          [date-stamp region service "aws4_request"])
        sts (str/join "\n"
                      ["AWS4-HMAC-SHA256"
                       amz-date
                       (str date-stamp "/" region "/" service "/aws4_request")
                       (sha256-hex (.getBytes canonical-request "UTF-8"))])]
    (format "%064x" (BigInteger. 1 (hmac-sha256 k-signing sts)))))

(defn- expected-v4-sig
  "按捕获到的客户端 header + data + key 重新拼 canonical request,算 V4 签名。
   注:
   - Content-Length 是 JDK HttpURLConnection restricted header,实际 HTTP 请求不带,
     但参与签名计算,所以从 data 长度算,不读 headers。
   - Host 也是 JDK restricted header,不在 wire,但参与签名,这里从 creds :endpoint 取。"
  [headers data key]
  (let [host (:endpoint creds)
        content-type (get headers "Content-Type")
        amz-date (get headers "x-amz-date")
        payload-hash (get headers "x-amz-content-sha256")
        content-length (str (alength data))
        canonical-uri (str "/" (:bucket creds) "/" key)
        headers-block (str "content-length:" content-length "\n"
                           "content-type:" content-type "\n"
                           "host:" host "\n"
                           "x-amz-content-sha256:" payload-hash "\n"
                           "x-amz-date:" amz-date "\n")
        date-stamp (subs amz-date 0 8)
        canonical-request (str/join "\n"
                                    ["PUT" canonical-uri ""
                                     headers-block
                                     "content-length;content-type;host;x-amz-content-sha256;x-amz-date"
                                     payload-hash])]
    (v4-sign {:secret (:sk creds)
              :region (:region creds)
              :date-stamp date-stamp
              :amz-date amz-date
              :canonical-request canonical-request})))

;; ============================================================================
;; URL / Host / ACL
;; ============================================================================

(deftest put-object-path-style-url
  (testing "URL 是 path-style 格式 https://<endpoint>/<bucket>/<key>"
    (let [data (.getBytes "hello world" "UTF-8")]
      (with-captured-put
        (fn [cap]
          (let [result (oss-ctyun/put-object creds "2026/07/08/photo.jpg" data "image/jpeg")]
            (is (= 200 (:status result)))
            (is (= "https://hangzhou7.zos.ctyun.cn/insp-task-photos/2026/07/08/photo.jpg"
                   (:url result)))
            (is (not (str/includes? (:url @cap) "insp-task-photos.zos"))
                "不是 virtual-hosted")))))))

(deftest put-object-host-header-not-explicitly-set
  (testing "不发 'Host' 到 :headers(JDK HttpURLConnection reserved,11+)
           —— path-style URL 让 JDK 从 URL 自动算 Host = endpoint"
    (with-captured-put
      (fn [cap]
        (oss-ctyun/put-object creds "x.jpg" (.getBytes "x" "UTF-8") "image/jpeg")
        (is (not (contains? (:opts @cap) "Host"))
            "显式设 Host 会触发 JDK restricted-header 异常")
        (testing "URL 是 path-style,JDK 会从 URL 算出 Host: <endpoint>"
          (is (str/includes? (:url @cap)
                             (str (:endpoint creds) "/" (:bucket creds)))
              "URL 形如 https://<endpoint>/<bucket>/...,JDK 抽 Host = endpoint"))))))

(deftest put-object-no-per-object-acl-header
  (testing "ZOS 不支持 x-amz-acl per-object header —— public-read 走 bucket policy"
    (with-captured-put
      (fn [cap]
        (oss-ctyun/put-object creds "x.jpg" (.getBytes "x" "UTF-8") "image/jpeg")
        (is (not (contains? (:opts @cap) "x-amz-acl")))))))

;; ============================================================================
;; V4 签名回归
;; ============================================================================

(deftest put-object-signature-matches-recomputed
  (let [data (.getBytes "hello world" "UTF-8")]
    (with-captured-put
      (fn [cap]
        (oss-ctyun/put-object creds "test/key.txt" data "text/plain")
        (let [headers (get-in @cap [:opts :headers])
              auth (get headers "Authorization")]
          (testing "Authorization 头格式 AWS4-HMAC-SHA256 ... Credential=..."
            (is (str/starts-with? auth "AWS4-HMAC-SHA256 "))
            (is (str/includes? auth "Credential=AKID-TEST/"))
            (is (str/includes? auth "SignedHeaders="))
            (is (str/includes? auth "Signature=")))
          (testing "SignedHeaders 不包含 x-amz-acl(走 bucket policy)"
            (let [[_ sh] (str/split auth #"SignedHeaders=")]
              (is (str/includes? sh "content-length"))
              (is (str/includes? sh "content-type"))
              (is (str/includes? sh "host"))
              (is (str/includes? sh "x-amz-content-sha256"))
              (is (str/includes? sh "x-amz-date"))
              (is (not (str/includes? sh "x-amz-acl")))))
          (testing "Signature = 用客户端发出的 header 算出的 V4 sig"
            (let [[_ sig-part] (str/split auth #"Signature=")
                  expected (expected-v4-sig headers data "test/key.txt")]
              (is (= expected sig-part)
                  (str "expected=" expected " actual=" sig-part)))))))))

;; ============================================================================
;; Error path
;; ============================================================================

(deftest put-object-error-path-returns-error-map
  (testing "非 200 响应返回 :error / :status / :body,不抛异常"
    (with-redefs [http/put (fn [_ _]
                             {:status 403
                              :body "<?xml ... SignatureDoesNotMatch"})]
      (let [r (oss-ctyun/put-object creds "x.txt" (.getBytes "x" "UTF-8") "text/plain")]
        (is (= 403 (:status r)))
        (is (str/includes? (:error r) "ctyun PUT failed 403"))
        (is (str/includes? (:body r) "SignatureDoesNotMatch"))))))

;; ============================================================================
;; 凭证不全
;; ============================================================================

(deftest put-object-missing-creds-throws
  (let [base {:ak "A" :sk "S" :endpoint "e.com" :bucket "b" :region "r"}
        cases [{:ak "" :sk "S" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "b" :region ""}]]
    (testing "任一字段缺失都抛 ex-info"
      (doseq [c cases]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"ctyun ZOS not configured"
             (oss-ctyun/put-object c "k" (.getBytes "x" "UTF-8") "text/plain"))
            (str "should throw with creds=" c))))
    (testing "完整凭证应至少到达 http/put"
      (with-redefs [http/put (fn [_ _] {:status 200 :body ""})]
        (is (some? (oss-ctyun/put-object base "k" (.getBytes "x" "UTF-8") "text/plain")))))))

;; ============================================================================
;; GET / get-object —— backend photo-proxy 等场景用
;;
;; 不签 SigV4 —— ctyun ZOS 鉴权逻辑:签名走 AK-IAM(查 s3:GetObject 权限),
;; 不签名走 bucket policy(Principal: * 命中)。客户桶 AK 没 s3:GetObject
;; 所以 get-object 不签名,直接 http/get 模仿 curl/browser。
;; ============================================================================

(defn- with-captured-get [body-fn]
  (let [out (atom nil)]
    (with-redefs [http/get (fn [u opts]
                             (reset! out {:url u :opts opts})
                             {:status 200
                              :body (.getBytes "fake-image-bytes" "UTF-8")
                              :headers {"Content-Type" "image/jpeg"
                                        "Content-Disposition" "attachment"}})]
      (body-fn out))
    out))

(deftest get-object-path-style-url
  (let [cap (with-captured-get
              (fn [_]
                (oss-ctyun/get-object creds "2026/07/11/photo.jpg")))]
    (is (= "https://hangzhou7.zos.ctyun.cn/insp-task-photos/2026/07/11/photo.jpg"
           (:url @cap)))))

(deftest get-object-does-not-send-auth-headers
  (testing "不签 SigV4:不发 Authorization / x-amz-date / x-amz-content-sha256"
    (let [cap (with-captured-get
                (fn [_]
                  (oss-ctyun/get-object creds "k.jpg")))
          headers (get-in @cap [:opts :headers])]
      (is (not (contains? headers "Authorization"))
          "unsigned → 不发 Authorization → ctyun 走 bucket policy")
      (is (not (contains? headers "x-amz-date")))
      (is (not (contains? headers "x-amz-content-sha256"))))))

(deftest get-object-forces-bytes-not-string
  (testing ":as :bytes 强制 body 返 byte[] —— 2026-07-11 现场八踩坑:
   ctyun 返 image/png,babashka.http-client 默认 auto-detect 把 body 当 String
   返(3.4 MB 二进制当 String),下游 detect-image-content-type 的 (aget data 0)
   抛 'Argument is not an array'。:as :bytes 不依赖 Content-Type 嗅探,稳。
   这条防止以后有人去掉 :as :bytes —— 删了产线立即爆。"
    (let [cap (with-captured-get
                (fn [_]
                  (oss-ctyun/get-object creds "k.jpg")))]
      (is (= :bytes (get-in @cap [:opts :as]))
          "opts 必须含 :as :bytes,否则 babashka.http-client 默认返 String"))))

(deftest get-object-error-path-returns-error-map
  (with-redefs [http/get (fn [_ _]
                           {:status 404
                            :body "<?xml ... NoSuchKey"})]
    (let [r (oss-ctyun/get-object creds "missing.jpg")]
      (is (= 404 (:status r)))
      (is (str/includes? (:error r) "ctyun GET failed 404"))
      (is (str/includes? (:body r) "NoSuchKey")))))

(deftest get-object-missing-creds-throws
  (let [cases [{:ak "" :sk "S" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "b" :region ""}]]
    (doseq [c cases]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"ctyun ZOS not configured"
           (oss-ctyun/get-object c "k"))
          (str "should throw with creds=" c)))))

;; ============================================================================
;; put-bucket-policy —— 给 ctyun ZOS 配 public-read 桶策略
;; ============================================================================

(deftest public-read-policy-json
  (testing "返回的 JSON 含 s3:GetObject + Principal:* + Resource:<bucket>/*"
    (let [j (oss-ctyun/public-read-policy "test-bucket")
          parsed (json/parse-string j true)]
      (is (= "2012-10-17" (:Version parsed)))
      (is (= "Allow" (-> parsed :Statement first :Effect)))
      (is (= ["s3:GetObject"] (-> parsed :Statement first :Action)))
      (is (= "*" (-> parsed :Statement first :Principal :AWS)))
      (is (= ["arn:aws:s3:::test-bucket/*"]
             (-> parsed :Statement first :Resource))))))

(deftest put-bucket-policy-signs-with-query-string
  (testing "PUT 走 ?policy= query,签名 canonical-query-string 非空"
    (let [cap (with-captured-put
                (fn [_]
                  (oss-ctyun/put-bucket-policy! creds "{}")))]
      (testing "URL 形如 https://<endpoint>/<bucket>?policy="
        (is (str/includes? (:url @cap) "?policy=")))
      (testing "Content-Type = application/json"
        (is (= "application/json"
               (get-in @cap [:opts :headers "Content-Type"]))))
      (testing "body 透传"
        (is (= "{}" (String. ^bytes (:body (:opts @cap)) "UTF-8")))))))

(deftest put-bucket-policy-missing-creds-throws
  (let [cases [{:ak "" :sk "S" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "" :endpoint "e.com" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "" :bucket "b" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "" :region "r"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket "b" :region ""}]]
    (doseq [c cases]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"ctyun ZOS not configured"
           (oss-ctyun/put-bucket-policy! c "{}"))
          (str "should throw with creds=" c)))))
