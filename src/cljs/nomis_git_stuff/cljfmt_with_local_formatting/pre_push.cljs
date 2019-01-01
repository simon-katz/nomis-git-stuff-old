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

(defn split-on-newline [s] (if (= s "") [] (str/split s #"\n")))
(defn split-on-space   [s] (str/split s #" "))

(defn stdin->push-info [s]
  (map split-on-space
       (split-on-newline s)))

(defn ensure-remote-name-ok [remote-name]
  (assert (= remote-name "origin") ; TODO Do you need this check?
          "ERROR: This only works when there is a single remote and it is named \"origin\"."))

(defn ensure-n-things-being-pushed-ok [push-info]
  (let [n-things-being-pushed (count push-info)]
    (when (not= n-things-being-pushed 1)
      (exit-with-error
       (gstring/format
        "Don't know what to do unless a single thing is being pushed. We have %s: %s"
        n-things-being-pushed
        push-info)))))

(defn ensure-not-confused-about-what-is-being-pushed [push-info]
  (assert (= (git/current-commit-sha)
             (-> push-info first second))
          "Confused about what is being pushed."))

(defn local-formatting-commit-message? [s]
  (str/starts-with? s "apply-local-formatting"))

(defn ensure-not-pushing-local-formatting [remote-name]
  ;; TODO Maybe you should be using the `push-info` to get the commits.
  ;;      But then would you need a git call for each commit to get the commit
  ;;      messages?
  (let [unpushed-commit-names (git/unpushed-commit-names remote-name)]
    (println (gstring/format "unpushed-commit-names = %s"
                             unpushed-commit-names))
    (when (some local-formatting-commit-message?
                unpushed-commit-names)
      (exit-with-error
       (str/join " "
                 ["Cannot push an \"apply-local-formatting\" commit."
                  "You may want the \"nomis-cljfmt-v2-git-push\" command."])))))

(defn ensure-push-ok [remote-name
                      push-info]
  (ensure-remote-name-ok remote-name)
  (ensure-n-things-being-pushed-ok push-info)
  (ensure-not-confused-about-what-is-being-pushed push-info)
  (ensure-not-pushing-local-formatting remote-name))

(defn run-some-testy-stuff [remote-name
                            remote-location
                            push-info]
  (println (gstring/format "remote-name from command-line parameters = \"%s\""
                           remote-name))
  (println (gstring/format "remote-name from `git/remote-name` = \"%s\""
                           (git/remote-name)))
  (println "remote-location =" remote-location)
  (println (gstring/format "push-info = %s" push-info))
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
  (println (gstring/format "(git/current-commit-sha) = \"%s\""
                           (git/current-commit-sha)))
  (println (gstring/format "(-> push-info first second) = \"%s\""
                           (-> push-info first second)))
  (println "________________________________________"))

(defn pre-push []
  ;; For details of command line args and stdin, see:
  ;; - https://git-scm.com/docs/githooks#_pre_push
  (let [[remote-name remote-location] cljs.core/*command-line-args*
        push-info (stdin->push-info (core/slurp core/*in*))]
    (run-some-testy-stuff remote-name
                          remote-location
                          push-info)
    (when-not (zero? (count push-info))
      (ensure-push-ok remote-name
                      push-info)
      (let [current-commit-sha (-> push-info first second)
            stash-name         (git/safekeeping-stash-name
                                "_nomis-cljfmt-with-local-formatting"
                                "pre-push"
                                current-commit-sha)]
        (git/stash-if-dirty-include-untracked stash-name)
        ;; check format
        ;; We created a stash; restore things.
        (git/apply-stash-if-ends-with stash-name)
        (println "Everything so far is OK.")
        (core/exit 1) ; TODO for now
        (println "SHOULDN'T SEE THIS.")))))
