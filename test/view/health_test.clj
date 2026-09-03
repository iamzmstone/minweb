(ns view.health-test
  (:require
   [cheshire.core :as json]
   [clojure.test :refer [deftest is]]
   [db-fixture :refer [with-test-db]]
   [minweb.database.dtlv :as db]
   [minweb.view.health :as health]
   [taoensso.timbre :as log]))

(defmacro ^:private quietly
  "Suppress timbre output — health-page is *supposed* to log the probe failure
   we inject on purpose, and its stacktrace otherwise buries the test summary."
  [& body]
  `(binding [log/*config* (assoc log/*config* :min-level :fatal)]
     ~@body))

(deftest health-ok-when-db-reachable-test
  (with-test-db
    (let [resp (health/health-page {})
          body (json/parse-string (:body resp) true)]
      (is (= 200 (:status resp)))
      (is (= "application/json" (get-in resp [:headers "Content-Type"])))
      (is (= "ok" (:status body)))
      (is (= "ok" (:db body)))
      (is (pos-int? (:timestamp body))))))

(deftest health-degraded-when-db-unreachable-test
  (with-redefs [db/ping (fn [] (throw (ex-info "conn closed" {})))]
    (let [resp (quietly (health/health-page {}))
          body (json/parse-string (:body resp) true)]
      (is (= 503 (:status resp)))
      (is (= "degraded" (:status body)))
      (is (= "error" (:db body)))
      (is (not (re-find #"conn closed" (:body resp)))
          "internal error detail must not leak to an unauthenticated endpoint"))))

(deftest ping-survives-empty-and-populated-db-test
  (with-test-db
    (is (nil? (db/ping)) "empty DB: query succeeds, aggregate yields nothing")
    (db/add-user {:email "a@b.c" :name "A" :password "Password1!"})
    (is (= 1 (db/ping)))))
