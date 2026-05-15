(ns test-runner
  (:require [clojure.test :refer [run-tests]]
            [common-test]
            [utils.response-test]
            [utils.encryption-test]
            [middleware.auth-test]
            [middleware.rate-limit-test]
            [view.core-test]
            [view.layout-test]
            [routes-test]))

(run-tests)
