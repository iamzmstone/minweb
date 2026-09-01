(ns db-fixture
  "Test fixture that runs body with an isolated Datalevin conn at a temp path.
   Rebinds minweb.database.dtlv/*conn* so all DB helpers route to the test DB
   without touching the real db/dtlv.db."
  (:require
   [babashka.fs :as fs]
   [clojure.edn :as edn]
   [minweb.database.dtlv]))

;; Pod already loaded by minweb.database.dtlv above — loading it twice breaks
;; the pod's request processor.
(require '[pod.huahaiy.datalevin :as d])

(defmacro with-test-db
  "Run body with a fresh conn at /tmp/dtlv-test-<UUID>. Closes conn + removes
   temp dir in a finally so subsequent test runs aren't blocked by stale
   LMDB locks."
  [& body]
  `(let [path# (str "/tmp/dtlv-test-" (random-uuid))
         conn# (d/get-conn path#
                            (edn/read-string (slurp "schema.edn"))
                            {:closed-schema? true
                             :validate-data? true})]
     (try
       (binding [minweb.database.dtlv/*conn* conn#]
         ~@body)
       (finally
         (try (d/close conn#) (catch Exception _#))
         (try (fs/delete-tree path#) (catch Exception _#))))))
