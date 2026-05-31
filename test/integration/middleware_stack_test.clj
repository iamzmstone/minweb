(ns integration.middleware-stack-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.middleware.security :as security]
   [minweb.utils.response :as resp]))

(defn chain-handler
  "Creates a handler with security headers applied"
  [handler]
  (-> handler
      security/wrap-security-headers))

(deftest full-stack-security-headers-test
  (testing "security headers present after middleware chain"
    (let [app-handler
          (fn [_req]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body "{}"})
          chained (chain-handler app-handler)
          resp (chained {:uri "/api" :request-method :get})]
      (is (= 200 (:status resp)))
      (is (map? (:headers resp)))
      (is (contains? (:headers resp) "X-Frame-Options"))
      (is (= "DENY" (get (:headers resp) "X-Frame-Options")))
      (is (contains? (:headers resp) "Content-Security-Policy"))
      (is (contains? (:headers resp) "Content-Type"))
      (is (= "application/json"
             (get (:headers resp) "Content-Type"))))))

(deftest full-stack-redirect-test
  (testing "redirect responses work with security headers"
    (let [app-handler (fn [_req]
                        (resp/redirect "/login"))
          chained (chain-handler app-handler)
          resp (chained {:uri "/" :request-method :get})]
      (is (map? resp))
      (is (contains? (:headers resp) "X-Frame-Options"))
      (is (= "DENY" (get (:headers resp) "X-Frame-Options"))))))

(deftest security-headers-merge-with-existing
  (testing "security headers are added to existing headers"
    (let [app-handler (fn [_req]
                        {:status 200
                         :headers {"Custom-Header" "value"}
                         :body "ok"})
          chained (chain-handler app-handler)
          resp (chained {})]
      (is (contains? (:headers resp) "X-Frame-Options"))
      (is (contains? (:headers resp) "Custom-Header"))
      (is (= "value" (get (:headers resp) "Custom-Header"))))))