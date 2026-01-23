(ns cc.mindward.ui.pages.game-life
  "Conway's Game of Life page - cellular automata with musical integration."
  (:require [hiccup2.core :as h]
            [cc.mindward.ui.layout :as layout]))

(defn game-life-page
  "Render Conway's Game of Life page.

   Options:
   - :session - Ring session map"
  [{:keys [session]}]
  (let [cdn-links (layout/cdn-links-map)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (str
            "<!DOCTYPE html>\n"
            (h/html
             [:html {:lang "en"}
              [:head
               [:meta {:charset "UTF-8"}]
               [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
               [:title "Conway's Life - Simplicity"]
               [:script {:src (:tailwind cdn-links)}]
               [:link {:href (:font cdn-links) :rel "stylesheet"}]
               [:style "
              body, html { margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden; background-color: #050505; }
              #lifeCanvas { display: block; width: 100%; height: 100%; }

              /* Controls */
              .life-controls {
                position: fixed;
                bottom: 20px;
                left: 50%;
                transform: translateX(-50%);
                display: flex;
                gap: 10px;
                z-index: 100;
              }

              .life-hud {
                position: fixed;
                top: 20px;
                left: 20px;
                font-family: 'Orbitron', monospace;
                background: rgba(0, 0, 0, 0.7);
                backdrop-filter: blur(4px);
                border: 1px solid #00f0ff;
                padding: 10px 15px;
                color: #00f0ff;
                z-index: 100;
              }
            "]]]
             [:body
              [:input {:type "hidden" :id "csrf-token" :value (:csrf-token session "")}]

               ;; Canvas
              [:canvas {:id "lifeCanvas" :aria-label "Conway's Game of Life canvas"}]

               ;; HUD
              [:div {:class "life-hud"}
               [:div "GENERATION: " [:span {:id "generation"} "0"]]
               [:div "POPULATION: " [:span {:id "population"} "0"]]]

               ;; Controls
              [:div {:class "life-controls"}
               [:button {:class "cyber-btn"
                         :onclick "togglePlay()"
                         :id "play-btn"}
                "PLAY"]
               [:button {:class "cyber-btn-secondary"
                         :onclick "step()"}
                "STEP"]
               [:button {:class "cyber-btn-secondary"
                         :onclick "clear()"}
                "CLEAR"]
               [:button {:class "cyber-btn-secondary"
                         :onclick "randomize()"}
                "RANDOM"]
               [:a {:href "/select-game"
                    :class "cyber-btn-secondary"}
                "BACK"]]

               ;; Controls and Game initialization script
              [:script (h/raw "
              const canvas = document.getElementById('lifeCanvas');
              const ctx = canvas.getContext('2d');
              const cellSize = 8;
              let cols, rows;
              let grid = [];
              let playing = false;
              let animationId = null;
              let generation = 0;

              function resize() {
                canvas.width = window.innerWidth;
                canvas.height = window.innerHeight;
                cols = Math.floor(canvas.width / cellSize);
                rows = Math.floor(canvas.height / cellSize);
                grid = createGrid();
                draw();
              }

              function createGrid() {
                return Array(rows).fill(null).map(() => Array(cols).fill(0));
              }

              function randomize() {
                grid = createGrid();
                for (let i = 0; i < rows; i++) {
                  for (let j = 0; j < cols; j++) {
                    grid[i][j] = Math.random() > 0.85 ? 1 : 0;
                  }
                }
                generation = 0;
                updateHUD();
                draw();
              }

              function clear() {
                grid = createGrid();
                generation = 0;
                playing = false;
                updateHUD();
                draw();
                document.getElementById('play-btn').textContent = 'PLAY';
              }

              function countNeighbors(x, y) {
                let sum = 0;
                for (let i = -1; i < 2; i++) {
                  for (let j = -1; j < 2; j++) {
                    const col = (x + i + cols) % cols;
                    const row = (y + j + rows) % rows;
                    sum += grid[row][col];
                  }
                }
                sum -= grid[y][x];
                return sum;
              }

              function computeNextGen() {
                const next = createGrid();
                let population = 0;
                for (let i = 0; i < rows; i++) {
                  for (let j = 0; j < cols; j++) {
                    const state = grid[i][j];
                    const neighbors = countNeighbors(j, i);

                    if (state === 0 && neighbors === 3) {
                      next[i][j] = 1;
                      population++;
                    } else if (state === 1 && (neighbors < 2 || neighbors > 3)) {
                      next[i][j] = 0;
                    } else {
                      next[i][j] = state;
                      if (state === 1) population++;
                    }
                  }
                }
                grid = next;
                generation++;
                updateHUD();
              }

              function draw() {
                ctx.fillStyle = '#050505';
                ctx.fillRect(0, 0, canvas.width, canvas.height);

                for (let i = 0; i < rows; i++) {
                  for (let j = 0; j < cols; j++) {
                    if (grid[i][j] === 1) {
                      ctx.fillStyle = '#00f0ff';
                      ctx.fillRect(j * cellSize, i * cellSize, cellSize - 1, cellSize - 1);
                    }
                  }
                }
              }

              function updateHUD() {
                document.getElementById('generation').textContent = generation;
                let population = 0;
                for (let i = 0; i < rows; i++) {
                  for (let j = 0; j < cols; j++) {
                    population += grid[i][j];
                  }
                }
                document.getElementById('population').textContent = population;
              }

              function step() {
                computeNextGen();
                draw();
              }

              function togglePlay() {
                playing = !playing;
                const btn = document.getElementById('play-btn');
                btn.textContent = playing ? 'PAUSE' : 'PLAY';
                if (playing) {
                  loop();
                } else {
                  cancelAnimationFrame(animationId);
                }
              }

              function loop() {
                if (!playing) return;
                computeNextGen();
                draw();
                animationId = requestAnimationFrame(loop);
              }

              // Initialize
              window.addEventListener('resize', resize);
              resize();
              randomize();
             ")]]))}))