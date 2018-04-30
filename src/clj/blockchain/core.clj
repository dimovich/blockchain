(ns blockchain.core
  (:require [taoensso.timbre :refer [info]]
            [roll.system :as system]))



(defn init []
  (system/init {:path "config.edn"}))


(defn -main []
  (init))

