(require '[cljs.build.api :as api]
         '[clojure.java.shell :as shell]
         '[clojure.string :as string])

;;; Configuration.

(def source-dir "src")

(def compiler-config (clojure.edn/read-string (slurp "cljs.edn")))

(def dev-config (merge compiler-config
                       {:optimizations :none}))


;;; Tasks mechanism.

(defmulti task first)

(defmethod task :default
  [args]
  (let [all-tasks (-> task methods (dissoc :default) keys sort (->> (interpose ", ") (apply str)))]
    (println "unknown or missing task argument. Choose one of:" all-tasks)
    (System/exit 1)))


;;; Helper functions.

(defn try-require [ns-sym]
  (try (require ns-sym) true (catch Exception e false)))

(defmacro with-namespaces
  [namespaces & body]
  (if (every? try-require namespaces)
    `(do ~@body)
    `(do (println "task not available - required dependencies not found")
         (System/exit 1))))


;;; Compiling task.

(defn compile-once []
  (api/build source-dir compiler-config))

(defn compile-refresh []
  (api/watch source-dir compiler-config))


(defmethod task "compile" [[_ type]]
  (case type
    (nil "once") (compile-once)
    "watch"      (compile-refresh)
    (do (println "Unknown argument to compile task:" type)
        (System/exit 1))))


;;; Figwheeling task

(defmethod task "figwheel" [[_]]
  (with-namespaces [figwheel-sidecar.repl-api]
    (figwheel-sidecar.repl-api/start-figwheel!
     {:figwheel-options {:server-port 5000
                         :nrepl-port       3312
                         :nrepl-middleware ["cider.nrepl/cider-middleware"
                                            "cemerick.piggieback/wrap-cljs-repl"]}
      :all-builds       [{:id           "dev"
                          :figwheel     {:on-jsload "blockchain.core/reload"}
                          :source-paths [source-dir]
                          :compiler     dev-config}]})))


;;; Build script entrypoint.

(task *command-line-args*)
