(ns hx-frame.interceptor
  (:require [hx-frame.registrar :as registrar]))

(def ^{:private true} empty-queue #queue [])

(defn- invoke-interceptor-fn
  ^{:doc "Invokes the handler of an intercptor that corresponds to the
direction that the chain is executing."
    :attribution "https://github.com/Day8/re-frame"}
  [context interceptor direction]
  (if-let [f (get interceptor direction)]
    (f context)
    context))

(defn- walk-interceptors
  ^{:doc "Walks the interceptor chain calling the corresponding handler for the
execution direction."
    :attribution "https://github.com/Day8/re-frame"}
  [context direction]
  (loop [context context]
    (let [{:keys [queue stack]} context]
      (if (empty? queue)
        context
        (let [interceptor (peek queue)]
          (recur (-> context
                     (assoc :queue (pop queue)
                            :stack (conj stack interceptor))
                     (invoke-interceptor-fn interceptor direction))))))))

(defn- enqueue
  ^{:doc "Sets a new queue on the interceptor chain context object"
    :attribution "https://github.com/Day8/re-frame"}
  [context interceptors]
  (update context :queue
          (fnil into empty-queue)
          interceptors))

(defn- change-direction
  ^{:doc "Changes the direction of the interceptor queue."
    :attribution "https://github.com/Day8/re-frame"}
  [context]
  (-> context
      (dissoc :queue)
      (enqueue (:stack context))))

(defn- create-context
  "Creates the interceptor chain context object. This is one area that differs
  from re-frame. hx-frame initializes the coeffects object with the :db value
  in addition to the :event vector. This is because hx-frame leverages react's
  context API and is expected to return state at the end of the state reduction
  function. The handling of the db effect occures in the state-reducer and not
  in a coeffect."
  [state event interceptors]
  (-> {}
      (assoc-in [:coeffects :event] event)
      (assoc-in [:coeffects :db] state)
      (enqueue interceptors)))

(defn process-interceptor-chain
  "Walks all intercptors proccesing each one's before fn, and then in reverse
  order, process its after fn."
  [state event interceptor-chain]
  (let [context (create-context state event interceptor-chain)]
    (-> context
        (walk-interceptors :before)
        (change-direction)
        (walk-interceptors :after))))

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
