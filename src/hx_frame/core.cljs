(ns hx-frame.core
  (:require
   [hx.hooks :as hooks]
   [hx.react :as hx :refer [defnc]]

   [hx-frame.db :as db]
   [hx-frame.dispatcher :as dispatcher]
   [hx-frame.interceptor :as interceptor]
   [hx-frame.registrar :as registrar]
   [hx-frame.effects :as effects]))

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
  ;; TODO: This throws in advanced with:
  ;; Invariant Violation: Invalid hook call. Hooks can only be called inside of
  ;; the body of a function component. This could happen for one of the
  ;; following reasons:
  (let [[state dispatch] (db/hx-reducer initial-state)]

    ;; Set a global dispatcher to support the ability to directly call it
    (when (nil? @dispatcher/react-dispatcher)
      (reset! dispatcher/react-dispatcher dispatch)
      (on-init))

    [:provider {:context db/app-state
                :value [state dispatch]}
     children]))

(def dispatch dispatcher/dispatch)
(def register-effect effects/register-effect)
(def reg-fx effects/register-effect)
