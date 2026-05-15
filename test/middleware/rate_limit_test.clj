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
                    :ip2 {:count 2 :ts recent-ts}}
          result (rl/clean-expired attempts now)]
      (is (nil? (get result :ip1)))
      (is (some? (get result :ip2)))))
  (testing "handles empty entry map without NPE"
    (let [now (System/currentTimeMillis)
          attempts {:bad-ip {}
                    :good-ip {:count 1 :ts (- now (* 20 60 1000))}}
          result (rl/clean-expired attempts now)]
      (is (nil? (get result :bad-ip)))
      (is (nil? (get result :good-ip)))))
  (testing "handles entry with nil ts without NPE"
    (let [now (System/currentTimeMillis)
          attempts {:bad-ip {:count 1 :ts nil}}
          result (rl/clean-expired attempts now)]
      (is (nil? (get result :bad-ip))))))

(deftest is-locked?-test
  (testing "detects locked IP"
    (let [now (System/currentTimeMillis)
          _ (reset! rl/login-attempts {:locked-ip {:count 5 :ts now}})
          lock-info (rl/is-locked? :locked-ip now)]
      (is (some? lock-info))
      (is (pos? (:remaining-secs lock-info)))))
  (testing "allows non-locked IP"
    (let [now (System/currentTimeMillis)
          _ (reset! rl/login-attempts {:normal-ip {:count 2 :ts now}})]
      (is (nil? (rl/is-locked? :normal-ip now)))))
  (testing "returns nil for empty entry"
    (let [now (System/currentTimeMillis)
          _ (reset! rl/login-attempts {:empty-ip {}})]
      (is (nil? (rl/is-locked? :empty-ip now)))))
  (testing "returns nil for entry with nil ts"
    (let [now (System/currentTimeMillis)
          _ (reset! rl/login-attempts {:nil-ts-ip {:count 5 :ts nil}})]
      (is (nil? (rl/is-locked? :nil-ts-ip now))))))

(run-tests)