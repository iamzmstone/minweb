(ns database.dtlv
  (:require
   [common :refer [env]]
   [taoensso.timbre :as log]
   [utils.encryption :refer [hash-password]]
   [clojure.edn :as edn]
   [babashka.pods :as pods]))

(pods/load-pod "dtlv")
(require '[pod.huahaiy.datalevin :as d])
(def Conn
  (d/get-conn (env :dtlv-opts)
              (edn/read-string (slurp "schema.edn"))
              {:closed-schema? true
               :validate-data? true}))

(defn db-schema
  []
  (d/update-schema
   Conn (edn/read-string (slurp "schema.edn")))
  (try
    (require 'database.migration)
    ((resolve 'database.migration/ensure-schema-version))
    (catch Exception e
      (log/info "Migration system not available:" (.getMessage e)))))

(defn schema
  []
  (into #{:db/id} (keys (d/schema Conn))))

(defn the-ent
  [eid]
  (when (pos-int? eid)
    (d/pull (d/db Conn) '[*] eid)))

(defn all-ents
  [attr]
  (let [q '[:find [(pull ?e pattern) ...]
            :in $ pattern ?attr
            :where [?e ?attr]]]
    (d/q q (d/db Conn) '[*] attr)))

(defn cnt
  [attr]
  (when-not (contains? (schema) attr)
    (log/warn (str "Attribute " attr " doesn't exists")))
  (d/cardinality (d/db Conn) attr))

(defn ent-by
  [k v]
  (let [q '[:find [(pull ?e pattern) ...]
            :in $ pattern ?k ?v
            :where
            [?e ?k ?v]]]
    (d/q q (d/db Conn) '[*] k v)))

(defn ent-query
  [criteria]
  (let [w (map (fn [[k v]] ['?e k v])
               criteria)
        q (->> w
               (concat '[:find (pull ?e [*])
                         :where])
               vec)]
    (-> (d/q q (d/db Conn))
        flatten)))

(defn ent-search
  [query]
  (let [q '[:find [(pull ?e [*]) ...]
            :in $ ?q
            :where
            [(fulltext $ ?q {:top 100}) [[?e _ _]]]]]
    (d/q q (d/db Conn) query)))

(defn count-by
  [k v]
  (let [q '[:find (count ?e)
            :in $ ?k ?v
            :where
            [?e ?k ?v]]]
    (ffirst (d/q q (d/db Conn) k v))))

(defn count-query
  [criteria]
  (let [w (map (fn [[k v]] ['?e k v]) criteria)
        q (->> w
               (concat '[:find (count ?e)
                         :where])
               vec)]
    (ffirst (-> (d/q q (d/db Conn))))))

(defn add-user
  [{:keys [email name password role privs]}]
  (d/transact! Conn [{:db/id -1
                      :user/email email
                      :user/name name
                      :user/role (or role :normal)
                      :user/privs
                      (or privs [:project :search])
                      :user/password
                      (hash-password password)}]))

(defn add-ents
  [ents]
  (let [attrs (keys (first ents))]
    (if (every? #(contains? (schema) %) attrs)
      (d/transact! Conn ents)
      (log/warn (str "Some of attr in "
                     attrs " hasn't installed yet")))))
(defn del-ent-attr
  [eid attr]
  (when (pos-int? eid)
    (d/transact! Conn [[:db.fn/retractAttribute eid attr]])))

(defn del-ent
  [eid]
  (when (pos-int? eid)
    (d/transact! Conn [[:db.fn/retractEntity eid]])))

(defn delete-by
  [criteria]
  (let [ents (ent-query criteria)]
    (println "deleted" (count ents) "records")
    (doseq [eid (map :db/id ents)]
      (del-ent eid))))

(defn delete-all
  [attr]
  (let [q '[:find ?e
            :in $ ?attr
            :where [?e ?attr]]
        eids (->> (d/q q (d/db Conn) attr)
                  (map first))]
    (doseq [eid eids]
      (del-ent eid))))

(defn update-ent
  [eid attrs]
  (when (pos-int? eid)
    (d/transact! Conn (mapv #(merge {:db/id eid} %) attrs))))

(defn prj-devices
  [prj-eid]
  (let [q '[:find
            ?dev-name ?dev-ip
            ?dev-type
            (pull ?dev-id [:device_point/support_team])
            :in $ ?pid
            :where
            [?dev-id :device_point/project ?pid]
            [?dev-id :device_point/name ?dev-name]
            [?dev-id :device_point/ip ?dev-ip]
            [?dev-id :device_point/type ?dev-type]]]
    (d/q q (d/db Conn) prj-eid)))
