;; minweb.middleware.session - Session timeout handling

(ns minweb.middleware.session
  (:require
   [minweb.common :refer [env]]
   [taoensso.timbre :as log]))

(defn wrap-session-timeout
  "Middleware that enforces session timeout.
   Sessions older than :session-ttl seconds (default 1800 = 30 min) are invalidated."
  [handler]
  (fn [req]
    (let [session (:session req)
          created-at (:session/created-at session)
          ttl (* 1000 (or (:session-ttl env) 1800))
          now (System/currentTimeMillis)]
      (if (and created-at (> (- now created-at) ttl))
        (do
          (log/info "Session expired for user-id:" (:user-id session))
          (-> (handler req)
              (assoc :session nil)))
        (handler req)))))

(defn wrap-session-create
  "Middleware that adds session-created-at timestamp on first creation."
  [handler]
  (fn [req]
    (let [session (:session req)
          existing-created (:session/created-at session)
          new-session (if existing-created
                        session
                        (assoc session :session/created-at (System/currentTimeMillis)))]
      (handler (assoc req :session new-session)))))