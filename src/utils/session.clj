(ns utils.session
  (:require
   [common :refer [env]]
   [database.user :as user]
   [taoensso.timbre :as log]))

(defn current-user
  "Get the current user id from the session and return the user object from the database"
  [req]
  (try
    (user/the-user (get-in req [:session :user-id] -1))
    (catch Exception e
      (log/error "Error fetching current user:" (.getMessage e))
      nil)))

(defn cur-page-size
  [req]
  (get-in req [:session :page-size] (env :page-size)))