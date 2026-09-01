(ns minweb.database.user
  (:require
   [minweb.database.dtlv :as db]
   [minweb.utils.encryption :as enc]
   [minweb.common :refer [env rand-password]]
   [taoensso.timbre :as log]))

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

(defn change-password
  "Hash new-password and update :user/password on the existing entity.
   Use instead of (add ...) — add creates a new tempid, never updates."
  [eid new-password]
  (when (pos-int? eid)
    (db/update-ent eid [{:user/password (enc/hash-password new-password)}])))

(defn update-user
  "In-place update of an existing user. Replaces :user/privs atomically
   (retract all + assert new) and updates :user/name. :user/email is NOT
   touched (unique-identity, read-only in UI)."
  [eid {:keys [name privs]}]
  (when (pos-int? eid)
    (db/update-ent-with-privs eid name (or privs []))))

(defn passwd-ok?
  [enc-pw pw]
  (enc/password= enc-pw pw))

(defn correct-password? [email password]
  (let [user (the-user email)]
    (if user
      (enc/password= (:user/password user) password)
      false)))

(defn default-user
  "Idempotent admin seeding.
   No args: throw if user with :init-email already exists.
   With `force` arg (any of `:force`, `\"force\"`, or `\":force\"`):
   delete existing then re-create.
   Prints new password to log on success."
  [& args]
  (let [force? (boolean (some #(contains? #{"force" ":force" :force} (str %)) args))
        email (or (:init-email env) "admin@example.com")
        name (or (:init-name env) "Admin")
        role (or (:init-role env) :admin)
        password (rand-password)
        existing (the-user email)]
    (cond
      (and existing force?)
      (do (log/info "Force: deleting existing user:" email)
          (db/del-ent (:db/id existing)))
      existing
      (throw (ex-info (str "User " email " already exists. Run `bb db-seed :force` to reset.")
                      {:email email
                       :existing-id (:db/id existing)})))
    (db/add-user {:email email
                  :name name
                  :password password
                  :role role})
    (log/info "Created default user:" email "with password:" password)
    {:email email :password password}))

(defn exists?
  [email]
  (let [users (all)]
    (> (count
        (filter #(= email (:user/email %)) users)) 0)))
