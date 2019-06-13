(ns hx-frame.interceptor-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]

   [hx-frame.interceptor :as i]))

(deftest ->interceptor
  (testing "Conforms map to interceptor spec"
    (is (= {:id :unnamed
            :before nil
            :after nil} (i/->interceptor nil)))))
