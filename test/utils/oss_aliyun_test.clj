(ns utils.oss-aliyun-test
  "minweb.utils.oss-aliyun 单测 —— 覆盖签名 + virtual-hosted URL + PUT/GET 行为。
   纯函数测试,凭证直接传 creds map,无 Datalevin/cprop 依赖。"
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]
   [babashka.http-client :as http]
   [minweb.utils.oss-aliyun :as oss-aliyun])
  (:import
   [javax.crypto Mac]
   [javax.crypto.spec SecretKeySpec]
   [java.util Base64]))

(def ^:private creds
  {:ak "AKID-TEST"
   :sk "SECRET-TEST"
   :endpoint "oss-cn-shanghai.aliyuncs.com"
   :bucket "test-bucket"})

;; ============================================================================
;; put-object 行为:virtual-hosted URL、headers、签名格式
;; ============================================================================

(deftest put-object-virtual-hosted-url
  (testing "URL 是 virtual-hosted 格式 https://<bucket>.<endpoint>/<key>"
    (with-redefs [http/put (fn [_ _] {:status 200 :body ""})]
      (let [data (.getBytes "hello world" "UTF-8")
            result (oss-aliyun/put-object creds "test/key.txt" data "text/plain")]
        (is (= 200 (:status result)))
        (is (= "https://test-bucket.oss-cn-shanghai.aliyuncs.com/test/key.txt"
               (:url result)))))))

(deftest put-object-sends-required-headers
  (testing "Content-Type + Date + Authorization 都在 :headers 里"
    (let [captured (atom nil)]
      (with-redefs [http/put (fn [_ opts]
                               (reset! captured {:opts opts})
                               {:status 200 :body ""})]
        (oss-aliyun/put-object creds "test/key.txt" (.getBytes "hello" "UTF-8") "text/plain")
        (let [headers (:headers (:opts @captured))]
          (testing "Content-Length 不手动设(JDK HttpURLConnection restricted)"
            (is (not (contains? headers "Content-Length"))))
          (testing "Content-Type 走 :headers(不能用 :content-type option)"
            (is (= "text/plain" (get headers "Content-Type"))))
          (testing "Authorization = OSS <ak>:<sig>"
            (is (str/starts-with? (get headers "Authorization") "OSS AKID-TEST:")))
          (testing "Date 走 GMT 格式"
            (let [d (get headers "Date")]
              (is (string? d))
              (is (str/includes? d "GMT"))))
          (testing "x-oss-object-acl:public-read 让 GET 不需要签名"
            (is (= "public-read" (get headers "x-oss-object-acl")))))))))

;; ============================================================================
;; 签名重算回归(regression for 2026-06-22 OSS 返 403 SignatureDoesNotMatch)
;; 不调 sign-headers,直接按 OSS 官方文档拼 StringToSign + HMAC-SHA1,
;; 跟 put-object 算出来的对一遍 —— 改 oss_aliyun.clj 时 StringToSign 格式错就挂。
;; ============================================================================

