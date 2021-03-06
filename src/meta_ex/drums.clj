(ns meta-ex.drums
  (:use [overtone.live]
        [meta-ex.sets.ignite]
        [meta-ex.kit.mixer]
        [meta-ex.synths.mixers :only [basic-mixer]])
  (:require [meta-ex.sets.ignite]
            [meta-ex.kit.monome-sequencer :as ms]
            [meta-ex.kit.sequencer :as seq]
            [meta-ex.kit.timing :as tim]
            [meta-ex.kit.sampler :as samp]
            [meta-ex.kit.mixer :as mx]
            [meta-ex.hw.polynome :as poly]
            [meta-ex.hw.fonome :as fon]))


;;(recording-start "~/Desktop/bristol.wav")
;;(recording-stop)
;; (stop)

(declare seq64)
(declare seq128)
(declare trigger-sampler128)
(declare trigger-sampler64)

(do

  (defonce drum-g             (group))
  (defonce drum-trigger-mix-g (group :after drum-g))
  (defonce drum-basic-mixer-g (group :after default-mixer-g))

  (defonce m64-b  (audio-bus 2 "m64 basic-mixer"))
  (defonce m128-b (audio-bus 2 "m128 basic-mixer"))

  (defonce seq64-f  (fon/mk-fonome ::seq64 8 5))
  (defonce seq128-f (fon/mk-fonome ::seq128 16 6))

  (defonce insta-pause64-f   (fon/mk-fonome ::pauser64 1 1))
  (defonce insta-pause128-f  (fon/mk-fonome ::pauser128 1 1))
  (defonce insta-pause-all-f (fon/mk-fonome ::pauser-all 1 1))

  (defonce bas-mix-s64  (basic-mixer [:head drum-basic-mixer-g] :in-bus m64-b :mute 0))
  (defonce bas-mix-s128 (basic-mixer [:head drum-basic-mixer-g] :in-bus m128-b :mute 0))

  (defonce trig128-mixer (mx/add-nk-mixer (nk-bank :m128) "m128-triggers" drum-trigger-mix-g m128-b))
  (defonce trig64-mixer  (mx/add-nk-mixer (nk-bank :m64) "m64-triggers" drum-trigger-mix-g m64-b))

  (when m64
    (defonce seq64 (ms/mk-monome-sequencer (nk-bank :m64) "m64" orig-samples seq64-f m64-b drum-g))
    (defonce __dock64__ (poly/dock-fonome! m64 seq64-f ::seq64 0 0))
    (defonce __dock_pause64__ (poly/dock-fonome! m64 insta-pause64-f ::pause64 7 7))
    (defonce trigger-sampler64 (samp/mk-sampler ::trigger-sampler64 trigger-samples drum-g (nkmx (nk-bank :m64) "m64-triggers") 8))
    (defonce __dockk_trigger__ (poly/dock-fonome! m64  (:fonome trigger-sampler64)  ::trigger-sampler64  0 6)))

  (when m128
    (defonce seq128 (ms/mk-monome-sequencer (nk-bank :m128) "m128" african-samples seq128-f m128-b drum-g))
    (defonce __dock128___ (poly/dock-fonome! m128 seq128-f ::seq128 0 0))
    (defonce __dock_pause128__ (poly/dock-fonome! m128 insta-pause128-f ::pause128 15 7))
    (defonce trigger-sampler128 (samp/mk-sampler ::trigger-sampler128 trigger-samples drum-g (nkmx (nk-bank :m128) "m128-triggers") 16))
    (defonce __dock_trigger128__ (poly/dock-fonome! m128 (:fonome trigger-sampler128) ::trigger-sampler128 0 6)))


  (on-event [:fonome :led-change (:id insta-pause64-f)]
            (fn [{:keys [x y new-leds]}]
              (let [on? (get new-leds [x y])]
                (if on?
                  (ctl bas-mix-s64 :mute 1)
                  (ctl bas-mix-s64 :mute 0))))
            ::seq64)

  (on-event [:fonome :press (:id insta-pause64-f)]
            (fn [{:keys [x y fonome]}]
              (fon/toggle-led fonome x y)
              )
            ::seq64-press)

  (on-event [:fonome :led-change (:id insta-pause128-f)]
            (fn [{:keys [x y new-leds]}]
              (let [on? (get new-leds [x y])]
                (if on?
                  (ctl bas-mix-s128 :mute 1)
                  (ctl bas-mix-s128 :mute 0))))
            ::seq128)

  (on-event [:fonome :press (:id insta-pause128-f)]
            (fn [{:keys [x y fonome]}]
              (fon/toggle-led fonome x y)
              )
            ::seq128-press)

  (on-latest-event [:v-nanoKON2 (nk-bank :m64) "m64-master" :control-change :slider7]
            (fn [{:keys [val]}]
              (ctl bas-mix-s64 :amp val))
            ::m64-master-amp)

  (on-latest-event [:v-nanoKON2 (nk-bank :m64) "m64-master" :control-change :slider6]
            (fn [{:keys [val]}]
              (ctl bas-mix-s64 :boost val))
            ::m64-master-boost)

  (on-latest-event [:v-nanoKON2 (nk-bank :m128) "m128-master" :control-change :slider7]
            (fn [{:keys [val]}]
              (ctl bas-mix-s128 :amp val))
            ::m128-master-amp)

  (on-latest-event [:v-nanoKON2 (nk-bank :m128) "m128-master" :control-change :slider6]
            (fn [{:keys [val]}]
              (ctl bas-mix-s128 :boost val))
            ::m128-master-boost))


