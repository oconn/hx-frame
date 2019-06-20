(ns hx-frame.db
  (:require
   [clojure.data :as data]
   [react :as react]

   [hx-frame.registrar :as registrar]
   [hx-frame.interceptor :as interceptor]))

(def dev-app-state
  "Set on state updates when goog.DEBUG is set to true"
  (atom nil))

(def app-state
  "Application state object"
  (react/createContext))

(defn state-reducer
  "Processes a given event by looking up it's registered interceptor chain
  and walking it."
  [state event]
  (let [event-key (first event)]
    (if-let [interceptor-chain (registrar/get-handler :event event-key)]
      (let [{:keys [effects]} (interceptor/process-interceptor-chain
                               state event interceptor-chain)

            db (or (:db effects)
                   (do
                     (js/console.error "DB effect removed from '" event-key "'")
                     state))]

        (when ^boolean js/goog.DEBUG
          (reset! dev-app-state db)

          ;; Tap used to communicate state changes to
          ;; https://github.com/Lokeh/punk2
          (tap> {:event event
                 :db-before state
                 :db-after db
                 :db-diff (first (data/diff db state))}))

        db)
      (do
        (when ^boolean js/goog.DEBUG
          (js/console.error
           "Event " event-key " has been dispatched but is not defined."))
        state))))
