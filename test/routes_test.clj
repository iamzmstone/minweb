(ns routes-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [routes :as r]))

(deftest route-test
  (testing "creates route map with path and method"
    (let [handler (fn [req] {:body "ok"})
          route (r/route "/test" :get handler)]
      (is (= "/test" (:path route)))
      (is (= :get (:method route)))
      (is (fn? (:response route))))))

(deftest http-methods-test
  (testing "creates routes for different HTTP methods"
    (let [handler (fn [req] {:body "ok"})]
      (is (= :get (:method (r/http-get "/path" handler))))
      (is (= :post (:method (r/http-post "/path" handler))))
      (is (= :put (:method (r/http-put "/path" handler))))
      (is (= :delete (:method (r/http-delete "/path" handler))))
      (is (= :option (:method (r/http-option "/path" handler)))))))

(deftest routes-test
  (testing "routes is a function that returns a router"
    (is (fn? r/routes))))

(run-tests)