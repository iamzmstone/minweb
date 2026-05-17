(ns view.components-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [view.core :as c]))

(deftest merge-classes-test
  (testing "joins non-blank classes"
    (is (= "foo bar" (c/merge-classes "foo" "bar")))
    (is (= "foo bar" (c/merge-classes "foo" "" "bar")))
    (is (= "foo" (c/merge-classes "foo" "")))
    (is (= "" (c/merge-classes "" ""))))
  (testing "filters blank strings"
    (is (= "a b" (c/merge-classes "a" nil "b")))
    (is (= "a" (c/merge-classes "a" false)))))

(run-tests)