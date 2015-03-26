;; gorilla-repl.fileformat = 1

;; **
;;; # Aircraft Detection
;; **

;; @@
(ns aircraft
  (:use [mrepl core]
        [embang runtime emit]
        [gorilla-plot.core :only [compose list-plot histogram]]))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-nil'>nil</span>","value":"nil"}
;; <=

;; **
;;; Radar tracking model inspired by the aircraft example in BLOG. To keep things simple, all positions are one-dimensional.
;; **

;; @@
(defanglican aircraft

  ; Aircraft are identified by integers from 0 to (- num-aircraft 1).
  [assume num-aircraft (+ 1 (sample (poisson 5)))]

  
  ; The true position of an aircraft.
  [assume aircraft-info 
    (mem (lambda (aircraft-id) 
      (let ((position  
              (sample (normal 2. 5.)))
            (num-blips
              (sample (discrete (list 0.1 0.4 0.5))))
            (blips
              (map (lambda (i)
                     (list aircraft-id i (sample (normal position 1.)))) 
                   (range num-blips))))
        (list position blips))))]
  
  [assume all-blips  
    (reduce (lambda (acc aircraft-id)
              (concat (second (aircraft-info aircraft-id)) acc))
            (repeat 3 '(0 0 0)) ; pad all-blips to avoid out-of-bounds
            (range num-aircraft))]

  ;; Observe three blips on the radar screen:
  [observe (normal (count all-blips) 1) 3] 

  ;; Observe the location of the three blips:
  [observe (normal (nth (nth all-blips 0) 2) 1) 1.]
  [observe (normal (nth (nth all-blips 1) 2) 1) 2.]
  [observe (normal (nth (nth all-blips 2) 2) 1) 3.]

  ;; Want to know the number of aircraft and their positions:
  [predict num-aircraft]
  [assume positions 
    (map (lambda (aircraft-id) (first (aircraft-info aircraft-id))) 
         (range num-aircraft))]
  [predict positions])
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;aircraft/aircraft</span>","value":"#'aircraft/aircraft"}
;; <=

;; **
;;; Initiate lazy inference using Lightweight Metropolis-Hastings. 
;; **

;; @@
(def samples (doquery :lmh aircraft nil))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;aircraft/samples</span>","value":"#'aircraft/samples"}
;; <=

;; **
;;; Decide how many samples we need (@@N@@), and retrieve them. 
;; **

;; @@
(def N 1	0000)
(def predicts (map get-predicts (take N samples)))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;aircraft/predicts</span>","value":"#'aircraft/predicts"}
;; <=

;; **
;;; Let's see what predicts are available:
;; **

;; @@
(first predicts)
;; @@
;; =>
;;; {"type":"list-like","open":"<span class='clj-map'>{</span>","close":"<span class='clj-map'>}</span>","separator":", ","items":[{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-symbol'>num-aircraft</span>","value":"num-aircraft"},{"type":"html","content":"<span class='clj-long'>6</span>","value":"6"}],"value":"[num-aircraft 6]"},{"type":"list-like","open":"","close":"","separator":" ","items":[{"type":"html","content":"<span class='clj-symbol'>positions</span>","value":"positions"},{"type":"list-like","open":"<span class='clj-list'>(</span>","close":"<span class='clj-list'>)</span>","separator":" ","items":[{"type":"html","content":"<span class='clj-double'>-3.302809025176212</span>","value":"-3.302809025176212"},{"type":"html","content":"<span class='clj-double'>0.5148480305327499</span>","value":"0.5148480305327499"},{"type":"html","content":"<span class='clj-double'>-1.6352168926606052</span>","value":"-1.6352168926606052"},{"type":"html","content":"<span class='clj-double'>2.1909118133031007</span>","value":"2.1909118133031007"},{"type":"html","content":"<span class='clj-double'>2.4170028957519127</span>","value":"2.4170028957519127"},{"type":"html","content":"<span class='clj-double'>4.141246535498192</span>","value":"4.141246535498192"}],"value":"(-3.302809025176212 0.5148480305327499 -1.6352168926606052 2.1909118133031007 2.4170028957519127 4.141246535498192)"}],"value":"[positions (-3.302809025176212 0.5148480305327499 -1.6352168926606052 2.1909118133031007 2.4170028957519127 4.141246535498192)]"}],"value":"{num-aircraft 6, positions (-3.302809025176212 0.5148480305327499 -1.6352168926606052 2.1909118133031007 2.4170028957519127 4.141246535498192)}"}
;; <=

;; **
;;; Well, the easiest thing to observe convergence on is the number of aircraft. Use it.
;; **

;; @@
(def nas (map #(get % 'num-aircraft) predicts))
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;aircraft/nas</span>","value":"#'aircraft/nas"}
;; <=

;; **
;;; Looks like we are looking at 3 (may change if you change the parameters) aircraft on average, not bad.
;; **

;; @@
(histogram nas)
;; @@
;; =>
;;; {"type":"vega","content":{"axes":[{"scale":"x","type":"x"},{"scale":"y","type":"y"}],"scales":[{"name":"x","type":"linear","range":"width","zero":false,"domain":{"data":"641d1da9-7694-4007-81cd-e0440ca7eaf4","field":"data.x"}},{"name":"y","type":"linear","range":"height","nice":true,"zero":false,"domain":{"data":"641d1da9-7694-4007-81cd-e0440ca7eaf4","field":"data.y"}}],"marks":[{"type":"line","from":{"data":"641d1da9-7694-4007-81cd-e0440ca7eaf4"},"properties":{"enter":{"x":{"scale":"x","field":"data.x"},"y":{"scale":"y","field":"data.y"},"interpolate":{"value":"step-before"},"fill":{"value":"steelblue"},"fillOpacity":{"value":0.4},"stroke":{"value":"steelblue"},"strokeWidth":{"value":2},"strokeOpacity":{"value":1}}}}],"data":[{"name":"641d1da9-7694-4007-81cd-e0440ca7eaf4","values":[{"x":1.0,"y":0},{"x":1.3333333333333335,"y":1620.0},{"x":1.666666666666667,"y":0.0},{"x":2.0000000000000004,"y":2893.0},{"x":2.333333333333334,"y":0.0},{"x":2.6666666666666674,"y":0.0},{"x":3.000000000000001,"y":4489.0},{"x":3.3333333333333344,"y":0.0},{"x":3.666666666666668,"y":0.0},{"x":4.000000000000001,"y":788.0},{"x":4.333333333333334,"y":0.0},{"x":4.666666666666667,"y":0.0},{"x":5.0,"y":0.0},{"x":5.333333333333333,"y":36.0},{"x":5.666666666666666,"y":0.0},{"x":5.999999999999999,"y":0.0},{"x":6.333333333333332,"y":174.0},{"x":6.666666666666665,"y":0}]}],"width":400,"height":247.2187957763672,"padding":{"bottom":20,"top":10,"right":10,"left":50}},"value":"#gorilla_repl.vega.VegaView{:content {:axes [{:scale \"x\", :type \"x\"} {:scale \"y\", :type \"y\"}], :scales [{:name \"x\", :type \"linear\", :range \"width\", :zero false, :domain {:data \"641d1da9-7694-4007-81cd-e0440ca7eaf4\", :field \"data.x\"}} {:name \"y\", :type \"linear\", :range \"height\", :nice true, :zero false, :domain {:data \"641d1da9-7694-4007-81cd-e0440ca7eaf4\", :field \"data.y\"}}], :marks [{:type \"line\", :from {:data \"641d1da9-7694-4007-81cd-e0440ca7eaf4\"}, :properties {:enter {:x {:scale \"x\", :field \"data.x\"}, :y {:scale \"y\", :field \"data.y\"}, :interpolate {:value \"step-before\"}, :fill {:value \"steelblue\"}, :fillOpacity {:value 0.4}, :stroke {:value \"steelblue\"}, :strokeWidth {:value 2}, :strokeOpacity {:value 1}}}}], :data [{:name \"641d1da9-7694-4007-81cd-e0440ca7eaf4\", :values ({:x 1.0, :y 0} {:x 1.3333333333333335, :y 1620.0} {:x 1.666666666666667, :y 0.0} {:x 2.0000000000000004, :y 2893.0} {:x 2.333333333333334, :y 0.0} {:x 2.6666666666666674, :y 0.0} {:x 3.000000000000001, :y 4489.0} {:x 3.3333333333333344, :y 0.0} {:x 3.666666666666668, :y 0.0} {:x 4.000000000000001, :y 788.0} {:x 4.333333333333334, :y 0.0} {:x 4.666666666666667, :y 0.0} {:x 5.0, :y 0.0} {:x 5.333333333333333, :y 36.0} {:x 5.666666666666666, :y 0.0} {:x 5.999999999999999, :y 0.0} {:x 6.333333333333332, :y 174.0} {:x 6.666666666666665, :y 0})}], :width 400, :height 247.2188, :padding {:bottom 20, :top 10, :right 10, :left 50}}}"}
;; <=

;; **
;;; Let's see how things converge. Compute average number of aircraft from the beginning to the current sample, and from the current sample to the end:
;; **

;; @@
(defn nas-avg
     [nas]
     (loop [an []
            sum 0.
            n 0
            nas nas]
       (if-let [[na & nas] (seq nas)]
         (let [sum (+ sum na)
               n (inc n)]
           (recur (conj an (/ sum n)) sum n nas))
         an)))

(def avg-nas-beg
  "average number of aircraft from beginning to the current sample"
  (nas-avg nas))

(def avg-nas-end
  "average number of aircraft from the current sample to the end"
  (reverse (nas-avg (reverse nas))))

 
;; @@
;; =>
;;; {"type":"html","content":"<span class='clj-var'>#&#x27;aircraft/avg-nas-end</span>","value":"#'aircraft/avg-nas-end"}
;; <=

;; @@
 (compose
    (list-plot avg-nas-beg :color "#00cccc" 
                    :plot-range [[0 N] [0 10]]
                    :joined true)
    (list-plot avg-nas-end :color "#cc00cc" :joined true))
;; @@
;; =>
;; <=

;; @@

;; @@