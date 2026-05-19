;; middleware.request-log - Request logging

(ns middleware.request-log
  (:require
   [clojure.string :as str]
   [taoensso.timbre :as log]))

(def ^:private request-id-header "X-Request-Id")

(defn generate-request-id []
  (str (java.util.UUID/randomUUID)))

(defn- important-uri?
  "Check if URI should be logged (important pages, not static assets)"
  [uri]
  (or (nil? uri)
      (boolean
       (when (string? uri)
         (not (or (str/starts-with? uri "/static/")
                  (str/starts-with? uri "/.well-known/")
                  (str/ends-with? uri ".js")
                  (str/ends-with? uri ".css")
                  (str/ends-with? uri ".svg")
                  (str/ends-with? uri ".png")
                  (str/ends-with? uri ".jpg")
                  (str/ends-with? uri ".ico")
                  (str/ends-with? uri ".woff")
                  (str/ends-with? uri ".woff2")
                  (str/ends-with? uri ".ttf")))))))

(defn wrap-request-logging
  "Middleware that adds request ID and logs important requests only.
   Ignores static assets (/static/, /.well-known/, *.css, *.js, etc.)"
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
          (when (and (important-uri? uri) (> duration 0))
            (log/debug uri method (.intValue duration) "ms"))
          (-> resp
              (assoc-in [:headers request-id-header] request-id)))
        (catch Exception e
          (log/error uri method (.getMessage e))
          (throw e))))))
