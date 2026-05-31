(ns minweb.view.layout
  (:require
   [minweb.middleware.auth :refer [authorized?]]
   [minweb.common :refer [env]]
   [hiccup2.core :as h]
   [minweb.utils.session :as s]
   [minweb.utils.response :as r]
   [minweb.view.core :as c]))

(def Title (or (env :title) "Min's web app framework"))
(def App-name (or (env :app-name) "Minweb"))
(def Menu
  [["/admin" "后台管理"]
   ["/chpwd" "修改密码"]
   ["/logout" "登出"]
   ["/about" "关于"]])

(defn paginator [req current-page pages base-url]
  (let* [sizes [20 40 60 80]
         sel-cls "mx-1 border rounded w-16 text-center text-gray-700 focus:ring-blue-500 focus:border-blue-500"
         a-cls "relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50"
         ps (s/cur-page-size req)
         q (:query-params req)
         next (when (not= current-page pages)
                (str base-url "?"
                     (r/query-params->url
                      (merge q {"page" (+ current-page 1)}))))
         previous (when (not= current-page 1)
                    (str base-url "?"
                         (r/query-params->url
                          (merge q {"page" (- current-page 1)}))))]
    [:div.mt-6.flex.justify-between.items-center
     [:div
      [:form {:method "post" :action "/change-page-size"}
       (c/csrf-token)
       [:span.text-sm.text-gray-700 "显示"]
       [:select (merge {:class sel-cls}
                       {:id "sel-ps"
                        :name "page-size"
                        :onchange "this.form.submit()"})
        (for [s sizes]
          [:option {:value s :selected (= ps s)}
           s])]]]
     [:nav.relative.z-0.inline-flex.shadow-sm.-space-x-px
      {:aria-label "Pagination"}
      [:a (merge {:class a-cls}
                 (if (nil? previous)
                   {:class "hidden"}
                   {:href previous}))
       [:span.sr-only "上一页"]
       [:svg.w-4.h-r {:viewBox "0 0 20 20" :fill "currentColor"
                      :aria-hidden "true"}
        [:path {:fill-rule "evenodd"
                :clip-rule "evenodd"
                :d "M12.707 5.293a1 1 0 010 1.414L9.414 10l3.293 3.293a1 1 0 01-1.414 1.414l-4-4a1 1 0 010-1.414l4-4a1 1 0 011.414 0z"}]]]
      [:span.text-blue-500.px-2.py-2.border.border-gray-300.font-semibold
       (format "%d/%d" current-page pages)]
      [:a (merge {:class a-cls}
                 (if (nil? next)
                   {:class "hidden"}
                   {:href next}))
       [:span.sr-only "下一页"]
       [:svg.w-4.h-r {:viewBox "0 0 20 20" :fill "currentColor"
                      :aria-hidden "true"}
        [:path {:fill-rule "evenodd"
                :clip-rule "evenodd"
                :d "M7.293 14.707a1 1 0 010-1.414L10.586 10 7.293 6.707a1 1 0 011.414-1.414l4 4a1 1 0 010 1.414l-4 4a1 1 0 01-1.414 0z"}]]]]]))

