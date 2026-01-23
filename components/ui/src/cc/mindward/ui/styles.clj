(ns cc.mindward.ui.styles
  "CSS styles for the Simplicity UI.
   
   Separates presentation concerns from component logic.
   All styles are defined as functions for better organization and testability.
   
   (fractal Clarity): Break down monolithic stylesheet into logical sections.")

;; === Color Palette ===
(def colors
  {:cyber-yellow "#fcee0a"
   :cyber-cyan   "#00f0ff"
   :cyber-red    "#ff003c"
   :success-green "#00ff00"
   :background   "#050505"
   :foreground   "#e2e8f0"
   :black        "#000"
   :gray-dark    "#1a1a1a"
   :gray-medium  "#2a2a2a"
   :gray-100     "#f5f5f5"
   :gray-200     "#e0e0e0"
   :gray-400     "#9ca3af"
   :gray-500     "#6b7280"
   :gray-700     "#333333"
   :gray-800     "#1f2937"
   :gray-900     "#111827"
   :zinc-800     "#27272a"
   :zinc-900     "#18181b"})

;; ------------------------------------------------------------
;; Helper Functions (μ Directness - DRY principle)
;; ------------------------------------------------------------

(defn c
  "Get color from palette by keyword.
   (∃ Truth): Single source of truth for colors."
  [color-key]
  (or (get colors color-key)
      (throw (ex-info "Unknown color key" {:key color-key :available (keys colors)}))))

;; ------------------------------------------------------------
;; CSS Section Functions (π Synthesis - compose from parts)
;; ------------------------------------------------------------

(defn base-styles
  "Base styles for body and universal selectors."
  []
  (str "
  /* === Base Styles === */
  * { box-sizing: border-box; }
  body { 
    background-color: " (c :background) "; 
    color: " (c :foreground) "; 
    background-image: linear-gradient(0deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent); 
    background-size: 50px 50px;
    min-height: 100vh;
  }
  @media (max-width: 768px) {
    body { background-size: 30px 30px; }
  }
  "))

;; Note: Animations, Color Utilities, and Accessibility are now handled by Tailwind Config in layout.clj

(defn stylesheet
  "Compose full stylesheet from modular sections.
   
   (fractal Clarity): Complex system broken into simple, composable parts."
  []
  (str
   (base-styles)
   (cyber-components)
   (cyber-buttons)
   (loading-state)
   (toast-notifications)))
