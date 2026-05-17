(ns config-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [common :refer [env]]))

(deftest env-config-test
  (testing "env returns value by key"
    (is (some? (env :app-name))))
  (testing "env returns nil for unknown key"
    (is (nil? (env :nonexistent-key))))
  (testing "env defaults are loaded"
    (is (string? (env :app-name)))))

(run-tests)