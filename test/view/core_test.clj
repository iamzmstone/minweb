(ns view.core-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [view.core :as c]))

(deftest badge-test
  (testing "renders badge with correct variant and size"
    (is (vector? (c/badge "text" {:variant :primary})))
    (is (vector? (c/badge "text" {:variant :success})))
    (is (vector? (c/badge "text" {:variant :warning})))
    (is (vector? (c/badge "text" {:variant :danger})))
    (is (vector? (c/badge "text" {:variant :info})))
    (is (vector? (c/badge "text" {:variant :secondary})))
    (is (vector? (c/badge "text" {:size :xs})))
    (is (vector? (c/badge "text" {:size :sm})))
    (is (vector? (c/badge "text" {:size :md})))))

(deftest callout-test
  (testing "renders callout with severity-based styling"
    (is (vector? (c/callout :info "Tip" "Content")))
    (is (vector? (c/callout :success "Success" "Content")))
    (is (vector? (c/callout :warning "Warning" "Content")))
    (is (vector? (c/callout :danger "Danger" "Content")))))

(deftest showbox-test
  (testing "renders showbox component"
    (is (vector? (c/showbox "Subject" "bg-red-500" nil "Remark")))
    (is (vector? (c/showbox "Subject" "bg-blue-500" {:href "/link" :txt "Link"} "Remark")))))

(deftest svrt-cls-test
  (testing "returns severity-based CSS classes"
    (is (= "bg-blue-100 text-blue-800" (c/svrt-cls "info")))
    (is (= "bg-green-500 text-white" (c/svrt-cls "success")))
    (is (= "bg-yellow-500 text-white" (c/svrt-cls "warning")))
    (is (= "bg-red-500 font-bold text-white" (c/svrt-cls "danger")))
    (is (= "nil" (c/svrt-cls "unknown")))))

(deftest type-of-test
  (testing "classifies value types"
    (is (= :nil (c/type-of nil)))
    (is (= :class (c/type-of :keyword)))
    (is (= :href-id (c/type-of "path/id=")))
    (is (= :href-self (c/type-of "path")))))

(deftest bs-to-tw-test
  (testing "converts bootstrap classes to tailwind"
    (is (= "italic font-medium text-amber-900" (c/bs-to-tw :rownum)))
    (is (= "text-green-500" (c/bs-to-tw :text-success)))
    (is (= "text-yellow-500" (c/bs-to-tw :text-warning)))
    (is (= "text-red-500 font-bold" (c/bs-to-tw :text-danger)))
    (is (= "text-blue-500" (c/bs-to-tw :text-primary)))
    (is (= "text-cyan-500" (c/bs-to-tw :text-info)))))

(deftest ratio-class-test
  (testing "returns class based on ratio value"
    (is (= :text-success (c/ratio-class "96")))
    (is (= :text-warning (c/ratio-class "90")))
    (is (= :text-danger (c/ratio-class "80")))))

(deftest severity-class-test
  (testing "returns severity class"
    (is (= :text-danger (c/severity-class "critical")))
    (is (= :text-warning (c/severity-class "warn")))
    (is (= :text-primary (c/severity-class "unknown")))))

(deftest online-class-test
  (testing "returns online status class"
    (is (= :text-success (c/online-class "在线")))
    (is (= :text-danger (c/online-class "离线")))
    (is (= :text-primary (c/online-class "unknown")))))

(deftest onu-state-class-test
  (testing "returns ONU state class"
    (is (= :text-success (c/onu-state-class "working")))
    (is (= :text-warning (c/onu-state-class "OffLine")))
    (is (= :text-danger (c/onu-state-class "LOS")))
    (is (= :text-danger (c/onu-state-class "DyingGasp")))
    (is (= :text-warning (c/onu-state-class "unknown")))))

(deftest severity-show-test
  (testing "shows severity in Chinese"
    (is (= "关键" (c/severity-show "critical")))
    (is (= "重要" (c/severity-show "warn")))
    (is (= "普通" (c/severity-show "unknown")))))

(deftest onu-state-show-test
  (testing "shows ONU state in Chinese"
    (is (= "正常" (c/onu-state-show "working")))
    (is (= "离线" (c/onu-state-show "OffLine")))
    (is (= "光信号丢失" (c/onu-state-show "LOS")))
    (is (= "掉电" (c/onu-state-show "DyingGasp")))
    (is (= "unknown" (c/onu-state-show "unknown")))))

(deftest bool-class-test
  (testing "returns boolean-based class"
    (is (= :text-danger (c/bool-class true true)))
    (is (= :text-success (c/bool-class true false)))
    (is (= :text-success (c/bool-class false true)))
    (is (= :text-danger (c/bool-class false false)))))

(deftest td-class-test
  (testing "returns td with appropriate class and content"
    (let [data {:online "在线" :ratio "96" :severity "critical"}]
      (is (vector? (c/td-class data :online :online)))
      (is (vector? (c/td-class data :ratio :ratio)))
      (is (vector? (c/td-class data :severity :severity)))
      (is (vector? (c/td-class data :unknown :text-primary))))))

(deftest change-page-size-test
  (testing "validates and updates page size in session"
    (let [req {:headers {"referer" "/admin"}
               :params {"page-size" "50"}
               :session {}}
          resp (c/change-page-size req)]
      (is (= 303 (:status resp)))
      (is (= {:page-size 50} (:session resp))))))

