(ns cc.mindward.game.interface
  "Game engine interface - Conway's Game of Life with musical integration.

   φ (Vitality): Game state evolves organically following cellular automata rules.
   ∃ (Truth): Musical triggers are deterministic based on pattern emergence."
  (:require [cc.mindward.game.impl :as impl]
            [cc.mindward.game.config :as config]
            [clojure.spec.alpha :as s]))

;; ------------------------------------------------------------
;; Domain Specs (π Synthesis)
;; ------------------------------------------------------------

(s/def :game/id keyword?)
(s/def :game/coordinate (s/tuple int? int?))
(s/def :game/board (s/coll-of :game/coordinate :kind set?))
(s/def :game/generation (s/and int? #(>= % 0)))
(s/def :game/score (s/and int? #(>= % 0)))

(s/def :game/musical-trigger
  (s/keys :req-un [:game/trigger :game/params]))

;; ------------------------------------------------------------
;; Domain Model (π Synthesis)
;; ------------------------------------------------------------

;; Board represented as set of living cell coordinates [x y]
;; Musical triggers use pattern classification thresholds

;; ------------------------------------------------------------
;; Config Accessors (μ Directness - single point of config access)
;; ------------------------------------------------------------

(defn board-max-x [] config/board-max-x)
(defn board-min-x [] config/board-min-x)
(defn board-max-y [] config/board-max-y)
(defn board-min-y [] config/board-min-y)

;; ------------------------------------------------------------
;; Query Operations (λ - pure lookups)
;; ------------------------------------------------------------

(defn get-board
  "Get the current game board state. Returns set of [x y] coordinates."
  [game-id]
  (impl/get-board game-id))

(defn get-generation
  "Get the current generation number for a game."
  [game-id]
  (impl/get-generation game-id))

(defn get-score
  "Calculate score based on board complexity and generation.
   Higher complexity + sustained life = higher score."
  [game-id]
  (impl/calculate-score game-id))

(defn list-saved-games
  "List all saved game states. Returns vector of {:id :name :generation :score}."
  []
  (impl/list-saved-games))

;; ------------------------------------------------------------
;; Command Operations (! suffix - side effects)
;; ------------------------------------------------------------

(defn create-game!
  "Create a new game with initial board state.
   initial-board: set of [x y] coordinates or nil for random generation."
  ([game-id] (create-game! game-id nil))
  ([game-id initial-board]
   (impl/create-game! game-id initial-board)))

(defn evolve!
  "Evolve the game board one generation using Conway's rules.
   Returns the new board state."
  [game-id]
  (impl/evolve! game-id))

(defn clear-cells!
  "Clear specific cells from the board.
   cells: set of [x y] coordinates to remove."
  [game-id cells]
  (impl/clear-cells! game-id cells))

(defn add-cells!
  "Add living cells to the board.
   cells: set of [x y] coordinates to add."
  [game-id cells]
  (impl/add-cells! game-id cells))

(defn save-game!
  "Save current game state with a name.
   Returns the saved game record."
  [game-id name]
  (impl/save-game! game-id name))

(defn load-game!
  "Load a saved game state by id. Returns new game-id."
  [saved-game-id new-game-id]
  (impl/load-game! saved-game-id new-game-id))

(defn delete-game!
  "Delete a saved game state."
  [saved-game-id]
  (impl/delete-game! saved-game-id))

;; ------------------------------------------------------------
;; Musical Integration (∃ Truth - deterministic pattern mapping)
;; ------------------------------------------------------------

(defn get-musical-triggers
  "Analyze board and return musical trigger events.
   Maps emergent patterns to audio parameters:
   - Oscillator density → harmonic complexity
   - Cell births/deaths → envelope triggers
   - Stable patterns → sustained drones
   - Chaos → noise bursts
   
   Returns vector of {:trigger :params} maps."
  [game-id]
  (impl/generate-musical-triggers game-id))

(defn initialize!
  "Initialize the game engine. Must be called once at startup.
   Starts background cleanup of stale game sessions."
  []
  (impl/initialize!))

(defn cleanup-stale-games!
  "Manually trigger cleanup of stale games.
   Returns the number of games removed."
  []
  (impl/cleanup-stale-games!))

(defn stop-cleanup-scheduler!
  "Stop the background cleanup scheduler. Call on shutdown."
  []
  (impl/stop-cleanup-scheduler!))

(defn health-check
  "Check game engine health status.
   Returns {:healthy? true/false :details {...}}"
  []
  (impl/health-check))
