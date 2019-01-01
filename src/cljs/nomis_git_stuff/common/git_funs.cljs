(ns nomis-git-stuff.common.git-funs ; TODO Rename `git-funs` -> `git`.
  (:require [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [clojure.string :as str]
            [goog.string :as gstring]
            [goog.string.format]
            [planck.shell :as shell]))

(defn bash [& args]
  (let [res (shell/sh "/bin/bash" "-c" (str/join " " args))]
    (if-not (zero? (:exit res))
      (throw (js/Error. (:err res)))
      (:out res))))

(defn bash-f [format-string & format-args]
  (bash (apply gstring/format format-string format-args)))

(defn remove-trailing-newline [s] ; TODO This is broken for newlines in the middle of `s`
  (str/replace s #"\n" ""))

(defn branch-name []
  (-> (bash "git rev-parse --abbrev-ref HEAD")
      remove-trailing-newline))

(defn stash-if-dirty-include-untracked [message]
  ;; ## TODO Take a closer look at these stashes.
  ;; ##      - Do they still seem to delete many files?
  ;; ##        - It's with-local-formatting pre-push stashes.
  ;; ##          - They show deleted files in the "Untracked" section.
  (bash "git stash push"
        "--quiet"
        "--include-untracked"
        "--message" message))

(defn git--replace-previous-n-commits-incl-staged [n
                                                   commit-sha]
  (println "Committing: Replacing last " n "commits with a single commit.")
  (bash "git reset --quiet --soft"
        (str "HEAD~" n))
  ;; TODO You added `--allow-empty` here, but the behaviour from
  ;;      the local formatting post-commit hook was probably better before,
  ;;      because you just got an "apply-local-formatting" commit.
  ;;      When you rewrite this in CLJS, look into doing different things
  ;;      depending on whether anything is being committed.
  (bash "git commit --quiet --no-verify --allow-empty"
        "-C" commit-sha))

(defn top-stash-name []
  (-> (bash "git stash list --format=%s | head -1")
      remove-trailing-newline))

(defn top-commit-message [n]
  (-> (bash "git log --format=%s -n " n " | tail -1")
      remove-trailing-newline))

(defn make-timestamp []
  (tf/unparse (tf/formatter "yyyy-MM-dd--hh-mm-ss")
              (time/now)))

(defn safekeeping-stash-name [kind type commit-sha]
  (gstring/format "%s--%s--for-%s--%s"
                  kind
                  (make-timestamp)
                  commit-sha
                  type))

(defn unpushed-commit-names [remote-name]
  ;; TODO Add validation.
  (-> (bash "git log --format=%s"
            (str remote-name "/" (branch-name) "..HEAD"))
      (str/split #"\n")))
