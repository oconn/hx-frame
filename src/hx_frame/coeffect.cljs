(ns hx-frame.coeffect
  (:require
   [hx-frame.interceptor :as interceptor]))

(defn- inject-coeffect
  [id]
  (interceptor/->interceptor
   {:id :coeffects
    :before (fn [context]
              (let [handler identity]
                (update context :coeffects handler)))}))

(def inject-db (inject-coeffect :db))
