(ns minweb.view.admin.user
  (:require
   [clojure.string :as str]
   [hiccup2.core :refer [html]]
   [minweb.utils.response :as r]
   [minweb.common :refer [env to-coll]]
   [minweb.view.core :as c :refer [log-user]]
   [minweb.database.dtlv :as dtlv]
   [minweb.database.user :as user]))

(def Privs
  [[:search "搜索"]])

(defn priv-name
  [privs]
  (let [f (fn [k]
            (-> (filter #(= k (first %)) Privs)
                first
                last))]
    (->> (mapv f privs)
         (str/join ","))))

(defn fetch-user
  []
  (->> (user/all)
       (filter #(= :normal (:user/role %)))
       (map #(assoc
              % :privs
              (priv-name (:user/privs %))))))

(defn usr-modal-edit
  [req]
  (let [uid (parse-long
             (get-in req [:params :id] "-1"))
        user (user/the-user uid)]
    (str
     (html
      [:div {:class "fixed inset-0 bg-black/50 flex items-center justify-center"
             :id "modal-user"}
       [:div.bg-cyan-100.p-6.shadow-md.rounded-lg.w-full.max-w-md
        [:h2.text-xl.font-bold.mb-4 "编辑用户"]
        [:form {:method "post"
                :action "/add-usr"
                :_ "on submit
                     set email to (value of the #email)
                     set name to (value of the #username)
                     set privs to (value of the <input[name='privs']:checked/>)
                     if no email or no name or no privs
                       alert('请输入必填内容') then halt
                     end
                     set err_found to false
                     for err in <p.err-msg/>
                       if err.innerHTML.length > 0
                         set err_found to true
                       end
                     end
                     if err_found
                       alert('请先修改错误') then halt
                     end"}
         (c/csrf-token)
         [:input {:type "hidden"
                  :name "task"
                  :value (if user "modify" "add")}]
         [:input {:type "hidden"
                  :name "uid"
                  :value uid}]
         [:div.mb-4
          [:label.block.text-sm.font-medium "Email"
           [:span.text-red-500 "*"]]
          [:input.w-full.bg-white.border.px-3.py-2.rounded-md
           {:type "email"
            :id "email"
            :name "email"
            :required true
            :value (:user/email user)
            :hx-get "/val-uniq/usr"
            :hx-target "next p"
            :hx-trigger "change, keyup delay:200ms changed"
            :readonly (not (nil? user))}]
          [:p.err-msg.text-red-500.text-sm.mt-1 ""]]
         [:div.mb-4
          [:label.block.text-sm.font-medium "用户名"
           [:span.text-red-500 "*"]]
          [:input.w-full.bg-white.border.px-3.py-2.rounded-md
           {:type "text"
            :id "username"
            :name "username"
            :required true
            :value (:user/name user)}]]
         [:div.mb-4
          [:label.block.text-sm.font-medium.mb-2
           "用户权限"
           [:span.text-red-500 "*"]]
          [:div.space-y-2
           (for [[k t] Privs]
             [:label.flex.items-center
              [:input.mr-2
               {:type "checkbox"
                :id "privs"
                :name "privs"
                :value k
                :checked
                (contains?
                 (into
                  #{} (:user/privs user))
                 k)}] t])]]
         [:div.flex.justify-end
          [:button.bg-gray-300.text-black.px-4.py-2.rounded.mr-2
           {:type "button"
            :_ "on click add .hidden to #modal-user"}
           "取消"]
          [:button.bg-blue-500.text-white.px-4.py-2.rounded
           {:type "submit"}
           "保存"]]]]]))))

(defn user-error
  [m]
  (if (or (= "" (:email m))
          (= "" (:name m))
          (= 0 (count (:privs m))))
    "字段内容不能为空"
    (if (and (= "add" (:task m))
             (user/exists? (:email m)))
      (str "Email:" (:email m) "已存在")
      nil)))

(defn add-user
  [req]
  (let [task (get-in req [:params "task"])
        uid (parse-long (get-in req [:params "uid"]))
        email (str/trim (get-in req [:params "email"]))
        name (str/trim (get-in req [:params "username"]))
        privs (->> (get-in req [:params "privs"])
                   to-coll
                   (map keyword))
        m {:task task :email email
           :name name :privs privs}]
    (if-let [err-msg
             (user-error m)]
      (do
        (log-user req
                  (str task " user failed:" err-msg "." m))
        (r/flash-msg
         (r/redirect "/mgmt/usr")
         "danger" (str err-msg ",用户保存失败.")))
      (do
        (when (pos-int? uid) ;; delete many attribute privileges for the user
          (dtlv/del-ent-attr uid :user/privs))
        (user/add {:email email
                   :name name
                   :password (env :init-pwd)
                   :role nil
                   :privs privs})
        (log-user req
                  (str task " user:" email " successfully."))
        (r/flash-msg
         (r/redirect "/mgmt/usr")
         "success" "用户保存成功!")))))