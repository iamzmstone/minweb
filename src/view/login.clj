(ns view.login
  (:require
   [taoensso.timbre :as log]
   [utils.response :as r]
   [utils.session :refer [current-user]]
   [view.core :as c]
   [view.layout :as l]
   [database.user
    :refer [correct-password? the-user passwd-ok? add]]))

(defn logout [req]
  (c/log-user req "logout")
  (assoc (r/flash-msg
          (r/redirect "/")
          "success" "退出系统成功")
         :session nil))

(defn login [req]
  (let [email (get-in req [:params "email"])
        passwd (get-in req [:params "password"])
        url (get-in req [:params "pre-url"] "/")]
    (if (correct-password?
         email passwd)
      (let [user (the-user email)]
        (log/info "User" (:user/name user) "logined in")
        (assoc (r/flash-msg
                (r/redirect url)
                "success" (str (:user/name user) ", 登陆系统成功"))
               :session {:user-id (:db/id user)}))
      (do
        (log/warn "User" email "login failed")
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
      [:div.mb-4
       [:label.block.text-sm.font-medium.text-gray-700.mb-1
        {:for "email"} "E-Mail"]
       [:input.w-full.px-4.py-2.bg-white.border.border-gray-300.rounded-md.focus:outline-none.focus:ring-2.focus:ring-blue-500
        {:type "email" :name "email" :id "email" :required true
         :placeholder "E-Mail"}]]
      [:div.mb-6
       [:label.block.text-sm.font-medium.text-gray-700.mb-1
        {:for "password"} "密码"]
       [:input.w-full.px-4.py-2.bg-white.border.border-gray-300.rounded-md.focus:outline-none.focus:ring-2.focus:ring-blue-500
        {:type "password" :name "password" :id "password" :required true}]]
      [:div.mb-4
       [:input.w-full.bg-blue-600.hover:bg-blue-700.text-white.font-semibold.py-2.px-4.rounded-md.cursor-pointer
        {:type "submit" :value "登录"}]]]]]))

(defn chpwd-form
  [req]
  (l/layout
   req
   [:div.max-w-md.mx-auto.p-4
    [:div.bg-blue-100.shadow-md.rounded-lg.p-6
     [:h1.text-2xl.font-bold.mb-6.text-ceter "修改密码"]
     [:form {:method "post" :action "/change-passwd"}
      (c/csrf-token)
      [:div.mb-4
       [:label.block.text-sm.font-medium.text-gray-700.mb-1
        {:for "oldpass"} "原密码"]
       [:input.w-full.bg-white.px-4.py-2.border.border-gray-300.rounded-md.focus:outline-none.focus:ring-2.focus:ring-blue-500
        {:type "password" :name "oldpass" :id "oldpass" :required true
         :placeholder "原密码"}]]
      [:div.mb-4
       [:label.block.text-sm.font-medium.text-gray-700.mb-1
        {:for "newpass"} "新密码"]
       [:input.w-full.bg-white.px-4.py-2.border.border-gray-300.rounded-md.focus:outline-none.focus:ring-2.focus:ring-blue-500
        {:type "password" :name "newpass" :id "newpass" :required true
         :placeholder "新密码"}]]
      [:div.mb-6
       [:label.block.text-sm.font-medium.text-gray-700.mb-1
        {:for "newpass-confirm"} "新密码确认"]
       [:input.w-full.bg-white.px-4.py-2.border.border-gray-300.rounded-md.focus:outline-none.focus:ring-2.focus:ring-blue-500
        {:type "password" :name "newpass-confirm" :id "newpass-confirm"
         :required true :placeholder "新密码确认"}]]
      [:div.mb-4
       [:input.w-full.bg-blue-600.hover:bg-blue-700.text-white.font-semibold.py-2.px-4.rounded-md.cursor-pointer
        {:type "submit" :value "修改密码"}]]]]]))

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
                :privs (:user/privileges user)
                :password npw})
          (c/log-user req "change password successfully")
          (r/flash-msg
           (r/redirect "/")
           "success" "密码修改成功"))
        (r/flash-msg
         (r/redirect "/change-passwd")
         "danger" "新密码长度必须大于5,必须匹配"))
      (r/flash-msg
       (r/redirect "/change-passwd")
       "danger" "原密码错误"))))