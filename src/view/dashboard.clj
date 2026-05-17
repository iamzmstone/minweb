(ns view.dashboard
  (:require
   [view.layout :refer [dashboard-layout]]
   [utils.session :as s]))

;; Icon functions return Hiccup SVG vectors
(defn icon-dashboard []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"}]])

(defn icon-user []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"}]])

(defn icon-resource []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"}]])

(defn icon-analytics []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"}]])

(defn icon-message []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"}]])

(defn icon-setting []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z M15 12a3 3 0 11-6 0 3 3 0 016 0z"}]])

(defn icon-help []
  [:svg {:class "w-5 h-5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
   [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
           :d "M8.228 9c.549-1.165 2.03-2 3.772-2 2.21 0 4 1.343 4 3 0 1.4-1.278 2.575-3.006 2.907-.542.104-.994.54-.994 1.093m0 3h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"}]])

(defn stat-card [icon color title value change]
  [:div {:class "bg-white rounded-xl p-6 shadow-md"}
   [:div {:class "flex items-center justify-between mb-4"}
    [:div {:class (str "w-12 h-12 rounded-xl flex items-center justify-center bg-" color "-100")}
     icon]
    [:span {:class (str "px-2 py-1 text-xs font-medium rounded-full bg-" color "-100 text-" color "-600")}
     change]]
   [:p {:class "text-sm text-gray-500"} title]
   [:p {:class "text-2xl font-bold text-gray-900"} value]])

(defn quick-action [href icon color label]
  [:a {:href href :class "flex flex-col items-center p-4 rounded-xl bg-gray-50 hover:bg-gray-100 transition-colors"}
   [:div {:class (str "w-12 h-12 rounded-full bg-" color "/10 flex items-center justify-center mb-3")}
    icon]
   [:span {:class "text-sm font-medium text-gray-900"} label]])

(defn activity-item [icon color title desc time]
  [:div {:class "flex items-start space-x-3"}
   [:div {:class (str "w-8 h-8 rounded-full bg-" color "-100 flex items-center justify-center flex-shrink-0")}
    icon]
   [:div {:class "flex-1 min-w-0"}
    [:p {:class "text-sm text-gray-900"} title]
    [:p {:class "text-xs text-gray-500"} desc]
    [:p {:class "text-xs text-gray-400 mt-1"} time]]])

(defn page [req]
  (let [user (s/current-user req)]
    (dashboard-layout
     req
     [{:href "/dashboard" :label "控制台" :icon (icon-dashboard) :active true}
      {:href "/users" :label "用户" :icon (icon-user)}
      {:href "/resources" :label "资源" :icon (icon-resource)}
      {:href "/analytics" :label "数据分析" :icon (icon-analytics)}
      {:href "/messages" :label "消息" :icon (icon-message) :badge "3"}
      {:section "设置"}
      {:href "/settings" :label "设置" :icon (icon-setting)}
      {:href "/help" :label "帮助" :icon (icon-help)}]
     [:div {:class "p-6"}
      ;; Welcome banner
      [:div {:class "mb-6"}
       [:div {:class "bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-6 text-white"}
        [:div {:class "flex items-center justify-between"}
         [:div
          [:h2 {:class "text-2xl font-bold mb-1"} (str "欢迎回来，" (:user/name user) "！")]
          [:p {:class "text-white/80"} "以下是您项目今天的概况。"]]
         [:a {:href "/users/new" :class "px-4 py-2 bg-white text-blue-600 rounded-lg font-medium hover:bg-white/90 transition-colors"} "添加用户"]]]]

    ;; Stats cards
    [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-6"}
     (stat-card
      [:svg {:class "w-6 h-6 text-blue-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"}]]
      "blue"
      "用户总数"
      "12,345"
      "+12%")
     (stat-card
      [:svg {:class "w-6 h-6 text-purple-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"}]]
      "purple"
      "资源"
      "1,234"
      "+8%")
     (stat-card
      [:svg {:class "w-6 h-6 text-green-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"}]]
      "green"
      "收入"
      "$98.5K"
      "+24%")
     (stat-card
      [:svg {:class "w-6 h-6 text-orange-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
       [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"}]]
      "orange"
      "增长"
      "23.5%"
      "+5%")]

    ;; Quick actions and activity
    [:div {:class "grid lg:grid-cols-3 gap-6"}
     [:div {:class "lg:col-span-2 bg-white rounded-xl shadow-md"}
      [:div {:class "px-6 py-4 border-b border-gray-200"}
       [:h3 {:class "text-lg font-semibold text-gray-900"} "快捷操作"]]
      [:div {:class "p-6 grid grid-cols-2 md:grid-cols-4 gap-4"}
       (quick-action
        "/users/new"
        [:svg {:class "w-6 h-6 text-blue-500" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"}]]
        "blue"
        "添加用户")
       (quick-action
        "/resources/new"
        [:svg {:class "w-6 h-6 text-purple-500" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 6v6m0 0v6m0-6h6m-6 0H6"}]]
        "purple"
        "新增资源")
       (quick-action
        "/reports/new"
        [:svg {:class "w-6 h-6 text-green-500" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M9 17v-2m3 2v-4m3 4v-6m2 10H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}]]
        "green"
        "生成报告")
       (quick-action
        "/settings"
        [:svg {:class "w-6 h-6 text-orange-500" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"}]]
        "orange"
        "设置")]]

     [:div {:class "bg-white rounded-xl shadow-md"}
      [:div {:class "px-6 py-4 border-b border-gray-200"}
       [:h3 {:class "text-lg font-semibold text-gray-900"} "最近活动"]]
      [:div {:class "p-6 space-y-4"}
       (activity-item
        [:svg {:class "w-4 h-4 text-green-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M5 13l4 4L19 7"}]]
        "green"
        "新用户注册"
        "Alice Smith 以编辑者身份加入"
        "2 分钟前")
       (activity-item
        [:svg {:class "w-4 h-4 text-blue-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"}]]
        "blue"
        "资源已更新"
        "营销 Q4 活动"
        "1 小时前")
       (activity-item
        [:svg {:class "w-4 h-4 text-yellow-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"}]]
        "yellow"
        "系统提醒"
        "服务器维护已安排"
        "3 小时前")
       (activity-item
        [:svg {:class "w-4 h-4 text-purple-600" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"}]]
        "purple"
        "新报告已生成"
        "月度数据分析摘要"
        "5 小时前")]]]

    [:div {:id "toast" :class "fixed bottom-4 right-4 z-50"}]]
    :header-user user
    :header-search true)))