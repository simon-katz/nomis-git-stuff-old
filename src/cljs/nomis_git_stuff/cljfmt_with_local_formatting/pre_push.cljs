(ns nomis-git-stuff.cljfmt-with-local-formatting.pre-push
  (:require [goog.string :as gstring]
            [goog.string.format]
            [planck.core :as core]
            [nomis-git-stuff.common.git-funs :as git-funs]))

(defn pre-push []

  (println (gstring/format "branch-name = \"%s\""
                           (git-funs/branch-name)))
  (println (gstring/format "top-stash-name = \"%s\""
                           (git-funs/top-stash-name)))
  (println (gstring/format "commit message #1 = \"%s\""
                           (git-funs/top-commit-message 1)))
  (println (gstring/format "commit message #2 = \"%s\""
                           (git-funs/top-commit-message 2)))
  (println (gstring/format "commit message #3 = \"%s\""
                           (git-funs/top-commit-message 3)))
  (println (gstring/format "safekeeping-stash-name = \"%s\""
                           (git-funs/safekeeping-stash-name "the-kind"
                                                            "the-type"
                                                            "the-commit-sha")))

  (println "Hello World!")

  (println "stdin =" (core/slurp core/*in*))

  (println "command line args =" cljs.core/*command-line-args*)

  ;; TODO If there's nothing to push, you won't get any lines on stdin.
  ;;      See https://stackoverflow.com/questions/22585091/git-hooks-pre-push-script-does-not-receive-input-via-stdin

  (core/exit 1)

  (println "SHOULDN'T SEE THIS."))
