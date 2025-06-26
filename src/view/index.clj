(ns view.index
  (:require
   [common :refer [env]]
   [view.core :refer [showbox]]
   [view.layout :refer [layout]]))

(def Title (or (env :title) "Web Application"))

(defn page [req]
  (let [subtitle "集成多种能力"
        topics [["交换机管理" "bg-blue-500" {:href "/switch" :txt "交换机信息"}
                 "交换机的重要端口状态和流量检测，VLAN和子网管理, 配置文件自动备份"]
                ["PON管理" "bg-yellow-500" {:href "/olt" :txt "PON信息"}
                 "OLT配置管理，ONU状态、光衰、流量检测"]
                ["项目设备管理" "bg-pink-500" {:href "/project" :txt "项目点位信息"}
                 "按项目分组显示在线设备,离线设备,设备网络在线率"]
                ["综合搜索能力" "bg-orange-500" nil
                 "通过IP地址、SN号等信息全方位搜索网络和故障信息"]
                ["故障管理" "bg-purple-500" {:href "/alert" :txt "故障告警"}
                 "重要光路的状态、流量监测，IP在线率监测，故障显示和告警"]]]
    (layout
     req
     [:div.container.mx-auto.p-4
      [:h1.text-3xl.font-bold.mb-6 Title]
      [:h6.text-lg.text-gray-950.font-semibold.border-b.border-gray-200.pb-2.mb-4 subtitle]
      [:div.space-y-6
       (for [t topics]
         (apply showbox t))]])))
