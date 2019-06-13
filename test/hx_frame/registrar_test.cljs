(ns hx-frame.registrar-test
  (:require
   [cljs.test :refer-macros [deftest is testing]]

   [hx-frame.registrar :as r]))

(deftest register-handler!
  (testing "Adds a handler to the registry"
    (is (empty? (:event @r/registrar)))
    (r/register-handler! :event :event/name (fn []))
    (is (not (empty? (:event @r/registrar)))))

  (testing "Does not add a handler with an supported type"
    (is (nil? (:some-unsupported-type @r/registrar)))
    (r/register-handler! :some-unsupported-type :event/unsupported (fn []))
    (is (nil? (:some-unsupported-type @r/registrar)))))

(deftest get-handler
  (testing "Returns a handler given it is registered"
    (is (some? (r/get-handler :event :event/name))))

  (testing "Returns nil given the requested handler is not registered"
    (is (nil? (r/get-handler :event :event/unsupported)))))

(deftest get-handlers-by-type
  (testing "Returns a map of handlers when they exist"
    (is (map? (r/get-handlers-by-type :event)))
    (is (= 1 (count (r/get-handlers-by-type :event)))))

  (testing "Returns nil when requesting a handler-type that is not supported"
    (is (nil? (r/get-handlers-by-type :unsupported)))))

(deftest is-registered?
  (testing "Returns true when a handler is registered"
    (is (true? (r/is-registered? :event :event/name))))

  (testing "Returns false when a handler is not registered"
    (is (false? (r/is-registered? :event :event/not-registered)))))

(deftest unregister-handler!
  (testing "Is a noop if the handler / handler-type does not exist"
    (let [before-val @r/registrar]
      (is (nil? (r/unregister-handler! :unsupported-type :unsupported/handler)))
      (is (= before-val @r/registrar))))

  (testing "Removes a handler if it exists"
    (let [before-val @r/registrar]
      (is (nil? (r/unregister-handler! :event :event/name)))
      (is (not= before-val @r/registrar)))))
