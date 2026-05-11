(ns view.login
  (:require
   [taoensso.timbre :as log]
   [utils.response :as r]
   [utils.session :refer [current-user]]
   [view.core :as c]
   [view.layout :as l]
   [database.user
    :refer [correct-password? the-user passwd-ok? add]]
   [middleware.rate-limit :refer [get-client-ip record-failed record-success]]))

(defn logout [req]
  (c/log-user req "logout")
  (assoc (r/flash-msg
          (r/redirect "/")
          "success" "退出系统成功")
         :session nil))

(defn login [req]
  (let [ip (get-client-ip req)
        email (get-in req [:params "email"])
        passwd (get-in req [:params "password"])
        url (get-in req [:params "pre-url"] "/")]
    (if (correct-password?
         email passwd)
      (do
        (record-success ip)
        (let [user (the-user email)]
          (log/info "User" (:user/name user) "logined in")
          (assoc (r/flash-msg
                  (r/redirect url)
                  "success" (str (:user/name user) ", 登陆系统成功"))
                 :session {:user-id (:db/id user)})))
      (do
        (record-failed ip)
        (log/warn "Login failed for" email)
        (r/flash-msg
         (r/redirect "/login")
         "danger" "用户名或密码错误")))))

(defn index [req]
  (l/layout
   req
   [:div.max-w-md.mx-auto.p-4
    [:div.bg-blue-100.shadow-md.rounded-lg.p-6
     [:h1.text-2xl.font-bold.mb-6.text-center "用户登录"]
     [:form {:method "post" :action "/login"}
      (c/csrf-token)
      (when-let [url (get-in req [:params "url"])]
        [:input {:type "hidden" :name "pre-url"
                 :value url}])
      (c/form-input {:type "email" :label "E-Mail"
                     :name "email" :required true
                     :placeholder "email"})
      (c/form-input {:type "password" :label "密码"
                     :name "password" :required true})
      (c/form-submit-btn "登录")]]]))

(defn chpwd-form
  [req]
  (l/layout
   req
   [:div.max-w-md.mx-auto.p-4
    [:div.bg-blue-100.shadow-md.rounded-lg.p-6
     [:h1.text-2xl.font-bold.mb-6.text-ceter "修改密码"]
     [:form {:method "post" :action "/chpwd"}
      (c/csrf-token)
      (c/form-input {:type "password" :label "原密码"
                     :name "oldpass" :required true})
      (c/form-input {:type "password" :label "新密码"
                     :name "newpass" :required true})
      (c/form-input {:type "password" :label "新密码确认"
                     :name "newpass-confirm" :required true})
      (c/form-submit-btn "修改密码")]]]))

(defn validate-passwd
  [npw npwc]
  (and (>= (count npw) 6)
       (>= (count npwc) 6)
       (= npw npwc)))

(defn change-passwd
  [req]
  (let [opw (get-in req [:params "oldpass"])
        npw (get-in req [:params "newpass"])
        npwc (get-in req [:params "newpass-confirm"])
        user (current-user req)]
    (if (passwd-ok? (:user/password user) opw)
      (if (validate-passwd npw npwc)
        (do
          (add {:email (:user/email user)
                :name (:user/name user)
                :role (:user/role user)
                :privs (:user/privs user)
                :password npw})
          (c/log-user req "change password successfully")
          (r/flash-msg
           (r/redirect "/")
           "success" "密码修改成功"))
        (r/flash-msg
         (r/redirect "/chpwd")
         "danger" "新密码长度必须大于5,必须匹配"))
      (r/flash-msg
       (r/redirect "/chpwd")
       "danger" "原密码错误"))))