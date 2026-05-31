(ns config-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.common :refer [env]]))

(deftest env-config-test
  (testing "env returns value by key"
    (is (some? (env :app-name))))
  (testing "env returns nil for unknown key"
    (is (nil? (env :nonexistent-key))))
  (testing "env defaults are loaded"
    (is (string? (env :app-name)))))

(deftest env-override-test
  (testing "env supports MINWEB_ prefix override"
    (let [test-env (merge env {:minweb-test-key "test-value"})]
      (is (= "test-value" (get test-env :minweb-test-key))))))
