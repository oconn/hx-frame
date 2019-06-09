(ns hx-frame.interceptor)

(defn ->interceptor
  [{:keys [id before after]}]
  {:id (or id :unnamed)
   :before (or before identity)
   :after (or after identity)})
