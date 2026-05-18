;; middleware.request-log - Request logging

(ns middleware.request-log
  (:require
   [taoensso.timbre :as log]))

(def ^:private request-id-header "X-Request-Id")

(defn generate-request-id []
  (str (java.util.UUID/randomUUID)))

(defn wrap-request-logging
  "Middleware that adds request ID and logs request summary."
  [handler]
  (fn [req]
    (let [request-id (or (get-in req [:headers request-id-header])
                         (generate-request-id))
          start-time (System/currentTimeMillis)
          method (name (or (:request-method req) :get))
          uri (:uri req)]
      (try
        (let [resp (handler (assoc req :request-id request-id))
              duration (- (System/currentTimeMillis) start-time)]
          (when (> duration 100)
            (log/warn uri method (.intValue duration) "ms"))
          (-> resp
              (assoc-in [:headers request-id-header] request-id)))
        (catch Exception e
          (log/error uri method (.getMessage e))
          (throw e))))))