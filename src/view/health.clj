;; view.health - Health check endpoint

(ns view.health
  (:require
   [common :refer [env]]
   [clojure.data.json :as json]))

(defn health-page [_]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str {:status "ok"
                          :app-name (env :app-name)
                          :timestamp (System/currentTimeMillis)})})