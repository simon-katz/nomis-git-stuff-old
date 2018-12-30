(ns nomis-git-stuff.common.git-funs
  (:require [clojure.string :as str]
            [planck.shell :as shell]))

(defn my-sh [command]
  (let [res (shell/sh "/bin/bash" "-c" command)]
    (if-not (zero? (:exit res))
      (throw (js/Error. (:err res)))
      (:out res))))

(defn remove-trailing-newline [s] ; TODO This is broken for newlines in the middle of `s`
  (str/replace s #"\n" ""))

(defn branch-name []
  ;; TODO Make the grep here a bit more selective.
  (-> (my-sh "git branch | grep \\* | cut -d ' ' -f2")
      remove-trailing-newline))

(defn top-stash-name []
  (-> (my-sh "git stash list --format=%s | head -1")
      remove-trailing-newline))

(defn top-commit-message [n]
  (-> (my-sh (str "git log --format=%s -n " n " | tail -1"))
      remove-trailing-newline))

