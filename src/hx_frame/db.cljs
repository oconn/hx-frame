(ns hx-frame.db
  (:require
   [react :as react]
   [hx.hooks :as hooks]

   [hx-frame.registrar :as registrar]
   [hx-frame.interceptor :as interceptor]))

(def dev-app-state
  "Set on state updates when goog.DEBUG is set to true"
  (atom nil))

(def app-state
  "Application state object"
  (react/createContext))

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
