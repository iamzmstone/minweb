(ns routes
  (:require
   [ruuter.core :as ruuter]
   [static :as static]
   [view.index :as index]
   [view.admin.core :as admin]
   [view.admin.user :as usradmin]
   [view.dashboard :as dashboard]
   [view.login :as login]
   [view.resource :as resource]))

(defn route
  [path method response-fn]
  {:path path
   :method method
   :response (fn [req]
               (let [resp (response-fn req)]
                 (if (string? resp)
                   {:sttus 200
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
    [(http-get "/static/:filename*" static/serve-static)
     (http-get "/" index/page)
     (http-get "/login" login/index)
     (http-get "/logout" login/logout)
     (http-post "/login" login/login)
     (http-get "/chpwd" login/chpwd-form)
     (http-post "/chpwd" login/change-passwd)
     (http-get "/admin" admin/index)
     (http-get "/mgmt/:type" admin/layout)
     (http-get "/show-modal/:type/:id" admin/show-modal)
     (http-post "/add-usr" usradmin/add-user)
     (http-get "/delete-it/:type/:id" admin/delete-it)
     (http-get "/val-uniq/:type" admin/val-uniq)
     (http-get "/dashboard" dashboard/page)
     (http-get "/resources" resource/page)
     ] %))
