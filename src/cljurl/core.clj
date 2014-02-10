(ns cljurl.core
  (:require [docopt.core :refer [docopt]]
            [clj-http.client :as client]
            [clostache.parser :refer [render render-resource]]
            [clojure.java.io :as io]
            [clojure.core.async :refer [<! >! go chan timeout]]))

(defn recur-this
  [iterators m]
  (let [ks (keys iterators)]
    (if-let [k (first ks)]
      (apply concat
             (for [var (eval (read-string (get iterators k)))]
               (recur-this (dissoc iterators k) (assoc m k var))))
      [m])))

(recur-this {:foo "(range 10)" :user "[:bar :baz]"} nil)

;(def request-channel (chan 1000))

(defn cljurl-request [request-channel template data]
  (go
     (>! request-channel {:url (render template data)
                          :method :get})))


#_(doseq [data (recur-this {:page "(range 10)"
                          :user "[\"bar\" \"baz\"]"} nil)]
  (cljurl-request
   "http://localhost:8000?page={{page}}&user={{user}}"
   data))

(defn listen [request-channel]
  (go (while true
        (<! (timeout 500))
        (let [r (<! request-channel)]
          (client/request r)))))

#_(listen)

(defn -main [& args]
  (let [[template mdef] args
        iterators (read-string mdef)
        request-channel (chan 1000)]
    (listen request-channel)
    (doseq [data (recur-this iterators nil)]
      (cljurl-request
       request-channel
       template
       data))))

#_(-main "http://localhost:8000/?page={{page}}"
         "{:page \"(range 10)\"}")
