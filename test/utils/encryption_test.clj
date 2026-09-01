(ns utils.encryption-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [minweb.utils.encryption :as enc])
  (:import
   [java.security SecureRandom]
   [javax.crypto SecretKeyFactory]
   [javax.crypto.spec PBEKeySpec]))

(defn- legacy-hash
  "Build a legacy 2-part hash (<hash-hex>$<salt-hex>) with the old params
   (4-byte salt, 65536 iter) — only for testing the backward-compat verify path."
  [password]
  (let [salt (byte-array 4)
        _ (.nextBytes (SecureRandom.) salt)
        spec (PBEKeySpec. (char-array password) salt 65536 128)
        factory (SecretKeyFactory/getInstance "PBKDF2WithHmacSHA256")
        digest (.getEncoded (.generateSecret factory spec))
        to-hex (fn [bs] (apply str (map #(format "%02x" %) bs)))]
    (str (to-hex digest) "$" (to-hex salt))))

(deftest bytes->hex-test
  (testing "bytes round-trip"
    (let [b (byte-array [(byte -1) (byte 0) (byte 127)])]
      (is (= "ff007f" (#'enc/bytes->hex b))))))

(deftest hash-password-format-test
  (testing "new hash format is 4-part with algo + iter prefix"
    (let [h (enc/hash-password "password")
          parts (str/split h #"\$")]
      (is (= 4 (count parts)))
      (is (= "pbkdf2-sha256" (first parts)))
      (is (= "600000" (second parts)))
      (is (= 32 (count (nth parts 2))) "salt: 16 bytes = 32 hex chars")
      (is (= 32 (count (nth parts 3))) "hash: 16 bytes = 32 hex chars"))))

(deftest hash-password-random-salt-test
  (testing "different calls produce different salts"
    (let [h1 (enc/hash-password "same")
          h2 (enc/hash-password "same")]
      (is (not= h1 h2)))))

(deftest password=-roundtrip-test
  (testing "new-format hash verifies correct password"
    (let [h (enc/hash-password "secret")]
      (is (enc/password= h "secret"))
      (is (not (enc/password= h "wrong"))))))

(deftest password=-legacy-compat-test
  (testing "legacy 2-part hash still verifies"
    (let [h (legacy-hash "secret")]
      (is (enc/password= h "secret"))
      (is (not (enc/password= h "wrong"))))))

(deftest legacy-hash?-test
  (testing "detects format"
    (is (enc/legacy-hash? (legacy-hash "x")))
    (is (not (enc/legacy-hash? (enc/hash-password "x"))))
    (is (not (enc/legacy-hash? nil)))))
