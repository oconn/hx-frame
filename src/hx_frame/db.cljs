(ns hx-frame.db
  (:require
   [datascript.core :as d]
   [react :as react]
   [hx.hooks :as hooks]

   [hx-frame.registrar :as registrar]
   [hx-frame.interceptor :as interceptor]))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Datascript Implementation
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce ^:private conn (atom nil))

;; Aliases
(def listen! d/listen!)
(def unlisten! d/unlisten!)

(defn get-conn
  "Returns the connection atom"
  [] @conn)

(defn get-db
  "Returns the db object from the connection object"
  [] (d/db (get-conn)))

(defn create-conn
  "Create Datascript connection"
  [{:keys [schema]}]
  (reset! conn (d/create-conn schema)))

(defn remove-nils
  "Removes nils from a map. Used to clean records on transaction."
  [record]
  (reduce-kv (fn [acc k v]
               (if (nil? v)
                 acc
                 (assoc acc k v)))
             {}
             record))

(defn transact!
  [datoms]
  (d/transact! (get-conn) datoms))

(defn q
  [query & args]
  (apply d/q query (get-db) args))

(defn pull
  [& args]
  (apply d/pull (get-db) args))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; React Context Implementation
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def dev-app-state
  "Set on state updates when goog.DEBUG is set to true"
  (atom nil))

(def app-state
  "Application state object"
  (react/createContext))

;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; State Reducer
;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- state-reducer
  "Processes a given event by looking up it's registered interceptor chain
  and walking it."
  [state event]
  (let [event-key (first event)]
    (if-let [interceptor-chain (registrar/get-handler :event event-key)]
      (let [{:keys [effects]} (interceptor/process-interceptor-chain
                               state event interceptor-chain)

            ;; Note: react will sometimes run a reducer multiple time if state
            ;;       does not change. This will cause problems if side-effects
            ;;       are triggered in the event. Introducing an incrementing
            ;;       counter solves this problem but I'm sure there is a better
            ;;       solution using useContext or useMemo on the reducer.
            db (update
                (or (:db effects)
                    (do
                      (js/console.error "DB effect removed from '" event-key "'")
                      state))
                :hx-event-counter
                inc)]

        (when ^boolean js/goog.DEBUG
          (reset! dev-app-state db))
        db)
      (do
        (when ^boolean js/goog.DEBUG
          (js/console.error
           "Event " event-key " has been dispatched but is not defined."))
        state))))

(defn hx-reducer
  [initial-state]
  (let [memoized-reducer (hooks/useCallback state-reducer [])]
    (hooks/useReducer memoized-reducer initial-state)))
