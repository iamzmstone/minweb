(ns minweb.database.dtlv
  (:require
   [minweb.common :refer [env]]
   [taoensso.timbre :as log]
   [minweb.utils.encryption :refer [hash-password]]
   [clojure.edn :as edn]
   [clojure.set :refer [difference]]
   [babashka.pods :as pods]))

(pods/load-pod "dtlv")
(require '[pod.huahaiy.datalevin :as d])

(def ^:dynamic *conn*
  "Single Datalevin connection per process — by design. LMDB rejects multiple
   connections to the same path within one process ('multiple LMDB environment'
   error). Reads via (d/db *conn*) return MVCC snapshots and are concurrent;
   writes go through d/transact! (sync) and are serialized on the single
   writer lock. The pod's d/transact-async deref-paths return a nil-future
   that NPEs, so this codebase uses sync transact!.

   ^:dynamic is preserved for test rebinding (binding) — no test uses it
   today, but the earmuffed name warrants the marker."
  (d/get-conn (env :dtlv-opts)
              (edn/read-string (slurp "schema.edn"))
              {:closed-schema? true
               :validate-data? true}))

(defn db-schema
  []
  (d/update-schema
   *conn* (edn/read-string (slurp "schema.edn")))
  (try
    (require 'database.migration)
    ((resolve 'database.migration/ensure-schema-version))
    (catch Exception e
      (log/info "Migration system not available:" (.getMessage e)))))

(defn schema
  []
  (into #{:db/id} (keys (d/schema *conn*))))

(defn ping
  "Cheapest possible real read against the live conn — proves the LMDB env is
   open and queryable. Throws if the connection is dead. Return value is
   meaningless (nil on an empty DB); callers should only care about throw/no-throw."
  []
  (ffirst (d/q '[:find (count ?e)
                 :where [?e :user/email]]
               (d/db *conn*))))

(defn the-ent
  [eid]
  (when (pos-int? eid)
    (d/pull (d/db *conn*) '[*] eid)))

(defn all-ents
  [attr]
  (let [q '[:find [(pull ?e pattern) ...]
            :in $ pattern ?attr
            :where [?e ?attr]]]
    (d/q q (d/db *conn*) '[*] attr)))

(defn cnt
  [attr]
  (when-not (contains? (schema) attr)
    (log/warn (str "Attribute " attr " doesn't exists")))
  (d/cardinality (d/db *conn*) attr))

(defn ent-by
  [k v]
  (let [q '[:find [(pull ?e pattern) ...]
            :in $ pattern ?k ?v
            :where
            [?e ?k ?v]]]
    (d/q q (d/db *conn*) '[*] k v)))

(defn ent-query
  [criteria]
  (let [w (map (fn [[k v]] ['?e k v])
               criteria)
        q (->> w
               (concat '[:find (pull ?e [*])
                         :where])
               vec)]
    (-> (d/q q (d/db *conn*))
        flatten)))

(defn ent-search
  [query]
  (let [q '[:find [(pull ?e [*]) ...]
            :in $ ?q
            :where
            [(fulltext $ ?q {:top 100}) [[?e _ _]]]]]
    (d/q q (d/db *conn*) query)))

(defn count-by
  [k v]
  (let [q '[:find (count ?e)
            :in $ ?k ?v
            :where
            [?e ?k ?v]]]
    (ffirst (d/q q (d/db *conn*) k v))))

(defn count-query
  [criteria]
  (let [w (map (fn [[k v]] ['?e k v]) criteria)
        q (->> w
               (concat '[:find (count ?e)
                         :where])
               vec)]
    (ffirst (-> (d/q q (d/db *conn*))))))

(defn add-user
  [{:keys [email name password role privs]}]
  (d/transact!
   *conn*
   [{:db/id -1
     :user/email email
     :user/name name
     :user/role (or role :normal)
     :user/privs
     (or privs [:project :search])
     :user/password
     (hash-password password)}]))

(defn add-ents
  [ents]
  (let [attrs (keys (first ents))
        all-attrs (schema)]
    (if (every? #(contains? all-attrs %) attrs)
      (d/transact! *conn* ents)
      (log/warn
       (str "There are attrs have not installed yet:"
            (difference (into #{} attrs) all-attrs))))))

(defn del-ent-attr
  [eid attr]
  (when (pos-int? eid)
    (d/transact! *conn* [[:db.fn/retractAttribute eid attr]])))

(defn del-ent
  [eid]
  (when (pos-int? eid)
    (d/transact! *conn* [[:db.fn/retractEntity eid]])))

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
        eids (->> (d/q q (d/db *conn*) attr)
                  (map first))]
    (doseq [eid eids]
      (del-ent eid))))

(defn update-ent
  [eid attrs]
  (when (pos-int? eid)
    (d/transact! *conn* (mapv #(merge {:db/id eid} %) attrs))))

(defn update-ent-with-privs
  "Atomically retract all :user/privs values on eid, then assert new-privs
   and update :user/name. Single d/transact! call = atomic. Use for
   cardinality-many attribute replacement where partial-stale window is
   unacceptable."
  [eid name new-privs]
  (when (pos-int? eid)
    (d/transact!
     *conn*
     [[:db.fn/retractAttribute eid :user/privs]
      {:db/id eid
       :user/name name
       :user/privs new-privs}])))

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
    (d/q q (d/db *conn*) prj-eid)))
