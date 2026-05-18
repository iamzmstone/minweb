(ns common
  (:require
   [cprop.core :refer [load-config]]
   [clojure.java.io :as io]
   [clojure.data.csv :as csv]
   [cheshire.core :as json]
   [clojure.string :as str])
  (:import
   [java.time Instant ZoneId ZonedDateTime]
   [java.time.format DateTimeFormatter]))

(defn env1
  [s]
  (System/getenv s))

(def Tpl-root "template")
(def Out-dir "out")
(def Public-dir "resources/public")
(def Utf8-bom "\uFEFF")

(def env
  (let [sys-env (System/getenv)
        minweb-env (into {}
                        (filter (fn [[k _]]
                                  (str/starts-with? (name k) "MINWEB_"))
                                sys-env))]
    (load-config
     :file "resources/config.edn"
     :env minweb-env)))

(defn windows
  []
  (str/includes? (System/getProperty "os.name") "Win"))

(defn rand-uuid
  []
  (java.util.UUID/randomUUID))

(defn rand-password
  "Generate a random 12-character password"
  []
  (let [chars "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"]
    (apply str (take 12 (repeatedly #(nth chars (rand-int (count chars))))))))

(defn valid-password?
  "Validate password complexity:
   - At least 8 characters
   - At least one uppercase letter
   - At least one lowercase letter
   - At least one digit
   - At least one special character (!@#$%^&*()_+-=)"
  [password]
  (boolean
   (when (and password (string? password) (>= (count password) 8))
     (let [has-upper (re-find #"[A-Z]" password)
           has-lower (re-find #"[a-z]" password)
           has-digit (re-find #"\d" password)
           has-special (re-find #"[!@#$%^&*()_+\-=]" password)]
       (and has-upper has-lower has-digit has-special)))))

(defn to-bj-time
  "opt can be {:format yyyy-MM-dd HH:mm:ss.SSSXXX}"
  [utc-time-str & opt]
  (let [instant (Instant/parse utc-time-str)
        beijing-zone (ZoneId/of "Asia/Shanghai")
        beijing-time (ZonedDateTime/ofInstant instant beijing-zone)
        {:keys [format] :or {format "yyyy-MM-dd HH:mm:ss"}} opt
        formatter (DateTimeFormatter/ofPattern format)]
    (.format formatter beijing-time)))

(defn cnt-space
  [s]
  (count (filter #(= \space %) s)))

(defn trunc-str
  [s len]
  (if (> (count s) len)
    (str (subs s 0 (- len 3)) "...")
    s))

(defn yes?
  [s]
  (if (and s (= "yes" (str/lower-case s))) true false))

(defn to-coll
  [x]
  (when x (if (coll? x) x [x])))

(defn pages
  [cnt page-size]
  (if (> (mod cnt page-size) 0)
    (inc (int (/ cnt page-size)))
    (/ cnt page-size)))

(defn valid-mobile?
  [s]
  (if (string? s)
    (boolean (re-matches #"^1[3-9]\d{9}$" s))
    false))

(defn valid-ip?
  [s]
  (let [parts (str/split s #"\.")
        valid-part? (fn [part]
                      (and (re-matches #"\d+" part)
                           (<= 0 (parse-long part) 255)
                           (or (= part "0")
                               (not (str/starts-with?
                                     part "0")))))]
    (and (= (count parts) 4)
         (every? valid-part? parts))))

(defn ip-to-int
  [ip]
  (when ip
    (try
      (reduce (fn [sum octet]
                (+ (bit-shift-left sum 8)
                   (parse-long octet)))
              0 (str/split ip #"\."))
      (catch Exception _ nil))))

(defn ip-in-net
  [ip mask gw]
  (let [ip (ip-to-int ip)
        mask (ip-to-int mask)
        gw (ip-to-int gw)
        network (bit-and gw mask)
        broadcast (bit-or network
                          (bit-and
                           (bit-not mask) 0xFFFFFFFF))]
    (and (>= ip network) (<= ip broadcast))))

(defn round-to
  "round a float to a given precision"
  [f p]
  (let [fmt (str "%.0" p "f")]
    (Float/parseFloat (format fmt (float f)))))

(defn timestamp
  []
  (System/currentTimeMillis))

(defn format-time
  [fmt]
  (.format
   (java.time.LocalDateTime/now)
   (java.time.format.DateTimeFormatter/ofPattern fmt)))

(defn current-time
  []
  (format-time "yyyy-MM-dd HH:mm:ss"))

(defn today
  []
  (format-time "yyyy-MM-dd"))

(defn duration-s
  "Get duration in second for 2 java.time vars"
  [t1 t2]
  (.between java.time.temporal.ChronoUnit/SECONDS t1 t2))

(defn humanize
  "Convert duration from second to human readable format 'x d x h x m x s'"
  [n]
  (let [d (quot n (* 3600 24))
        h (quot (mod n (* 3600 24)) 3600)
        m (quot (mod n 3600) 60)
        s (mod n 60)]
    (format "%d d %d h %d m %d s" d h m s)))

(defn humanize-between
  "Humanize duration between 2 java.time vars"
  [t1 t2]
  (-> (duration-s t1 t2) humanize))

(defn to-int
  [^String s]
  (when-let [d (last (re-find #"(\d+)" s))]
    (parse-long d)))

(defn load-flds
  [f re]
  (let [s (slurp f)]
    (when (not-empty s)
      (some->> s str/split-lines
               (map #(str/split % re))))))

(defn load-csv
  [f & opts]
  (let [{:keys [sep q] :or {sep \, q \"}} opts
        opts {:separator sep :quote q}]
    (with-open [reader (io/reader f)]
      (try
        (doall
         (csv/read-csv reader opts))
        (catch Exception e
          [:err (.getMessage e)])))))

(defn log
  [^String s]
  (println (current-time) ": " s))

(defn to-json
  [data]
  (json/generate-string data {:escape-non-ascii true}))

(defn from-json
  [s]
  (json/parse-string s true))

(defn encode-base64
  [s]
  (.encodeToString (java.util.Base64/getEncoder) (.getBytes s)))

(defn decode-base64
  [s]
  (String. (.decode (java.util.Base64/getDecoder) s)))

(defn urlencode
  [s]
  (java.net.URLEncoder/encode s "UTF-8"))

(defn urldecode
  [s]
  (java.net.URLDecoder/decode s "UTF-8"))

(defn parse-q
  "parse query-string of url"
  [q]
  (let [kvs (->> (str/split q #"&")
                 (map #(str/split % #"="))
                 (map #(vector (keyword (first %))
                               (second %))))]
    (into {} kvs)))

(defn inst-to-s
  [inst]
  (let [fmt (java.text.SimpleDateFormat.
             "yyyy-MM-dd HH:mm:ss")]
    (.format fmt inst)))

(defn upd-vals
  [m ks f]
  (reduce #(update %1 %2 f) m ks))
