#!/usr/bin/env bb

(ns memory-simple
  (:require [clojure.string :as str]
            [babashka.fs :as fs]))

;; ============================================================================
;; MEMENTUM CLI - Simple Git Memory System
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

(defn shell [cmd]
  (-> (babashka.process/shell {:out :string} cmd)
      :out
      str/trim))

(defn ensure-memories-dir []
  (fs/create-dirs "memories"))

(defn print-help []
  (println "MEMENTUM CLI - Git Memory System")
  (println)
  (println "Usage:")
  (println "  ./scripts/memory-simple.clj <command>")
  (println)
  (println "Commands:")
  (println "  store     - Instructions for storing a memory")
  (println "  recall    - Recall recent memories (n=2)")
  (println "  search    - Search memories by content")
  (println "  symbol    - Search memories by symbol")
  (println "  stats     - Show memory statistics")
  (println "  validate  - Validate memory files")
  (println "  help      - Show this help")
  (println)
  (println "Examples:")
  (println "  ./scripts/memory-simple.clj recall")
  (println "  ./scripts/memory-simple.clj stats")
  (println)
  (println "To store a memory manually:")
  (println "  1. Create file: memories/{symbol}-YYYY-MM-DD-{slug}.md")
  (println "  2. Add content (≤200 tokens)")
  (println "  3. Commit: git add memories/*.md")
  (println "  4. Commit: git commit -m \"{symbol} {slug}\"")
  (println)
  (println "Symbols: 🧠💡 insight  🔄Δ pattern-shift  🎯⚡ decision  🌀 meta"))

(defn store-info []
  (println "To store a MEMENTUM memory:")
  (println)
  (println "1. Choose a symbol:")
  (println "   🧠💡 insight     - Novel architectural insight")
  (println "   🔄Δ pattern-shift - Significant pattern shift")
  (println "   🎯⚡ decision     - Strategic decision (>1 week impact)")
  (println "   🌀 meta         - Meta-learning that changes approach")
  (println)
  (println "2. Create memory file:")
  (println "   memories/{symbol}-YYYY-MM-DD-{slug}.md")
  (println "   Example: memories/insight-2024-01-15-mementum-implementation.md")
  (println)
  (println "3. Add content (≤200 tokens, compress ruthlessly)")
  (println)
  (println "4. Commit to git:")
  (println "   git add memories/*.md")
  (println "   git commit -m \"{emoji} {symbol} {slug}\"")
  (println "   Example: git commit -m \"🧠💡 insight mementum-implementation\""))

(defn recall-memories []
  (ensure-memories-dir)
  (println "Recent memories (n=2):")
  (println (shell "git log -n 2 -- memories/ --oneline 2>/dev/null || echo 'No memories found'")))

(defn search-memories []
  (ensure-memories-dir)
  (print "Enter search query: ")
  (flush)
  (let [query (read-line)]
    (println (str "Searching for: \"" query "\""))
    (println (shell (str "git grep -i \"" query "\" memories/ 2>/dev/null || echo 'No matches found'")))))

(defn symbol-search []
  (ensure-memories-dir)
  (print "Enter symbol (insight|pattern-shift|decision|meta): ")
  (flush)
  (let [symbol (read-line)]
    (println (str "Searching for symbol: " symbol))
    (println (shell (str "git log --grep \"" symbol "\" -- memories/ --oneline 2>/dev/null || echo 'No matches found'")))))

(defn memory-stats []
  (ensure-memories-dir)
  (println "═══════════════════════════════════════════════════════════")
  (println "  MEMENTUM Statistics")
  (println "═══════════════════════════════════════════════════════════")
  
  ;; Count memory files
  (let [memories-count (try
                         (Integer/parseInt (shell "bash -c 'find memories -name \"*.md\" -type f 2>/dev/null | wc -l | tr -d \" \"'"))
                         (catch Exception _ 0))]
    (println (str "Total memories: " memories-count))
    
    (println "\nBy symbol:")
    (try
      (println (shell "bash -c 'find memories -name \"*.md\" -type f 2>/dev/null | xargs -I {} basename {} | cut -d- -f1 | sort | uniq -c | sort -rn'"))
      (catch Exception _
        (println "  No memories found")))
    
    (println "\nRecent memory commits:")
    (try
      (println (shell "git log -n 5 -- memories/ --oneline"))
      (catch Exception _
        (println "  No memory commits found")))
    
    (println "═══════════════════════════════════════════════════════════")))

(defn validate-memories []
  (ensure-memories-dir)
  (println "Validating MEMENTUM memories...")
  
  (let [files (fs/glob "memories" "*.md")
        valid-symbols #{"insight" "pattern-shift" "decision" "meta"}
        issues (atom [])]
    
    (doseq [file files]
      (let [filename (fs/file-name file)
            parts (str/split filename #"-")]
        (when (< (count parts) 3)
          (swap! issues conj (str "Invalid filename format: " filename)))
        
        (let [symbol (first parts)]
          (when-not (valid-symbols symbol)
            (swap! issues conj (str "Invalid symbol in: " filename " (got: " symbol ")"))))))
    
    (if (empty? @issues)
      (println "✓ All memory files are valid!")
      (do
        (println "⚠️  Validation issues found:")
        (doseq [issue @issues]
          (println "  " issue))))))

(defn -main [& args]
  (let [command (first args)]
    (case command
      "store" (store-info)
      "recall" (recall-memories)
      "search" (search-memories)
      "symbol" (symbol-search)
      "stats" (memory-stats)
      "validate" (validate-memories)
      "help" (print-help)
      (print-help))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))