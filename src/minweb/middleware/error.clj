(ns minweb.middleware.error
  (:require
   [minweb.view.layout :refer [error-page]]))

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
      (catch Exception _e
        #_(log/error "Unhandled exception:" e)
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