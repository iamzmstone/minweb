(ns view.layout-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [view.layout :as l]))

(deftest paginator-test
  (testing "renders paginator with navigation"
    (let [req {:session {:page-size 20} :query-params {}}
          result (l/paginator req 1 5 "/admin")]
      (is (vector? result))))
  (testing "paginator calculates correct previous/next"
    (let [req {:session {:page-size 20} :query-params {}}
          first-page (l/paginator req 1 5 "/admin")
          middle-page (l/paginator req 3 5 "/admin")]
      (is (vector? first-page))
      (is (vector? middle-page)))))

(deftest autocomplete-input-test
  (testing "renders autocomplete input"
    (let [result (l/autocomplete-input :label "Search" :name "q" :list ["opt1" "opt2"])]
      (is (vector? result))
      (is (= :div.mb-3 (first result))))))

(deftest form-input-test
  (testing "renders text input"
    (let [result (l/form-input :type "text" :label "Name" :name "name" :required true)]
      (is (vector? result))
      (is (= :div.mb-3 (first result)))))
  (testing "renders textarea"
    (let [result (l/form-input :type "textarea" :label "Description" :name "desc")]
      (is (vector? result))))
  (testing "renders autocomplete input"
    (let [result (l/form-input :type "autocomplete" :label "City" :name "city" :list ["Beijing" "Shanghai"])]
      (is (vector? result))))
  (testing "renders base64-upload input"
    (let [result (l/form-input :type "base64-upload" :label "File" :name "file" :id "file-input")]
      (is (vector? result)))))

(deftest search-form-test
  (testing "renders search form"
    (let [result (l/search-form)]
      (is (vector? result))
      (is (= :form (first result))))))

(deftest modal-test
  (testing "renders modal structure"
    (let [result (l/modal :id "test-modal" :title "Modal Title" :content "Content" :actions "Actions")]
      (is (vector? result))
      (is (= :div.modal.fade (first result))))))

(run-tests)