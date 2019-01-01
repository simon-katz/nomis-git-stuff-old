(ns nomis-git-stuff.common.git
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

(defn remove-trailing-newline [s]
  (str/replace s #"\n$" ""))

(defn split-on-newline
  "Split `s` into multiple strings, one for each line. Ignore any trailing blank lines."
  [s]
  (str/split s #"\n"))

(defn branch-name []
  (-> (bash "git rev-parse --abbrev-ref HEAD")
      remove-trailing-newline))

(defn remote-name []
  (-> (bash "git remote") ; TODO Is there a better way (eg with rev-parse)?
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

(defn apply-stash [] ; TODO Change this to pop when are sure all is OK.
  (bash "git stash apply --quiet --index"))

(defn apply-stash-if-ends-with [s]
  (when (str/ends-with? (top-stash-name)
                        ;; TODO Note difference between these two things.
                        ;;      What's the best way to handle it?
                        s)
    ;; We created a stash; restore things.
    (apply-stash)))

(defn top-commit-message [n]
  (-> (bash "git log --format=%s -n " n " | tail -1")
      remove-trailing-newline))

(defn current-commit-sha []
  (-> (bash "git rev-parse HEAD")
      remove-trailing-newline))

(defn make-timestamp []
  (tf/unparse (tf/formatter "yyyy-MM-dd--HH-mm-ss")
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
      split-on-newline))
