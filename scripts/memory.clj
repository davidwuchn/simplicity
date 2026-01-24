#!/usr/bin/env bb

(ns memory
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [babashka.fs :as fs]
            [babashka.process :as proc]))

;; ============================================================================
;; MEMENTUM CLI - Git Memory System
;; ============================================================================
;;
;; λ store(x) → memories/{symbol}-{date}-{slug}.md → git commit -m "{symbol} x"
;; λ recall(q,n=2) → git log -n n -- memories/ | git grep -i q
;;
;; Symbols: 🧠💡 insight  🔄Δ pattern-shift  🎯⚡ decision  🌀 meta
;;
;; repo=memory | commits=timeline | git=database
;; [phi fractal euler tao pi mu] | [Δ λ ∞/0 | ε/φ Σ/μ c/h] | OODA
;; Human ⊗ AI
;; ============================================================================

(def valid-symbols
  "Valid memory symbols with their emoji representations"
  {:insight "🧠💡"
   :pattern-shift "🔄Δ"
   :decision "🎯⚡"
   :meta "🌀"})

(defn shell
  "Execute shell command and return output"
  [cmd]
  (-> (proc/shell {:out :string} cmd)
      :out
      str/trim))

(defn git-status
  "Check git status for memories directory"
  []
  (shell "git status --porcelain memories/"))

(defn ensure-memories-dir
  "Ensure memories directory exists"
  []
  (fs/create-dirs "memories"))

