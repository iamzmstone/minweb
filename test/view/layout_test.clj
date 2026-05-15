(ns view.layout-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [view.layout :as l]))

(deftest modal-test
  (testing "renders modal structure"
    (let [result (l/modal :id "test" :title "Title" :content "Content" :actions "Actions")]
      (is (vector? result)))))

(run-tests)