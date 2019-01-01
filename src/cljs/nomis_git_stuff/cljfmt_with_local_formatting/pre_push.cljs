(ns nomis-git-stuff.cljfmt-with-local-formatting.pre-push
  (:require [goog.string :as gstring]
            [goog.string.format]
            [planck.core :as core]
            [nomis-git-stuff.common.git :as git]))

(defn pre-push []

  (println "command line args =" cljs.core/*command-line-args*)

  (let [[remote-name remote-location] cljs.core/*command-line-args*]
    (println "remote-name =" remote-name)
    (println "remote-location =" remote-location)
    (assert (= remote-name "origin") ; TODO Do you need this check?
            "ERROR: This only works when there is a single remote and it is named \"origin\".")
    (let [unpushed-commit-names (git/unpushed-commit-names remote-name)]
      (println (gstring/format "unpushed-commit-names = %s"
                               unpushed-commit-names))))

  (println (gstring/format "branch-name = \"%s\""
                           (git/branch-name)))
  (println (gstring/format "top-stash-name = \"%s\""
                           (git/top-stash-name)))
  (println (gstring/format "commit message #1 = \"%s\""
                           (git/top-commit-message 1)))
  (println (gstring/format "commit message #2 = \"%s\""
                           (git/top-commit-message 2)))
  (println (gstring/format "commit message #3 = \"%s\""
                           (git/top-commit-message 3)))
  (println (gstring/format "safekeeping-stash-name = \"%s\""
                           (git/safekeeping-stash-name "the-kind"
                                                       "the-type"
                                                       "the-commit-sha")))

  (println "Hello World!")

  (println "stdin =" (core/slurp core/*in*))

  ;; TODO If there's nothing to push, you won't get any lines on stdin.
  ;;      See https://stackoverflow.com/questions/22585091/git-hooks-pre-push-script-does-not-receive-input-via-stdin

  (println (git/bash "git remote"))

  (core/exit 1)

  (println "SHOULDN'T SEE THIS."))
