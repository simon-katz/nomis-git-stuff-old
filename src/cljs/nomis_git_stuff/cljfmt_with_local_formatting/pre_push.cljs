(ns nomis-git-stuff.cljfmt-with-local-formatting.pre-push
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [planck.core :as core]
            [nomis-git-stuff.common.git :as git]))

(defn exit-with-error
  ([msg]
   (exit-with-error msg 1))
  ([msg status-code]
   (println "Error:" msg) ; Can we write to stderr?
   (core/exit status-code)))

(defn local-formatting-commit-message? [s]
  (str/starts-with? s "apply-local-formatting"))

(defn run-some-testy-stuff [remote-name
                            remote-location
                            stdin]
  (println "remote-name =" remote-name)
  (println "remote-location =" remote-location)
  ;; TODO If there's nothing to push, you won't get any lines on stdin.
  ;;      See https://stackoverflow.com/questions/22585091/git-hooks-pre-push-script-does-not-receive-input-via-stdin
  (println (gstring/format "stdin = \"%s\"" stdin))
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
  (println "________________________________________"))

(defn pre-push []
  (let [[remote-name remote-location] cljs.core/*command-line-args*
        stdin (core/slurp core/*in*)]
    (run-some-testy-stuff remote-name
                          remote-location
                          stdin)
    (println "git remote =" (git/bash "git remote"))


    (assert (= remote-name "origin") ; TODO Do you need this check?
            "ERROR: This only works when there is a single remote and it is named \"origin\".")
    (let [unpushed-commit-names (git/unpushed-commit-names remote-name)]
      (println (gstring/format "unpushed-commit-names = %s"
                               unpushed-commit-names))
      (when (some local-formatting-commit-message?
                  unpushed-commit-names)
        (exit-with-error
         (str/join " "
                   ["Cannot push an \"apply-local-formatting\" commit."
                    "You may want the \"nomis-cljfmt-v2-git-push\" command."]))))

    (core/exit 1) ; TODO for now

    (println "SHOULDN'T SEE THIS.")))
