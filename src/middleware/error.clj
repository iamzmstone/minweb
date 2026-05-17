(ns middleware.error
  (:require [taoensso.timbre :as log]))

(defn error-page
  [status title message]
  (str
   "<!DOCTYPE html><html><head>"
   "<meta charset=\"utf-8\">"
   "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
   "<title>" title "</title>"
   "<style>
     body { font-family: system-ui, -apple-system, sans-serif; background: #f5f5f5; }
     .container { max-width: 500px; margin: 100px auto; padding: 20px; }
     .card { background: white; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); padding: 40px; text-align: center; }
     .status { font-size: 72px; font-weight: bold; color: #1f2937; margin: 0; }
     .message { color: #6b7280; margin: 16px 0 24px; font-size: 18px; }
     .link { color: #3b82f6; text-decoration: none; }
     .link:hover { text-decoration: underline; }
   </style>"
   "</head><body>"
   "<div class=\"container\">"
   "<div class=\"card\">"
   "<p class=\"status\">" status "</p>"
   "<p class=\"message\">" message "</p>"
   "<a class=\"link\" href=\"/\">返回首页</a>"
   "</div></div>"
   "</body></html>"))

(defn not-found-page []
  (error-page 404 "404" "抱歉，找不到您要访问的页面"))

(defn server-error-page []
  (error-page 500 "500" "服务器内部错误，请稍后重试"))

(defn wrap-404
  "Catches 404 responses and renders friendly page"
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (and (map? resp) (= 404 (:status resp)))
        {:status 404
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (not-found-page)}
        resp))))

(defn wrap-error-handler
  "Catches exceptions and renders 500 error page"
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (log/error "Unhandled exception:" e)
        {:status 500
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (server-error-page)}))))

(defn wrap-not-found
  "Returns 404 for routes that don't match"
  [handler]
  (fn [req]
    (let [resp (handler req)]
      (if (nil? resp)
        {:status 404
         :headers {"Content-Type" "text/html; charset=utf-8"}
         :body (not-found-page)}
        resp))))