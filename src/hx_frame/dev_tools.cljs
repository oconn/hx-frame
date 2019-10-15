(ns hx-frame.dev-tools
  "Utility functions for working with hx-frame"
  (:require
   [clojure.pprint :as pprint]
   [hx-frame.db :as db]))

(def ^:private default-excluded-keys
  [:hx-frame-message
   :hx-frame-analytics
   :hx-frame-router
   :hx-frame-request
   :hx-frame-sockets
   :hx-event-counter])

(defn print-state
  [{:keys [exclude-keys]
    :or {exclude-keys []}}]
  (pprint/pprint
   (apply dissoc @db/dev-app-state (into default-excluded-keys
                                         exclude-keys))))

(defn print-keys-at
  [lens]
  (-> @db/dev-app-state
      (get-in lens)
      keys
      prn))

(defn print-state-at
  [lens]
  (-> @db/dev-app-state
      (get-in lens)
      pprint/pprint))
