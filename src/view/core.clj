(ns view.core
  (:require
   [taoensso.timbre :as log]
   [clojure.string :as str]
   [utils.session :refer [current-user]]
   [ring.middleware.anti-forgery :as af]))

(defn log-user
  [req str]
  (let [u (current-user req)]
    (log/info "User" (:user/name u) str)))

(defn csrf-token []
  [:input {:type "hidden"
           :name "__anti-forgery-token"
           :value af/*anti-forgery-token*}])

;; ========================================
;; Component Configuration
;; ========================================

(def badge-variant-classes
  {:primary "bg-purple-200 text-purple-800"
   :info "bg-blue-200 text-blue-800"
   :success "bg-green-200 text-green-800"
   :warning "bg-yellow-200 text-yellow-800"
   :danger "bg-red-200 text-red-500"
   :secondary "bg-gray-200 text-gray-800"})

(def badge-size-classes
  {:xs "text-xs px-2 py-0.5"
   :sm "text-xs px-2.5 py-0.5"
   :md "text-sm px-3 py-1"})

(def input-size-classes
  {:xs "px-2 py-1 text-xs"
   :sm "px-3 py-1.5 text-sm"
   :md "px-4 py-2 text-base"
   :lg "px-5 py-2.5 text-lg"})

(defn merge-classes
  "Join classes, filtering out blank strings."
  [& classes]
  (str/join " " (remove str/blank? classes)))

;; ========================================
;; Basic Components
;; ========================================

(defn badge
  "Badge with variant and size support.

   Options:
   - :variant - :primary, :info, :success, :warning, :danger, :secondary (default :info)
   - :size - :xs, :sm, :md (default :sm)
   - :class - extra CSS classes"
  [text {:keys [variant size class]
         :or {variant :info size :sm}}]
  (let [variant-c (get badge-variant-classes variant)
        size-c (get badge-size-classes size)]
    [:span {:class (merge-classes "inline-flex items-center rounded-full font-medium" variant-c size-c class)}
     text]))

(defn callout
  [svrt tip content]
  (let [[color sign]
        (case svrt
          :info
          ["bg-blue-50 border-blue-500 text-blue-700" "ℹ"]
          :success
          ["bg-green-50 border-green-500 text-green-700" "✔"]
          :warning
          ["bg-yellow-50 border-yellow-500 text-yellow-700" "⚠"]
          :danger
          ["bg-red-50 border-red-500 text-red-700" "✘"]
          ["bg-blue-50 border-blue-500 text-blue-700" "ℹ"])]
    [:div.border-l-4.p-4.rounded-md.mb-4.flex.items-start.gap-2
     {:class color}
     [:span.font-bold sign]
     [:div
      [:h4.font-semibold tip]
      [:p content]]]))

(defn alert
  [req]
  (let [msg (get-in req [:flash :message])
        severity (:severity msg)
        msg (:message msg)]
    (when msg
     (let [cls (case severity
                 "info" "bg-blue-100 text-blue-800"
                 "success" "bg-green-500 text-white"
                 "warning" "bg-yellow-500 text-white"
                 "danger" "bg-red-500 font-bold text-white"
                 "bg-blue-100 text-blue-800")]
       [:div {:id "flashMessage"
              :class (str cls " fixed top-20 left-1/2 transform -translate-x-1/2 text-white px-4 py-2 rounded shadow-lg transition-opacity duration-300 flex items-center justify-between min-w-[250px] max-w-md")
              :_ "on load wait 5s then add .opacity-0"}
        [:span msg]
        [:button
         {:_ "on click add .opacity-0 to #flashMessage"
          :class "ml-4 text-white font-bold"} "x"]]))))

;; ========================================
;; Form Components
;; ========================================

(defn form-input
  "Form input with consistent API.

   Options:
   - :type - input type (text, email, password, number, etc.)
   - :label - label text
   - :name - input name
   - :size - :xs, :sm, :md, :lg (default :md)
   - :variant - :default, :error (adds red border)
   - :disabled - boolean
   - :required - boolean
   - :placeholder - placeholder text
   - :value - default value
   - :class - extra CSS classes
   - :error - error message text"
  [{:keys [type label name size variant disabled required placeholder value id error autocomplete]
    :or {type "text" size :md variant :default required false}}]
  (let [size-c (get input-size-classes size)
        autocomplete-c (or autocomplete
                           (case type
                             "email" "email"
                             "password" "current-password"
                             "tel" "tel"
                             "url" "url"
                             "search" "search"
                             "username" "username"
                             nil)
                           (case name
                             "username" "username"
                             "email" "email"
                             "password" "current-password"
                             "tel" "tel"
                             nil))
        variant-c (case variant
                     :error "border-red-500 focus:ring-red-500"
                     :default "border-gray-300 focus:ring-blue-500")
        base "w-full bg-white border rounded-md focus:outline-none focus:ring-2 transition"
        disabled-c (when disabled "opacity-50 cursor-not-allowed")
        id-c (or id name)
        label-c (str "block text-sm font-medium mb-1"
                    (if (= variant :error) " text-red-600" " text-gray-700"))]
    [:div.my-2
     (when label [:label {:for id-c :class label-c} label])
     [:input
      {:type type
       :name name
       :id id-c
       :autocomplete autocomplete-c
       :class (merge-classes base size-c variant-c disabled-c)
       :placeholder placeholder
       :value value
       :required required
       :disabled disabled}]
     (when error [:span.text-sm.text-red-600.mt-1 error])]))

(defn form-submit-btn
  [v]
  (let [cls "w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-md cursor-pointer"]
    [:div.my-2
     [:input {:class cls :type "submit" :value v}]]))

(defn form-select [{:keys [options prompt disabled size class error id name label autocomplete]
                  :or {size :md}}]
  (let [size-c (get {:xs "text-xs" :sm "text-sm" :md "text-base" :lg "text-lg"} size)
        base "w-full bg-white border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition appearance-none pr-8"
        id-c (or id name)
        disabled-c (when disabled "opacity-50 cursor-not-allowed")
        label-c "block text-sm font-medium text-gray-700 mb-1"
        autocomplete-c (or autocomplete
                           (case name
                             "role" "organization-title"
                             "country" "country"
                             "timezone" "timezone"
                             nil))]
    [:div.my-2
     (when label
       [:label {:for id-c :class label-c} label])
     [:div.relative
      [:select
       {:name name
        :id id-c
        :disabled disabled
        :autocomplete autocomplete-c
        :class (str base " " size-c " " disabled-c " " (or class ""))}
       (when prompt [:option {:value ""} prompt])
       (for [[v l] options]
         [:option {:value v} l])]
      [:div.absolute.inset-y-0.right-0.flex.items-center.pr-3.pointer-events-none
       [:svg {:class "w-4 h-4 text-gray-400" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M19 9l-7 7-7-7"}]]]]
     (when error [:span.text-sm.text-red-600.mt-1 error])]))

(defn form-checkbox [{:keys [label name id value checked disabled class]
                     :or {checked false}}]
  (let [disabled-c (when disabled "opacity-50 cursor-not-allowed")
        id-c (or id name)]
    [:div.my-2.flex.items-center
     [:input
      {:type "checkbox"
       :name name
       :id id-c
       :value (or value "on")
       :checked checked
       :disabled disabled
       :class (str "h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 " disabled-c " " (or class ""))}]
     (when label
       [:label {:for id-c :class "ml-2 block text-sm text-gray-700"} label])]))

(defn form-radio [{:keys [label name id value checked disabled class group]
                  :or {checked false}}]
  (let [disabled-c (when disabled "opacity-50 cursor-not-allowed")
        radio-name (or group name)
        id-c (or id name)]
    [:div.my-2.flex.items-center
     [:input
      {:type "radio"
       :name radio-name
       :id id-c
       :value (or value "on")
       :checked checked
       :disabled disabled
       :class (str "h-4 w-4 rounded-full border-gray-300 text-blue-600 focus:ring-blue-500 " disabled-c " " (or class ""))}]
     (when label
       [:label {:for id-c :class "ml-2 block text-sm text-gray-700"} label])]))

(defn form-toggle [{:keys [label name id value checked disabled]
                   :or {checked false}}]
  (let [disabled-c (when disabled "opacity-50 cursor-not-allowed")
        id-c (or id name)]
    [:label {:for id-c :class (str "my-2 flex items-center cursor-pointer " disabled-c)}
     [:div.relative.inline-block.w-11.h-6.flex-shrink-0
      [:input
       {:type "checkbox"
        :name name
        :id id-c
        :value (or value "on")
        :checked checked
        :disabled disabled
        :class (str "sr-only peer " disabled-c)}]
      [:div
       {:class (str "cursor-pointer absolute inset-0 w-full h-full bg-gray-300 rounded-full transition-colors duration-200 ease-in-out before:absolute before:content-[''] before:h-4 before:w-4 before:bg-white before:rounded-full before:left-0.5 before:top-1 before:transition-transform before:duration-200 before:ease-in-out before:translate-x-0 peer-checked:bg-blue-600 peer-checked:before:translate-x-5 "
              disabled-c)}]]
     (when label [:span {:class "ml-3 text-sm text-gray-700"} label])]))

;; ========================================
;; Display Components
;; ========================================

(defn card [{:keys [title content footer class variant]
            :or {variant :default}}]
  (let [base "bg-white rounded-lg shadow-md overflow-hidden"
        variant-c (case variant
                     :default ""
                     :bordered "border border-gray-200"
                     :elevated "shadow-lg")
        card-class (str base " " variant-c " " (or class ""))]
    [:div {:class card-class}
     (when title
       [:div.p-4.border-b.border-gray-200
        [:h3.font-semibold.text-lg.text-gray-800 title]])
     (when content
       [:div.p-4 content])
     (when footer
       [:div.p-4.border-t.border-gray-200.bg-gray-50 footer])]))

(defn stat-card [{:keys [label value icon class trend]
                 :or {trend :neutral}}]
  (let [base "bg-white rounded-lg shadow p-6"
        trend-color (case trend
                      :up "text-green-600"
                      :down "text-red-600"
                      :neutral "text-gray-600")
        trend-icon (case trend
                     :up "↑"
                     :down "↓"
                     nil)
        icon-bg-color (case trend
                        :up "bg-green-100"
                        :down "bg-red-100"
                        "bg-gray-100")
        icon-color (case trend
                     :up "text-green-600"
                     :down "text-red-600"
                     "text-gray-600")]
    [:div {:class (str base " " (or class ""))}
     [:div.flex.items-center.justify-between.mb-4
      (when icon
        [:div {:class (str "w-12 h-12 rounded-xl flex items-center justify-center " icon-bg-color)}
         [:span {:class (str "w-6 h-6 " icon-color)} icon]])
      (when trend-icon
        [:span {:class (str "text-sm font-medium " trend-color)} trend-icon])]
     [:p.text-sm.text-gray-500.mb-1 label]
     [:p.text-2xl.font-bold.text-gray-900 (str value)]]))

(defn breadcrumb [{:keys [items class]}]
  [:nav {:class (str "flex items-center space-x-2 text-sm " (or class "")) :aria-label "breadcrumb"}
   [:ol.flex.items-center.space-x-2
    (for [[i item] (map-indexed vector items)]
      [:li.flex.items-center
       (if (= i (dec (count items)))
         [:span.text-gray-700.font-medium (if (map? item) (:label item) item)]
         [:span.text-gray-400 "/"])
       (when-not (= i (dec (count items)))
         [:a {:href (if (map? item) (:href item "#") "#") :class "text-blue-600 hover:underline"} (if (map? item) (:label item) item)])])]])

(defn empty-state [{:keys [icon title description action class]}]
  (let [base "flex flex-col items-center justify-center py-12 px-4 text-center"]
    [:div {:class (str base " " (or class ""))}
     (when icon [:div.text-5xl.mb-4 icon])
     (when title [:h3.font-semibold.text-lg.text-gray-900.mb-2 title])
     (when description [:p.text-gray-500.mb-6 description])
     (when action [:div action])]))

(defn loading-spinner [{:keys [size label class]
                       :or {size :md}}]
  (let [size-c (case size :xs "w-4 h-4" :sm "w-6 h-6" :md "w-8 h-8" :lg "w-12 h-12" "w-8 h-8")]
    [:div {:class (str "inline-flex items-center gap-2 " (or class "")) :role "status"}
     [:svg.animate-spin {:class size-c :xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"}
      [:circle {:class "opacity-25" :cx "12" :cy "12" :r "10" :stroke "currentColor" :stroke-width "4"}]
      [:path {:class "opacity-75" :fill "currentColor" :d "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"}]]
     (when label [:span.text-gray-500 label])]))

(defn progress-bar [{:keys [value max-val label variant class]
                    :or {value 0 max-val 100 variant :default}}]
  (let [pct (min 100 (max 0 (* 100 (/ value (float max-val)))))
        variant-c (case variant :default "bg-blue-600" :success "bg-green-600" :warning "bg-yellow-600" :danger "bg-red-600" "bg-blue-600")]
    [:div {:class (str "w-full " (or class ""))}
     (when label [:div.text-sm.text-gray-600.mb-1 label])
     [:div.h-2.w-full.bg-gray-200.rounded-full.overflow-hidden
      [:div {:class (str "h-full rounded-full " variant-c) :style {:width (str pct "%")}}]]]))

(defn toast [{:keys [message type class]
             :or {type :info}}]
  (let [type-c (case type :info "bg-blue-500" :success "bg-green-500" :warning "bg-yellow-500" :danger "bg-red-500" "bg-blue-500")]
    [:div {:class (str "fixed bottom-4 right-4 px-4 py-3 rounded-lg shadow-lg text-white " type-c " " (or class ""))}
     message]))

(defn confirm-dialog [{:keys [title message confirm-text cancel-text class]
                        :or {confirm-text "Confirm" cancel-text "Cancel"}}]
  [:div {:class (str "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 " (or class ""))}
   [:div {:class "bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4"}
    [:h3 {:class "text-lg font-semibold text-gray-900 mb-2"} title]
    [:p {:class "text-gray-600 mb-6"} message]
    [:div {:class "flex justify-end space-x-3"}
     [:button {:class "px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"} cancel-text]
     [:button {:class "px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-md"} confirm-text]]]])

(defn grid [{:keys [cols items class]
           :or {cols 3}}]
  (let [col-class (case cols
                    1 "grid-cols-1"
                    2 "grid-cols-1 md:grid-cols-2"
                    3 "grid-cols-1 md:grid-cols-2 lg:grid-cols-3"
                    4 "grid-cols-1 md:grid-cols-2 lg:grid-cols-4"
                    (str "grid-cols-" cols))]
    [:div {:class (str "grid gap-4 " col-class " " (or class ""))}
     (for [item items]
       [:div item])]))

(defn container [{:keys [title content class]}]
  (let [base "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"]
    [:div {:class (str base " " (or class ""))}
     (when title [:h1 {:class "text-3xl font-bold text-gray-900 mb-6"} title])
     (when content [:div content])]))

(defn sidebar [{:keys [title items active class]}]
  (let [base "w-64 bg-white border-r border-gray-200 min-h-screen"]
    [:div {:class (str base " " (or class ""))}
     (when title [:div {:class "p-4 border-b border-gray-200"}
                  [:h2 {:class "text-lg font-semibold text-gray-800"} title]])
     [:nav {:class "p-4"}
      (for [item items]
        (let [is-active (= (:id item) active)]
          [:a {:href (:href item "#")
               :class (str "block px-3 py-2 rounded-md text-sm font-medium "
                         (if is-active "bg-blue-100 text-blue-700" "text-gray-700 hover:bg-gray-100"))}
           (:label item)]))]]))

(defn sidebar-item [{:keys [href label icon active badge class]}]
  [:a {:href (or href "#")
       :class (str "flex items-center px-4 py-3 rounded-lg transition-colors "
                   (if active "bg-white/10 text-white" "text-white/70 hover:bg-white/5 hover:text-white") " " (or class ""))}
   (when icon [:svg {:class "w-5 h-5 mr-3" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
               [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d icon}]])
   [:span label]
   (when badge [:span {:class "ml-auto px-2 py-0.5 text-xs font-medium rounded-full bg-red-500"} badge])])

(defn accordion [{:keys [items class]}]
  [:div {:class (str "space-y-2 " (or class ""))}
   (for [[i item] (map-indexed vector items)]
     (let [id (str "accordion-" i)]
       [:div {:class "border border-gray-200 rounded-lg"}
        [:button {:class "w-full px-4 py-3 text-left flex justify-between items-center hover:bg-gray-50"
                 :_ (format "on click toggle .hidden on #%s-content" id)}
         [:span {:class "font-medium text-gray-900"} (:title item)]
         [:span {:class "transform transition-transform"} "▾"]]
        [:div {:id (str id "-content") :class "hidden px-4 py-3 text-gray-600"}
         (:content item)]]))])

(defn tree-view [{:keys [nodes class]}]
  [:ul {:class (str "space-y-1 " (or class ""))}
   (for [node nodes]
     [:li
      [:div.flex.items-center.py-1
       (when (:children node)
         [:button {:class "w-5 h-5 flex items-center justify-center text-gray-400 hover:text-gray-600"
                  :_ "on click toggle .hidden on next .tree-children"} "▸"])
       [:span.text-gray-700 (:label node)]]
      (when (:children node)
        [:ul {:class "tree-children hidden pl-5 border-l border-gray-200 ml-2"}
         (for [child (:children node)]
           [:li [:span.text-gray-600 (:label child)]])])])])

(defn tabs-view
  [title tabs pages]
  (let [nav-cls "flex flex-col sm:flex-row border-b border-gray-200"
        btn-cls "tab-button px-12 py-4 text-left font-medium bg-amber-100 hover:bg-amber-200 hover:text-blue-600 transition-colors border-b-2 transition-border"]
    [:div
     [:h1 {:class "text-2xl md:text-3xl font-bold text-gray-900 mb-6"} title]
     [:div.bg-white.rounded-xl.shadow-md.overflow-hidden
      [:nav {:class nav-cls}
       (for [tab tabs]
         [:button
          {:class (if (:default tab)
                    (str btn-cls " text-blue-500 border-blue-500")
                    (str btn-cls " border-transparent"))
           :data-tab (:id tab)
           :role "tab"
           :aria-selected (if (:default tab) "true" "false")
           :aria-controls (str (:id tab) "-content")
           :_ (format
               "on click
                remove .text-blue-500 .border-blue-500 from .tab-button
                then add .border-transparent to .tab-button
                then set {aria-selected: 'false'} on .tab-button
                then add .text-blue-500 .border-blue-500 to me
                then remove .border-transparent from me
                then set {aria-selected: 'true'} on me
                then add .hidden to .tab-content
                then set {aria-hidden: 'true'} on .tab-content
                then remove .hidden from #%s-content
                then set {aria-hidden: 'false'} on #%s-content"
               (:id tab) (:id tab))}
          (when (:icon tab) (:icon tab))
          [:span (:text tab)]])]
      [:div.p-6
       (for [page pages]
         [:div.tab-content.space-y-4
          {:id (str (:id page) "-content")
           :class (if (:default page) "" "hidden")
           :role "tabpanel"
           :aria-hidden (if (:default page) "false" "true")}
          (:content page)])]]]))

;; ========================================
;; Dashboard Components
;; ========================================

(defn page-header [{:keys [title user show-search notification-count]
                   :or {show-search true notification-count 0}}]
  [:header.bg-white.border-b.border-gray-200
   [:div.flex.items-center.justify-between.px-6.py-4
    [:div.flex.items-center.space-x-4
     [:h1.text-xl.font-semibold.text-gray-900 title]]
    [:div.flex.items-center.space-x-4
     ;; Search
     (when show-search
       [:div.relative.hidden.md:block
        [:svg {:class "absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"}]]
        [:input
         {:type "search"
          :name "q"
          :id "header-search"
          :placeholder "搜索..."
          :class "w-64 pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"}]])
     ;; Notifications
     [:button
      {:class "relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"}
      [:svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"}]]
      (when (pos? notification-count)
        [:span.absolute.top-1.right-1.w-2.h-2.bg-red-500.rounded-full])]
     ;; User menu
     (when user
       [:div.flex.items-center.space-x-3
        [:img {:src (:avatar user "/static/img/avatar-default.svg")
               :alt (:name user)
               :class "w-8 h-8 rounded-full"}]
        [:span.hidden.md:block.text-sm.font-medium.text-gray-700 (:name user)]
        [:svg {:class "w-4 h-4 text-gray-400" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M19 9l-7 7-7-7"}]]])]]])

(defn user-avatar [{:keys [name src size class]
                   :or {size :md}}]
  (let [size-c (case size :xs "w-6 h-6" :sm "w-8 h-8" :md "w-10 h-10" :lg "w-12 h-12" "w-10 h-10")
        fallback-bg "bg-gray-300"]
    [:img {:src (or src "/static/img/avatar-default.svg")
           :alt name
           :class (str size-c " rounded-full object-cover " fallback-bg " " (or class ""))}]))

(defn welcome-banner [{:keys [title subtitle action action-text class]}]
  [:div {:class (str "bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-6 text-white " (or class ""))}
   [:div.flex.items-center.justify-between
    [:div
     [:h2 {:class "text-2xl font-bold mb-1"} title]
     (when subtitle
       [:p {:class "text-white/80"} subtitle])]
    (when action
      [:a {:href action
           :class "px-4 py-2 bg-white text-blue-600 rounded-lg font-medium hover:bg-white/90 transition-colors"}
       action-text])]])

(defn activity-item [{:keys [icon icon-bg icon-color title description time class]
                     :or {icon-bg "bg-gray-100" icon-color "text-gray-600"}}]
  [:div {:class (str "flex items-start space-x-3 " (or class ""))}
   [:div {:class (str "w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 " icon-bg)}
    [:span {:class (str "w-4 h-4 " icon-color)} icon]]
   [:div.flex-1.min-w-0
    [:p.text-sm.text-gray-900 title]
    (when description
      [:p.text-xs.text-gray-500 description])
    (when time
      [:p.text-xs.text-gray-400.mt-1 time])]])

(defn activity-feed [{:keys [items class]}]
  [:div {:class (str "divide-y divide-gray-100 " (or class ""))}
   (for [item items]
     (activity-item item))])

(defn quick-action [{:keys [href icon label class]}]
  [:a {:href (or href "#")
       :class (str "flex flex-col items-center p-4 rounded-xl bg-gray-50 hover:bg-gray-100 transition-colors " (or class ""))}
   [:div {:class "w-12 h-12 rounded-full flex items-center justify-center mb-3 bg-blue-100"}
    [:span {:class "w-6 h-6 text-blue-600"} icon]]
   [:span.text-sm.font-medium.text-gray-900 label]])

(defn stats-icon [{:keys [icon color class]
                  :or {color :blue}}]
  (let [color-c (case color
                  :blue "bg-blue-100 text-blue-600"
                  :purple "bg-purple-100 text-purple-600"
                  :green "bg-green-100 text-green-600"
                  :orange "bg-orange-100 text-orange-600"
                  :red "bg-red-100 text-red-600"
                  "bg-gray-100 text-gray-600")]
    [:div {:class (str "w-12 h-12 rounded-xl flex items-center justify-center " color-c " " (or class ""))}
     [:span {:class "w-6 h-6"} icon]]))

(defn dropdown-menu [{:keys [items class]}]
  [:div {:class (str "absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50 " (or class ""))
         :_ "on click outside remove me"}
   (for [item items]
     [:a {:href (:href item "#")
         :class (str "block px-4 py-2 text-sm " (if (= (:variant item) :danger) "text-red-600" "text-gray-700") " hover:bg-gray-100")}
      (:label item)])])

(defn notification-badge [{:keys [count class]}]
  (when (pos? count)
    [:span {:class (str "absolute -top-1 -right-1 w-5 h-5 flex items-center justify-center text-xs font-medium text-white bg-red-500 rounded-full " (or class ""))}
     (if (> count 99) "99+" count)]))

(defn page-title [{:keys [title subtitle class]}]
  [:div {:class (str "mb-6 " (or class ""))}
   [:h1 {:class "text-2xl md:text-3xl font-bold text-gray-900"} title]
   (when subtitle
     [:p {:class "mt-1 text-gray-500"} subtitle])])
(defn search-bar [{:keys [placeholder on-post target hx-indicator class], :or {placeholder "搜索..."}}] (let [input-cls "w-full pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500" wrapper-cls "relative hidden md:block" icon-cls "absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"] [:div {:class (str "flex items-center " (or class ""))} [:div {:class wrapper-cls} [:svg {:class icon-cls, :fill "none", :stroke "currentColor", :viewBox "0 0 24 24"} [:path {:stroke-linecap "round", :stroke-linejoin "round", :stroke-width "2", :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"}]] (if on-post [:input {:type "search", :name "q", :id "input-search", :placeholder placeholder, :class input-cls, :hx-post on-post, :hx-target (or target "body"), :hx-indicator (or hx-indicator "#search-spinner")}] [:input {:type "search", :name "q", :id "input-search", :placeholder placeholder, :class input-cls}])]]))
(defn data-table [{:keys [columns rows actions empty-message class], :or {empty-message "暂无数据"}}] (let [th-cls "px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider" td-cls "px-4 py-3 text-sm text-gray-900" get-val (fn [row col] (:value (get row (:key col)))) render-action-btn (fn [a] [:button {:type "button", :class "text-blue-600 hover:text-blue-800 text-sm font-medium", :on-click (:on-click a)} (:label a)]) render-row (fn [row] (list [:tr {:class "hover:bg-gray-50"} (for [c columns] [:td {:class td-cls} (get-val row c)]) (when actions [:td {:class td-cls} [:div {:class "flex items-center space-x-2"} (for [a actions] (render-action-btn a))]])]))] [:div {:class (str "overflow-hidden rounded-lg shadow " (or class ""))} [:table {:class "min-w-full divide-y divide-gray-200"} [:thead {:class "bg-gray-50"} [:tr (for [col columns] [:th {:class th-cls} (:label col)]) (when actions [:th {:class th-cls} "操作"])] (if (empty? rows) [:tbody [:tr [:td {:colspan (count columns), :class "text-center py-8 text-gray-500"} empty-message]]] [:tbody {:class "bg-white divide-y divide-gray-200"} (for [row rows] (render-row row))])]]]))