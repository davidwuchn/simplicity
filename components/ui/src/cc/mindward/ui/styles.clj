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

(defn cyber-components
  "Cyber-themed UI components (cards, inputs, buttons)."
  []
  (str "
  /* === Cyber Components === */
  .font-cyber { font-family: 'Orbitron', sans-serif; }
  .cyber-card { 
    background-color: " (c :black) "; 
    border: 2px solid " (c :cyber-yellow) "; 
    box-shadow: 6px 6px 0px 0px " (c :cyber-cyan) "; 
    padding: 2rem;
    transition: all 0.3s ease;
  }
  .cyber-card:hover {
    box-shadow: 8px 8px 0px 0px " (c :cyber-cyan) ", 0 0 20px rgba(0, 240, 255, 0.3);
    transform: translate(-2px, -2px);
  }
  
  .cyber-input { 
    background-color: " (c :gray-dark) "; 
    border: 1px solid " (c :cyber-cyan) "; 
    color: " (c :cyber-cyan) "; 
    border-radius: 0;
    transition: all 0.2s ease;
  }
  .cyber-input:focus { 
    border-color: " (c :cyber-yellow) "; 
    outline: none; 
    box-shadow: 0 0 10px " (c :cyber-yellow) ", inset 0 0 5px rgba(252, 238, 10, 0.1);
    transform: scale(1.01);
  }
  .cyber-input:invalid:not(:placeholder-shown) {
    border-color: " (c :cyber-red) ";
    box-shadow: 0 0 10px rgba(255, 0, 60, 0.5);
  }
  "))

(defn cyber-buttons
  "Primary and secondary cyber-themed buttons."
  []
  (str "
  /* === Cyber Buttons === */
  .cyber-btn { 
    background-color: " (c :cyber-yellow) "; 
    color: " (c :black) "; 
    font-weight: 900; 
    text-transform: uppercase; 
    border: none; 
    clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); 
    padding: 12px 24px; 
    transition: all 0.2s ease;
    cursor: pointer;
    position: relative;
    overflow: hidden;
  }
  .cyber-btn::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: rgba(255, 255, 255, 0.3);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: width 0.3s, height 0.3s;
  }
  .cyber-btn:hover::before {
    width: 300px;
    height: 300px;
  }
  .cyber-btn:hover { 
    background-color: " (c :cyber-cyan) "; 
    box-shadow: 4px 4px 0px " (c :cyber-red) "; 
    transform: translate(-2px, -2px); 
  }
  .cyber-btn:active {
    transform: translate(0, 0);
    box-shadow: 2px 2px 0px " (c :cyber-red) ";
  }
  .cyber-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
  
  .cyber-btn-secondary { 
    background-color: " (c :gray-medium) "; 
    color: " (c :cyber-cyan) "; 
    border: 2px solid " (c :cyber-cyan) "; 
    font-weight: 700; 
    text-transform: uppercase; 
    clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); 
    padding: 10px 22px; 
    transition: all 0.2s ease;
    cursor: pointer;
  }
  .cyber-btn-secondary:hover { 
    background-color: " (c :cyber-cyan) "; 
    color: " (c :black) "; 
    box-shadow: 4px 4px 0px " (c :cyber-yellow) ";
    transform: translate(-2px, -2px);
  }
  "))

(defn loading-state
  "Loading spinner and disabled state styles."
  []
  (str "
  /* === Loading State === */
  .loading {
    position: relative;
    pointer-events: none;
  }
  .loading::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 20px;
    height: 20px;
    margin: -10px 0 0 -10px;
    border: 2px solid " (c :cyber-cyan) ";
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }
  "))

(defn toast-notifications
  "Toast notification container and variants."
  []
  (str "
  /* === Toast Notifications === */
  #toast-container {
    position: fixed;
    top: 80px;
    right: 20px;
    z-index: 9999;
    display: flex;
    flex-direction: column;
    gap: 10px;
    pointer-events: none;
  }
  .toast {
    background: " (c :black) ";
    border: 2px solid;
    padding: 16px 20px;
    min-width: 300px;
    max-width: 400px;
    box-shadow: 6px 6px 0px 0px;
    animation: slideIn 0.3s ease-out;
    pointer-events: auto;
    font-size: 14px;
    font-weight: 700;
  }
  .toast.success { border-color: " (c :success-green) "; box-shadow: 6px 6px 0px 0px " (c :success-green) "; color: " (c :success-green) "; }
  .toast.error { border-color: " (c :cyber-red) "; box-shadow: 6px 6px 0px 0px " (c :cyber-red) "; color: " (c :cyber-red) "; }
  .toast.info { border-color: " (c :cyber-cyan) "; box-shadow: 6px 6px 0px 0px " (c :cyber-cyan) "; color: " (c :cyber-cyan) "; }
  .toast.warning { border-color: " (c :cyber-yellow) "; box-shadow: 6px 6px 0px 0px " (c :cyber-yellow) "; color: " (c :cyber-yellow) "; }
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
