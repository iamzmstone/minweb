(ns view.index
  (:require
   [common :refer [env]]
   [view.core :refer [showbox]]
   [view.layout :refer [layout]]))

(def Title (or (env :title) "Web Application"))

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
     [:div.container.mx-auto.p-4
      [:h1.text-3xl.font-bold.mb-6 Title]
      [:h6.text-lg.text-gray-950.font-semibold.border-b.border-gray-200.pb-2.mb-4 subtitle]
      [:div.space-y-6
       (for [t topics]
         (apply showbox t))]])))
