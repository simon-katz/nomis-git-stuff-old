(ns nomis-git-stuff.cljfmt-with-local-formatting.post-commit
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [nomis-git-stuff.common.git :as git]
            [nomis-git-stuff.common.utils :as u]
            [planck.core :as core]
            [planck.io :as io]
            [planck.shell :as shell]))

(def doing-post-commit-rewriting-filename
  ".git/_nomis-doing-post-commit-rewriting")

(defn local-formatting-commit-message? [s] ; TODO Copy/paste from other hook.
  (str/starts-with? s "apply-local-formatting"))

(defn clojure-ish-file? [s]
  (or (str/ends-with? s ".clj")
      (str/ends-with? s ".cljs")
      (str/ends-with? s ".cljx")
      (str/ends-with? s ".cljc")))

(defn reformat-and-commit [user-commit-sha]
  ;; For details of command line args and stdin, see:
  ;; - https://git-scm.com/docs/githooks#_post_commit
  ;; (There are no args and no stdin.)
  (println "Applying cljfmt formatting.")
  ;; # There's a possibility here of exceeding the maximum shell command length.
  ;; # - TODO Make sure you detect and report any such error.
  ;; # - If this is a problem, don't create and use `changed_files`.
  (let [changed-files (->> (git/changed-files (str user-commit-sha "~2")
                                              user-commit-sha)
                           (filter clojure-ish-file?)
                           (str/join " "))]
    (println "The changed Clojure files are:" changed-files)
    (u/bash "lein cljfmt fix" changed-files))
  (git/add ".")
  (println "Committing: cljfmt formatting.")
  (git/commit--quiet--no-verify--allow-empty "apply-cljfmt-formatting"))

(defn create-commit-for-apply-local-formatting [user-commit-sha]
  (println "Committing: apply-local-formatting.")
  ;; Set index to be as it was.
  (u/bash "git checkout --quiet" user-commit-sha ".")
  ;; Create the commit.
  ;; TODO Get the exact 'apply-local-formatting' message from the commit.
  (git/commit--quiet--no-verify--allow-empty "apply-local-formatting"))

(defn restore-uncommitted-changes [stash-name]
  (println "Restoring any uncommitted changes.")
  (git/apply-stash-if-ends-with--not-index stash-name))

(defn print-in-nested-post-commit-hook-message []
  (println "    post-commit hook: Doing nothing, because we are in a nested post-commit hook."))

(defn print-this-is-an-apply-local-formatting-commit []
  (println "    post-commit hook: Doing nothing, because this is an \"apply-local-formatting\" commit."))

(defn print-previous-commit-is-not-apply-local-formatting-message []
  (println "    post-commit hook: Doing nothing, because previous commit is not an \"apply-local-formatting\" commit -- commit message is: "
           (git/top-commit-message 2)))

(defn post-commit []
  (cond
    (u/file-exists? doing-post-commit-rewriting-filename)
    (print-in-nested-post-commit-hook-message)
    ;;
    (local-formatting-commit-message? (git/top-commit-message 1))
    (print-this-is-an-apply-local-formatting-commit)
    ;;
    (not (local-formatting-commit-message? (git/top-commit-message 2)))
    (print-previous-commit-is-not-apply-local-formatting-message)
    ;;
    :else
    (do
      (println "Preparing to reformat.")
      (u/touch doing-post-commit-rewriting-filename)
      (let [user-commit-sha (git/current-commit-sha)
            stash-name      (git/safekeeping-stash-name
                             "_nomis-cljfmt-with-local-formatting"
                             "post-commit"
                             user-commit-sha)]
        (git/stash-if-dirty-include-untracked stash-name)
        (reformat-and-commit user-commit-sha)
        (git/replace-previous-n-commits-incl-staged 3 user-commit-sha)
        (create-commit-for-apply-local-formatting user-commit-sha)
        (restore-uncommitted-changes stash-name))
      (io/delete-file doing-post-commit-rewriting-filename))))
