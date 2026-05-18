;; middleware.request-log - Request logging and tracing

(ns middleware.request-log
  (:require
   [taoensso.timbre :as log]))

(def ^:private request-id-header "X-Request-Id")

(defn generate-request-id []
  (str (java.util.UUID/randomUUID)))

(defn wrap-request-logging
  "Middleware that logs request details and adds a unique request ID."
  [handler]
  (fn [req]
    (let [request-id (or (get-in req [:headers request-id-header])
                         (generate-request-id))
          start-time (System/currentTimeMillis)
          method (name (or (:request-method req) :get))
          uri (:uri req)
          remote-addr (get-in req [:headers "x-forwarded-for"]
                             (get-in req [:headers "x-real-ip"]
                                   (:remote-addr req)))]
      (log/info "Request started:" method uri "request-id:" request-id "ip:" remote-addr)
      (try
        (let [resp (handler (assoc req :request-id request-id))
              duration (- (System/currentTimeMillis) start-time)]
          (log/info "Request completed:" method uri "request-id:" request-id "status:" (:status resp) "duration:" duration "ms")
          (-> resp
              (assoc-in [:headers request-id-header] request-id)))
        (catch Exception e
          (log/error "Request failed:" method uri "request-id:" request-id "error:" (.getMessage e))
          (throw e))))))