(ns nomis-git-stuff.cljfmt-with-local-formatting.pre-push
  (:require [planck.core :as core]
            [nomis-git-stuff.common.git-funs :as git-funs]))

(defn pre-push []
  (println "Hello World!")

  (println "stdin =" (core/slurp core/*in*))

  (println "command line args =" cljs.core/*command-line-args*)

  ;; TODO If there's nothing to push, you won't get any lines on stdin.
  ;;      See https://stackoverflow.com/questions/22585091/git-hooks-pre-push-script-does-not-receive-input-via-stdin

  (core/exit 1)

  (println "SHOULDN'T SEE THIS."))
