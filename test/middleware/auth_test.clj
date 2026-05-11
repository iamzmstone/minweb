(ns middleware.auth-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [middleware.auth :as auth]))

(deftest path-restricted?-test
  (testing "detects restricted paths"
    (is (true? (auth/path-restricted? "/admin")))
    (is (true? (auth/path-restricted? "/switch")))
    (is (true? (auth/path-restricted? "/search")))
    (is (true? (auth/path-restricted? "/olt")))
    (is (true? (auth/path-restricted? "/alert"))))
  (testing "allows non-restricted paths"
    (is (false? (auth/path-restricted? "/login")))
    (is (false? (auth/path-restricted? "/static/css/style.css")))))

(deftest authorized?-test
  (testing "admin has access to all"
    (let [admin-user {:user/role :admin :user/email "admin@test.com"}]
      (is (true? (auth/authorized? admin-user "/admin")))
      (is (true? (auth/authorized? admin-user "/any-path")))))
  (testing "normal user with search privilege"
    (let [user {:user/role :normal :user/privs [:search]}]
      (is (true? (auth/authorized? user "/search")))
      (is (true? (auth/authorized? user "/")))
      (is (true? (auth/authorized? user "/chpwd")))
      (is (false? (auth/authorized? user "/admin")))))
  (testing "normal user with switch privilege"
    (let [user {:user/role :normal :user/privs [:switch]}]
      (is (true? (auth/authorized? user "/switch")))
      (is (true? (auth/authorized? user "/sw-info")))
      (is (false? (auth/authorized? user "/search"))))))

(run-tests)