(ns middleware.rate-limit
  (:require
   [common :refer [env]]
   [taoensso.timbre :as log]
   [utils.response :as r]
   [clojure.string :as str]))

(def login-attempts (atom {}))

(defn get-client-ip [req]
  (or (get-in req [:headers "x-forwarded-for"])
      (get-in req [:headers "x-real-ip"])
      (get-in req [:remote-addr])
      "unknown"))

(defn max-attempts []
  (or (env :rate-limit-max-attempts) 5))

(defn lockout-minutes []
  (or (env :rate-limit-lockout-minutes) 15))

(defn clean-expired [attempts now]
  (into {} (filter (fn [[ip entry]]
                     (when-let [ts (:ts entry)]
                       (<= now ts)))
                   attempts)))

(defn is-locked? [ip now]
  (let [attempts @login-attempts
        entry (get attempts ip)
        count (:count entry)
        ts (:ts entry)]
    (when (and entry count ts)
      (let [expiry (+ ts (* (lockout-minutes) 60 1000))]
        (when (and (>= count (max-attempts)) (< now expiry))
          {:remaining-secs (quot (- expiry now) 1000)
           :attempts count})))))

(defn record-failed [ip]
  (let [now (System/currentTimeMillis)
        attempts @login-attempts
        entry (get attempts ip)
        count (if entry (:count entry) 0)
        ts (if entry (:ts entry) now)]
    (swap! login-attempts assoc ip {:count (inc count) :ts ts})
    (when (>= (inc count) (max-attempts))
      (log/warn "IP" ip "locked out due to" (inc count) "failed attempts"))))

(defn record-success [ip]
  (swap! login-attempts update ip dissoc :count))

(defn wrap-rate-limit
  [handler paths]
  (fn [req]
    (let [ip (get-client-ip req)
          path (:uri req)
          now (System/currentTimeMillis)]
      (swap! login-attempts clean-expired now)
      (if (some #(= path %) paths)
        (if-let [lock-info (is-locked? ip now)]
          (do
            (log/warn "Rate limit hit for IP" ip)
            (r/flash-msg
             (r/redirect "/login")
             "danger" (str "登录过于频繁，请" (:remaining-secs lock-info) "秒后重试")))
          (handler req))
        (handler req)))))
