;; database.migration - Schema version management and migrations

(ns database.migration
  (:require
   [database.dtlv :as db]
   [taoensso.timbre :as log]))

(def current-schema-version 1)

(defn schema-version []
  (let [result (db/ent-by :db.schema/version [:db.schema/version 1])]
    (if (empty? result)
      0
      (:db.schema/version (first result)))))

(defn set-schema-version [v]
  (let [existing (db/ent-by :db.schema/version [:db.schema/version 1])]
    (if (empty? existing)
      (db/add-ents [{:db.schema/version v}])
      (db/update-ent (:db/id (first existing)) [{:db.schema/version v}]))))

(defn migrate
  "Run pending migrations up to current-schema-version"
  []
  (let [current (schema-version)
        target current-schema-version]
    (log/info "Current schema version:" current "Target:" target)
    (when (< current target)
      (log/info "Running migrations from" current "to" target)
      (doseq [v (range (inc current) (inc target))]
        (log/info "Applying migration" v)
        (case v
          1 (do
              (log/info "Migration 1: Initial schema")
              (set-schema-version 1))
          ;; Add more migrations here
          ))
      (log/info "Migration complete. Schema version:" (schema-version)))))

(defn ensure-schema-version []
  (when (zero? (schema-version))
    (migrate)))