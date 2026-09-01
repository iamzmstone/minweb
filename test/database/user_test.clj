(ns database.user-test
  (:require
   [clojure.test :refer [deftest is]]
   [db-fixture :refer [with-test-db]]
   [minweb.common :as c]
   [minweb.database.dtlv :as db]
   [minweb.database.user :as user]
   [minweb.utils.encryption :as enc]))

(deftest default-user-creates-test
  (with-test-db
    (let [r (user/default-user)]
      (is (string? (:email r)))
      (is (string? (:password r)))
      (is (= 1 (count (user/all))))
      (is (user/exists? (:init-email c/env))))))

(deftest default-user-idempotent-test
  (with-test-db
    (user/default-user)
    (is (thrown-with-msg? Exception #"already exists"
          (user/default-user)))))

(deftest default-user-force-resets-test
  (with-test-db
    (let [r1 (user/default-user)
          r2 (user/default-user :force)
          users (user/all)]
      (is (= 1 (count users)))
      (is (not= (:password r1) (:password r2))
          "force regenerates random password")
      (is (enc/password= (:user/password (first users)) (:password r2))
          "stored hash matches the newly generated password")
      (is (not (enc/password= (:user/password (first users)) (:password r1)))
          "old password no longer valid"))))

(deftest add-user-test
  (with-test-db
    (user/add {:email "a@b.c" :name "A" :password "Password1!" :role :admin})
    (is (user/exists? "a@b.c"))
    (let [u (user/the-user "a@b.c")]
      (is (= "A" (:user/name u)))
      (is (enc/password= (:user/password u) "Password1!"))
      (is (not (enc/legacy-hash? (:user/password u)))
          "new add produces new-format hash"))))

(deftest change-password-test
  (with-test-db
    (user/add {:email "a@b.c" :name "A" :password "OldPass1!" :role :admin})
    (let [u (user/the-user "a@b.c")]
      (user/change-password (:db/id u) "NewPass1!")
      (let [u' (user/the-user "a@b.c")]
        (is (enc/password= (:user/password u') "NewPass1!"))
        (is (not (enc/password= (:user/password u') "OldPass1!")))))))

(deftest update-user-test
  (with-test-db
    (user/add {:email "a@b.c" :name "A"
               :password "Password1!" :role :admin
               :privs [:project :search]})
    (let [u (user/the-user "a@b.c")]
      (user/update-user (:db/id u)
                        {:name "Renamed" :privs [:admin]})
      (let [u' (user/the-user "a@b.c")]
        (is (= "Renamed" (:user/name u')))
        (is (= #{:admin} (set (:user/privs u')))
            "old privs retracted, new privs set")))))

(deftest correct-password?-upgrades-legacy-test
  (with-test-db
    (user/add {:email "a@b.c" :name "A" :password "Password1!" :role :admin})
    ;; Inject legacy-format hash into DB
    (let [u (user/the-user "a@b.c")
          salt (byte-array 4)
          _ (.nextBytes (java.security.SecureRandom.) salt)
          spec (javax.crypto.spec.PBEKeySpec.
                (char-array "Password1!") salt 65536 128)
          factory (javax.crypto.SecretKeyFactory/getInstance
                   "PBKDF2WithHmacSHA256")
          digest (.getEncoded (.generateSecret factory spec))
          to-hex (fn [bs] (apply str (map #(format "%02x" %) bs)))
          legacy (str (to-hex digest) "$" (to-hex salt))]
      (db/update-ent (:db/id u) [{:user/password legacy}])
      (is (enc/legacy-hash? (:user/password (user/the-user "a@b.c")))
          "legacy hash injected"))
    ;; correct-password? should verify and rehash
    (is (true? (user/correct-password? "a@b.c" "Password1!")))
    (let [u' (user/the-user "a@b.c")]
      (is (not (enc/legacy-hash? (:user/password u')))
          "hash upgraded to new format on successful login"))
    ;; idempotency: second call works without double-upgrade
    (is (true? (user/correct-password? "a@b.c" "Password1!")))))
