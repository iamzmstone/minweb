;; minweb.view.health - Health check endpoint

(ns minweb.view.health
  (:require
   [minweb.common :refer [env]]
   [minweb.database.dtlv :as db]
   [taoensso.timbre :as log]
   [cheshire.core :as json]))

(defn db-ok?
  []
  (try
    (db/ping)
    true
    (catch Exception e
      ;; Detail stays in the log — /health is unauthenticated, so the response
      ;; must not leak DB paths or internal state.
      (log/error e "Health check: DB probe failed")
      false)))

(defn health-page [_]
  (let [ok? (db-ok?)]
    {:status (if ok? 200 503)
     :headers {"Content-Type" "application/json"}
     :body (json/generate-string
            {:status (if ok? "ok" "degraded")
             :db (if ok? "ok" "error")
             :app-name (env :app-name)
             :timestamp (System/currentTimeMillis)})}))
