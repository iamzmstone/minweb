(ns view.login-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [minweb.view.login :as login]))

(deftest validate-passwd-test
  (testing "validates password requirements"
    (is (true? (login/validate-passwd
                "Shaoxing1!" "Shaoxing1!")))
    (is (true? (login/validate-passwd
                "Cloud8@Dx" "Cloud8@Dx")))
    (is (false? (login/validate-passwd "12345" "12345")))
    (is (false? (login/validate-passwd "123456" "123457")))
    (is (false? (login/validate-passwd "short" "short")))
    (is (false? (login/validate-passwd
                 "longerpassword" "diffpassword")))))
