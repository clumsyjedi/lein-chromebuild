(ns leiningen.chromebuild
  (:require [leiningen.help :as lhelp]
            [leiningen.core.main :as lmain]
            [leiningen.cljsbuild :refer [cljsbuild]]
            [cljsbuild.util]
            [clojure.java.io :as io]
    ))

(defn copy-dir [src-dir target-dir]
  (.mkdirs (io/file target-dir))
  (doseq [src-file (file-seq (io/file src-dir))
          :let [target-file (io/file (str target-dir "/" (.getName src-file)))]
          :when (.isFile src-file)]
    (io/copy src-file target-file)))

(defn- once
  [project 
   {:keys [resource-paths unpacked-target-path]
    :or {resource-paths ["resources/js" "resources/html" "resources/images"] 
         unpacked-target-path "target/unpacked"}} 
   args]
  (doseq [src-dir resource-paths]
    (copy-dir src-dir unpacked-target-path))
  (cljsbuild project "once")
  (println "built extension to" unpacked-target-path))


(defn- auto [project options args]
  (cljsbuild.util/once-every 1000 "chromebuild" #(once project options args)))

(defn- clean [])

(defn- extract-options [project]
  (:chromebuild project))

(defn chromebuild
  "Run the chromebuild plugin."
  {:help-arglists '([once auto clean])
   :subtasks [#'once #'auto #'clean]}
  ([project]
   (println
     (lhelp/help-for "cljsbuild"))
   (lmain/abort))
  ([project subtask & args]
   (let [options (extract-options project)]
     (case subtask
       "once" (once project options args)
       "auto" (auto project options args)
       "clean" (clean project options)
       (do
         (println
           "Subtask" (str \" subtask \") "not found."
           (lhelp/subtask-help-for *ns* #'chromebuild))
         (lmain/abort))))))


