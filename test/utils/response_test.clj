(ns utils.response-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [utils.response :as r]))

(deftest redirect-test
  (testing "creates 303 redirect response"
    (let [resp (r/redirect "/login")]
      (is (= 303 (:status resp)))
      (is (= "/login" (get-in resp [:headers "Location"]))))))

(deftest flash-msg-test
  (testing "adds flash message to response"
    (let [res {:status 200 :body "test"}
          result (r/flash-msg res "success" "Operation completed")]
      (is (= {:message {:severity "success" :message "Operation completed"}}
             (:flash result))))))

(deftest query-params->url-test
  (testing "converts map to URL query string"
    (is (= "a=1&b=2" (r/query-params->url {"a" "1" "b" "2"})))
    (is (= "page=1" (r/query-params->url {"page" "1"})))
    (is (= "" (r/query-params->url {}))))
  (testing "handles edge cases"
    (is (string? (r/query-params->url {"key" "value with spaces"})))))

(run-tests)