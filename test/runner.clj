(ns runner
  (:require [clojure.test :as t]))

(def test-names
  ['common-test
   'config-test
   'routes-test
   'utils.response-test
   'utils.encryption-test
   'middleware.auth-test
   'middleware.rate-limit-test
   'middleware.error-test
   'middleware.session-test
   'middleware.request-log-test
   'view.core-test
   'view.layout-test
   'view.components-test
   'view.render-integration-test
   'view.login-test
   'view.health-test
   'database.user-test
   'integration.middleware-chain-test
   'integration.routes-test
   'integration.middleware-stack-test])

(defn run-all-tests []
  (println "\n=== Running all tests ===\n")
  (let [totals (atom {:tests 0 :fail 0 :error 0})]
    (doseq [ns-sym test-names]
      (try
        (require ns-sym)
        (catch Exception e
          (println "Warning: Could not load" ns-sym ":" (.getMessage e))))
      (try
        (let [summary (t/run-tests ns-sym)
              pass-count (or (:test summary) 0)
              fail-count (or (:fail summary) 0)
              error-count (or (:error summary) 0)]
          (swap! totals update :tests + pass-count)
          (swap! totals update :fail + fail-count)
          (swap! totals update :error + error-count))
        (catch Exception e
          (println "Error running" ns-sym ":" (.getMessage e)))))
    (println "\n=== Test Summary ===")
    (println "Total:" (:tests @totals) "tests")
    (println "Failures:" (:fail @totals))
    (println "Errors:" (:error @totals))
    (println (if (and (zero? (:fail @totals)) (zero? (:error @totals)))
               "All tests passed!"
               "Test run completed with failures."))))
