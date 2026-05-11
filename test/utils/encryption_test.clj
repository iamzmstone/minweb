(ns utils.encryption-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [utils.encryption :as enc]))

(deftest bytes->hex-test
  (testing "converts bytes to hex"
    (is (= "ff" (enc/bytes->hex [(byte -1)])))
    (is (= "00" (enc/bytes->hex [(byte 0)])))))

(deftest md5-test
  (testing "computes MD5"
    (is (string? (enc/md5 "test")))
    (is (= (enc/md5 "hello") (enc/md5 "hello")))))

(deftest hash-password-test
  (testing "hashes password"
    (let [h (enc/hash-password "pass")]
      (is (string? h))
      (is (some? h)))))

(deftest get-salt-test
  (testing "extracts salt"
    (is (= "5678def" (enc/get-salt "abc123$5678def")))))

(run-tests)