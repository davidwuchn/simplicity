(ns build
  "Build script for creating deployment artifacts.
   
   Usage:
   - Build uberjar: clojure -T:build uberjar
   - Clean: clojure -T:build clean"
  (:require [clojure.tools.build.api :as b]))

(def lib 'cc.mindward/simplicity)
(def version (clojure.string/trim (slurp "VERSION")))
(def class-dir "target/classes")
(def uber-file "target/simplicity-standalone.jar")

;; Basis for uberjar (production dependencies only)
(def basis (b/create-basis {:project "deps.edn"
                            :aliases [:prod]}))

(defn clean
  "Remove the target directory."
  [_]
  (println "Cleaning target directory...")
  (b/delete {:path "target"})
  (println "Clean complete."))

(defn uberjar
  "Build an uberjar for production deployment.
   
   The uberjar includes:
   - All application code
   - All dependencies
   - Resources (logback.xml, public assets)
   - Main class: cc.mindward.web-server.core
   
   Run with: java -jar target/simplicity-standalone.jar"
  [_]
  (clean nil)
  (println (str "\nBuilding uberjar: " uber-file))
  (println (str "Version: " version))
  (println (str "Library: " lib "\n"))
  
  ;; Copy source files
  (println "Copying source files...")
  (b/copy-dir {:src-dirs ["bases/web-server/src"
                          "components/auth/src"
                          "components/user/src"
                          "components/game/src"
                          "components/ui/src"]
               :target-dir class-dir})
  
  ;; Copy resources
  (println "Copying resources...")
  (b/copy-dir {:src-dirs ["bases/web-server/resources"]
               :target-dir class-dir})
  
  ;; Copy VERSION file to classpath root
  (println "Copying VERSION file...")
  (b/copy-file {:src "VERSION"
                :target (str class-dir "/VERSION")})
  
  ;; Compile Java classes (AOT compilation for main)
  (println "Compiling main class...")
  (b/compile-clj {:basis basis
                  :ns-compile '[cc.mindward.web-server.core]
                  :class-dir class-dir})
  
  ;; Create uberjar
  (println "Creating uberjar...")
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis basis
           :main 'cc.mindward.web-server.core})
  
  (println (str "\n✓ Build complete: " uber-file))
  (println "\nRun with:")
  (println "  java -jar target/simplicity-standalone.jar")
  (println "\nOr with custom port:")
  (println "  PORT=8080 java -jar target/simplicity-standalone.jar")
  (println "\nEnvironment variables:")
  (println "  PORT         - HTTP port (default: 3000)")
  (println "  DB_PATH      - Database path (default: ./simplicity.db)")
  (println "  LOG_LEVEL    - Log level: DEBUG|INFO|WARN|ERROR (default: INFO)")
  (println "  LOG_PATH     - Log directory (default: ./logs)")
  (println "  ENABLE_HSTS  - Enable HSTS header: true|false (default: false)"))

(defn jar-info
  "Display information about the built jar."
  [_]
  (if (.exists (java.io.File. uber-file))
    (do
      (println (str "Jar file: " uber-file))
      (println (str "Size: " (/ (.length (java.io.File. uber-file)) 1024 1024) " MB"))
      (println "\nManifest:")
      (let [jar (java.util.jar.JarFile. uber-file)
            manifest (.getManifest jar)]
        (doseq [[k v] (.getMainAttributes manifest)]
          (println (str "  " k ": " v)))))
    (println "Jar file not found. Run 'clojure -T:build uberjar' first.")))
