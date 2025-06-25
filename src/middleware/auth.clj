(ns middleware.auth
  (:require
   [utils.response :as r]
   [clojure.string :as str]
   [taoensso.timbre :as log]
   [utils.session :as session]))

(def user-privs
  #{"/" "/profile" "/change-passwd" "/logout"})

(def privileges
  {:switch #{"/switch" "/sw-info" "/vlan"}
   :pon #{"/olt" "/card" "/pon" "/onu"
         "/onu-state" "/state-onu" "/olt-cmd"}
   :project #{"/project" "/device" "/dev-state"
              "/state-his" "/export-prj"
              "/export-long-offline"}
   :alert #{"/alert"}
   :search #{"/search"}
   :admin #{"/admin" "/mgmt" "/show-modal" "/change-prj"
            "/add-usr" "/add-prj" "/add-dvc" "/delete-it"
            "/restart-nagios" "/upload-devs"}})

(def restricted-pages
  (into #{}
   (concat
    user-privs
    (:switch privileges)
    (:pon privileges)
    (:project privileges)
    (:alert privileges)
    (:search privileges)
    (:admin privileges))))

(defn path-restricted?
  [path]
  (let [path (->> (str/split path #"\/")
                  second
                  (str "/"))]
    (contains? restricted-pages path)))

(defn authorized?
  [user path]
  (if (= :admin (:user/role user))
    true
    (let [privs (->> (:user/privileges user)
                     (reduce #(concat %1 (privileges %2))
                             user-privs)
                     (into #{}))]
      (contains? privs path))))

(defn wrap-auth
  [handler]
  (fn [req]
    (let [path (:uri req)]
      (if (path-restricted? path)
        (if-let [user (session/current-user req)]
          (if (authorized? user path)
            (handler req)
            (do
              (log/warn "User" (:user/email user)
                        "try to visit unauthorized path: " path)
              (r/flash-msg
               (r/redirect "/")
               "warning" "没有权限访问")))
          (r/redirect (str "/login?url=" (:uri req))))
        (handler req)))))