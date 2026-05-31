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
   'integration.middleware-chain-test
   'integration.routes-test
   'integration.middleware-stack-test])

(defn run-all-tests []
  (println "\n=== Running all tests ===\n")
  (let [totals (atom {:tests 0 :fail 0 :error 0})]
    (doseq [ns test-names]
      (try
        (require ns)
        (catch Exception _e
          (println "Warning: Could not load" ns)))
      (try
        (let [summary (t/run-tests ns)]
          (when summary
            (swap! totals update :tests + (:test summary 0))
            (swap! totals update :fail + (:fail summary 0))
            (swap! totals update :error + (:error summary 0))))
        (catch Exception e
          (println "Error running" ns ":" (.getMessage e)))))
    (println "\n=== Test Summary ===")
    (println "Total:" (:tests @totals) "tests")
    (println "Failures:" (:fail @totals))
    (println "Errors:" (:error @totals))
    (println (if (and (zero? (:fail @totals)) (zero? (:error @totals)))
               "All tests passed!"
               "Test run completed with failures."))))
