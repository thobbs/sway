(ns sway.dynamic
  (:require [genartlib.algebra :refer [rescale interpolate angular-coords point-dist point-angle]]
            [genartlib.curves :refer [chaikin-curve curve-length]]
            [genartlib.random :refer [gauss weighted-choice odds abs-gauss]]
            [genartlib.util :refer [set-color-mode w h pi enumerate between?]]
            [quil.core :as q])
  (:import [sway ProximityChecker]))

(defn setup []
  (q/smooth)
  (q/hint :disable-async-saveframe))

(declare actual-draw)

(defn draw []
  (q/no-loop)
  (set-color-mode)

  (let [cur-time (System/currentTimeMillis)
        ; seed (System/nanoTime)
        seed 155047463470100]

    (println "setting seed to:" seed)
    (q/random-seed seed)

    (try
      (actual-draw)
      (catch Throwable t
        (println "Exception in draw function:" t)))

    (println "gen time:" (/ (- (System/currentTimeMillis) cur-time) 1000.0) "s")
    (q/save (str "img-" cur-time "-" seed ".jpg"))))

(defn adjust-flow-points
  [flow-points disturbance-x disturbance-y disturbance-radius disturbance-theta]
  (for [[x y theta] flow-points]
    (let [disturbance-dist (q/dist disturbance-x disturbance-y x y)
          theta-adjust (if (> disturbance-dist disturbance-radius)
                         0
                         (rescale disturbance-dist 0 disturbance-radius disturbance-theta 0))
          final-theta (+ theta theta-adjust)]
      [x y final-theta])))

(defn inverse-adjust-flow-points
  [flow-points disturbance-x disturbance-y pos-adjust?]
  (for [[x y theta] flow-points]
    (let [disturbance-dist (q/dist disturbance-x disturbance-y x y)
          theta-adjust (* (q/sqrt (/ disturbance-dist (w 0.25)))
                          (if pos-adjust? (pi 0.025) (pi -0.025)))
          final-theta (+ theta theta-adjust)]
      [x y final-theta])))

(defn get-flow-points
  [default-theta]
  (let [left-x (w -1.2)
         right-x (w 1.2)
         top-y (h -0.4)
         bottom-y (h 1.4)
         spacing (int (w 0.005))
         flow-points (for [y (range top-y bottom-y spacing)
                           x (range left-x right-x spacing)]
                       [x y default-theta])

         num-disturbances 15
         theta-variance (pi 0.10)]

    (loop [n num-disturbances
           flow-points flow-points]
      (if (zero? n)
        (vec flow-points)
        (let [disturbance-x (q/random left-x right-x)
              disturbance-y (q/random top-y bottom-y)
              disturbance-theta (gauss 0 theta-variance)
              disturbance-radius (max (w 0.1) (abs-gauss (w 0.35) (w 0.15)))
              pos-adjust? (odds 0.5)

              adjusted (if (odds 0.7)
                         (adjust-flow-points flow-points
                                             disturbance-x disturbance-y
                                             disturbance-radius disturbance-theta)
                         (inverse-adjust-flow-points
                           flow-points disturbance-x disturbance-y pos-adjust?))]
          (recur (dec n) adjusted))))))

