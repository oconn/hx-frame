(ns hx-frame.effects
  (:require
   [hx-frame.dispatcher :as dispatcher]
   [hx-frame.registrar :as registrar]))

(defn register-effect
  [handler-id handler]
  (registrar/register-handler! :effect handler-id handler))

(register-effect
 :dispatch
 (fn [value]
   (if-not (vector? value)
     (js/console.error "dispatch expected a vector but got: " value)
     (dispatcher/dispatch value))))

(register-effect
  :dispatch-n
  (fn [value]
    (if-not (sequential? value)
      (js/console.error ":dispatch-n expected a collection but got: " value)
      (doseq [event (remove nil? value)]
        (dispatcher/dispatch event)))))
