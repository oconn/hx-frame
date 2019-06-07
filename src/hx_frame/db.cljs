(ns hx-frame.db
  (:require
   [react :as react]

   [hx-frame.dispatcher :as dispatcher]
   [hx-frame.registrar :as registrar]))

(def app-state (react/createContext))
(defonce ^{:private true} hx-frame-effect-id ::effect-id)

(defn- is-effect? [event]
  (-> event first (= hx-frame-effect-id)))

(defn state-reducer
  "Dispatches the corresponding event handler"
  [state event]
  (if (is-effect? event)
    (if-let [effect-handler (registrar/get-handler :effect (second event))]
      (do
        (effect-handler (nth event 2))
        state)
      (do
        (js/console.error "Effect " (second event) " not defined.")
        state))
    (if-let [handler (registrar/get-handler :event (first event))]
      (let [updated-context (handler {:db state} event)
            effects (map identity (dissoc updated-context :db))]

        (when (seq effects)
          (doseq [[effect-id effect-data] effects]
            (@dispatcher/dispatch! [hx-frame-effect-id effect-id effect-data])))

        (:db updated-context))
      (do
        (js/console.error "Event " (first event) " not defined.")
        state))))
