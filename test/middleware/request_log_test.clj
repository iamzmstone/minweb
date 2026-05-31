(ns middleware.request-log-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [minweb.middleware.request-log :as req-log]))

(deftest generate-request-id-test
  (testing "generates unique UUIDs"
    (let [id1 (req-log/generate-request-id)
          id2 (req-log/generate-request-id)]
      (is (string? id1))
      (is (string? id2))
      (is (not= id1 id2))
      (is (re-matches #"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" id1)))))

(deftest wrap-request-logging-test
  (testing "adds request-id to request"
    (let [handler (fn [_req]
                    {:status 200 :body "ok" :headers {}})
          wrapped (req-log/wrap-request-logging handler)
          req {:request-method :get :uri "/test"}
          resp (wrapped req)]
      (is (string? (get-in resp [:headers "X-Request-Id"])))))

  (testing "preserves existing request-id"
    (let [existing-id "existing-id-123"
          handler (fn [_req]
                    {:status 200 :body "ok" :headers {}})
          wrapped (req-log/wrap-request-logging handler)
          req {:request-method :get :uri "/test" :headers {"X-Request-Id" existing-id}}
          resp (wrapped req)]
      (is (= existing-id (get-in resp [:headers "X-Request-Id"]))))))

(run-tests)