(defn count-tokens
  "Count tokens in text (rough approximation)"
  [text]
  (count (str/split text #"\s+")))

(defn store-memory
  "λ store(x) - Store a critical memory"
  [{:keys [symbol slug content interactive]}]
  (ensure-memories-dir)
  
  (let [symbol-key (keyword symbol)
        _ (when-not (valid-symbols symbol-key)
            (println (str "Invalid symbol. Must be one of: "
                          (str/join ", " (map name (keys valid-symbols)))))
            (System/exit 1))
        
        date (.format (t/local-date) "yyyy-MM-dd")
        filename (str "memories/" symbol "-" date "-" slug ".md")
        token-count (count-tokens content)]
    
    (when (fs/exists? filename)
      (println (str "Warning: File already exists: " filename))
      (print "Overwrite? (y/N): ")
      (flush)
      (when-not (= "y" (read-line))
        (println "Aborted.")
        (System/exit 0)))
    
    (when (> token-count 200)
      (println (str "Warning: Memory exceeds 200 tokens (" token-count ")."))
      (when interactive
        (print "Continue anyway? (y/N): ")
        (flush)
        (when-not (= "y" (read-line))
          (println "Aborted. Compress ruthlessly.")
          (System/exit 0))))
    
    (spit filename content)
    (println (str "✓ Memory saved to: " filename))
    
    (let [git-status (git-status)]
      (if (str/blank? git-status)
        (do
          (println "✓ File is already tracked by git")
          filename)
        (do
          (println "\nTo commit this memory:")
          (println (str "  git add " filename))
          (println (str "  git commit -m \"" (get valid-symbols symbol-key) " " symbol " " slug "\""))
          filename)))))

(defn recall-memories
  "λ recall(q,n=2) - Recall memories with optional query and depth"
  [{:keys [query depth symbol]}]
  (ensure-memories-dir)
  
  (cond
    query
    (do
      (println (str "Searching memories for: \"" query "\""))
      (println (shell (str "git grep -i \"" query "\" memories/"))))
    
    symbol
    (do
      (println (str "Searching memories by symbol: " symbol))
      (println (shell (str "git log --grep \"" symbol "\" -- memories/ --oneline"))))
    
    :else
    (let [n (or depth 2)]
      (println (str "Recent memories (n=" n "):"))
      (println (shell (str "git log -n " n " -- memories/ --oneline"))))))

(defn ooda-loop
  "Run MEMENTUM OODA loop"
  []
  (println "═══════════════════════════════════════════════════════════")
  (println "  MEMENTUM OODA Loop")
  (println "═══════════════════════════════════════════════════════════")
  
  ;; 1. OBSERVE
  (println "\n1. OBSERVE (recent context, n=13):")
  (println (shell "git log -n 13 -- memories/ --oneline"))
  
  ;; 2. ORIENT
  (println "\n2. ORIENT (search for patterns):")
  (print "Enter search query (or press Enter to skip): ")
  (flush)
  (let [query (read-line)]
    (when-not (str/blank? query)
      (println (shell (str "git grep -i \"" query "\" memories/")))))
  
  ;; 3. DECIDE & 4. ACT
  (println "\n3. DECIDE (create/update memory)")
  (println "4. ACT (git commit)")
  (println "\nRun 'bb memory:store' or './scripts/memory.clj store' to create a new memory.")))

(defn memory-stats
  "Show MEMENTUM statistics"
  []
  (ensure-memories-dir)
  
  (let [memory-files (fs/glob "memories" "*.md")
        memory-count (count memory-files)
        symbols (->> memory-files
                    (map #(first (str/split (fs/file-name %) #"-")))
                    frequencies
                    (sort-by val >))
        
        total-tokens (->> memory-files
                         (map #(count-tokens (slurp (str %))))
                         (reduce + 0))
        
        avg-tokens (if (pos? memory-count)
                     (int (/ total-tokens memory-count))
                     0)]
    
    (println "═══════════════════════════════════════════════════════════")
    (println "  MEMENTUM Statistics")
    (println "═══════════════════════════════════════════════════════════")
    (println (str "Total memories: " memory-count))
    (println (str "Total tokens: " total-tokens " (avg: " avg-tokens ")"))
    
    (println "\nBy symbol:")
    (doseq [[symbol count] symbols]
      (println (str "  " symbol ": " count " memories")))
    
    (println "\nRecent memories:")
    (println (shell "git log -n 5 -- memories/ --oneline"))
    
    (println "═══════════════════════════════════════════════════════════")))

(defn validate-memories
  "Validate all memories against MEMENTUM rules"
  []
  (ensure-memories-dir)
  
  (let [memory-files (fs/glob "memories" "*.md")
        issues (atom [])]
    
    (doseq [file memory-files]
      (let [filename (fs/file-name file)
            content (slurp (str file))
            token-count (count-tokens content)
            
            ;; Parse filename
            parts (str/split filename #"-")
            symbol (first parts)
            date (second parts)
            slug (->> (drop 2 parts)
                     (str/join "-")
                     (str/replace #"\.md$" ""))]
        
        ;; Check token limit
        (when (> token-count 200)
          (swap! issues conj {:file filename
                              :issue :token-limit-exceeded
                              :details (str token-count " tokens (>200)")}))
        
        ;; Check symbol validity
        (when-not (valid-symbols (keyword symbol))
          (swap! issues conj {:file filename
                              :issue :invalid-symbol
                              :details (str "Invalid symbol: " symbol)}))
        
        ;; Check date format
        (when-not (re-matches #"\d{4}-\d{2}-\d{2}" date)
          (swap! issues conj {:file filename
                              :issue :invalid-date-format
                              :details (str "Invalid date format: " date)}))))
    
    (if (empty? @issues)
      (println "✓ All memories are valid!")
      (do
        (println "⚠️  Memory validation issues found:")
        (doseq [{:keys [file issue details]} @issues]
          (println (str "  " file ": " issue " - " details)))))))

(defn print-help
  "Print help message"
  []
  (println "MEMENTUM CLI - Git Memory System")
  (println)
  (println "Usage:")
  (println "  ./scripts/memory.clj <command> [options]")
  (println)
  (println "Commands:")
  (println "  store     - Store a critical memory (λ store)")
  (println "  recall    - Recall memories (λ recall)")
  (println "  ooda      - Run OODA loop")
  (println "  stats     - Show memory statistics")
  (println "  validate  - Validate all memories")
  (println "  help      - Show this help")
  (println)
  (println "Examples:")
  (println "  ./scripts/memory.clj store --symbol insight --slug architecture-pattern")
  (println "  ./scripts/memory.clj recall --query \"polylith\"")
  (println "  ./scripts/memory.clj recall --depth 5")
  (println "  ./scripts/memory.clj recall --symbol decision")
  (println)
  (println "Symbols: 🧠💡 insight  🔄Δ pattern-shift  🎯⚡ decision  🌀 meta")
  (println "Auto-Trigger: Store ONLY when critical (novel insight, pattern shift, etc.)")
  (println "Token Budget: Each memory ≤200 tokens. Compress ruthlessly."))

(defn -main
  "Main entry point"
  [& args]
  (let [command (first args)
        opts (->> (rest args)
                 (partition 2)
                 (map (fn [[k v]] [(keyword (str/replace k #"^--" "")) v]))
                 (into {}))]
    
    (case command
      "store" (if (and (:symbol opts) (:slug opts))
                (do
                  (print "Enter memory content (press Ctrl+D when done):\n")
                  (flush)
                  (let [content (slurp *in*)]
                    (store-memory (assoc opts :content content))))
                (do
                  (println "Error: --symbol and --slug are required for store command")
                  (println "Example: ./scripts/memory.clj store --symbol insight --slug my-memory")))
      
      "recall" (recall-memories opts)
      "ooda" (ooda-loop)
      "stats" (memory-stats)
      "validate" (validate-memories)
      "help" (print-help)
      (print-help))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))