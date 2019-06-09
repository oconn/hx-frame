(ns hx-frame.core
  (:require
   [hx.hooks :as hooks]
   [hx.react :as hx :refer [defnc]]

   [hx-frame.coeffect :as coeffect]
   [hx-frame.db :as db]
   [hx-frame.dispatcher :as dispatcher]
   [hx-frame.interceptor :as interceptor]
   [hx-frame.registrar :as registrar]))

(def ^{:private true} initialized (atom false))
(def dispatch dispatcher/dispatch)

(defn- event-handler->interceptor
  [handler]
  (interceptor/->interceptor
   {:id :event-handler
    :before (fn [context]
              (let [{:keys [db event]} (:coeffects context)]
                (assoc-in context [:effects :db] (handler db event))))}))

(defn- event-fx-handler->interceptor
  [handler]
  (interceptor/->interceptor
   {:id :event-fx-handler
    :before (fn [context]
              (let [{:keys [event] :as coeffects} (:coeffects context)]
                (assoc context :effects (handler coeffects event))))}))

(defn- format-interceptor-chain
  [interceptor-chain]
  (->> interceptor-chain
       flatten
       (remove nil?)))

;;

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
   (register-event handler-id nil handler))
  ([handler-id interceptors handler]
   (registrar/register-handler!
    :event handler-id
    (format-interceptor-chain
     [coeffect/inject-db
      interceptors
      (event-handler->interceptor handler)]))))

(defn register-event-fx
  ([handler-id handler]
   (register-event-fx handler-id nil handler))
  ([handler-id interceptors handler]
   (registrar/register-handler!
    :event handler-id
    (format-interceptor-chain
     [coeffect/inject-db
      interceptors
      (event-fx-handler->interceptor handler)]))))

(defn register-effect
  [handler-id handler]
  (registrar/register-handler! :effect handler-id handler))

(defn register-coeffect
  [handler-id handler]
  (registrar/register-handler! :coeffect handler-id handler))

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

;;

(register-coeffect :db (fn db-coeffects-handler
                         [coeffects]
                         (assoc coeffects :db db/app-state)))
