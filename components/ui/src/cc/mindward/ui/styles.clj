(ns cc.mindward.ui.styles
  "CSS styles for the Simplicity UI.
   
   Separates presentation concerns from component logic.
   All styles are defined as data structures for easier manipulation.")

;; === Color Palette ===
(def colors
  {:cyber-yellow "#fcee0a"
   :cyber-cyan   "#00f0ff"
   :cyber-red    "#ff003c"
   :background   "#050505"
   :foreground   "#e2e8f0"
   :black        "#000"
   :gray-100     "#f5f5f5"
   :gray-200     "#e0e0e0"
   :gray-400     "#9ca3af"
   :gray-500     "#6b7280"
   :gray-700     "#333333"
   :gray-800     "#1f2937"
   :gray-900     "#111827"
   :zinc-800     "#27272a"
   :zinc-900     "#18181b"})

;; === Breakpoints ===
(def breakpoints
  {:sm "640px"
   :md "768px"
   :lg "1024px"
   :xl "1280px"})

;; === Main Stylesheet ===
(defn stylesheet []
  "
  /* === Base Styles === */
  * { box-sizing: border-box; }
  body { 
    background-color: #050505; 
    color: #e2e8f0; 
    font-family: 'Orbitron', sans-serif; 
    background-image: linear-gradient(0deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(255, 255, 255, .05) 25%, rgba(255, 255, 255, .05) 26%, transparent 27%, transparent 74%, rgba(255, 255, 255, .05) 75%, rgba(255, 255, 255, .05) 76%, transparent 77%, transparent); 
    background-size: 50px 50px;
    min-height: 100vh;
  }
  
  /* === Cyber Components === */
  .font-cyber { font-family: 'Orbitron', sans-serif; }
  .cyber-card { 
    background-color: #000; 
    border: 2px solid #fcee0a; 
    box-shadow: 6px 6px 0px 0px #00f0ff; 
    padding: 2rem;
    transition: all 0.3s ease;
  }
  .cyber-card:hover {
    box-shadow: 8px 8px 0px 0px #00f0ff, 0 0 20px rgba(0, 240, 255, 0.3);
    transform: translate(-2px, -2px);
  }
  
  .cyber-input { 
    background-color: #1a1a1a; 
    border: 1px solid #00f0ff; 
    color: #00f0ff; 
    border-radius: 0;
    transition: all 0.2s ease;
  }
  .cyber-input:focus { 
    border-color: #fcee0a; 
    outline: none; 
    box-shadow: 0 0 10px #fcee0a, inset 0 0 5px rgba(252, 238, 10, 0.1);
    transform: scale(1.01);
  }
  .cyber-input:invalid:not(:placeholder-shown) {
    border-color: #ff003c;
    box-shadow: 0 0 10px rgba(255, 0, 60, 0.5);
  }
  
  .cyber-btn { 
    background-color: #fcee0a; 
    color: #000; 
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
    background-color: #00f0ff; 
    box-shadow: 4px 4px 0px #ff003c; 
    transform: translate(-2px, -2px); 
  }
  .cyber-btn:active {
    transform: translate(0, 0);
    box-shadow: 2px 2px 0px #ff003c;
  }
  .cyber-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    transform: none;
  }
  
  .cyber-btn-secondary { 
    background-color: #2a2a2a; 
    color: #00f0ff; 
    border: 2px solid #00f0ff; 
    font-weight: 700; 
    text-transform: uppercase; 
    clip-path: polygon(10% 0, 100% 0, 100% 70%, 90% 100%, 0 100%, 0 30%); 
    padding: 10px 22px; 
    transition: all 0.2s ease;
    cursor: pointer;
  }
  .cyber-btn-secondary:hover { 
    background-color: #00f0ff; 
    color: #000; 
    box-shadow: 4px 4px 0px #fcee0a;
    transform: translate(-2px, -2px);
  }
  
  /* === Animations === */
  .glitch-text { 
    text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; 
    animation: glitch 1s infinite alternate-reverse; 
  }
  @keyframes glitch { 
    0% { text-shadow: 2px 0 #ff003c, -2px 0 #00f0ff; } 
    25% { text-shadow: -2px 0 #ff003c, 2px 0 #00f0ff; } 
    50% { text-shadow: 2px 0 #00f0ff, -2px 0 #fcee0a; } 
    100% { text-shadow: -2px 0 #00f0ff, 2px 0 #ff003c; } 
  }
  
  @keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
  }
  .fade-in {
    animation: fadeIn 0.5s ease-out;
  }
  
  @keyframes slideIn {
    from { transform: translateX(-100%); opacity: 0; }
    to { transform: translateX(0); opacity: 1; }
  }
  .slide-in {
    animation: slideIn 0.3s ease-out;
  }
  
  /* === Colors === */
  .text-cyber-yellow { color: #fcee0a; }
  .text-cyber-cyan { color: #00f0ff; }
  .text-cyber-red { color: #ff003c; }
  .border-cyber-yellow { border-color: #fcee0a; }
  .border-cyber-cyan { border-color: #00f0ff; }
  .border-cyber-red { border-color: #ff003c; }
  
  /* === Loading State === */
  .loading {
    position: relative;
    pointer-events: none;
    opacity: 0.6;
  }
  .loading::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 20px;
    height: 20px;
    margin: -10px 0 0 -10px;
    border: 2px solid #00f0ff;
    border-top-color: transparent;
    border-radius: 50%;
    animation: spin 0.6s linear infinite;
  }
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
  
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
    background: #000;
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
  .toast.success { border-color: #00ff00; box-shadow: 6px 6px 0px 0px #00ff00; color: #00ff00; }
  .toast.error { border-color: #ff003c; box-shadow: 6px 6px 0px 0px #ff003c; color: #ff003c; }
  .toast.info { border-color: #00f0ff; box-shadow: 6px 6px 0px 0px #00f0ff; color: #00f0ff; }
  .toast.warning { border-color: #fcee0a; box-shadow: 6px 6px 0px 0px #fcee0a; color: #fcee0a; }
  
  /* === Responsive Design === */
  @media (max-width: 768px) {
    body { background-size: 30px 30px; }
    .cyber-card { 
      padding: 1.5rem; 
      box-shadow: 4px 4px 0px 0px #00f0ff; 
    }
    .glitch-text { font-size: 2.5rem !important; }
    #toast-container { right: 10px; left: 10px; top: 60px; }
    .toast { min-width: auto; max-width: 100%; }
    nav .hidden-mobile { display: none !important; }
  }
  
  @media (max-width: 640px) {
    .cyber-btn, .cyber-btn-secondary {
      padding: 10px 18px;
      font-size: 0.875rem;
    }
  }
  
  /* === Accessibility === */
  .sr-only {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }
  
  :focus-visible {
    outline: 2px solid #fcee0a;
    outline-offset: 4px;
  }
  ")
