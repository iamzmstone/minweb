(ns core
  (:require
   [babashka.cli :as cli]
   [routes :as ro]
   [ring.middleware.anti-forgery :as af]
   [ring-multipart-nodeps.core :as mp]
   [ring.middleware.session :as s]
   [ring.middleware.params :as p]
   [ring.middleware.flash :as f]
   [middleware.auth :as auth]
   [taoensso.timbre :as log]
   [taoensso.timbre.appenders.core :as appenders]
   [org.httpkit.server :as srv]))

(def server (atom nil))

(def cli-options-spec {:help {:alias :h}
                       :port {:coerce :int
                              :default 6666
                              :alias :p}})

(log/merge-config!
 {:timestamp-opts {:pattern "yyyy/MM/dd HH:mm:ss ZZ"
                   :locale (java.util.Locale. "zh_CN")
                   :timezone (java.util.TimeZone/getTimeZone
                              "Asia/Shanghai")}
  :level :info
  :appenders {:spit (appenders/spit-appender
                     {:fname "./application.log"})}})
(defn show-help []
  (println (cli/format-opts {:spec cli-options-spec})))

(defn start-server [port]
  (log/info "Server starting up!")
  (reset! server
          (srv/run-server
           (->
            #'ro/routes
            auth/wrap-auth
            (af/wrap-anti-forgery {:anti-forgery true
                                   :token-expiry (* 60 60 24)})
            f/wrap-flash
            s/wrap-session
            mp/wrap-multipart-params
            p/wrap-params)
           {:port port
            :join? false})))

(defn -main
  "Start the web server default on port 6666"
  [& args]
  (let [cli-options (cli/parse-opts args {:spec cli-options-spec})
        {:keys [port help]} cli-options]
    (if help
      (show-help)
      (do (start-server port)
          (log/info
           (str "Happy coding @ http://localhost:" port))
          @(promise)))))

;;
;; Repl functions. To startup and stop the system
;;
(comment (start-server 6666))
(comment (@server))
