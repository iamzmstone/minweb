(ns minweb.utils.encryption
  (:require [clojure.string :as str])
  (:import [java.security MessageDigest SecureRandom]
           [javax.crypto SecretKeyFactory]
           [javax.crypto.spec PBEKeySpec]))

(def ^:private ^:const iter-count
  ;; PBKDF2-SHA256 — OWASP Password Storage Cheat Sheet 2023 最低推荐。
  ;; 升 iter 时直接改这个常量;hash 格式自带 iter,旧 hash 仍可验证。
  600000)

(def ^:private ^:const salt-bytes 16)
(def ^:private ^:const hash-bytes 16)

(defn- bytes->hex
  [byt]
  (apply str (map #(format "%02x" %) byt)))

(defn- hex->bytes
  [hex]
  (->> (.toByteArray (BigInteger. hex 16))
       (drop-while #(= % 0))
       byte-array))

(defn hash-password
  "Hash password with PBKDF2-SHA256, 16-byte salt, 600k iterations.
   Output: `pbkdf2-sha256$600000$<salt-hex>$<hash-hex>`.
   Format encodes algorithm + iter so future parameter bumps don't need code
   changes; password= reads them back from the stored hash."
  ([password]
   (hash-password
    password
    (let [s (byte-array salt-bytes)]
      (.nextBytes (SecureRandom.) s)
      s)))
  ([password salt]
   (let [spec (PBEKeySpec. (char-array password) salt iter-count (* 8 hash-bytes))
         factory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256")
         digest (.getEncoded (.generateSecret factory spec))]
     (str/join "$" ["pbkdf2-sha256" (str iter-count)
                    (bytes->hex salt) (bytes->hex digest)]))))

(defn- legacy?
  "Legacy 2-part format: <hash-hex>$<salt-hex> (no algo/iter prefix)."
  [password-hash]
  (= 2 (count (str/split password-hash #"\$"))))

(defn password=
  "Constant-time verify. Accepts new (4-part) and legacy (2-part) formats;
   reads algorithm + iter from the stored hash, so a future bump leaves old
   hashes verifiable."
  [password-hash given-password]
  (let [parts (str/split password-hash #"\$")
        [iters salt-hex expected-hex]
        (if (= 4 (count parts))
          [(parse-long (nth parts 1))
           (nth parts 2) (nth parts 3)]
          ;; legacy: <hash-hex>$<salt-hex>, old iters = 65536
          [65536 (nth parts 1) (nth parts 0)])
        salt (hex->bytes salt-hex)
        spec (PBEKeySpec. (char-array given-password) salt iters (* 8 hash-bytes))
        factory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256")
        actual (.getEncoded (.generateSecret factory spec))
        expected (hex->bytes expected-hex)]
    (MessageDigest/isEqual actual expected)))

(defn legacy-hash?
  "True if stored hash uses legacy (weak) params — caller can trigger rehash."
  [password-hash]
  (and password-hash (legacy? password-hash)))
