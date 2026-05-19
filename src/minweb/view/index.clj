(ns minweb.view.index
  (:require
   [minweb.common :refer [env]]
   [minweb.view.layout :refer [layout]]))

(def Title (or (env :title) "Web Application"))

(defn showbox
  [subject color link remark]
  (let [out-cls "p-6 bg-white rounded-lg shadow hover:shadow-lg transition-transform border border-gray-200"]
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

(defn page [req]
  (let [subtitle "subtitle"
        topics [["topic1" "bg-blue-500" {:href "/topic1" :txt "topic1 information"}
                 "description of topic1"]
                ["topic2" "bg-blue-500" {:href "/topic2" :txt "topic2 information"}
                 "description of topic2"]
                ["topic3" "bg-blue-500" {:href "/topic3" :txt "topic3 information"}
                 "description of topic3"]
                ["topic4" "bg-blue-500" {:href "/topic4" :txt "topic4 information"}
                 "description of topic4"]]]
    (layout
     req
     [:div.container.mx-auto.p-6
      [:h1.text-3xl.font-bold.mb-6.text-gray-900 Title]
      [:h6.text-lg.text-gray-700.font-semibold.border-b.border-gray-200.pb-2.mb-4 subtitle]
      [:div.space-y-6
       (for [t topics]
         (apply showbox t))]])))
