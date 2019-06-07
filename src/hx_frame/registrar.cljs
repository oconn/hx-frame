(ns hx-frame.registrar
  "Manages hanlders for events & subscriptions.

  Inspired by Day8/re-frame's registrar")

(def handler-types #{:event :subscription})

(def registrar (atom {}))

(defn get-handlers-by-type
  [handler-type]
  (get @registrar handler-type))

(defn get-handler
  "Returns the requested handler."
  [handler-type handler-id]
  (let [handler (get-in @registrar [handler-type handler-id])]
    (or handler
        (do
          (js/console.warn
           (str "No " (name handler-type) " defined for " handler-id))
          nil))))

(defn register-handler!
  "Registers a handler"
  [handler-type handler-id handler]
  (swap! registrar assoc-in [handler-type handler-id] handler)
  handler)
