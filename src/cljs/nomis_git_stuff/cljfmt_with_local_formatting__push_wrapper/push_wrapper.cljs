(ns nomis-git-stuff.cljfmt-with-local-formatting--push-wrapper.push-wrapper
  (:require [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [nomis-git-stuff.common.git :as git]
            [nomis-git-stuff.common.utils :as u]
            [planck.core :as core]
            [planck.io :as io]
            [planck.shell :as shell]))

(def filename-for-in-push-wrapper ".git/_nomis-in-pre-push-wrapper")

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
      (u/bash "git commit --quiet --no-verify --allow-empty"
              "-C" sha))))

(defn push []
  (println "Pushing.")
  (git/push))

(defn set-index-to-as-it-was [user-commit-sha]
  (u/bash "git checkout --quiet" user-commit-sha "."))

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

  (if (u/file-exists? filename-for-in-push-wrapper)
    (binding [*out* planck.core/*err*]
      (println filename-for-in-push-wrapper
               "already exists -- something must have gone wrong before -- investigate, and/or delete the file and try again."))
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
        (try
          (let [pushed-sha (git/->sha (str "HEAD~" n-unpushed-shas))]
            (println "pushed-sha =" pushed-sha)
            (u/touch filename-for-in-push-wrapper)
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
                (restore-uncommitted-changes stash-name))))
          (finally
            (io/delete-file filename-for-in-push-wrapper)))))))

;; **** Before-push
;; ***** Create a cljfmt-for-all-files commit at the start, and remove it if no changes
;; ***** For each commit
;; **** Push
;; ***** Create ".git/_nomis-in-pre-push-wrapper"
;; ***** `git push`
;; ***** Delete ".git/_nomis-in-pre-push-wrapper"
;; **** After-push
;; ***** If there was an apply-local-formatting commit, create-commit-for-apply-local-formatting
;; ***** restore-uncommitted-changes
