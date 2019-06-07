(ns hx-frame.dispatcher)

(def dispatch! (atom nil))

(def ^{:private true} event-queue (atom #queue []))

(add-watch
 event-queue :event-dispatcher
 (fn [key reference old-state new-state]
   (when-not (empty? new-state)
     (let [event (peek new-state)]
       (@dispatch! event)

       ;; Note: Not sure blocking is needed here, so we'll reset! the ref as
       ;; soon as the event is handed off to react's dispatcher. If blocking
       ;; is needed for some reason, the callback fn could be leveraged to
       ;; support this. (setState(updater[, callback]))
       (reset! reference (pop new-state))))))

(defn dispatch [event]
  (@dispatch! event)
  #_(swap! event-queue conj event))
