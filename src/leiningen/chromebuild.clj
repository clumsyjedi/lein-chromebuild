(ns leiningen.chromebuild
  (:require [leiningen.help :as lhelp]
            [leiningen.core.main :as lmain]
            [leiningen.cljsbuild :refer [cljsbuild]]
            [cljsbuild.util]
            [clojure.java.io :as io]
            [juxt.dirwatch :refer  (watch-dir)])
  (:import [java.util.zip ZipOutputStream ZipEntry]))

(defn copy-dir [src-dir target-dir]
  (.mkdirs (io/file target-dir))
  (doseq [src-file (file-seq (io/file src-dir))
          :let [target-file (io/file (str target-dir "/" (.getName src-file)))]
          :when (.isFile src-file)]
    (io/copy src-file target-file)))

(defn- zip-dir [src-dir target]
  (let [n-prefix (if-some [prefix (.getParent (io/file src-dir))]
                   (inc (count prefix)) 0)]
    (with-open [out (ZipOutputStream. (io/output-stream target))]
      (doseq [f (file-seq (io/file src-dir))
              :let [entry (subs (.getPath f) n-prefix)]]
        (if (.isDirectory f)
          (.putNextEntry out (ZipEntry. (str entry "/")))
          (do (.putNextEntry out (ZipEntry. entry))
              (io/copy f out)
              (.closeEntry out)))))))

(defn- once
  [project 
   {{:keys [resource-paths unpacked-target-path] 
     :or {resource-paths ["resources/js" "resources/html" "resources/images"] 
          unpacked-target-path "target/unpacked"}
     :as chromebuild} :chromebuild
    :as opts} 
   args]
  (doseq [src-dir resource-paths]
    (copy-dir src-dir unpacked-target-path))
  (cljsbuild project "once")
  (println "Built extension to" unpacked-target-path))


(defn- auto 
  [project 
   {{:keys [resource-paths] 
     :or {resource-paths ["resources/js" "resources/html" "resources/images"]}} 
    :chromebuild
    :as opts}
   args]
  (once project opts args)
  (doseq [path (concat (:source-paths project) resource-paths)]
    (watch-dir (fn [& _] (once project opts args))
               (io/file path)))
  @(future (loop [] (Thread/sleep 1000) (recur))))

(defn- clean
  [project 
   {{:keys [unpacked-target-path] :or {unpacked-target-path "target/unpacked"}
     :as chromebuild} :chromebuild
    :as opts} 
   args]
  (cljsbuild project "clean")
  (letfn [(rm [file]
            (when (.exists file)
              (println "deleting" file)
              (if (.isDirectory file)
                (do (doseq [f (.listFiles file)] 
                      (rm f))
                    (io/delete-file file))
                (io/delete-file file))))]
    (rm (io/file unpacked-target-path))
    (rm (io/file (str unpacked-target-path ".zip")))))

(defn- zip
  "Package up unpacked target into a zip file."
  [project
   {{:keys [unpacked-target-path] :or {unpacked-target-path "target/unpacked"}
     :as chromebuild} :chromebuild
     :as opts}
   args]
  (once project opts args)
  (let [target (str unpacked-target-path ".zip")]
    (zip-dir unpacked-target-path target)
    (println "Created" target)))

(defn- extract-options [project]
  (select-keys project [:chromebuild :cljsbuild]))

(defn chromebuild
  "Run the chromebuild plugin."
  {:help-arglists '([once auto clean zip])
   :subtasks [#'once #'auto #'clean #'zip]}
  ([project]
   (println
     (lhelp/help-for "cljsbuild"))
   (lmain/abort))
  ([project subtask & args]
   (let [options (extract-options project)]
     (case subtask
       "once" (once project options args)
       "auto" (auto project options args)
       "clean" (clean project options args)
       "zip" (zip project options args)
       (do
         (println
           "Subtask" (str \" subtask \") "not found."
           (lhelp/subtask-help-for *ns* #'chromebuild))
         (lmain/abort))))))


