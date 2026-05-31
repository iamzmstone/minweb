(ns common-test
  (:require
   [clojure.test :refer [deftest is testing run-tests]]
   [minweb.common :as c]))

(deftest windows-test
  (testing "detects Windows OS"
    (is (boolean? (c/windows)))))

(deftest rand-uuid-test
  (testing "generates random UUID"
    (let [uuid1 (c/rand-uuid)
          uuid2 (c/rand-uuid)]
      (is (uuid? uuid1))
      (is (not= uuid1 uuid2)))))

(deftest to-bj-time-test
  (testing "converts UTC to Beijing time"
    (let [result (c/to-bj-time "2024-01-15T10:30:00Z")]
      (is (string? result))
      (is (= "2024-01-15" (subs result 0 10)))))
  (testing "custom format"
    (let [result (c/to-bj-time "2024-01-15T10:30:00Z" {:format "yyyy-MM"})]
      (is (= "2024-01" result)))))

(deftest cnt-space-test
  (testing "counts spaces in string"
    (is (= 3 (c/cnt-space "a b c d")))
    (is (= 0 (c/cnt-space "abc")))
    (is (= 1 (c/cnt-space "a b")))))

(deftest trunc-str-test
  (testing "truncates long strings"
    (is (= "abc..." (c/trunc-str "abcdefghij" 6)))
    (is (= "short" (c/trunc-str "short" 10))))
  (testing "edge cases"
    (is (= "" (c/trunc-str "" 5)))
    (is (= "ab" (c/trunc-str "ab" 3)))))

(deftest yes?-test
  (testing "recognizes yes variants"
    (is (true? (c/yes? "yes")))
    (is (true? (c/yes? "YES")))
    (is (true? (c/yes? "Yes")))
    (is (false? (c/yes? "no")))
    (is (false? (c/yes? nil)))
    (is (false? (c/yes? "")))))

(deftest to-coll-test
  (testing "converts to collection"
    (is (= [1 2 3] (c/to-coll [1 2 3])))
    (is (= [1] (c/to-coll 1)))
    (is (nil? (c/to-coll nil)))))

(deftest pages-test
  (testing "calculates page count"
    (is (= 10 (c/pages 100 10)))
    (is (= 11 (c/pages 101 10)))
    (is (= 0 (c/pages 0 10)))
    (is (= 1 (c/pages 5 10)))))

(deftest valid-mobile?-test
  (testing "validates mobile numbers"
    (is (true? (c/valid-mobile? "13812345678")))
    (is (true? (c/valid-mobile? "19912345678")))
    (is (false? (c/valid-mobile? "12345678901")))
    (is (false? (c/valid-mobile? "1234567890")))
    (is (false? (c/valid-mobile? "abc")))
    (is (false? (c/valid-mobile? nil)))))

(deftest valid-ip?-test
  (testing "validates IP addresses"
    (is (true? (c/valid-ip? "192.168.1.1")))
    (is (true? (c/valid-ip? "0.0.0.0")))
    (is (true? (c/valid-ip? "255.255.255.255")))
    (is (false? (c/valid-ip? "256.1.1.1")))
    (is (false? (c/valid-ip? "192.168.1")))
    (is (false? (c/valid-ip? "192.168.1.1.1")))
    (is (false? (c/valid-ip? "abc")))))

(deftest ip-to-int-test
  (testing "converts IP to integer"
    (is (= 3232235777 (c/ip-to-int "192.168.1.1")))
    (is (= 0 (c/ip-to-int "0.0.0.0")))
    (is (= 255 (c/ip-to-int "0.0.0.255")))
    (is (= nil (c/ip-to-int "invalid")))))

(deftest ip-in-net-test
  (testing "checks if IP is in network"
    (is (true? (c/ip-in-net "192.168.1.100" "255.255.255.0" "192.168.1.1")))
    (is (false? (c/ip-in-net "192.168.2.100" "255.255.255.0" "192.168.1.1")))
    (is (true? (c/ip-in-net "10.0.0.1" "255.0.0.0" "10.0.0.0")))))

(deftest round-to-test
  (testing "rounds float to precision"
    (let [result (c/round-to 3.14159 2)]
      (is (> result 3.13))
      (is (< result 3.15)))
    (is (= (c/round-to 3.001 1) 3.0))))

(deftest timestamp-test
  (testing "returns current timestamp"
    (is (pos? (c/timestamp)))
    (is (number? (c/timestamp)))))

(deftest format-time-test
  (testing "formats current time"
    (let [result (c/format-time "yyyy-MM-dd")]
      (is (string? result))
      (is (= 10 (count result))))))

(deftest current-time-test
  (testing "returns current time string"
    (let [result (c/current-time)]
      (is (string? result))
      (is (re-matches #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}" result)))))

(deftest today-test
  (testing "returns today's date"
    (let [result (c/today)]
      (is (string? result))
      (is (re-matches #"\d{4}-\d{2}-\d{2}" result)))))

(deftest humanize-test
  (testing "converts seconds to human readable"
    (is (= "1 d 0 h 0 m 0 s" (c/humanize 86400)))
    (is (= "0 d 1 h 0 m 0 s" (c/humanize 3600)))
    (is (= "0 d 0 h 1 m 30 s" (c/humanize 90)))
    (is (= "0 d 0 h 0 m 0 s" (c/humanize 0)))))

(deftest to-int-test
  (testing "extracts integer from string"
    (is (= 123 (c/to-int "abc123def")))
    (is (= 456 (c/to-int "456")))
    (is (= nil (c/to-int "no-number")))
    (is (= nil (c/to-int "")))))

(deftest encode-base64-test
  (testing "encodes to base64"
    (is (= "aGVsbG8=" (c/encode-base64 "hello")))
    (is (= "dGVzdA==" (c/encode-base64 "test")))))

(deftest decode-base64-test
  (testing "decodes from base64"
    (is (= "hello" (c/decode-base64 "aGVsbG8=")))
    (is (= "test" (c/decode-base64 "dGVzdA==")))))

(deftest urlencode-test
  (testing "URL encodes string"
    (is (= "hello+world" (c/urlencode "hello world")))
    (is (= "a%3Db" (c/urlencode "a=b")))))

(deftest urldecode-test
  (testing "URL decodes string"
    (is (= "hello world" (c/urldecode "hello+world")))
    (is (= "a=b" (c/urldecode "a%3Db")))))

(deftest parse-q-test
  (testing "parses query string"
    (let [result (c/parse-q "a=1&b=2")]
      (is (= {:a "1" :b "2"} result)))))

(deftest upd-vals-test
  (testing "updates multiple keys with function"
    (is (= {:a 2 :b 3 :c 3} (c/upd-vals {:a 1 :b 2 :c 3} [:a :b] inc)))))

(deftest valid-password?-test
  (testing "validates password complexity"
    (is (true? (c/valid-password? "Pass123!")))
    (is (true? (c/valid-password? "Abc12345@#$")))
    (is (false? (c/valid-password? "Short1!")))   ; only 7 chars - too short
    (is (false? (c/valid-password? "nouppercase123!")))
    (is (false? (c/valid-password? "NOLOWERCASE123!")))
    (is (false? (c/valid-password? "NoDigits!!!")))
    (is (false? (c/valid-password? "NoSpecialChar123")))
    (is (false? (c/valid-password? "")))
    (is (false? (c/valid-password? nil)))))

(run-tests)