(defn search-form
  []
  (let [input-cls "bg-white rounded-l-md border border-gray-300 p-2 focus:outline-none focus:ring-2 focus:ring-blue-400 focus:border-blue-500 transition text-black"
        btn-cls "bg-green-500 hover:bg-green-600 text-white p-2 rounded-t-md transition"]
    [:form
     {:class "flex mt-4 lg:mt-0 lg:ml-4"
      :_ "on submit
            set q to (value of the #input-q)
            if q.length is less than 2
              alert('请输入至少2个字符') then halt
            end"}
     (c/csrf-token)
     [:input
      {:class input-cls :type "search" :name "q" :id "input-q" :required true}]
     [:button
      {:hx-post "/search"
       :hx-target "body"
       :hx-indicator "#search-spinner"
       :class btn-cls
       :type "submit"} "搜索"]
     [:img.htmx-indicator
      {:id "search-spinner"
       :src "/static/img/loading.svg"
       :alt "Searching..."}]]))

(defn navbar
  [menu form-search]
  (let [nav-cls "w-full lg:flex lg:items-center lg:w-auto hidden transition-all duration-300 ease-in-out"
        ul-cls "flex flex-col lg:flex-row lg:space-x-4"
        a-cls "block py-2 hover:text-green-400 transition-colors"]
    [:nav.bg-gray-900.text-white
     [:div.max-w-7xl.mx-auto.flex.flex-wrap.items-center.justify-between.p-4
      [:a.text-2xl.font-bold {:href "/"} App-name]
      [:button
       {:class "block lg:hidden" :id "menu-toggle"
        :_ "on click
               toggle .hidden on #navbar"}
       [:svg.w-6.h-6
        {:fill "none" :stroke "currentColor"
         :viewBox "0 0 24 24"}
        [:path
         {:stroke-linecap "round"
          :stroke-linejoin "round"
          :stroke-width "2"
          :d "M4 6h16M4 12h16M4 18h16"}]]]
      [:div#navbar
       {:class nav-cls}
       [:ul {:class ul-cls}
        (for [m menu]
          [:li
           [:a {:class a-cls
                :href (first m)}
            (last m)]])]
       (if form-search form-search "")]]]))

(defn nav-view [req]
  (let [user (s/current-user req)
        form-search
        (when
         (or
          (= :admin (:user/role user))
          (contains?
           (into #{} (:user/privs user))
           :search))
          (search-form))
        menu
        (when user
          (if (= :admin (:user/role user))
            Menu
            (->>
             Menu
             (filter
              #(authorized? user (first %))))))]
    (when user
      (navbar menu form-search))))

(defn layout [req & body]
  (str
   "<!DOCTYPE html>"
   (h/html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content
              "width=device-width, initial-scale=1"}]
      [:title Title]
      [:link {:href "/static/css/tw_out.css" :rel "stylesheet"}]]
     [:body.min-h-screen.flex.flex-col
      [:div.flex-1
       (nav-view req)
       (c/alert req)
       [:div.mx-8.mt-2
        body]]
      [:footer.bg-gray-100.py-4.mt-4
       [:div.max-w-7xl.mx-auto.text-center.text-gray-500.text-sm
        "© 2026 iamzmstone"]]
      [:script {:src "/static/js/htmx.min.js"}]
      [:script {:src "/static/js/hyperscript.min.js"}]]])))

(defn modal [& {:keys [id title content actions]}]
  [:div.modal.fade {:tabindex -1 :id id}
   [:div.modal-dialog
    [:div.modal-content
     [:div.modal-header
      [:h5.modal-title title]
      [:button.btn-close {:type "button" :data-bs-dismiss "modal"}]]
     [:div.modal-body content]
     [:div.modal-footer actions]]]])

(defn dashboard-sidebar-item [item]
  [:li
   [:a {:href (:href item)
        :class (str "flex items-center gap-3 px-4 py-3 rounded-lg transition-colors "
                    (if (:active item)
                      "bg-purple-600 text-white"
                      "text-gray-300 hover:bg-gray-700 hover:text-white"))}
    (when (:icon item)
      (if (keyword? (:icon item))
        (c/icon (:icon item) {:size :sm})
        [:span (:icon item)]))
    [:span (:label item)]]])

(defn dashboard-sidebar [title items]
  [:aside {:class "flex flex-col w-64 bg-gray-900 text-white min-h-screen"}
   [:div {:class "p-6 border-b border-gray-700"}
    [:h2 {:class "text-xl font-bold"} title]]
   [:nav {:class "flex-1 px-4 py-4"}
    [:ul {:class "space-y-2"}
     (for [item items]
       (dashboard-sidebar-item item))]]])

(defn dashboard-layout
  [_req sidebar-items body & {:keys [header-title header-user header-search header-notifications]
                              :or {header-search true}}]
  (str
   "<!DOCTYPE html>"
   (h/html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title Title]
      [:link {:href "/static/css/tw_out.css" :rel "stylesheet"}]]
     [:body.min-h-screen.bg-gray-100
      [:div.flex
       (dashboard-sidebar App-name sidebar-items)
       [:div.flex-1.flex.flex-col
        (c/page-header {:title (or header-title "")
                        :user header-user
                        :show-search header-search
                        :notification-count (or header-notifications 0)})
        [:main.flex-1.p-6
         body]]]
      [:script {:src "/static/js/htmx.min.js"}]
      [:script {:src "/static/js/hyperscript.min.js"}]]])))

(defn error-page [status title message]
  (str
   "<!DOCTYPE html>"
   (h/html
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport"
              :content "width=device-width, initial-scale=1"}]
      [:title title]
      [:link {:href "/static/css/tw_out.css"
              :rel "stylesheet"}]]
     [:body {:class "min-h-screen flex items-center justify-center bg-gray-100"}
      [:div {:class "bg-white rounded-xl shadow-lg p-12 max-w-md w-full text-center"}
       [:p {:class "text-7xl font-bold text-gray-900 mb-4"}
        status]
       [:p {:class "text-gray-500 text-lg mb-6"} message]
       [:a {:href "/" :class "text-blue-600 hover:underline"}
        "返回首页"]]]])))