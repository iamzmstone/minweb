(ns view.admin.core
  (:require
   [clojure.string :as str]
   [hiccup2.core :refer [html]]
   [selmer.parser :refer [render-file]]
   [common :refer [env Tpl-root]]
   [utils.session :refer [current-user]]
   [view.core :as c :refer [log-user type-of td-class]]
   [database.user :as user]
   [view.admin.user :as usradmin]))

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
  (html
   [:nav
    [:ul
     (for [[name url icon] (map #(conj %1 %2) Menu-items Icons)]
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
           :href "/"} "返回首页"]]]]))

(def User-header
  ["Email" "姓名" "权限" "操作"])

(def User-keys
  [[:user/email nil]
   [:user/name nil]
   [:privs nil]])

(defn render
  [m]
  (render-file (str Tpl-root "/page_main.tpl") m))

(defn del-style
  [t id]
  (let [disable-cls "bg-gray-400 cursor-not-allowed"
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
         [:button.text-white.px-2.py-1.rounded.ml-2
          (let [{:keys [disabled? class confirm]} (del-style type (:db/id d))]
           {:hx-get (str "/delete-it/" (name type) "/" (:db/id d))
            :hx-confirm confirm
            :disabled disabled?
            :class class
            :hx-target "closest tr"
            :hx-swap "outerHTML swap:1s"})
          "删除"]]])]]])

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
           {:type "text"
            :class "w-1/3"
            :placeholder "过滤..."
            :_ "on input show <tbody>tr/> in <table/>
                when its textContent.toLowerCase()
                contains my value.toLowerCase()"}]
          [:button.bg-blue-500.text-white.px-4.py-2.rounded.mb-4
           {:hx-get (str "/show-modal/" (name type) "/-1")
            :hx-target "#div-modal"}
           (str "添加" text)]])
        data (fetch-data req type)
        [header keys]
        (case type
          :usr [User-header User-keys])
        table (html (table-with-rownum type header keys data))]
    (render {:title (str text "管理") :menu Menu
             :flash flash
             :content (str div-filter table)
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
