(ns integration.routes-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [minweb.routes :as r]))

(deftest route-response-string-body-test
  (testing "returns proper response map with headers for string body"
    (let [handler (fn [_req] "<html></html>")
          route (r/route "/test" :get handler)
          response-fn (:response route)
          resp (response-fn {})]
      (is (map? resp))
      (is (= 200 (:status resp)))
      (is (string? (:body resp)))
      (is (map? (:headers resp)))
      (is (contains? (:headers resp) "Content-Type"))
      (is (= "text/html; charset=utf-8" (get (:headers resp) "Content-Type"))))))

(deftest route-response-map-body-test
  (testing "preserves existing response map with headers"
    (let [handler (fn [_req] {:status 201 :body "json" :headers {"Content-Type" "application/json"}})
          route (r/route "/api" :post handler)
          response-fn (:response route)
          resp (response-fn {})]
      (is (map? resp))
      (is (= 201 (:status resp)))
      (is (= "json" (:body resp)))
      (is (= "application/json" (get (:headers resp) "Content-Type"))))))

(deftest http-methods-return-correct-routes
  (testing "http-get creates route with GET method"
    (let [route (r/http-get "/test" (fn [_req] "body"))]
      (is (= :get (:method route)))
      (is (fn? (:response route)))))

  (testing "http-post creates route with POST method"
    (let [route (r/http-post "/test" (fn [_req] "body"))]
      (is (= :post (:method route)))
      (is (fn? (:response route)))))

  (testing "http-put creates route with PUT method"
    (let [route (r/http-put "/test" (fn [_req] "body"))]
      (is (= :put (:method route)))
      (is (fn? (:response route)))))

  (testing "http-delete creates route with DELETE method"
    (let [route (r/http-delete "/test" (fn [_req] "body"))]
      (is (= :delete (:method route)))
      (is (fn? (:response route))))))

(run-tests)