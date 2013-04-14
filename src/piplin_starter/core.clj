(ns piplin-starter.core
  (:refer-clojure :as clj :exclude [not= bit-or bit-xor + - * bit-and inc dec bit-not < > <= >= = cast not cond condp and or bit-shift-left bit-shift-right pos? neg? zero?])
  (:use piplin.core plumbing.core))

(def johnson-counter
  ;We can convert maps of data to simulatable/synthesizable objects by "modulizing" them
  (modulize :johnson ;:johnson is the name of this module
    ;This map specifies all of the logic. fnk is like fn, but it takes only a single
    ;argument, which is a map. It then extracts keys from the maps in the argument
    ;position. Because we have lots of names wires, fnk is how we specify logic, since
    ;it allows us to succinctly write complex dataflows.
    {:q (fnk [q direction]
             (mux2 direction ;mux2 is like if, but synthesizable
               (bit-cat
                 (bit-slice q 1 4)
                 (bit-not (bit-slice q 0 1)))
               (bit-cat
                 (bit-not (bit-slice q 3 4))
                 (bit-slice q 0 3))))}
    ;This map specifies registers. You cannot have cycles in your logic, unless they're
    ;split by registers.
    {:q #b0000}))

(def johnson-director
  (modulize :root
    ;This wire has no inputs; it just constructs a submodule
    {:output (fnk []
                  (:q (johnson-counter :direction true)))}
    ;No additional registers defined
    {}))

(def compiled-module (compile-root johnson-director))

(comment
  (sim compiled-module 100)

  ;Verify takes an uncompiled module that requires no inputs and a number of cycles
  ;It returns the Verilog source code to verify that the results are identical
  (spit "tmp" (verify johnson-director 100))

  ;We'll just shell out with clojure
  (require 'clojure.java.shell)
  (clojure.java.shell/sh "iverilog" "tmp")
  ;You should see "tests passed" if the simulations matched
  (clojure.java.shell/sh "./a.out")

  (println
    (->verilog (compile-root johnson-counter
                             :direction (input "dir" (anontype :boolean)))
               {[:johnson :q] "out"})))
