(ns middleware.rate-limit-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [middleware.rate-limit :as rl]))

(deftest get-client-ip-test
  (testing "extracts client IP from request"
    (is (= "192.168.1.1" (rl/get-client-ip {:headers {"x-forwarded-for" "192.168.1.1"}})))
    (is (= "10.0.0.1" (rl/get-client-ip {:headers {"x-real-ip" "10.0.0.1"}})))
    (is (= "127.0.0.1" (rl/get-client-ip {:remote-addr "127.0.0.1"})))
    (is (= "unknown" (rl/get-client-ip {})))))

(deftest clean-expired-test
  (testing "removes expired entries"
    (let [now (System/currentTimeMillis)
          old-ts (- now (* 20 60 1000))
          recent-ts now
          attempts {:ip1 {:count 1 :ts old-ts}
                    :ip2 {:count 2 :ts recent-ts}}]
      (let [result (rl/clean-expired attempts now)]
        (is (nil? (get result :ip1)))
        (is (some? (get result :ip2)))))))

(run-tests)