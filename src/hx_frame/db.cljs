(ns hx-frame.db
  (:require
   [react :as react]

   [hx-frame.registrar :as registrar]))

(def app-state (react/createContext))

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

(defn state-reducer
  "Processes a given event by looking up it's registered interceptor chain
  and walking it."
  [state event]
  (let [event-key (first event)]
    (if-let [interceptor-chain (registrar/get-handler :event event-key)]
      (let [context (create-context state event interceptor-chain)
            {:keys [effects]} (-> context
                                  (walk-interceptors :before)
                                  (change-direction)
                                  (walk-interceptors :after))]
        (:db effects))
      (do
        (js/console.error "Event " event-key " not defined.")
        state))))
