(ns hx-frame.dispatcher)

(def react-dispatcher (atom nil))

(defn dispatch [event]
  (@react-dispatcher event))
