(ns view.login-test
  (:require [clojure.test :refer [deftest is testing run-tests]]
            [minweb.view.login :as login]))

(deftest validate-passwd-test
  (testing "validates password requirements"
    (is (true? (login/validate-passwd "123456" "123456")))
    (is (true? (login/validate-passwd "password123" "password123")))
    (is (false? (login/validate-passwd "12345" "12345")))
    (is (false? (login/validate-passwd "123456" "123457")))
    (is (false? (login/validate-passwd "short" "short")))
    (is (false? (login/validate-passwd "longerpassword" "diffpassword")))))

(run-tests)