(ns view.dashboard
  (:require
   [view.layout :refer [dashboard-layout]]
   [view.core :as c]
   [utils.session :as s]))

(def sidebar-items
  [{:href "/dashboard" :label "控制台" :icon :dashboard :active true}
   {:href "/users" :label "用户" :icon :user}
   {:href "/resources" :label "资源" :icon :resource}
   {:href "/analytics" :label "数据分析" :icon :analytics}
   {:href "/messages" :label "消息" :icon :message :badge "3"}
   {:section "设置"}
   {:href "/settings" :label "设置" :icon :settings}
   {:href "/help" :label "帮助" :icon :help}])

(defn page [req]
  (let [user (s/current-user req)]
    (dashboard-layout
     req
     sidebar-items
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
     (c/stat-card {:label "用户总数" :value "12,345" :trend :up :trend-value "+12%"
                   :icon (c/icon :user)})
     (c/stat-card {:label "资源" :value "1,234" :trend :up :trend-value "+8%"
                   :icon (c/icon :resource)})
     (c/stat-card {:label "收入" :value "$98.5K" :trend :up :trend-value "+24%"
                   :icon (c/icon :analytics)})
     (c/stat-card {:label "增长" :value "23.5%" :trend :up :trend-value "+5%"
                   :icon (c/icon :message)})]

    ;; Quick actions and activity
    [:div {:class "grid lg:grid-cols-3 gap-6"}
     [:div {:class "lg:col-span-2 bg-white rounded-xl shadow-md"}
      [:div {:class "px-6 py-4 border-b border-gray-200"}
       [:h3 {:class "text-lg font-semibold text-gray-900"} "快捷操作"]]
      [:div {:class "p-6 grid grid-cols-2 md:grid-cols-4 gap-4"}
       (c/quick-action {:href "/users/new" :icon (c/icon :user) :label "添加用户"})
       (c/quick-action {:href "/resources/new" :icon (c/icon :resource) :label "新增资源"})
       (c/quick-action {:href "/reports/new" :icon (c/icon :analytics) :label "生成报告"})
       (c/quick-action {:href "/settings" :icon (c/icon :settings) :label "设置"})]]

     [:div {:class "bg-white rounded-xl shadow-md"}
      [:div {:class "px-6 py-4 border-b border-gray-200"}
       [:h3 {:class "text-lg font-semibold text-gray-900"} "最近活动"]]
      [:div {:class "p-6 space-y-4"}
       (c/activity-item {:icon "✓" :icon-bg "bg-green-100" :icon-color "text-green-600"
                         :title "新用户注册" :description "Alice Smith 以编辑者身份加入" :time "2 分钟前"})
       (c/activity-item {:icon "📄" :icon-bg "bg-blue-100" :icon-color "text-blue-600"
                         :title "资源已更新" :description "营销 Q4 活动" :time "1 小时前"})
       (c/activity-item {:icon "⚠" :icon-bg "bg-yellow-100" :icon-color "text-yellow-600"
                         :title "系统提醒" :description "服务器维护已安排" :time "3 小时前"})
       (c/activity-item {:icon "📊" :icon-bg "bg-purple-100" :icon-color "text-purple-600"
                         :title "新报告已生成" :description "月度数据分析摘要" :time "5 小时前"})]]]

    [:div {:id "toast" :class "fixed bottom-4 right-4 z-50"}]]
    :header-user user
    :header-search true)))