(deftest put-object-signature-matches-recomputed
  (let [captured (atom nil)]
    (with-redefs [http/put (fn [_ opts]
                             (reset! captured {:opts opts})
                             {:status 200 :body ""})]
      (oss-aliyun/put-object creds "test/key.txt" (.getBytes "hello world" "UTF-8") "text/plain")
      (let [headers (:headers (:opts @captured))
            auth (get headers "Authorization")
            date (get headers "Date")
            [prefix sig] (str/split auth #":" 2)
            ;; OSS 规范 StringToSign:
            ;;   HTTP-Verb\nContent-MD5\nContent-Type\nDate\n
            ;;   CanonicalizedOSSHeaders\nCanonicalizedResource
            expected-sts (str/join "\n"
                                   ["PUT"
                                    ""
                                    "text/plain"
                                    date
                                    "x-oss-object-acl:public-read"
                                    "/test-bucket/test/key.txt"])
            mac (Mac/getInstance "HmacSHA1")
            _ (.init mac (SecretKeySpec.
                          (.getBytes "SECRET-TEST" "UTF-8") "HmacSHA1"))
            digest (.doFinal mac (.getBytes expected-sts "UTF-8"))
            expected-sig (.encodeToString (Base64/getEncoder) digest)]
        (testing "Authorization 头格式 OSS <ak>:<sig>"
          (is (= "OSS AKID-TEST" prefix)))
        (testing "签名 = Base64(HmacSHA1(sk, sts))"
          (is (= expected-sig sig)
              (str "expected=" expected-sig " actual=" sig)))))))

;; ============================================================================
;; Error path:404 / 403 把响应包成 {:error ... :status ...}
;; ============================================================================

(deftest put-object-error-path-returns-error-map
  (testing "非 200 响应返回 :error / :status / :body,不抛异常"
    (with-redefs [http/put (fn [_ _]
                             {:status 403
                              :body "<?xml version... SignatureDoesNotMatch"})]
      (let [r (oss-aliyun/put-object creds "x.txt" (.getBytes "x" "UTF-8") "text/plain")]
        (is (= 403 (:status r)))
        (is (str/includes? (:error r) "403"))
        (is (str/includes? (:body r) "SignatureDoesNotMatch"))))))

;; ============================================================================
;; 凭证不全 → 抛 ex-info,不静默
;; ============================================================================

(deftest put-object-missing-creds-throws
  (testing "ak/sk/endpoint/bucket 任一缺失都抛 ex-info(不静默打网络)"
    (let [cases [{:ak "" :sk "S" :endpoint "e.com" :bucket "b"}
                 {:ak "A" :sk "" :endpoint "e.com" :bucket "b"}
                 {:ak "A" :sk "S" :endpoint "" :bucket "b"}
                 {:ak "A" :sk "S" :endpoint "e.com" :bucket ""}]]
      (doseq [c cases]
        (is (thrown-with-msg?
             clojure.lang.ExceptionInfo
             #"Aliyun OSS not configured"
             (oss-aliyun/put-object c "k" (.getBytes "x" "UTF-8") "text/plain"))
            (str "should throw with creds=" c))))))

;; ============================================================================
;; GET / get-object —— backend photo-proxy 等场景用
;;
;; Aliyun GET StringToSign:VERB + \n + Content-MD5 + \n + Content-Type + \n
;;                        + Date + \n + CanonicalizedOSSHeaders + \n + CanonicalizedResource
;; GET: 三个空字段(MD5 / Type / CanonicalizedOSSHeaders),Date + Resource 拼。
;; 不发 x-oss-object-acl(PUT 才需要)。
;; ============================================================================

(defn- with-captured-get [body-fn]
  (let [out (atom nil)]
    (with-redefs [http/get (fn [u opts]
                             (reset! out {:url u :opts opts})
                             {:status 200
                              :body (.getBytes "fake-bytes" "UTF-8")
                              :headers {"Content-Type" "image/png"}})]
      (body-fn out))
    out))

(deftest get-object-virtual-hosted-url
  (testing "URL 是 virtual-hosted https://<bucket>.<endpoint>/<key>"
    (with-captured-get
      (fn [cap]
        (let [result (oss-aliyun/get-object creds "photos/2026/x.jpg")]
          (is (= 200 (:status result)))
          (is (= "image/png" (:content-type result)))
          (is (= "https://test-bucket.oss-cn-shanghai.aliyuncs.com/photos/2026/x.jpg"
                 (:url @cap))))))))

(deftest get-object-sends-required-headers
  (testing "Date + Authorization;不发 x-oss-object-acl(PUT 才需要)"
    (with-captured-get
      (fn [cap]
        (oss-aliyun/get-object creds "k.txt")
        (let [headers (:headers (:opts @cap))]
          (testing "Date 是 GMT 格式"
            (is (str/includes? (get headers "Date") "GMT")))
          (testing "Authorization = OSS <ak>:<sig>"
            (is (str/starts-with? (get headers "Authorization") "OSS AKID-TEST:")))
          (testing "x-oss-object-acl 不发"
            (is (not (contains? headers "x-oss-object-acl")))))))))

(deftest get-object-signature-matches-recomputed
  (testing "GET 签名:StringToSign 三空(MD5/Type/CanonicalizedHeaders)+ Date + Resource"
    (let [captured (atom nil)]
      (with-redefs [http/get (fn [_ opts]
                               (reset! captured {:opts opts})
                               {:status 200 :body (.getBytes "x" "UTF-8")})]
        (oss-aliyun/get-object creds "photos/x.txt")
        (let [headers (:headers (:opts @captured))
              auth (get headers "Authorization")
              date (get headers "Date")
              [_ sig] (str/split auth #":" 2)
              expected-sts (str/join "\n"
                                     ["GET" "" "" date "" "/test-bucket/photos/x.txt"])
              mac (Mac/getInstance "HmacSHA1")
              _ (.init mac (SecretKeySpec.
                            (.getBytes "SECRET-TEST" "UTF-8") "HmacSHA1"))
              digest (.doFinal mac (.getBytes expected-sts "UTF-8"))
              expected-sig (.encodeToString (Base64/getEncoder) digest)]
          (is (= expected-sig sig)
              (str "expected=" expected-sig " actual=" sig)))))))

(deftest get-object-error-path-returns-error-map
  (testing "非 200 返 {:error ... :status ... :body ...} 不抛"
    (with-redefs [http/get (fn [_ _]
                             {:status 403
                              :body "<?xml ... SignatureDoesNotMatch"})]
      (let [r (oss-aliyun/get-object creds "x.jpg")]
        (is (= 403 (:status r)))
        (is (str/includes? (:error r) "Aliyun GET failed 403"))
        (is (str/includes? (:body r) "SignatureDoesNotMatch"))))))

(deftest get-object-forces-bytes-not-string
  (testing ":as :bytes 强制 body 返 byte[] —— 同 ctyun 现场八踩坑:
   babashka.http-client 默认 auto-detect 对 image/png 响应把 body 当
   String(3.4MB 二进制当 String),下游 byte 操作会挂。:as :bytes 不依赖
   Content-Type 嗅探,直接拿原始字节,稳。这条防止以后有人去掉 :as :bytes。"
    (let [captured (atom nil)]
      (with-redefs [http/get (fn [_ opts]
                               (reset! captured {:opts opts})
                               {:status 200 :body (.getBytes "x" "UTF-8")})]
        (oss-aliyun/get-object creds "k.jpg")
        (is (= :bytes (get-in @captured [:opts :as]))
            "opts 必须含 :as :bytes,否则 babashka.http-client 默认返 String")))))

(deftest get-object-missing-creds-throws
  (let [cases [{:ak "" :sk "S" :endpoint "e.com" :bucket "b"}
               {:ak "A" :sk "" :endpoint "e.com" :bucket "b"}
               {:ak "A" :sk "S" :endpoint "" :bucket "b"}
               {:ak "A" :sk "S" :endpoint "e.com" :bucket ""}]]
    (doseq [c cases]
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo
           #"Aliyun OSS not configured"
           (oss-aliyun/get-object c "k"))
          (str "should throw with creds=" c)))))
