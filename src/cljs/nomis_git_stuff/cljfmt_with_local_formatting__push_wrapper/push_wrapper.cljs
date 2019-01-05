(ns nomis-git-stuff.cljfmt-with-local-formatting--push-wrapper.push-wrapper
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [nomis-git-stuff.common.git :as git]
            [nomis-git-stuff.common.utils :as u]
            [planck.core :as core]
            [planck.io :as io]
            [planck.shell :as shell]))

(defn stash [stash-name]
  (println "Stashing if dirty.")
  (git/stash-if-dirty-include-untracked stash-name))

(defn reset-to-remote-commit [ref]
  (println "Resetting (hard) to remote commit" ref)
  (git/reset-hard ref))

(defn reformat-and-commit-if-dirty []
  (println "Applying cljfmt formatting.")
  (u/bash "lein cljfmt fix")
  (if-not (git/dirty?)
    (println "    cljfmt formatting made no changes.")
    (do
      (println "    Committing: apply-cljfmt-formatting.")
      (git/add ".")
      (git/commit--quiet--no-verify--allow-empty "apply-cljfmt-formatting"))))

(defn checkout-and-reformat-and-commit-if-dirty [sha]
  (println "    Checking out" sha)
  (git/checkout-pathspec=dot sha)
  (println "    Applying cljfmt formatting.")
  (u/bash "lein cljfmt fix")
  (if-not (git/dirty?)
    (println "    Repo is clean -- not committing for" sha)
    (do
      (println "    Committing for" sha)
      (git/add ".")
      (git/commit--quiet--no-verify--allow-empty-v2 "-C" sha))))

(defn push []
  (println "Pushing.")
  (git/push "--no-verify"))

(defn set-index-to-as-it-was [user-commit-sha]
  (git/checkout-pathspec=dot user-commit-sha))

(defn maybe-create-local-formatting-commit [user-commit-sha]
  (when (git/dirty?)
    (println "Committing: apply-local-formatting.")
    (git/commit--quiet--no-verify--allow-empty "apply-local-formatting")))

(defn restore-uncommitted-changes [stash-name]
  (println "Restoring any uncommitted changes.")
  (git/apply-stash-if-ends-with--not-index stash-name))

(defn push-wrapper []
  (git/run-some-testy-stuff "dummy-remote-name-from-command-line"
                            "dummy-remote-location"
                            [])
  (println "==== push-wrapper stdout")
  (u/err-println "==== push-wrapper stderr")
  (let [remote-name        (git/remote-name)
        branch-name        (git/branch-name)
        ;; TODO Are you are making assumptions about the name of the
        ;;      remote branch?
        remote-branch-name (str remote-name "/" branch-name)
        unpushed-shas      (git/range->shas remote-branch-name "HEAD")
        n-unpushed-shas    (count unpushed-shas)
        user-commit-sha    (last unpushed-shas)]
    (println "unpushed-shas   =" unpushed-shas)
    (println "user-commit-sha =" user-commit-sha)
    (when unpushed-shas
      (let [pushed-sha (git/->sha (str "HEAD~" n-unpushed-shas))]
        (println "pushed-sha =" pushed-sha)
        (let [stash-name (git/safekeeping-stash-name
                          "_nomis-cljfmt-with-local-formatting--push-wrapper"
                          "push-wrapper"
                          user-commit-sha)]
          (do
            (stash stash-name)
            (reset-to-remote-commit remote-branch-name)
            (reformat-and-commit-if-dirty)
            (doseq [sha unpushed-shas]
              (println "Processing" sha (git/ref->commit-message sha))
              (checkout-and-reformat-and-commit-if-dirty sha))
            (push)
            (set-index-to-as-it-was user-commit-sha)
            (maybe-create-local-formatting-commit user-commit-sha)
            (restore-uncommitted-changes stash-name)))))))
