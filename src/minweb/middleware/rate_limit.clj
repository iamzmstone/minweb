(ns minweb.middleware.rate-limit
  (:require
   [clojure.string :as str]
   [minweb.common :refer [env]]
   [taoensso.timbre :as log]
   [minweb.utils.response :as r]))

(def login-attempts (atom {}))

(defn max-attempts []
  (or (env :rate-limit-max-attempts) 5))

(defn lockout-minutes []
  (or (env :rate-limit-lockout-minutes) 15))

(defn lockout-window-ms []
  (* (lockout-minutes) 60 1000))

(defn get-client-ip
  "Client IP for rate-limit keying. X-Forwarded-For / X-Real-IP are only
   honoured when :trust-proxy-headers is set, because any client can forge
   them — untrusted, they let an attacker rotate the key to dodge the limit
   or pin a victim's IP to lock them out. Behind a proxy, XFF is a
   comma-separated chain; the leftmost entry is the original client."
  [req]
  (or (when (env :trust-proxy-headers)
        (or (some-> (get-in req [:headers "x-forwarded-for"])
                    (str/split #",")
                    first
                    str/trim
                    not-empty)
            (get-in req [:headers "x-real-ip"])))
      (:remote-addr req)
      "unknown"))

(defn clean-expired
  "Drop entries whose lockout window has elapsed. Keeps an entry while
   now < ts + window — note ts is the *first* failure, so the comparison must
   be against the window end, not against now itself."
  [attempts now]
  (let [window (lockout-window-ms)]
    (into {} (filter (fn [[_ip entry]]
                       (when-let [ts (:ts entry)]
                         (< now (+ ts window))))
                     attempts))))

(defn is-locked? [ip now]
  (let [attempts @login-attempts
        entry (get attempts ip)
        attempt-count (:count entry)
        ts (:ts entry)]
    (when (and entry attempt-count ts)
      (let [expiry (+ ts (lockout-window-ms))]
        (when (and (>= attempt-count (max-attempts)) (< now expiry))
          {:remaining-secs (quot (- expiry now) 1000)
           :attempts attempt-count})))))

(defn record-failed [ip]
  (let [now (System/currentTimeMillis)
        attempts @login-attempts
        entry (get attempts ip)
        attempt-count (if entry (:count entry) 0)
        ts (if entry (:ts entry) now)]
    (swap! login-attempts assoc ip {:count (inc attempt-count) :ts ts})
    (when (>= (inc attempt-count) (max-attempts))
      (log/warn "IP" ip "locked out due to" (inc attempt-count) "failed attempts"))))

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
