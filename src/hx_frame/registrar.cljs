(ns hx-frame.registrar
  "Manages hanlders for events & subscriptions.

  Inspired by Day8/re-frame's registrar")

(def ^{:private true}
  handler-types #{:event :subscription :effect :coeffect})

(defn- is-supported-type?
  [handler-type]
  (contains? handler-types handler-type))

(defn- log-unsupported-type!
  [handler-type]
  (js/console.error
   (str "Handler type is not supported '" handler-type "'."
        " Supported handler types are " handler-types ".")))

(def registrar
  (atom {:event {} :subscription {} :effect {} :coeffect {}}))

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

(defn is-registered?
  "Check to for handler registration"
  [handler-type handler-id]
  (some? (get-handler handler-type handler-id)))

(defn register-handler!
  "Registers a handler"
  [handler-type handler-id handler]
  (if (is-supported-type? handler-type)
    (swap! registrar assoc-in [handler-type handler-id] handler)
    (log-unsupported-type! handler-type))
  handler)

(defn unregister-handler!
  "Removes a handler from the registry"
  [handler-type handler-id]
  (if (is-supported-type? handler-type)
    (swap! registrar update handler-type dissoc handler-id)
    (log-unsupported-type! handler-type))
  nil)
