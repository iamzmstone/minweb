(ns middleware.session-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.middleware.session :as session]))

(deftest wrap-session-timeout-test
  (testing "allows fresh sessions"
    (let [handler (fn [_req] {:status 200 :body "ok"})
          wrapped (session/wrap-session-timeout handler)
          req {:session
               {:user-id 1
                :session/created-at
                (- (System/currentTimeMillis) 60000)}} ; 1 min ago
          resp (wrapped req)]
      (is (= 200 (:status resp)))))

  (testing "expires old sessions"
    (let [handler (fn [_req] {:status 200 :body "ok"})
          wrapped (session/wrap-session-timeout handler)
          req {:session
               {:user-id 1
                :session/created-at
                (- (System/currentTimeMillis) 4000000000)}} ; way older than 30 min
          resp (wrapped req)]
      (is (nil? (get-in resp [:session :user-id]))))))

(deftest wrap-session-create-test
  (testing "adds created-at to new session"
    (let [handler (fn [req] {:status 200 :body "ok" :session (get-in req [:session])})
          wrapped (session/wrap-session-create handler)
          req {:session {:user-id 1}}
          resp (wrapped req)]
      (is (some? (get-in resp
                         [:session :session/created-at])))))

  (testing "preserves existing created-at"
    (let [existing-ts (- (System/currentTimeMillis) 10000)
          handler (fn [req]
                    {:status 200 :body "ok"
                     :session (get-in req [:session])})
          wrapped (session/wrap-session-create handler)
          req {:session {:user-id 1
                         :session/created-at existing-ts}}
          resp (wrapped req)]
      (is (= existing-ts
             (get-in resp [:session :session/created-at]))))))