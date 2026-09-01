(ns middleware.rate-limit-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.common :as c]
   [minweb.middleware.rate-limit :as rl]))

(defn- minutes-ago [n]
  (- (System/currentTimeMillis) (* n 60 1000)))

(deftest get-client-ip-test
  (testing "proxy headers are ignored by default — they are client-forgeable"
    (is (= "127.0.0.1"
           (rl/get-client-ip
            {:headers {"x-forwarded-for" "1.2.3.4"}
             :remote-addr "127.0.0.1"}))
        "spoofed XFF must not become the rate-limit key")
    (is (= "127.0.0.1"
           (rl/get-client-ip
            {:headers {"x-real-ip" "1.2.3.4"}
             :remote-addr "127.0.0.1"})))
    (is (= "unknown" (rl/get-client-ip {}))))

  (testing "proxy headers honoured when :trust-proxy-headers is set"
    (with-redefs [c/env (assoc c/env :trust-proxy-headers true)]
      (is (= "1.2.3.4"
             (rl/get-client-ip {:headers {"x-forwarded-for" "1.2.3.4"}})))
      (is (= "1.2.3.4"
             (rl/get-client-ip
              {:headers {"x-forwarded-for" "1.2.3.4, 10.0.0.1, 10.0.0.2"}}))
          "leftmost XFF entry is the original client")
      (is (= "1.2.3.4"
             (rl/get-client-ip {:headers {"x-real-ip" "1.2.3.4"}})))
      (is (= "127.0.0.1"
             (rl/get-client-ip {:headers {"x-forwarded-for" "  "}
                                :remote-addr "127.0.0.1"}))
          "blank XFF falls back to socket address"))))

(deftest clean-expired-test
  (testing "keeps entries still inside the lockout window"
    (let [now (System/currentTimeMillis)
          attempts {:fresh {:count 3 :ts now}
                    :recent {:count 3 :ts (minutes-ago 5)}
                    :stale {:count 3 :ts (minutes-ago 20)}}
          result (rl/clean-expired attempts now)]
      (is (some? (get result :fresh)))
      (is (some? (get result :recent))
          "regression: a 5-minute-old entry inside the 15-min window survives")
      (is (nil? (get result :stale)))))

  (testing "entries are not all wiped on a same-millisecond sweep"
    (let [now (System/currentTimeMillis)
          attempts {:ip1 {:count 5 :ts (- now 1)}}]
      (is (= attempts (rl/clean-expired attempts now))
          "regression: an entry 1ms old must not be discarded")))

  (testing "drops malformed entries without NPE"
    (let [now (System/currentTimeMillis)]
      (is (nil? (get (rl/clean-expired {:bad {}} now) :bad)))
      (is (nil? (get (rl/clean-expired {:bad {:count 1 :ts nil}} now) :bad))))))

(deftest is-locked?-test
  (testing "detects locked IP"
    (let [now (System/currentTimeMillis)
          _ (reset! rl/login-attempts {:locked-ip {:count 5 :ts now}})
          lock-info (rl/is-locked? :locked-ip now)]
      (is (some? lock-info))
      (is (pos? (:remaining-secs lock-info)))))
  (testing "allows non-locked IP"
    (let [now (System/currentTimeMillis)]
      (reset! rl/login-attempts {:normal-ip {:count 2 :ts now}})
      (is (nil? (rl/is-locked? :normal-ip now)))))
  (testing "lock expires after the window"
    (let [now (System/currentTimeMillis)]
      (reset! rl/login-attempts {:old-ip {:count 5 :ts (minutes-ago 20)}})
      (is (nil? (rl/is-locked? :old-ip now)))))
  (testing "returns nil for malformed entries"
    (reset! rl/login-attempts {:empty-ip {} :nil-ts-ip {:count 5 :ts nil}})
    (is (nil? (rl/is-locked? :empty-ip (System/currentTimeMillis))))
    (is (nil? (rl/is-locked? :nil-ts-ip (System/currentTimeMillis))))))

(deftest wrap-rate-limit-blocks-after-max-attempts-test
  (testing "lockout survives the per-request clean-expired sweep"
    (reset! rl/login-attempts {})
    (let [hits (atom 0)
          handler (fn [_] (swap! hits inc) {:status 200 :body "ok"})
          wrapped (rl/wrap-rate-limit handler ["/login"])
          req {:uri "/login" :remote-addr "9.9.9.9"}]
      ;; Burn through the allowance the way a brute-force attempt would.
      (dotimes [_ (rl/max-attempts)]
        (wrapped req)
        (rl/record-failed "9.9.9.9"))
      (let [resp (wrapped req)
            hits-before @hits]
        (is (contains? #{302 303} (:status resp))
            "regression: limiter must redirect once max attempts is reached")
        (wrapped req)
        (is (= hits-before @hits)
            "handler is not invoked while the IP is locked out"))))

  (testing "unrelated paths are never rate limited"
    (reset! rl/login-attempts {"9.9.9.9" {:count 99 :ts (System/currentTimeMillis)}})
    (let [wrapped (rl/wrap-rate-limit (fn [_] {:status 200 :body "ok"}) ["/login"])]
      (is (= 200 (:status (wrapped {:uri "/dashboard" :remote-addr "9.9.9.9"}))))))

  (testing "record-success clears the failure count"
    (reset! rl/login-attempts {})
    (dotimes [_ (rl/max-attempts)] (rl/record-failed "8.8.8.8"))
    (is (some? (rl/is-locked? "8.8.8.8" (System/currentTimeMillis))))
    (rl/record-success "8.8.8.8")
    (is (nil? (rl/is-locked? "8.8.8.8" (System/currentTimeMillis)))))

  (testing "cleanup drops keys once their window elapses"
    (reset! rl/login-attempts {"1.1.1.1" {:count 5 :ts (minutes-ago 20)}})
    (let [wrapped (rl/wrap-rate-limit (fn [_] {:status 200 :body "ok"}) ["/login"])]
      (is (= 200 (:status (wrapped {:uri "/login" :remote-addr "1.1.1.1"}))))
      (is (empty? @rl/login-attempts)
          "expired entries are evicted, bounding map growth"))))