(defn get-flow-branch
  [flow-points start-point num-points theta-skew skew-decay depth branch-odds thickness default-theta]
  (let [left-x (w -1.2)
        right-x (w 1.2)
        top-y (h -0.4)
        ; bottom-y (h 1.4)
        spacing (int (w 0.005))
        row-width (int (Math/ceil (/ (- right-x left-x) spacing)))]

    (loop [n 0
           last-branch-at -1000
           x (first start-point)
           y (second start-point)
           theta-skew theta-skew
           branch-odds branch-odds
           branch-left? (odds 0.5)
           thickness (* thickness 0.99)
           points []
           other-branches []]

      (if (>= n num-points)

        (conj other-branches {:points points
                              :depth depth})

        (let [new-theta-skew (* skew-decay theta-skew)
              skew-variance (rescale depth 0 10 (pi 0.01) (pi 0.03))
              new-theta-skew (gauss new-theta-skew skew-variance)

              x-offset (int (/ (- x left-x) spacing))
              y-offset (int (/ (- y top-y) spacing))
              index (+ x-offset (* row-width y-offset))
              [_ _ theta] (if (between? index 0 (dec (count flow-points)))
                            (nth flow-points index)
                            [0 0 default-theta])
              theta (+ theta theta-skew)

              next-x (+ x (* (w 0.005) (q/cos theta)))
              next-y (+ y (* (w 0.005) (q/sin theta)))

              new-skew (rescale depth 1 10 (pi 0.30) (pi 0.17))
              new-skew (gauss new-skew (* new-skew 0.4))

              new-branches (when (and (< depth 10)
                                      (> n (rescale depth 0 10 25 4))
                                      (> (- n last-branch-at) (rescale depth 0 10 8 -2))
                                      (odds branch-odds))
                             (get-flow-branch
                               flow-points
                               [next-x next-y]
                               (int (* num-points 0.5))
                               (if branch-left? new-skew (* new-skew -1))
                               (* skew-decay (rescale depth 0 10 0.90 0.5))
                               (inc depth)
                               (* branch-odds 2.5)
                               (max (* thickness 0.6)
                                    (w 0.0010))
                               default-theta))

              other-branches (if new-branches
                               (concat other-branches new-branches)
                               other-branches)
              branch-left? (if new-branches
                             (not branch-left?)
                             branch-left?)
              last-branch-at (if new-branches
                               n
                               last-branch-at)]

          (recur (inc n)
                 last-branch-at
                 next-x
                 next-y
                 new-theta-skew
                 (* branch-odds 0.99)
                 branch-left?
                 (* thickness 0.987)
                 (conj points [x y thickness]) other-branches))))))

(defn get-flow-line
  [flow-points start-point seg-len default-theta]
  (let [left-x (w -1.2)
        right-x (w 1.2)
        top-y (h -0.4)
        ; bottom-y (h 1.4)
        spacing (int (w 0.005))
        row-width (int (Math/ceil (/ (- right-x left-x) spacing)))
        num-points (abs-gauss seg-len (* seg-len 0.35))
        [x y] start-point]

    (loop [n num-points
           x x
           y y
           points []]

      (if (> n 0)

        (let [x-offset (int (/ (- x left-x) spacing))
              y-offset (int (/ (- y top-y) spacing))
              index (+ x-offset (* row-width y-offset))
              [_ _ theta] (if (between? index 0 (dec (count flow-points)))
                            (nth flow-points index)
                            [0 0 default-theta])

              next-x (+ x (* (w 0.005) (q/cos theta)))
              next-y (+ y (* (w 0.005) (q/sin theta)))]

          (recur (dec n) next-x next-y (conj points [x y])))

        ; base case
        points))))

(defn get-fat-curve-top
  [curve]
  (concat
    (for [[cur-point next-point] (partition 2 1 curve)]
      (let [angle (point-angle cur-point next-point)
            [x y thickness] cur-point]
        (angular-coords x y (- angle (pi 0.5)) thickness)))

    (let [[pen-point last-point] (take-last 2 curve)
          angle (point-angle pen-point last-point)
          [x y thickness] last-point]
      [(angular-coords x y (- angle (pi 0.5)) thickness)])))

(defn get-fat-curve-bottom
  [curve]
  (concat
    (for [[cur-point next-point] (partition 2 1 curve)]
      (let [angle (point-angle cur-point next-point)
            [x y thickness] cur-point]
        (angular-coords x y (+ angle (pi 0.5)) thickness)))

    (let [[pen-point last-point] (take-last 2 curve)
          angle (point-angle pen-point last-point)
          [x y thickness] last-point]
      [(angular-coords x y (+ angle (pi 0.5)) thickness)])))

