(ns routes
  (:require
   [ruuter.core :as ruuter]
   [static :as static]
   [view.login :as login]))

(defn route
  [path method response-fn]
  {:path path
   :method method
   :response (fn [req]
               (let [resp (response-fn req)]
                 (if (string? resp)
                   {:status 200
                    :body resp}
                   resp)))})

(defn http-get
  [path response-fn]
  (route path :get response-fn))

(defn http-post
  [path response-fn]
  (route path :post response-fn))

(defn http-put
  [path response-fn]
  (route path :put response-fn))

(defn http-delete
  [path response-fn]
  (route path :delete response-fn))

(defn http-option
  [path response-fn]
  (route path :option response-fn))

(def routes
  #(ruuter/route
    [(http-get "/static/:filename" static/serve-static)
     (http-get "/login" login/index)] %))