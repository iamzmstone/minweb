(ns view.examples
  (:require
   [view.layout :as layout]
   [view.core :as c]))

(defn example-page [req]
  (layout/layout
   req
   [:div.max-w-6xl.mx-auto
    [:h1.text-4xl.font-bold.text-gray-900.mb-8 "UI Components Example"]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Badge"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.flex.flex-wrap.gap-2
      (c/badge "Primary" {:variant :primary})
      (c/badge "Info" {:variant :info})
      (c/badge "Success" {:variant :success})
      (c/badge "Warning" {:variant :warning})
      (c/badge "Danger" {:variant :danger})
      (c/badge "Secondary" {:variant :secondary})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Badge Sizes"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.flex.items-center.gap-4
      (c/badge "XS" {:size :xs})
      (c/badge "SM" {:size :sm})
      (c/badge "MD" {:size :md})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Callout"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/callout :info "Tip" "This is an informational message")
      (c/callout :success "Success" "Operation completed successfully")
      (c/callout :warning "Warning" "Please review your input")
      (c/callout :danger "Danger" "This action cannot be undone")]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Form Input"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/form-input {:type "text" :label "Username" :name "username" :placeholder "Enter username" :required true})
      (c/form-input {:type "email" :label "Email" :name "email" :placeholder "Enter email"})
      (c/form-input {:type "password" :label "Password" :name "password" :disabled true})
      (c/form-input {:type "text" :label "With Error" :name "error" :variant :error :error "This field is required"})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Form Select"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      [:div.max-w-xs
       (c/form-select {:name "role" :label "Role" :options [["admin" "Administrator"] ["user" "Regular User"] ["guest" "Guest"]] :prompt "Select a role"})]]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Form Checkbox"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/form-checkbox {:name "agree" :label "I agree to the terms"})
      (c/form-checkbox {:name "newsletter" :label "Subscribe to newsletter" :checked true})
      (c/form-checkbox {:name "disabled" :label "Disabled option" :disabled true})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Form Radio"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/form-radio {:name "gender" :label "Male" :value "m" :group "gender"})
      (c/form-radio {:name "gender" :label "Female" :value "f" :group "gender" :checked true})
      (c/form-radio {:name "gender" :label "Other" :value "o" :group "gender"})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Form Toggle"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.flex.flex-col.gap-4
      (c/form-toggle {:name "notifications" :label "Enable notifications"})
      (c/form-toggle {:name "dark-mode" :label "Dark mode" :checked true})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Card"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      [:div.grid.grid-cols-3.gap-4
       (c/card {:title "Default Card" :content "This is a basic card."})
       (c/card {:title "Bordered Card" :variant :bordered :content "Card with border."})
       (c/card {:title "Elevated Card" :variant :elevated :content "Card with shadow."})]]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Card with Footer"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.max-w-md
      (c/card {:title "Card Title" :content "Card content." :footer [:button.px-4.py-2.bg-blue-600.text-white.rounded-md "Action"]})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Stat Card"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      [:div.grid.grid-cols-4.gap-4
       (c/stat-card {:label "Total Users" :value 1234})
       (c/stat-card {:label "Revenue" :value "$9,999"})
       (c/stat-card {:label "Orders" :value 567})
       (c/stat-card {:label "Active" :value 42})]]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Empty State"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.max-w-md
      [:div.flex.flex-col.items-center.justify-center.py-12.px-4.text-center
       [:div.text-5xl.mb-4 "📭"]
       [:h3.font-semibold.text-lg.text-gray-900.mb-2 "No Results Found"]
       [:p.text-gray-500.mb-4 "There are no items matching your criteria."]
       [:button.px-4.py-2.bg-blue-600.text-white.rounded-md "Try Again"]]]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Breadcrumb"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/breadcrumb {:items ["Home" "Users" "Profile" "Settings"]})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Progress Bar"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.max-w-md
      (c/progress-bar {:value 0 :label "Empty"})
      [:div.h-2]
      (c/progress-bar {:value 25 :label "In Progress"})
      [:div.h-2]
      (c/progress-bar {:value 65 :label "Almost Done"})
      [:div.h-2]
      (c/progress-bar {:value 100 :label "Complete" :variant :success})
      [:div.h-2]
      (c/progress-bar {:value 85 :label "Warning" :variant :warning})
      [:div.h-2]
      (c/progress-bar {:value 30 :label "Error" :variant :danger})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Loading Spinner"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.flex.items-center.gap-8
      (c/loading-spinner {:size :xs})
      (c/loading-spinner {:size :sm})
      (c/loading-spinner {:size :md})
      (c/loading-spinner {:size :lg})
      (c/loading-spinner {:size :lg :label "Loading..."})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Grid"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/grid {:cols 3 :items ["Item 1" "Item 2" "Item 3" "Item 4" "Item 5"]})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Sidebar"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      (c/sidebar {:title "Navigation"
                  :items [{:id "dashboard" :label "Dashboard" :href "/"}
                          {:id "users" :label "Users" :href "/users"}
                          {:id "settings" :label "Settings" :href "/settings"}]
                  :active "dashboard"})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Accordion"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.max-w-2xl
      (c/accordion {:items [{:title "Section 1: Getting Started" :content "Welcome to the guide."}
                            {:title "Section 2: Configuration" :content "Configure your settings."}
                            {:title "Section 3: FAQ" :content "Frequently asked questions."}]})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Tree View"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.max-w-md
      (c/tree-view {:nodes [{:label "src"
                             :children [{:label "app"}
                                        {:label "components"}
                                        {:label "utils"}]}
                            {:label "test"
                             :children [{:label "app_test.clj"}
                                        {:label "utils_test.clj"}]}
                            {:label "README.md"}]})]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Toast"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2.flex.gap-3
      [:div.px-4.py-3.rounded-lg.shadow-lg.text-white.bg-blue-500 "Info message"]
      [:div.px-4.py-3.rounded-lg.shadow-lg.text-white.bg-green-500 "Success!"]
      [:div.px-4.py-3.rounded-lg.shadow-lg.text-white.bg-yellow-500 "Warning!"]
      [:div.px-4.py-3.rounded-lg.shadow-lg.text-white.bg-red-500 "Error!"]]]

    [:div.mb-12.p-6.bg-white.rounded-lg.shadow
     [:h2.text-xl.font-semibold.mb-4 "Confirm Dialog"]
     [:div.p-4.border.border-dashed.border-gray-300.rounded.mt-2
      [:div.border.border-gray-200.rounded-lg.bg-white.max-w-md
       [:div.p-4
        [:h3.text-lg.font-semibold.text-gray-900.mb-2 "Confirm Action"]
        [:p.text-gray-600.mb-4 "Are you sure you want to proceed?"]
        [:div.flex.gap-3.justify-end
         [:button.px-4.py-2.text-gray-700.bg-gray-100.rounded-md "Cancel"]
         [:button.px-4.py-2.text-white.bg-blue-600.rounded-md "Confirm"]]]]]]]))