(defn make-fat-curve
  [curve round-start?]
  (let [top (get-fat-curve-top curve)
        bot (get-fat-curve-bottom curve)]
    (if-not round-start?
      (concat top (reverse bot))
      (let [joined (concat (take 1 (drop 1 bot))
                           [(take 2 (first curve))]
                           (drop 1 top)
                           (reverse (drop 1 bot)))
            smoothed (chaikin-curve
                       joined
                       2
                       0.25)]
        smoothed))))

(defn interpolate-curve
  [curve t & [curve-len]]
  (let [first-point (first curve)
        last-point (last curve)]

    (cond
      (<= t 0)
      first-point

      (>= t 1)
      last-point

      (= 2 (count curve))
      (let [mid-x (interpolate (first first-point) (first last-point) t)
            mid-y (interpolate (second first-point) (second last-point) t)]
        [mid-x mid-y])

      :else
      (let [total-len (or curve-len (curve-length curve))
            target-len (* total-len t)]

        (loop [curve (rest curve)
               current-length 0
               prev-point first-point]

          (if (empty? curve)
            last-point ; probably shouldn't happen?

            (let [new-point (first curve)
                  new-dist (point-dist prev-point new-point)
                  new-length (+ current-length new-dist)]

              (if (< new-length target-len)
                (recur (rest curve) new-length new-point)

                ; we need to split
                (let [dist-needed (- target-len current-length)
                      inner-t (/ dist-needed new-dist)
                      x (interpolate (first prev-point) (first new-point) inner-t)
                      y (interpolate (second prev-point) (second new-point) inner-t)]
                  [x y])))))))))

(def cream [25 2 89])
(def dim-red [358 59 86])
(def magenta [343 55 87])
(def magenta2 [343 38 90])
(def pink [352 18 95])
(def pink2 [346 38 93])
(def pale-blue [232 49 76])
(def blue [232 51 56])
(def dark-blue [230 37 35])

(defn pick-color
  []
  (weighted-choice
    cream      0.10
    dim-red    0.10
    magenta    0.10
    magenta2   0.15
    pink       0.15
    pink2      0.10
    pale-blue  0.15
    blue       0.15))

(defn make-segs
  [all-branches checker]
  (loop [branches all-branches
         current-branch nil
         current-points []
         was-good? false
         current-seg []
         all-segs []]

    (if (empty? current-points)

      (if (empty? branches)
        ; all done
        (conj all-segs {:seg current-seg
                        :color (:color current-branch)
                        :depth (:depth current-branch)})

        ; start a new line
        (recur (rest branches)
               (first branches)
               (:points (first branches))
               false
               []
               (if (empty? current-seg)
                 all-segs
                 (conj all-segs {:seg current-seg
                                 :color (:color current-branch)
                                 :depth (:depth current-branch)}))))

      ; continue the current line
      (let [point (first current-points)
            [x y margin] point
            margin (+ margin (w 0.0002))

            border-padding (w -0.05)
            point-is-good? (and (between? x border-padding (- (w) border-padding))
                                (between? y border-padding (- (h) border-padding))
                                (not (.checkForCollision
                                       checker x y margin (:id current-branch))))

            check-multi? (empty? current-seg)]

        (if point-is-good?

          ; the new point is good
          (if-not check-multi?
            (do
              (.addPoint checker x y margin (:id current-branch))
              (recur branches
                     current-branch
                     (rest current-points)
                     true
                     (conj current-seg [x y margin])
                     all-segs))

            ; check and (potentially) add the first few points
            (let [check-count 20 ; (max 2 (int (/ margin (w 0.001))))
                  extra-points (take check-count current-points)
                  all-good?
                  (every?
                    (fn [[next-x next-y margin]]
                      (and next-x
                           next-y
                           (and (between? next-x border-padding (- (w) border-padding))
                                (between? next-y border-padding (- (h) border-padding (w 0.015)))
                                (not (.checkForCollision
                                       checker next-x next-y margin (:id current-branch))))))
                    extra-points)]
              (if-not all-good?
                (recur branches
                       current-branch
                       (rest current-points)
                       false
                       []
                       all-segs)
                (do
                  (.addPoint checker x y margin (:id current-branch))
                  (doseq [[x y] extra-points]
                    (.addPoint checker x y margin (:id current-branch)))
                  (recur branches
                         current-branch
                         (drop check-count current-points)
                         true
                         (vec extra-points)
                         all-segs)))))

          ; the new point is not good, discard and continue
          (if was-good?
            (recur branches current-branch (rest current-points)
                   false [] (conj all-segs {:seg current-seg
                                            :color (:color current-branch)
                                            :depth (:depth current-branch)}))
            (recur branches current-branch (rest current-points)
                   false [] all-segs)))))))

