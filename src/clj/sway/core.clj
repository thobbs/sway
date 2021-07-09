(ns sway.core
  (:require [quil.core :as q]
            [sway.dynamic :as dynamic])
  (:gen-class))

(q/defsketch example
             :title "Sway"
             :setup dynamic/setup
             :draw dynamic/draw
             ; 9'3"w X 8'6"h
             ; 111" w x 102" h
             ; plus 5" margin (total 10")
             ; 121" w x 112" h
             ; :size [18150 16800])
             :size [2160 2000])


(defn refresh []
  (use :reload 'sketch.dynamic)
  (.redraw example))

(defn get-applet []
  example)
