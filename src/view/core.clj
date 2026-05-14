(ns view.core
  (:require
   [taoensso.timbre :as log]
   [clojure.string :as str]
   [utils.session :refer [current-user]]
   [utils.response :as r]
   [hiccup2.core :as h]
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

(defn badge
  "Badge with variant and size support.

   Options:
   - :variant - :primary, :info, :success, :warning, :danger, :secondary (default :info)
   - :size - :xs, :sm, :md (default :sm)
   - :class - extra CSS classes"
  [text {:keys [variant size class]
         :or {variant :info size :sm}
         :as opts}]
  (let [variant-c (get badge-variant-classes variant)
        size-c (get badge-size-classes size)]
    [:span {:class (merge-classes "inline-flex items-center rounded-full font-medium" variant-c size-c class)}
     text]))

(defn callout
  [svrt tip content]
  (let [[color sign]
        (case svrt
          :info
          ["bg-blue-50/50 border-blue-500 text-blue-700" "ℹ"]
          :success
          ["bg-green-50/50 border-green-500 text-green-700" "✔"]
          :warning
          ["bg-yellow-50/50 border-yellow-500 text-yellow-700" "⚠"]
          :danger
          ["bg-red-50/50 border-red-500 text-red-700" "✘"]
          ["bg-blue-50/50 border-blue-500 text-blue-700" "ℹ"])]
    [:div.border-l-4.p-4.rounded-md.mb-4.flex.items-start.gap-2
     {:class color}
     [:span.font-bold sign]
     [:div
      [:h4.font-semibold tip]
      [:p content]]]))

(defn showbox
  [subject color link remark]
  (let [out-cls "p-6 bg-white rounded-lg shadow hover:shadow-lg hover:scale-105 transition-transform"]
    [:div {:class out-cls}
     [:div.flex.items-start.space-x-4
      [:div.flex-shrink-0.w-8.h-8.rounded {:class color}]
      [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
       [:div.flex.justify-between
        [:strong subject]
        (when link
          [:a {:class "text-blue-500 hover:underline"
               :href (:href link)}
           (:txt link)])]
       [:span remark]]]]))

(defn change-page-size [req]
  (let [referer (get-in req [:headers "referer"])
        ps (-> (get-in req [:params "page-size"])
               parse-long)
        session (assoc (:session req) :page-size ps)]
    (assoc (r/redirect referer)
           :session session)))

(defn svrt-cls
  [severity]
  (case severity
    "info" "bg-blue-100 text-blue-800"
    "success" "bg-green-500 text-white"
    "warning" "bg-yellow-500 text-white"
    "danger" "bg-red-500 font-bold text-white"
    "nil"))

(defn type-of
  [v]
  (cond (nil? v) :nil
        (keyword? v) :class
        (re-find #"id=$" v) :href-id
        :else :href-self))

(defn bs-to-tw
  [cls]
  (case cls
    :rownum "italic font-medium text-amber-900"
    :text-success "text-green-500"
    :text-warning "text-yellow-500"
    :text-danger "text-red-500 font-bold"
    :text-primary "text-blue-500"
    :text-info "text-cyan-500"))

(defn ratio-class
  [r]
  (let [f (read-string r)]
    (cond (> f 95) :text-success
          (> f 85) :text-warning
          :else :text-danger)))

(defn severity-class
  [s]
  (case s
    "critical" :text-danger
    "warn" :text-warning
    :text-primary))

(defn online-class
  [s]
  (case s
    "在线" :text-success
    "离线" :text-danger
    :text-primary))

(defn onu-state-class
  [s]
  (case s
    "working" :text-success
    "OffLine" :text-warning
    "LOS" :text-danger
    "DyingGasp" :text-danger
    :text-warning))

(defn severity-show
  [s]
  (case s
    "critical" "关键"
    "warn" "重要"
    "普通"))

(defn onu-state-show
  [s]
  (case s
    "working" "正常"
    "OffLine" "离线"
    "LOS" "光信号丢失"
    "DyingGasp" "掉电"
    s))

(defn bool-class
  [v t]
  (let [b (if t v (not v))]
    (if b :text-danger :text-success)))

(defn td-class
  [d k v]
  (let [default "border-0 p-2"
        cls (case v
              :ratio (bs-to-tw (ratio-class (k d)))
              :online (bs-to-tw (online-class (k d)))
              :onu-state (bs-to-tw (onu-state-class (k d)))
              :severity (bs-to-tw (severity-class (k d)))
              :true (bs-to-tw (bool-class (k d) true))
              :false (bs-to-tw (bool-class (k d) false))
              :ch-cut ""
              (bs-to-tw v))
        show (cond
               (boolean? (k d)) (if (k d) "是" "否")
               (= :severity v) (severity-show (k d))
               (= :onu-state v) (onu-state-show (k d))
               (= :ch-cut v) (str/replace (k d) " " "")
               :else (k d))]
    [:td {:class (str default " " cls)} show]))

(defn alert
  [req]
  (let [msg (get-in req [:flash :message])
        severity (:severity msg)
        msg (:message msg)]
    (when msg
     (let [cls (svrt-cls severity)]
       [:div {:id "flashMessage"
              :class (str cls " fixed top-20 left-1/2 transform
                      -translate-x-1/2 text-white
                      px-4 py-2 rounded shadow-lg
                      transition-opacity duration-300 flex
                      items-center justify-between min-w-[250px] max-w-md")
              :_ "on load wait 5s then add .opacity-0"}
        [:span msg]
        [:button
         {:_ "on click add .opacity-0 to #flashMessage"
          :class "ml-4 text-white font-bold"} "x"]]))))

(defn input-filter
  []
  [:input
   {:class "w-full my-2 p-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
    :type "text"
    :placeholder "请输入过滤信息"
    :_ "on input show <tbody>tr/> in <table/>
                    when its textContent.toLowerCase()
                    contains my value.toLowerCase()"}])

(defn code-page [_req title content]
  (str
   (h/html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:title title]
      [:link {:href "/statc/css/tw_out.css" :rel "stylesheet"}]
      [:body.bg-gray-50.min-h-screen.flex.flex-col.justify-center.p-4
       [:div.max-w-8xl.mx-auto.p-4.bg-white.rounded-lg.shadow-md
        [:div.p-4.border-b.border-gray-200.flex.justify-between.items-center
         [:h2.text-xl.font-semibold.text-gray-800 title]]
        [:div.p-4
         [:pre.bg-gray-50.p-3.border.rounded
          [:code content]]]]]]])))

(defn table-view
  [title headers keys data]
  (let [a-cls "text-blue-600 hover:text-blue-800 hover:underline transition-color"]
   [:div.my-4
    [:h1.text-2xl.font-bold.mb-6 title]
    [:div.bg-white.rounded.shadow.overflow-hidden
     [:table.w-full.border-collapse.border.border-gray-300
      [:thead
       [:tr.bg-blue-800
        (for [h headers]
          [:th.border.p-2.text-white.font-bold h])]]
      [:tbody
       (for [d data]
         [:tr {:class "odd:bg-blue-100 hover:bg-blue-200"}
          (for [[k v] keys]
            (if (map? d)
              (case (type-of v)
                :href-id
                [:td.border-0.p-2
                 [:a
                  {:href (str v (:db/id d))
                   :class (if (contains?
                               #{:button :button-new} k)
                            "text-white bg-blue-600 hover:bg-blue-800 px-4 py-2 rounded"
                            a-cls)
                   :target (if (= :button-new k)
                             "_blank"
                             "")}
                  (k d)]]
                :href-self
                [:td.border-0.p-2
                 [:a {:class a-cls :href (str v (k d))}
                  (k d)]]
                :class
                (td-class d k v)
                [:td.border-0.p-2 (k d)])
              (if v
                [:td.border-0.p-2
                 [:a {:class a-cls :href (str v (d (count headers)))}
                  (d k)]]
                [:td.border-0.p-2 (d k)])))])]]]]))

(defn table-view-with-rownum
  [title headers keys data]
  (let [headers (into ["行号"] headers)
        cnt (count data)
        [keys data]
        (if (map? (first data))
          [(into [[:rownum :rownum]] keys)
           (mapv #(assoc %2 :rownum (str "#" %1))
                 (range 1 (inc cnt)) data)]
          [(into [[0 :rownum]]
                 (mapv #(vector % nil) (range 1 (count headers))))
           (mapv #(into [(str "#" %1)] %2)
                 (range 1 (inc cnt)) data)])]
    (table-view title headers keys data)))

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
  [{:keys [type label name size variant disabled required placeholder value class id error]
    :or {type "text" size :md variant :default required false}
    :as opts}]
  (let [size-c (get input-size-classes size)
        variant-c (case variant
                     :error "border-red-500 focus:ring-red-500"
                     :default "border-gray-300 focus:ring-blue-500")
        base "w-full bg-white border rounded-md focus:outline-none focus:ring-2 transition"
        disabled-c (when disabled "opacity-50 cursor-not-allowed")
        label-c (str "block text-sm font-medium mb-1"
                    (if (= variant :error) " text-red-600" " text-gray-700"))]
    [:div.my-2
     (when label [:label {:for name :class label-c} label])
     [:input
      {:type type
       :name name
       :id id
       :class (merge-classes base size-c variant-c disabled-c (when disabled "cursor-not-allowed"))
       :placeholder placeholder
       :value value
       :required required
       :disabled disabled
       :aria-disabled disabled}]
     (when error [:span.text-sm.text-red-600.mt-1 error])]))

(defn form-submit-btn
  [v]
  (let [cls "w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-md cursor-pointer"]
    [:div.my-2
     [:input {:class cls :type "submit" :value v}]]))

(defn form-select [{:keys [options prompt disabled size class error]
                  :or {size :md}
                  :as opts}]
  (let [size-c (get {:xs "text-xs" :sm "text-sm" :md "text-base" :lg "text-lg"} size)
        base "w-full bg-white border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition appearance-none"
        disabled-c (when disabled "opacity-50 cursor-not-allowed")
        label-c "block text-sm font-medium text-gray-700 mb-1"]
    [:div.my-2
     (when (:label opts)
       [:label {:for (:name opts) :class label-c} (:label opts)])
     [:select
      {:name (:name opts)
       :disabled disabled
       :class (str base " " size-c " " disabled-c " " (or class ""))}
      (when prompt [:option {:value ""} prompt])
      (for [[v l] options]
        [:option {:value v} l])]
     (when error [:span.text-sm.text-red-600.mt-1 error])]))

(defn form-checkbox [{:keys [label name value checked disabled class]
                     :or {checked false}
                     :as opts}]
  (let [base "w-full bg-white border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 transition appearance-none"
        disabled-c (when disabled "opacity-50 cursor-not-allowed")]
    [:div.my-2.flex.items-center
     [:input
      {:type "checkbox"
       :name name
       :id (:id opts name)
       :value (or value "on")
       :checked checked
       :disabled disabled
       :class (str "h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-blue-500 " disabled-c " " (or class ""))}]
     (when label
       [:label {:for (:id opts name) :class "ml-2 block text-sm text-gray-700"} label])]))

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
                    (str btn-cls
                         " text-blue-500 border-blue-500")
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

