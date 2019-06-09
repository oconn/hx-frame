(ns hx-frame.interceptor
  (:require [hx-frame.registrar :as registrar]))

(defn ->interceptor
  [{:keys [id before after]}]
  {:id (or id :unnamed)
   :before before
   :after after})

(def do-fx
  (->interceptor
   {:id :do-fx
    :after (fn [context]
             (doseq [[effect-key effect-value] (-> context
                                                   :effects
                                                   (dissoc :db))]
               (if-let [effect-fn (registrar/get-handler :effect effect-key)]
                 (effect-fn effect-value)
                 (js/console.error "No effect found for `" effect-key "`")))

             context)}))
