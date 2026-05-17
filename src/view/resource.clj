(ns view.resource
  (:require
   [view.layout :as l]
   [view.core :as c]
   [utils.session :as s]))

(def sidebar-items
  [{:href "/dashboard" :label "Dashboard" :icon "📊"}
   {:href "/users" :label "用户" :icon "👥"}
   {:href "/resources" :label "资源" :icon "📁" :active true}
   {:href "/analytics" :label "分析" :icon "📈"}
   {:href "/settings" :label "设置" :icon "⚙️"}])

(def sample-resources
  [{:name "项目文档.pdf" :category "文档" :size "2.5 MB" :updated "3天前" :owner "张三"}
   {:name "产品视频.mp4" :category "视频" :size "156 MB" :updated "1周前" :owner "李四"}
   {:name "logo.png" :category "图片" :size "128 KB" :updated "2天前" :owner "王五"}
   {:name "背景音乐.mp3" :category "音频" :size "8.2 MB" :updated "5天前" :owner "赵六"}
   {:name "数据报告.xlsx" :category "文档" :size "1.1 MB" :updated "今天" :owner "张三"}])

(def category-badges
  {:文档 {:variant :info :label "文档"}
   :视频 {:variant :primary :label "视频"}
   :图片 {:variant :success :label "图片"}
   :音频 {:variant :warning :label "音频"}})

(def action-buttons
  [{:label "查看" :on-click "viewResource()"}
   {:label "编辑" :on-click "editResource()"}
   {:label "删除" :on-click "deleteResource()"}])

(defn resource-page [req]
  (let [user (s/current-user req)
        columns [{:key :name :label "资源"}
                 {:key :category :label "分类"}
                 {:key :size :label "大小"}
                 {:key :updated :label "修改时间"}
                 {:key :owner :label "所有者"}]
        rows (mapv (fn [r]
                    (let [badge-info (get category-badges (keyword (:category r)))]
                      {:name {:value (:name r)}
                       :category {:value (c/badge (:label badge-info) {:variant (:variant badge-info)})}
                       :size {:value (:size r)}
                       :updated {:value (:updated r)}
                       :owner {:value (:owner r)}}))
                  sample-resources)]
    (l/dashboard-layout
     req
     sidebar-items
     [:div.space-y-4
      [:div.flex.justify-between.items-center
       [:div (c/search-bar {:placeholder "搜索资源..."})]
       [:button {:type "button"
                 :class "px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium"
                 :on-click "openFormModal()"}
        "添加资源"]]
      (c/data-table
       {:columns columns
        :rows rows
        :actions action-buttons})
      (l/paginator req 1 5 "/resources")]
     :header-title "资源管理"
     :header-user user
     :header-search true)))

(defn page [req]
  (resource-page req))