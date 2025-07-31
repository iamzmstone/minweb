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

(defn badge
  [type text]
  (let [color
        (case type
          :primary "bg-purple-200 text-purple-800"
          :info "bg-blue-200 text-blue-800"
          :success "bg-green-200 text-green-800"
          :warning "bg-yellow-200 text-yellow-800"
          :danger "bg-red-200 text-red-500"
          "bg-indigo-200 text-indigo-800")]
    [:span.inline-flex.items-center.rounded-full.text-xs.font-medium.font-bold
     {:class (str color " px-2.5 py-0.5")}
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
  [{:keys [type label id name value required placeholder]
    :or {required false
         value ""
         placeholder ""}}]
  (let [input-cls "w-full px-4 py-2 bg-white border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"]
    [:div.my-2
     [:label.block.text-sm.font-medium.text-gay-700.mb-1
      {:for name} label]
     [:input {:class input-cls
              :type type
              :name name
              :id id
              :value value
              :required required
              :placeholder placeholder}]]))

(defn form-submit-btn
  [v]
  (let [cls "w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded-md cursor-pointer"]
    [:div.my-2
     [:input {:class cls :type "submit" :value v}]]))