(defn actual-draw []
  (apply q/background dark-blue)

  (q/no-stroke)

  (let [default-theta (pi 1.5)
        flow-points (get-flow-points default-theta)

        num-points 170
        branch-odds 0.30
        skew-decay 0.99
        initial-thickness (w 0.007)

        checker (ProximityChecker.)

        trees (for [x (range (w -0.4) (w 1.4) (w 0.02))]
                (let [num-points (max 50 (gauss num-points 30))
                      initial-y (min (gauss (h 0.72) (h 0.14))
                                     (gauss (h 0.95) (h 0.02)))
                      thickness-mean (rescale
                                       initial-y
                                       (h 0.6) (h 1.0)
                                       (* initial-thickness 0.35) (* initial-thickness 1.5))
                      initial-thickness (gauss thickness-mean (* initial-thickness 0.1))
                      branches (get-flow-branch
                                 flow-points
                                 [x initial-y] num-points 0 skew-decay 0 branch-odds
                                 initial-thickness default-theta)]
                  {:initial-y initial-y
                   :color (pick-color)
                   :branches branches}))
        trees (reverse (sort-by :initial-y trees))

        all-branches (for [[tree-id tree] (enumerate trees)
                           branch (:branches tree)]
                       {:points (:points branch)
                        :depth (:depth branch)
                        :color (:color tree)
                        :id tree-id})

        all-segs (make-segs all-branches checker)]

    (doseq [seg all-segs]
      (when (> (count (:seg seg)) 2)
        (apply q/fill (:color seg))
        (let [curve (:seg seg)
              fat-curve (make-fat-curve curve true)]
          (q/begin-shape)
          (doseq [[x y] fat-curve]
            (q/vertex x (- y (h 0.06))))
          (q/end-shape))))

    (dotimes [_ 12000]
      (let [start-x (q/random 0 (w))
            start-y (gauss (h 0.73) (h 0.27))
            curve-len (rescale start-y (h 0.3) (h) 2.3 6.5)
            sweight-mean (rescale start-y (h 0.3) (h) (w 0.0008) (w 0.0015))]
        (when (< start-y (h 1.07))
          (let [curve (get-flow-line flow-points [start-x start-y] curve-len 0)
                curve (for [[x y] curve]
                        [(gauss x (w 0.0003)) y])
                collision? (some
                             (fn [seg]
                               (some
                                 (fn [[x2 y2 margin]]
                                   (some
                                     (fn [[x1 y1]]
                                       (< (q/dist x1 y1 x2 y2) (+ margin (w 0.0016))))
                                     curve))
                                 (:seg seg)))
                             all-segs)]
            (when-not collision?
              (q/no-fill)
              (q/stroke 232 46 45)
              (q/stroke-weight (max (w 0.0004)
                                    (gauss sweight-mean (* sweight-mean 0.5))))
              (q/begin-shape)
              (doseq [[x y] curve]
                (q/vertex x (- y (h 0.06))))
              (q/end-shape))))))))
