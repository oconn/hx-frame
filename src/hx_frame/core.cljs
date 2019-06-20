(ns hx-frame.core
  (:require
   [hx.hooks :as hooks]
   [hx.react :as hx :refer [defnc]]

   [hx-frame.db :as db]
   [hx-frame.interceptor :as interceptor]
   [hx-frame.registrar :as registrar]))

(def ^{:private true} react-dispatcher (atom nil))

(defn- event-db-handler->interceptor
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

;; Public API

(defn dispatch [event]
  (@react-dispatcher event))

(defn subscribe
  "Listens to global state changes (useContext)"
  [[handler-id & args]]
  (if-let [handler (registrar/get-handler :subscription handler-id)]
    (let [[state _] (hooks/useContext db/app-state)]
      (handler state (into [handler-id] args)))
    (js/console.error "Subscription " handler-id " not defined.")))

(defn register-event-db
  "Adds an event to the registrar"
  ([handler-id handler]
   (register-event-db handler-id nil handler))
  ([handler-id interceptors handler]
   (registrar/register-handler!
    :event handler-id
    (format-interceptor-chain
     [interceptor/do-fx
      interceptors
      (event-db-handler->interceptor handler)]))))

;; Alias
(def reg-event-db register-event-db)

(defn register-event-fx
  ([handler-id handler]
   (register-event-fx handler-id nil handler))
  ([handler-id interceptors handler]
   (registrar/register-handler!
    :event handler-id
    (format-interceptor-chain
     [interceptor/do-fx
      interceptors
      (event-fx-handler->interceptor handler)]))))

;; Alias
(def reg-event-fx register-event-fx)

(defn register-effect
  [handler-id handler]
  (registrar/register-handler! :effect handler-id handler))

;; Alias
(def reg-fx register-effect)

(defn register-coeffect
  [handler-id handler]
  (registrar/register-handler! :coeffect handler-id handler))

;; Alias
(def reg-cofx register-coeffect)

(defn inject-coeffect
  [id]
  (interceptor/->interceptor
   {:id :coeffects
    :before (fn [context]
              (if-let [handler (registrar/get-handler :coeffect id)]
                (update context :coeffects handler)
                context))}))

(def register-subscription (partial registrar/register-handler! :subscription))

;; Alias
(def reg-sub register-subscription)

(defnc Provider
  [{:keys [children initial-state on-init]
    :or {on-init identity}}]
  (let [[state dispatch] (hooks/useReducer db/state-reducer initial-state)]

    ;; Set a global dispatcher to support the ability to directly call it
    (when (nil? @react-dispatcher)
      (reset! react-dispatcher dispatch)
      (on-init))

    [:provider {:context db/app-state
                :value [state dispatch]}
     children]))
