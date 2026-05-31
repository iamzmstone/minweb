(ns middleware.error-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.string :as str]
   [minweb.middleware.error :as error]
   [minweb.view.layout :refer [error-page]]))

(deftest error-page-test
  (testing "error-page generates HTML with status and message"
    (let [result (error-page 404 "Not Found" "Page not found")]
      (is (string? result))
      (is (str/includes? result "404"))
      (is (str/includes? result "Not Found"))
      (is (str/includes? result "Page not found"))
      (is (str/includes? result "<!DOCTYPE html>")))))

(deftest not-found-page-test
  (testing "not-found-page returns 404 styled page"
    (let [result (error/not-found-page)]
      (is (string? result))
      (is (str/includes? result "404")))))

(deftest server-error-page-test
  (testing "server-error-page returns 500 styled page"
    (let [result (error/server-error-page)]
      (is (string? result))
      (is (str/includes? result "500")))))

(deftest wrap-not-found-test
  (testing "returns not-found page for nil response"
    (let [handler (fn [_] nil)
          wrapped (error/wrap-not-found handler)
          resp (wrapped {:uri "/unknown"})]
      (is (= 404 (:status resp)))
      (is (str/includes? (:body resp) "404"))))

  (testing "passes through non-nil responses"
    (let [handler (fn [_] {:status 200 :body "ok"})
          wrapped (error/wrap-not-found handler)
          resp (wrapped {:uri "/known"})]
      (is (= 200 (:status resp)))
      (is (= "ok" (:body resp))))))

(deftest wrap-error-handler-test
  (testing "catches exception and returns 500 page"
    (let [handler (fn [_] (throw (ex-info "test" {})))
          wrapped (error/wrap-error-handler handler)
          resp (wrapped {})]
      (is (= 500 (:status resp)))
      (is (str/includes? (:body resp) "500"))))

  (testing "passes through normal responses"
    (let [handler (fn [_] {:status 200 :body "ok"})
          wrapped (error/wrap-error-handler handler)
          resp (wrapped {})]
      (is (= 200 (:status resp)))
      (is (= "ok" (:body resp))))))