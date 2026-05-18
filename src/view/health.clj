;; view.health - Health check endpoint

(ns view.health
  (:require
   [common :refer [env]]
   [cheshire.core :as json]))

(defn health-page [_]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string {:status "ok"
                                :app-name (env :app-name)
                                :timestamp (System/currentTimeMillis)})})