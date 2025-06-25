(ns database.user
  (:require
   [database.dtlv :as db]
   [utils.encryption :as enc]))

(defn the-user
  [u]
  (when u
    (let [e (if (int? u)
              (db/the-ent u)
              (-> (db/ent-by :user/email u)
                  first))]
      (when (contains? e :user/email)
        e))))

(defn all
  []
  (db/all-ents :user/email))

(defn delete!
  [id]
  (db/del-ent id))

(defn add
  [user]
  (db/add-user user))

(defn passwd-ok?
  [enc-pw pw]
  (enc/password= enc-pw pw))

(defn correct-password? [email password]
  (let [user (the-user email)]
    (if user
      (enc/password= (:user/password user) password)
      false)))

(defn default-user
  []
  (db/add-user {:email "zengm@189.cn"
                :name "曾民"
                :password "758252"
                :role :admin}))

(defn exists?
  [email]
  (let [users (all)]
    (> (count
        (filter #(= email (:user/email %)) users)) 0)))
