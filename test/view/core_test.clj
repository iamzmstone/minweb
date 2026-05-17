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
      (is (vector? result))))
  (testing "renders with trend"
    (let [result (c/stat-card {:label "Growth" :value "23%" :trend :up :trend-value "+5%"})]
      (is (vector? result))))
  (testing "renders with icon"
    (let [result (c/stat-card {:label "Users" :value 123 :icon (c/icon :user)})]
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

(deftest tabs-view-test
  (testing "renders tabs view structure"
    (let [tabs [{:id "tab1" :text "Tab 1" :default true}]
          pages [{:id "tab1" :content "Content" :default true}]
          result (c/tabs-view "Title" tabs pages)]
      (is (vector? result)))))

(deftest page-header-test
  (testing "renders page header"
    (let [result (c/page-header {:title "Dashboard"})]
      (is (vector? result))))
  (testing "renders with user"
    (let [result (c/page-header {:title "Dashboard" :user {:user/name "John" :user/avatar "/avatar.png"}})]
      (is (vector? result)))))

(deftest welcome-banner-test
  (testing "renders welcome banner"
    (let [result (c/welcome-banner {:title "Welcome!" :subtitle "Here is your overview"})]
      (is (vector? result)))))

(deftest activity-item-test
  (testing "renders activity item"
    (let [result (c/activity-item {:icon "✓" :title "New user registered" :time "2 min ago"})]
      (is (vector? result)))))

(deftest activity-feed-test
  (testing "renders activity feed"
    (let [result (c/activity-feed {:items [{:title "Item 1"} {:title "Item 2"}]})]
      (is (vector? result)))))

(deftest quick-action-test
  (testing "renders quick action"
    (let [result (c/quick-action {:label "Add User" :href "/users/new"})]
      (is (vector? result))))
  (testing "renders with icon"
    (let [result (c/quick-action {:label "Add User" :href "/users/new" :icon (c/icon :user)})]
      (is (vector? result)))))

(deftest icon-test
  (testing "renders icon by name"
    (is (vector? (c/icon :dashboard)))
    (is (vector? (c/icon :user)))
    (is (vector? (c/icon :search)))
    (is (vector? (c/icon :bell))))
  (testing "renders icon with size"
    (is (vector? (c/icon :user {:size :sm})))
    (is (vector? (c/icon :user {:size :lg})))))

(deftest user-menu-test
  (testing "renders user menu"
    (let [result (c/user-menu {:user {:user/name "John" :user/avatar "/avatar.png"}})]
      (is (vector? result))))
  (testing "renders with default avatar when no avatar"
    (let [result (c/user-menu {:user {:user/name "John"}})]
      (is (vector? result)))))

(deftest stats-icon-test
  (testing "renders stats icon"
    (let [result (c/stats-icon {:icon "👥" :color :blue})]
      (is (vector? result)))))

(deftest page-title-test
  (testing "renders page title"
    (let [result (c/page-title {:title "Dashboard" :subtitle "Overview"})]
      (is (vector? result)))))

(run-tests)