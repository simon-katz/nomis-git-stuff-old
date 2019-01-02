(ns nomis-git-stuff.common.utils
  (:require [clojure.string :as str]
            [cljs-time.core :as time]
            [cljs-time.format :as tf]
            [goog.string :as gstring]
            [goog.string.format]
            [planck.core :as core]
            [planck.shell :as shell]))

(defn exit-with-error
  ([msg]
   (exit-with-error msg 1))
  ([msg status-code]
   (println "Error:" msg) ; Can we write to stderr?
   (core/exit status-code)))

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
  (if (= s "") ; TODO Create `str-split`, and use it from `split-on-space` too.
    []
    (str/split s #"\n")))

(defn split-on-space [s]
  (str/split s #" "))

(defn make-timestamp []
  (tf/unparse (tf/formatter "yyyy-MM-dd--HH-mm-ss")
              (time/now)))