(comment
  (defn get-sin-ctl
    [sequencer idx]
    (:sin-ctl (nth (:mixers  @(:sequencer sequencer)) idx)))


  (ctl (get-sin-ctl seq128 0)
       :freq-mul-7 5/7
       :mul-7 3
       :add-7 0)

  (ctl (get-sin-ctl seq64 0)
       :freq-mul-15 5/7
       :mul-15 0.5
       :add-15 0.5
       :amp-15 1))

(comment
  (ms/swap-samples! seq64 african-samples)
  (ms/swap-samples! seq64 ambient-drum-samples)
  (ms/swap-samples! seq64 orig-samples)
  (ms/swap-samples! seq64 mouth-samples)
  (ms/swap-samples! seq64 transition-samples)

  (ms/swap-samples! seq128 african-samples)
  (ms/swap-samples! seq128 ambient-drum-samples)
  (ms/swap-samples! seq128 orig-samples)
  (ms/swap-samples! seq128 mouth-samples))

;;(ms/stop-sequencer seq128)
;;(ms/stop-sequencer seq64)
;;  (.printStackTrace (agent-error (:state (:fonome seq128))))
;; (def c-sequencer (seq/mk-sequencer "m128" african-samples 16 drum-g tim/beat-b tim/beat-count-b 0))
;;(def c-sequencer4 (seq/mk-sequencer 16 "yo5" orig-samples 8 drum-g tim/beat-b tim/beat-count-b 0))

(comment
  (seq/sequencer-write! c-sequencer4 0 [1 1 1 1 1 1 1 1])
  (seq/sequencer-write! c-sequencer4 1 (repeat 8 0))
  (seq/sequencer-write! c-sequencer4 3 [0  0 0 0 0 0 0 0])
  (seq/sequencer-write! c-sequencer4 0 [0 1 0 1 0 1 0 1]))

;; (seq/sequencer-set-out-bus! (:sequencer sequencer) 0)
;; (seq/sequencer-set-out-bus! (:sequencer sequencer2) 0)

;; (ctl (:group c-sequencer) :out-bus (mx :master-drum))

;; (ctl (-> sequencer :sequencer :mixer-group) :out-bus (nkmx :m0))
;; (ctl (-> sequencer2 :sequencer :mixer-group) :out-bus (mx :drum-beats))

;; (seq/sequencer-set-out-bus! c-sequencer4 (nkmx :s0))
;; (seq/sequencer-set-out-bus! c-sequencer (nkmx :s1))
;; (stop)
