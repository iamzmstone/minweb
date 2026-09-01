(ns minweb.view.core
  (:require
   [taoensso.timbre :as log]
   [minweb.utils.session :refer [current-user]]
   [ring.middleware.anti-forgery :as af]
   [minweb.middleware.security :as sec]
   [minweb.view.config]
   [minweb.view.icons :as icons]
   [minweb.view.form]))

(defn log-user
  [req msg]
  (let [u (current-user req)]
    (log/info "User" (:user/name u) msg)))

(defn csrf-token []
  [:input {:type "hidden"
           :name "__anti-forgery-token"
           :value af/*anti-forgery-token*}])

(defn csp-nonce-attr
  "Return {:nonce <value>} for the current request's CSP nonce, or {} when
   no nonce is bound (e.g. in tests). Merge into the parent element map."
  []
  (when-let [n sec/*csp-nonce*]
    {:nonce n}))

;; ========================================
;; Re-export from view.icons and view.config
;; ========================================

(def icon-paths icons/icon-paths)
(defn icon [& args] (apply icons/icon args))
(defn user-menu [& args] (apply icons/user-menu args))
(defn merge-classes [& args] (apply minweb.view.config/merge-classes args))
(def badge-variant-classes minweb.view.config/badge-variant-classes)
(def badge-size-classes minweb.view.config/badge-size-classes)
(def input-size-classes minweb.view.config/input-size-classes)

;; ========================================
;; Basic Components
;; ========================================

(defn badge
  "Badge with variant and size support.

   Options:
   - :variant - :primary, :info, :success, :warning, :danger, :secondary (default :info)
   - :size - :xs, :sm, :md (default :sm)
   - :class - extra CSS classes"
  [text {:as opts :keys [variant size]
         :or {variant :info size :sm}}]
  (let [variant-c (get badge-variant-classes variant)
        size-c (get badge-size-classes size)]
    [:span
     {:class
      (merge-classes
       "inline-flex items-center rounded-full font-medium"
       variant-c size-c (:class opts))} text]))

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
        text (:message msg)]
    (when text
      (let [cls (case severity
                  "info" "bg-blue-100 text-blue-800"
                  "success" "bg-green-500 text-white"
                  "warning" "bg-yellow-500 text-white"
                  "danger" "bg-red-500 font-bold text-white"
                  "bg-blue-100 text-blue-800")]
        [:div (merge {:id "flashMessage"
                      :class (str cls " fixed top-20 left-1/2 transform -translate-x-1/2 text-white px-4 py-2 rounded shadow-lg transition-opacity duration-300 flex items-center justify-between min-w-[250px] max-w-md")
                      :_ "on load wait 5s then add .opacity-0"}
                     (csp-nonce-attr))
         [:span text]
         [:button
          (merge {:_ "on click add .opacity-0 to #flashMessage"
                  :class "ml-4 text-white font-bold"}
                 (csp-nonce-attr))
          "x"]]))))

;; ========================================
;; Re-export from view.form
;; ========================================

(defn form-input [& args] (apply minweb.view.form/form-input args))
(defn form-submit-btn [& args] (apply minweb.view.form/form-submit-btn args))
(defn form-select [& args] (apply minweb.view.form/form-select args))
(defn form-checkbox [& args] (apply minweb.view.form/form-checkbox args))
(defn form-radio [& args] (apply minweb.view.form/form-radio args))
(defn form-toggle [& args] (apply minweb.view.form/form-toggle args))

;; ========================================
;; Display Components
;; ========================================

(defn card [{:as opts :keys [title content footer variant]
             :or {variant :default}}]
  (let [klass (:class opts)
        base "bg-white rounded-lg shadow-md overflow-hidden"
        variant-c (case variant
                    :default ""
                    :bordered "border border-gray-200"
                    :elevated "shadow-lg")
        card-class (str base " " variant-c " " (or klass ""))]
    [:div {:class card-class}
     (when title
       [:div.p-4.border-b.border-gray-200
        [:h3.font-semibold.text-lg.text-gray-800 title]])
     (when content
       [:div.p-4 content])
     (when footer
       [:div.p-4.border-t.border-gray-200.bg-gray-50 footer])]))

(defn stat-card [{:as opts :keys [label value trend trend-value]
                  :or {trend :neutral}}]
  (let [klass (:class opts)
        icon-val (:icon opts)
        base "bg-white rounded-lg shadow p-6"
        trend-color (case trend
                      :up "text-green-600"
                      :down "text-red-600"
                      :neutral "text-gray-600")
        icon-bg-color (case trend
                        :up "bg-green-100"
                        :down "bg-red-100"
                        "bg-gray-100")]
    [:div {:class (str base " " (or klass ""))}
     [:div.flex.items-center.justify-between.mb-4
      (when icon-val
        [:div {:class (str "w-12 h-12 rounded-xl flex items-center justify-center " icon-bg-color)}
         icon-val])
      (when trend-value
        [:span {:class (str "text-sm font-medium " trend-color)} trend-value])]
     [:p.text-sm.text-gray-500.mb-1 label]
     [:p.text-2xl.font-bold.text-gray-900 (str value)]]))

(defn breadcrumb [{:as opts :keys [items]}]
  (let [klass (:class opts)]
    [:nav {:class (str "flex items-center space-x-2 text-sm " (or klass "")) :aria-label "breadcrumb"}
     [:ol.flex.items-center.space-x-2
      (for [[i item] (map-indexed vector items)]
        [:li.flex.items-center
         (if (= i (dec (count items)))
           [:span.text-gray-700.font-medium
            (if (map? item) (:label item) item)]
           [:span.text-gray-400 "/"])
         (when-not (= i (dec (count items)))
           [:a {:href (if (map? item)
                        (:href item "#") "#")
                :class "text-blue-600 hover:underline"}
            (if (map? item) (:label item) item)])])]]))

(defn empty-state
  [{:as opts :keys [title description action]}]
  (let [klass (:class opts)
        icon-val (:icon opts)
        base "flex flex-col items-center justify-center py-12 px-4 text-center"]
    [:div {:class (str base " " (or klass ""))}
     (when icon-val [:div.text-5xl.mb-4 icon-val])
     (when title
       [:h3.font-semibold.text-lg.text-gray-900.mb-2 title])
     (when description [:p.text-gray-500.mb-6 description])
     (when action [:div action])]))

(defn loading-spinner
  [{:as opts :keys [size label]
    :or {size :md}}]
  (let [klass (:class opts)
        size-c (case size
                 :xs "w-4 h-4"
                 :sm "w-6 h-6"
                 :md "w-8 h-8"
                 :lg "w-12 h-12" "w-8 h-8")]
    [:div {:class
           (str "inline-flex items-center gap-2 "
                (or klass "")) :role "status"}
     [:svg.animate-spin
      {:class size-c
       :xmlns "http://www.w3.org/2000/svg"
       :fill "none" :viewBox "0 0 24 24"}
      [:circle {:class "opacity-25"
                :cx "12" :cy "12" :r "10"
                :stroke "currentColor" :stroke-width "4"}]
      [:path {:class "opacity-75"
              :fill "currentColor"
              :d "M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"}]]
     (when label [:span.text-gray-500 label])]))

(defn progress-bar
  [{:as opts :keys [value max-val label variant]
    :or {value 0 max-val 100 variant :default}}]
  (let [klass (:class opts)
        pct (min 100 (max 0 (* 100 (/ value (float max-val)))))
        variant-c (case variant
                    :default "bg-blue-600"
                    :success "bg-green-600"
                    :warning "bg-yellow-600"
                    :danger "bg-red-600" "bg-blue-600")]
    [:div {:class (str "w-full " (or klass ""))}
     (when label [:div.text-sm.text-gray-600.mb-1 label])
     [:div.h-2.w-full.bg-gray-200.rounded-full.overflow-hidden
      [:div {:class (str "h-full rounded-full " variant-c)
             :style {:width (str pct "%")}}]]]))

(defn toast [{:as opts :keys [message type]
              :or {type :info}}]
  (let [klass (:class opts)
        type-c (case type
                 :info "bg-blue-500"
                 :success "bg-green-500"
                 :warning "bg-yellow-500"
                 :danger "bg-red-500" "bg-blue-500")]
    [:div
     {:class
      (str "fixed bottom-4 right-4 px-4 py-3 rounded-lg shadow-lg text-white "
           type-c " " (or klass ""))}
     message]))

(defn confirm-dialog
  [{:as opts :keys [title message confirm-text cancel-text]
    :or {confirm-text "Confirm" cancel-text "Cancel"}}]
  (let [klass (:class opts)]
    [:div
     {:class
      (str "fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 "
           (or klass ""))}
     [:div
      {:class "bg-white rounded-lg shadow-xl p-6 max-w-md w-full mx-4"}
      [:h3
       {:class "text-lg font-semibold text-gray-900 mb-2"} title]
      [:p {:class "text-gray-600 mb-6"} message]
      [:div {:class "flex justify-end space-x-3"}
       [:button
        {:class "px-4 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-md"}
        cancel-text]
       [:button
        {:class "px-4 py-2 text-white bg-blue-600 hover:bg-blue-700 rounded-md"}
        confirm-text]]]]))

(defn grid
  [{:as opts :keys [cols items]
    :or {cols 3}}]
  (let [klass (:class opts)
        col-class
        (case cols
          1 "grid-cols-1"
          2 "grid-cols-1 md:grid-cols-2"
          3 "grid-cols-1 md:grid-cols-2 lg:grid-cols-3"
          4 "grid-cols-1 md:grid-cols-2 lg:grid-cols-4"
          (str "grid-cols-" cols))]
    [:div
     {:class (str "grid gap-4 " col-class " "
                  (or klass ""))}
     (for [item items]
       [:div item])]))

(defn container [{:as opts :keys [title content]}]
  (let [klass (:class opts)
        base "mx-auto max-w-7xl px-4 sm:px-6 lg:px-8"]
    [:div {:class (str base " " (or klass ""))}
     (when title
       [:h1 {:class "text-3xl font-bold text-gray-900 mb-6"}
        title])
     (when content [:div content])]))

(defn sidebar [{:as opts :keys [title items active]}]
  (let [klass (:class opts)
        base "w-64 bg-white border-r border-gray-200 min-h-screen"]
    [:div {:class (str base " " (or klass ""))}
     (when title
       [:div {:class "p-4 border-b border-gray-200"}
        [:h2 {:class "text-lg font-semibold text-gray-800"}
         title]])
     [:nav {:class "p-4"}
      (for [item items]
        (let [is-active (= (:id item) active)]
          [:a
           {:href (:href item "#")
            :class (str "block px-3 py-2 rounded-md text-sm font-medium "
                        (if is-active "bg-blue-100 text-blue-700" "text-gray-700 hover:bg-gray-100"))}
           (:label item)]))]]))

(defn sidebar-item
  [{:as opts :keys [href label]}]
  (let [klass (:class opts)
        icon-val (:icon opts)
        active? (:active opts)
        badge-label (:badge opts)
        active-class (if active? "bg-white/10 text-white" "text-white/70 hover:bg-white/5 hover:text-white")]
    [:a {:href (or href "#")
         :class (str "flex items-center px-4 py-3 rounded-lg transition-colors "
                     active-class " " (or klass ""))}
     (when icon-val [:svg {:class "w-5 h-5 mr-3"
                           :fill "none" :stroke "currentColor"
                           :viewBox "0 0 24 24"}
                     [:path {:stroke-linecap "round"
                             :stroke-linejoin "round"
                             :stroke-width "2" :d icon-val}]])
     [:span label]
     (when badge-label
       [:span {:class "ml-auto px-2 py-0.5 text-xs font-medium rounded-full bg-red-500"}
        badge-label])]))

(defn accordion [{:as opts :keys [items]}]
  (let [klass (:class opts)]
    [:div {:class (str "space-y-2 " (or klass ""))}
     (for [[i item] (map-indexed vector items)]
       (let [id (str "accordion-" i)]
         [:div {:class "border border-gray-200 rounded-lg"}
          [:button
           {:class "w-full px-4 py-3 text-left flex justify-between items-center hover:bg-gray-50"
            :_ (format "on click toggle .hidden on #%s-content"
                       id)}
           [:span {:class "font-medium text-gray-900"}
            (:title item)]
           [:span {:class "transform transition-transform"} "▾"]]
          [:div {:id (str id "-content")
                 :class "hidden px-4 py-3 text-gray-600"}
           (:content item)]]))]))

(defn tree-view [{:as opts :keys [nodes]}]
  (let [klass (:class opts)]
    [:ul {:class (str "space-y-1 " (or klass ""))}
     (for [node nodes]
       [:li
        [:div.flex.items-center.py-1
         (when (:children node)
           [:button
            (merge {:class "w-5 h-5 flex items-center justify-center text-gray-400 hover:text-gray-600"
                    :_ "on click toggle .hidden on next .tree-children"}
                   (csp-nonce-attr))
            "▸"])
         [:span.text-gray-700 (:label node)]]
        (when (:children node)
          [:ul
           {:class "tree-children hidden pl-5 border-l border-gray-200 ml-2"}
           (for [child (:children node)]
             [:li [:span.text-gray-600 (:label child)]])])])]))

(defn tabs-view
  [title tabs pages]
  (let [nav-cls "flex flex-col sm:flex-row border-b border-gray-200"
        btn-cls "tab-button px-12 py-4 text-left font-medium bg-amber-100 hover:bg-amber-200 hover:text-blue-600 transition-colors border-b-2 transition-border"]
    [:div
     [:h1 {:class "text-2xl md:text-3xl font-bold text-gray-900 mb-6"}
      title]
     [:div.bg-white.rounded-xl.shadow-md.overflow-hidden
      [:nav {:class nav-cls}
       (for [tab tabs]
         [:button
          {:class
           (if (:default tab)
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

(defn page-header
  [{:keys [title user show-search notification-count]
    :or {show-search true notification-count 0}}]
  [:header.bg-white.border-b.border-gray-200
   [:div.flex.items-center.justify-between.px-6.py-4
    [:div.flex.items-center.space-x-4
     [:h1.text-xl.font-semibold.text-gray-900 title]]
    [:div.flex.items-center.space-x-4
     ;; Search
     (when show-search
       [:div.relative.hidden.md:block
        (icon :search
              {:size :sm
               :class "absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"})
        [:input
         {:type "search"
          :name "q"
          :id "header-search"
          :placeholder "搜索..."
          :class "w-64 pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"}]])
     ;; Notifications
     [:button
      {:class "relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"}
      (icon :bell {:size :md})
      (when (pos? notification-count)
        [:span.absolute.top-1.right-1.w-2.h-2.bg-red-500.rounded-full])]
     ;; User menu
     (when user
       (user-menu {:user user}))]]])

(defn user-avatar
  [{:as opts :keys [name src size]
    :or {size :md}}]
  (let [klass (:class opts)
        size-c (case size
                 :xs "w-6 h-6"
                 :sm "w-8 h-8"
                 :md "w-10 h-10"
                 :lg "w-12 h-12" "w-10 h-10")
        fallback-bg "bg-gray-300"]
    [:img {:src (or src "/static/img/avatar-default.svg")
           :alt name
           :class (str size-c " rounded-full object-cover "
                       fallback-bg " " (or klass ""))}]))

(defn welcome-banner
  [{:as opts :keys [title subtitle action action-text]}]
  (let [klass (:class opts)]
    [:div {:class
           (str "bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-6 text-white "
                (or klass ""))}
     [:div.flex.items-center.justify-between
      [:div
       [:h2 {:class "text-2xl font-bold mb-1"} title]
       (when subtitle
         [:p {:class "text-white/80"} subtitle])]
      (when action
        [:a {:href action
             :class "px-4 py-2 bg-white text-blue-600 rounded-lg font-medium hover:bg-white/90 transition-colors"}
         action-text])]]))

(defn activity-item
  [{:as opts :keys [title description]}]
  (let [klass (:class opts)
        icon-val (:icon opts)
        icon-bg (get opts :icon-bg "bg-gray-100")
        time-val (:time opts)]
    [:div {:class (str "flex items-start space-x-3 "
                       (or klass ""))}
     [:div {:class (str "w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 " icon-bg)}
      icon-val]
     [:div.flex-1.min-w-0
      [:p.text-sm.text-gray-900 title]
      (when description
        [:p.text-xs.text-gray-500 description])
      (when time-val
        [:p.text-xs.text-gray-400.mt-1 time-val])]]))

(defn activity-feed
  [{:as opts :keys [items]}]
  (let [klass (:class opts)]
    [:div {:class (str "divide-y divide-gray-100 "
                       (or klass ""))}
     (for [item items]
       (activity-item item))]))

(defn quick-action [{:as opts :keys [href label]}]
  (let [klass (:class opts)
        icon-val (:icon opts)]
    [:a {:href (or href "#")
         :class (str "flex flex-col items-center p-4 rounded-xl bg-gray-50 hover:bg-gray-100 transition-colors "
                     (or klass ""))}
     [:div {:class "w-12 h-12 rounded-full flex items-center justify-center mb-3 bg-blue-100"}
      icon-val]
     [:span.text-sm.font-medium.text-gray-900 label]]))

(defn stats-icon
  [{:as opts :keys [color]
    :or {color :blue}}]
  (let [klass (:class opts)
        icon-val (:icon opts)
        color-c
        (case color
          :blue "bg-blue-100 text-blue-600"
          :purple "bg-purple-100 text-purple-600"
          :green "bg-green-100 text-green-600"
          :orange "bg-orange-100 text-orange-600"
          :red "bg-red-100 text-red-600"
          "bg-gray-100 text-gray-600")]
    [:div
     {:class (str "w-12 h-12 rounded-xl flex items-center justify-center "
                  color-c " "
                  (or klass ""))}
     [:span {:class "w-6 h-6"} icon-val]]))

(defn dropdown-menu
  [{:as opts :keys [items]}]
  (let [klass (:class opts)]
    [:div
     (merge {:class (str "absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-50 "
                         (or klass ""))
             :_ "on click outside remove me"}
            (csp-nonce-attr))
     (for [item items]
       [:a {:href (:href item "#")
            :class (str "block px-4 py-2 text-sm "
                        (if (= (:variant item) :danger)
                          "text-red-600" "text-gray-700")
                        " hover:bg-gray-100")}
        (:label item)])]))

(defn notification-badge
  [{:as opts}]
  (let [cnt (:count opts)
        klass (:class opts)]
    (when (pos? cnt)
      [:span
       {:class (str "absolute -top-1 -right-1 w-5 h-5 flex items-center justify-center text-xs font-medium text-white bg-red-500 rounded-full "
                    (or klass ""))}
       (if (> cnt 99) "99+" cnt)])))

(defn page-title
  [{:as opts :keys [title subtitle]}]
  (let [klass (:class opts)]
    [:div {:class (str "mb-6 " (or klass ""))}
     [:h1
      {:class "text-2xl md:text-3xl font-bold text-gray-900"}
      title]
     (when subtitle
       [:p {:class "mt-1 text-gray-500"} subtitle])]))

(defn search-bar
  [{:as opts :keys [placeholder on-post target hx-indicator], :or {placeholder "搜索..."}}]
  (let [klass (:class opts)
        input-cls "w-full pl-10 pr-4 py-2 text-sm bg-gray-50 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        wrapper-cls "relative hidden md:block"
        icon-cls "absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"]
    [:div {:class
           (str "flex items-center "
                (or klass ""))}
     [:div {:class wrapper-cls}
      [:svg {:class icon-cls
             :fill "none"
             :stroke "currentColor"
             :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round"
               :stroke-linejoin "round"
               :stroke-width "2"
               :d "M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"}]]
      (if on-post
        [:input {:type "search"
                 :name "q"
                 :id "input-search"
                 :placeholder placeholder
                 :class input-cls
                 :hx-post on-post
                 :hx-target (or target "body")
                 :hx-indicator (or hx-indicator
                                   "#search-spinner")}]
        [:input {:type "search"
                 :name "q"
                 :id "input-search"
                 :placeholder placeholder
                 :class input-cls}])]]))

(defn data-table
  [{:as opts :keys [columns rows actions empty-message]
    :or {empty-message "暂无数据"}}]
  (let [klass (:class opts)
        th-cls "px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider"
        td-cls "px-4 py-3 text-sm text-gray-900"
        get-val
        (fn [row col]
          (:value (get row (:key col))))
        render-action-btn
        (fn [a]
          [:button
           {:type "button"
            :class "text-blue-600 hover:text-blue-800 text-sm font-medium"
            :on-click (:on-click a)}
           (:label a)])
        render-row
        (fn [row]
          (list [:tr {:class "hover:bg-gray-50"}
                 (for [c columns]
                   [:td {:class td-cls} (get-val row c)])
                 (when actions
                   [:td {:class td-cls}
                    [:div
                     {:class "flex items-center space-x-2"}
                     (for [a actions]
                       (render-action-btn a))]])]))]
    [:div
     {:class (str "overflow-hidden rounded-lg shadow "
                  (or klass ""))}
     [:table {:class "min-w-full divide-y divide-gray-200"}
      [:thead {:class "bg-gray-50"}
       [:tr (for [col columns]
              [:th {:class th-cls}
               (:label col)])
        (when actions
          [:th {:class th-cls} "操作"])]
       (if (empty? rows)
         [:tbody
          [:tr
           [:td {:colspan (count columns)
                 :class "text-center py-8 text-gray-500"}
            empty-message]]]
         [:tbody
          {:class "bg-white divide-y divide-gray-200"}
          (for [row rows] (render-row row))])]]]))
