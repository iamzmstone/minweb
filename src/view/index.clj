(ns view.index
  (:require
   [common :refer [env]]
   [view.layout :as l]))

(def Title (or (env :title) "Web Application"))

(defn page [req]
  (l/layout
   req
   [:div.container.mx-auto.p-4
    [:h1.text-3xl.font-bold.mb-6 Title]
    [:div.space-y-6
     [:div.p-6.bg-white.rounded-lg.shadow.hover:shadow-lg.hover:scale-105.transition-transform
      [:h6.text-lg.font-semibold.border-b.border-gray-200.pb-2.mb-4 "集成多种能力"]
      [:div.flex.items-start.space-x-4
       [:div.flex-shrink-0.w-8.h-8.bg-blue-500.rounded]
       [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
        [:div.flex.justify-between
         [:strong "交换机管理"]
         [:a.text-blue-500.hover:underline {:href "/switch"} "交换机信息"]]
        [:span
         "交换机的重要端口状态和流量检测，VLAN和子网管理, 配置文件自动备份"]]]]
     [:div.p-6.bg-white.rounded-lg.shadow.hover:shadow-lg.hover:scale-105.transition-transform
      [:div.flex.items-start.space-x-4
       [:div.flex-shrink-0.w-8.h-8.bg-yellow-500.rounded]
       [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
        [:div.flex.justify-between
        [:strong "PON管理"]
        [:a.text-blue-500.hover:underline {:href "/olt"} "PON信息"]]
       [:span "OLT配置管理，ONU状态、光衰、流量检测"]]]]
     [:div.p-6.bg-white.rounded-lg.shadow.hover:shadow-lg.hover:scale-105.transition-transform
      [:div.flex.items-start.space-x-4
       [:div.flex-shrink-0.w-8.h-8.bg-pink-500.rounded]
       [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
        [:div.flex.justify-between
         [:strong "项目设备管理" ]
         [:a.text-blue-500.hover:underline {:href "/project"} "项目点位信息"]]
        [:span "按项目分组显示在线设备,离线设备,设备网络在线率"]]]]
     [:div.p-6.bg-white.rounded-lg.shadow.hover:shadow-lg.hover:scale-105.transition-transform
      [:div.flex.items-start.space-x-4
       [:div.flex-shrink-0.w-8.h-8.bg-orange-500.rounded]
       [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
        [:div.flex.justify-between
         [:strong "综合搜索能力"]]
        [:span "通过IP地址、SN号等信息全方位搜索网络和故障信息"]]]]
     [:div.p-6.bg-white.rounded-lg.shadow.hover:shadow-lg.hover:scale-105.transition-transform
      [:div.flex.items-start.space-x-4
       [:div.flex-shrink-0.w-8.h-8.bg-purple-500.rounded]
       [:div.flex-1.text-gray-700.text-sm.border-b.border-gray-200.pb-2
        [:div.flex.justify-between
         [:strong "故障管理"]
         [:a.text-blue-500.hover:underline {:href "/alert"} "故障告警"]]
       [:span "重要光路的状态、流量监测，IP在线率监测，故障显示和告警"]]]]]]))