(deftest form-input-test
  (testing "renders form input with label"
    (let [result (c/form-input {:type "text" :label "Name" :name "name" :required true})]
      (is (vector? result))
      (is (= :div.my-2 (first result)))))
  (testing "renders with default values"
    (let [result (c/form-input {:type "text" :label "Name" :name "name"})]
      (is (vector? result))))
  (testing "renders with size variants"
    (is (vector? (c/form-input {:name "xs" :size :xs})))
    (is (vector? (c/form-input {:name "lg" :size :lg}))))
  (testing "renders with error state"
    (let [result (c/form-input {:name "error" :variant :error :error "Invalid input"})]
      (is (vector? result))
      (is (= :div.my-2 (first result)))))
  (testing "renders disabled state"
    (let [result (c/form-input {:name "disabled" :disabled true})]
      (is (vector? result)))))

(deftest form-submit-btn-test
  (testing "renders submit button"
    (let [result (c/form-submit-btn "Submit")]
      (is (vector? result)))))

(deftest tabs-view-test
  (testing "renders tabs view structure"
    (let [tabs [{:id "tab1" :text "Tab 1" :default true}]
          pages [{:id "tab1" :content "Content" :default true}]
          result (c/tabs-view "Title" tabs pages)]
      (is (vector? result)))))

(deftest form-select-test
  (testing "renders select with options"
    (let [result (c/form-select {:name "role" :label "Role" :options [["admin" "Admin"] ["user" "User"]]})]
      (is (vector? result))))
  (testing "renders with prompt"
    (let [result (c/form-select {:name "role" :prompt "Select..." :options []})]
      (is (vector? result))))
  (testing "renders with error"
    (let [result (c/form-select {:name "role" :error "Required"})]
      (is (vector? result)))))

(deftest form-checkbox-test
  (testing "renders checkbox with label"
    (let [result (c/form-checkbox {:name "agree" :label "I agree"})]
      (is (vector? result))))
  (testing "renders checked state"
    (let [result (c/form-checkbox {:name "agree" :checked true})]
      (is (vector? result))))
  (testing "renders disabled state"
    (let [result (c/form-checkbox {:name "agree" :disabled true})]
      (is (vector? result)))))

(deftest form-radio-test
  (testing "renders radio with label"
    (let [result (c/form-radio {:name "gender" :label "Male" :value "m"})]
      (is (vector? result))))
  (testing "renders with group"
    (let [result (c/form-radio {:name "gender" :group "gender-group"})]
      (is (vector? result)))))

(deftest form-toggle-test
  (testing "renders toggle with label"
    (let [result (c/form-toggle {:name "toggle" :label "Enable"})]
      (is (vector? result))))
  (testing "renders checked state"
    (let [result (c/form-toggle {:name "toggle" :checked true})]
      (is (vector? result)))))

(deftest card-test
  (testing "renders card with title and content"
    (let [result (c/card {:title "Title" :content "Content"})]
      (is (vector? result))))
  (testing "renders card with footer"
    (let [result (c/card {:title "Title" :footer "Footer"})]
      (is (vector? result)))))
(deftest stat-card-test
  (testing "renders stat card with label and value"
    (let [result (c/stat-card {:label "Users" :value 123})]
      (is (vector? result)))))
(deftest breadcrumb-test
  (testing "renders breadcrumb with items"
    (let [result (c/breadcrumb {:items ["Home" "Users"]})]
      (is (vector? result)))))
(deftest empty-state-test
  (testing "renders empty state with icon and title"
    (let [result (c/empty-state {:icon "📭" :title "No Data" :description "Nothing here"})]
      (is (vector? result)))))
(deftest loading-spinner-test
  (testing "renders spinner with size"
    (let [result (c/loading-spinner {:size :lg})]
      (is (vector? result))))
  (testing "renders with label"
    (let [result (c/loading-spinner {:size :md :label "Loading..."})]
      (is (vector? result)))))
(deftest progress-bar-test
  (testing "renders progress bar with value"
    (let [result (c/progress-bar {:value 65})]
      (is (vector? result))))
  (testing "renders with label"
    (let [result (c/progress-bar {:value 50 :label "Progress"})]
      (is (vector? result)))))
(deftest toast-test
  (testing "renders toast with message"
    (let [result (c/toast {:message "Success!" :type :success})]
      (is (vector? result)))))
(deftest confirm-dialog-test
  (testing "renders dialog with title and message"
    (let [result (c/confirm-dialog {:title "Confirm" :message "Are you sure?"})]
      (is (vector? result)))))
(deftest grid-test
  (testing "renders grid with items"
    (let [result (c/grid {:cols 2 :items ["A" "B"]})]
      (is (vector? result)))))
(deftest container-test
  (testing "renders container with title"
    (let [result (c/container {:title "Page Title"})]
      (is (vector? result)))))
(deftest sidebar-test
  (testing "renders sidebar with items"
    (let [result (c/sidebar {:title "Menu" :items [{:id "a" :label "Item A"}]})]
      (is (vector? result)))))
(deftest accordion-test
  (testing "renders accordion with items"
    (let [result (c/accordion {:items [{:title "Section" :content "Content"}]})]
      (is (vector? result)))))
(deftest tree-view-test
  (testing "renders tree with nodes"
    (let [result (c/tree-view {:nodes [{:label "Root" :children [{:label "Child"}]}]})]
      (is (vector? result)))))

(run-tests)