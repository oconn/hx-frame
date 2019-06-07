(ns hx-frame.core
  (:require
   [hx.hooks :as hooks]
   [hx.react :as hx :refer [defnc]]

   [hx-frame.db :as db]
   [hx-frame.dispatcher :as dispatcher]
   [hx-frame.registrar :as registrar]))

(def ^{:private true} initialized (atom false))
(def dispatch dispatcher/dispatch)

(defn subscribe
  "Listens to global state changes (useContext)"
  [[handler-id & args]]
  (if-let [handler (registrar/get-handler :subscription handler-id)]
    (let [[state _] (hooks/useContext db/app-state)]
      (handler state (into [handler-id] args)))
    (js/console.error "Subscription " handler-id " not defined.")))

(defn register-event
  "Adds an event to the registrar"
  ([handler-id handler]
   (register-event handler-id [] handler))
  ([handler-id interceptors handler]
   (registrar/register-handler! :event handler-id
                                (fn [{:keys [db]} event]
                                  {:db (handler db event)}))))

(defn register-event-fx
  ([handler-id handler]
   (register-event-fx handler-id [] handler))
  ([handler-id interceptors handler]
   (registrar/register-handler! :event handler-id handler)))

(defn register-effect
  [handler-id handler]
  (registrar/register-handler! :effect handler-id handler))

(def register-subscription (partial registrar/register-handler! :subscription))

(defnc Provider
  [{:keys [children initial-state on-init]
    :or {on-init identity}}]
  (let [[state dispatch] (hooks/useReducer db/state-reducer initial-state)]

    ;; Set a global dispatcher to support the ability to directly call it
    (when (false? @initialized)
      (reset! dispatcher/dispatch! dispatch)
      (reset! initialized true)
      (on-init))

    [:provider {:context db/app-state
                :value [state dispatch]}
     children]))
