(ns minweb.view.admin.core
  (:require
   [clojure.string :as str]
   [hiccup2.core :refer [html]]
   [selmer.parser :refer [render-file]]
   [minweb.common :refer [env Tpl-root]]
   [minweb.utils.session :refer [current-user]]
   [minweb.view.core :as c :refer [log-user]]
   [minweb.database.user :as user]
   [minweb.view.admin.user :as usradmin]))

(def Mgmt-items
  {:usr "用户"})

(def Icons
  [[:svg.h-6.w-6.text-white
    {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
     :stroke-width "1.5" :stroke "currentColor"}
    [:path {:stroke-linecap "round" :stroke-linejoin "round"
            :d "M17.982 18.725A7.488 7.488 0 0 0 12 15.75a7.488 7.488 0 0 0-5.982 2.975m11.963 0a9 9 0 1 0-11.963 0m11.963 0A8.966 8.966 0 0 1 12 21a8.966 8.966 0 0 1-5.982-2.275M15 9.75a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"}]]])

(def Menu-items
  (for [[k v] Mgmt-items]
    [(str v "管理") (str "/mgmt/" (name k))]))

(def Menu
  (let [merged (map conj Menu-items Icons)]
    (html
     [:nav
      [:ul
       (for [[name url icon] merged]
         [:li.mb-3.flex.items-center.gap-2
          icon
          [:a {:class "hover:text-gray-400"
               :href url} name]])
       [:li.mt-12.flex.items-center.gap-2
        [:svg.h-6.w-6.text-white
         {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
          :stroke-width "1.5" :stroke "currentColor"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round"
                 :d "M15.75 19.5 8.25 12l7.5-7.5"}]]
        [:a {:class "hover:text-gray-400"
             :href "/"} "返回首页"]]]])))

(def User-header
  ["Email" "姓名" "权限" "操作"])

(def User-keys
  [[:user/email nil]
   [:user/name nil]
   [:privs nil]])

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

(defn render
  [m]
  (render-file (str Tpl-root "/page_main.tpl") m))

(defn del-style
  [t id]
  (let [_disable-cls "bg-gray-400 cursor-not-allowed"
        enable-cls "bg-red-500"]
    (case t
      :usr (let [u (user/the-user id)]
             {:disabled? false :class enable-cls
              :confirm (str "确定删除用户:" (:user/name u))}))))

(defn table
  [type cols keys data]
  [:div.bg-white.p-4.rounded.shadow
   [:table.w-full.border-collapse.border.border-gray-300
    [:thead
     [:tr.bg-gray-600
      (for [col cols]
        [:th.border.p-2.text-white.font-bold col])]]
    [:tbody
     (for [d data]
       [:tr.odd:bg-gray-100.hover:bg-gray-200
        (for [[k v] keys]
          (case (type-of v)
            :class (td-class d k v)
            [:td.border-0.p-2 (str (k d))]))
        [:td.border-0.p-2
         [:button.bg-green-500.text-white.px-2.py-1.rounded
          {:hx-get (str "/show-modal/"
                        (name type) "/" (:db/id d))
           :hx-target "#div-modal"}
          "编辑"]
         (let [{:keys [disabled? confirm] :as style} (del-style type (:db/id d))]
           [:form {:method "post"
                   :style "display:inline"
                   :class "inline"
                   :hx-post (str "/delete-it/" (name type) "/" (:db/id d))
                   :hx-confirm confirm
                   :hx-target "closest tr"
                   :hx-swap "outerHTML swap:1s"}
            (c/csrf-token)
            [:button.text-white.px-2.py-1.rounded.ml-2
             {:type "submit"
              :disabled disabled?
              :class (:class style)}
             "删除"]])]])]]])

(defn table-with-rownum
  [type cols keys data]
  (let [cols (into ["行号"] cols)
        cnt (count data)
        [keys data]
        (if (map? (first data))
          [(into [[:rownum :rownum]] keys)
           (mapv #(assoc %2 :rownum (str "#" %1))
                 (range 1 (inc cnt)) data)]
          [(into [[0 :rownum]]
                 (mapv #(vector % nil) (range 1 (count cols))))
           (mapv #(into [(str "#" %1)] %2)
                 (range 1 (inc cnt)) data)])]
    (table type cols keys data)))

(defn index
  [req]
  (let [title (str (env :title) "后台系统管理平台")
        menu Menu
        user (current-user req)
        content (html [:p (str (:user/name user) ", 欢迎进入管理后台")])]
    (render {:title title :company (env :company) :menu menu :content content})))

(defn fetch-data
  [_req type]
  (case type
    :usr (usradmin/fetch-user)))

(defn show-modal
  [req]
  (let [type (get-in req [:params :type])]
    (case (keyword type)
      :usr (usradmin/usr-modal-edit req))))

(defn layout
  [req]
  (let [type (keyword (get-in req [:params :type] "usr"))
        text (Mgmt-items (keyword type))
        flash (html (c/alert req))
        div-filter
        (html
         [:div.flex.items-center.justify-between.mb-4
          [:input.px-4.py-2.rounded-md.border.border-gray-300.focus:border-blue-500.transition
           (merge {:type "text"
                   :class "w-1/3"
                   :placeholder "过滤..."
                   :_ "on input show <tbody>tr/> in <table/>
                       when its textContent.toLowerCase()
                       contains my value.toLowerCase()"}
                  (c/csp-nonce-attr))]
          [:button.bg-blue-500.text-white.px-4.py-2.rounded.mb-4
           {:hx-get (str "/show-modal/" (name type) "/-1")
            :hx-target "#div-modal"}
           (str "添加" text)]])
        data (fetch-data req type)
        [header keys]
        (case type
          :usr [User-header User-keys])
        table-html (html (table-with-rownum type header keys data))]
    (render {:title (str text "管理") :menu Menu
             :flash flash
             :content (str div-filter table-html)
             :company (env :company)})))

(defn delete-it
  [req]
  (let [type (keyword (get-in req [:params :type]))
        id (parse-long (get-in req [:params :id]))]
    (case type
      :usr (when-let [u (user/the-user id)]
             (user/delete! id)
             (log-user
              req (str "user:" (:user/email u) " deleted.")))))
  "")

(defn validate-uniq
  [t v]
  (case t
    :usr (if (user/exists? v)
           (str "Email:" v " 已存在")
           "")))

(defn val-uniq
  [req]
  (let [type (keyword (get-in req [:params :type]))
        v (case type
            :usr (str/trim (get-in req [:params "email"])))]
    (validate-uniq type v)))
