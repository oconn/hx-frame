(ns hx-frame.interceptor-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]

   [hx-frame.interceptor :as i]))

(def inc-counter
  (i/->interceptor
   {:id :inc-counter
    :before #(update-in % [:coeffects :db :counter] inc)}))

(def skip-rest
  (i/->interceptor
   {:id :skip-rest
    :before #(assoc % :queue #queue [])}))

(deftest ->interceptor
  (testing "Conforms map to interceptor spec"
    (is (= {:id :unnamed
            :before nil
            :after nil} (i/->interceptor nil)))))

(deftest process-interceptor-chain
  (let [state {:counter 0}
        event [:inc-counter]]
    (testing "When no interceptor chain is passed, coeffects remain unchanged."
      (is (= (:coeffects (i/process-interceptor-chain state event []))
             {:db state
              :event event})))

    (testing "Interceptors apply changes"
      (is (= (get-in (i/process-interceptor-chain state event [inc-counter])
                     [:coeffects :db :counter])
             1))
      (is (= (get-in (i/process-interceptor-chain state event [inc-counter
                                                               inc-counter
                                                               inc-counter])
                     [:coeffects :db :counter])
             3)))

    (testing "Interceptors can modify the queue"
      (is (= (get-in (i/process-interceptor-chain state event [inc-counter
                                                               inc-counter
                                                               skip-rest
                                                               inc-counter
                                                               inc-counter])
                     [:coeffects :db :counter])
             2)))))
