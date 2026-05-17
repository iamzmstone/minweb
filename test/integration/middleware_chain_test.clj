(ns integration.middleware-chain-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [middleware.security :as security]))

(deftest security-headers-test
  (testing "adds security headers to response"
    (let [handler (fn [_req] {:status 200 :body "ok" :headers {}})
          wrapped (security/wrap-security-headers handler)
          resp (wrapped {})]
      (is (map? resp))
      (is (map? (:headers resp)))
      (is (contains? (:headers resp) "X-Frame-Options"))
      (is (= "DENY" (get (:headers resp) "X-Frame-Options")))
      (is (= "nosniff" (get (:headers resp) "X-Content-Type-Options")))
      (is (= "no-cache, no-store, must-revalidate" (get (:headers resp) "Cache-Control"))))))

(deftest security-headers-preserves-existing-headers
  (testing "preserves existing Content-Type header"
    (let [handler (fn [_req] {:status 200 :body "html" :headers {"Content-Type" "text/html"}})
          wrapped (security/wrap-security-headers handler)
          resp (wrapped {})]
      (is (= "text/html" (get (:headers resp) "Content-Type")))
      (is (= "DENY" (get (:headers resp) "X-Frame-Options"))))))

(deftest security-headers-handles-missing-headers
  (testing "handles response with no headers key"
    (let [handler (fn [_req] {:status 200 :body "ok"})
          wrapped (security/wrap-security-headers handler)
          resp (wrapped {})]
      (is (map? resp))
      (is (map? (:headers resp)))
      (is (contains? (:headers resp) "X-Frame-Options"))))

  (testing "handles nil headers"
    (let [handler (fn [_req] {:status 200 :body "ok" :headers nil})
          wrapped (security/wrap-security-headers handler)
          resp (wrapped {})]
      (is (map? resp))
      (is (map? (:headers resp)))
      (is (contains? (:headers resp) "X-Frame-Options")))))

(run-tests)