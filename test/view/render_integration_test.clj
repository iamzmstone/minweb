(ns view.render-integration-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [clojure.string :as str]
            [minweb.view.layout :as l]
            [minweb.view.core :as c]))

(deftest layout-renders-complete-html-test
  (testing "layout produces complete HTML document"
    (let [req {}
          result (l/layout req [:div "content"])]
      (is (string? result))
      (is (str/includes? result "<!DOCTYPE html>"))
      (is (str/includes? result "<html"))
      (is (str/includes? result "</html>"))
      (is (str/includes? result "<head>"))
      (is (str/includes? result "</head>"))
      (is (str/includes? result "<body"))
      (is (str/includes? result "</body>")))))

(deftest layout-includes-static-resources-test
  (testing "layout includes htmx and hyperscript"
    (let [req {}
          result (l/layout req [:div])]
      (is (str/includes? result "/static/js/htmx.min.js"))
      (is (str/includes? result "/static/js/hyperscript.min.js")))))

(deftest layout-includes-tailwind-css-test
  (testing "layout links tailwind css"
    (let [req {}
          result (l/layout req [:div])]
      (is (str/includes? result "/static/css/tw_out.css")))))

(deftest dashboard-layout-renders-complete-html-test
  (testing "dashboard-layout produces complete HTML document"
    (let [req {}
          sidebar-items [{:href "/a" :label "A" :active true}]
          body [:div "content"]
          result (l/dashboard-layout req sidebar-items body)]
      (is (string? result))
      (is (str/includes? result "<!DOCTYPE html>"))
      (is (str/includes? result "<html"))
      (is (str/includes? result "<body"))
      (is (str/includes? result "</body>")))))

(deftest form-input-component-test
  (testing "form-input renders input element"
    (let [result (c/form-input {:type "text" :name "test" :label "Test"})]
      (is (vector? result))
      (is (= :div.my-2 (first result)))))
  (testing "form-input renders textarea when type is textarea"
    (let [result (c/form-input {:type "textarea" :name "test" :label "Test"})]
      (is (vector? result))))
  (testing "form-input renders with error state"
    (let [result (c/form-input {:name "test" :error "Invalid"})]
      (is (vector? result)))))

(deftest page-header-renders-test
  (testing "page-header renders with title"
    (let [result (c/page-header {:title "Dashboard"})]
      (is (vector? result))))
  (testing "page-header renders with user info"
    (let [result (c/page-header {:title "Dashboard" :user {:user/name "John" :user/avatar "/a.png"}})]
      (is (vector? result)))))

(deftest badge-renders-test
  (testing "badge renders with variant"
    (is (vector? (c/badge "text" {:variant :primary})))
    (is (vector? (c/badge "text" {:variant :success})))
    (is (vector? (c/badge "text" {:variant :danger})))))

(deftest card-renders-test
  (testing "card renders with title and content"
    (let [result (c/card {:title "Title" :content "Content"})]
      (is (vector? result))))
  (testing "card renders with footer"
    (let [result (c/card {:title "Title" :content "Content" :footer "Footer"})]
      (is (vector? result)))))

(run-tests)