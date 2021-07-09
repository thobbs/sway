(ns sway.runcore
  (:require [quil.core :as q])
  (:require [sway.dynamic :as dynamic])
  (:gen-class))

(defn -main []
  (q/sketch
    :title "Sway"
    :setup dynamic/setup
    :draw dynamic/draw
    :size [2160 2000]
    :features [:exit-on-close]))
