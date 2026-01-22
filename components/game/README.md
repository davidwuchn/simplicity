# Game Component

The Game component implements Conway's Game of Life with musical integration. It handles cellular automata evolution, pattern recognition, and musical trigger generation.

## Architecture

**Interface**: `cc.mindward.game.interface`  
**Implementation**: `cc.mindward.game.impl`

This component follows Polylith encapsulation - all business logic is accessed through the `interface` namespace.

## Features

- Conway's Game of Life cellular automata
- Efficient board representation using sets
- Pattern recognition (still lifes, oscillators, spaceships)
- Musical trigger generation based on emergent patterns
- Game state persistence and scoring
- Generation tracking

## API Reference

### Initialization

#### `initialize!`
Initialize the game engine. Must be called once at startup.

```clojure
(require '[cc.mindward.game.interface :as game])

(game/initialize!)
```

### Query Operations (Pure Functions)

#### `get-board`
Get the current game board state.

**Parameters**: `game-id` (string/keyword)  
**Returns**: Set of `[x y]` coordinates representing living cells

```clojure
(game/get-board :my-game)
;; => #{[0 1] [1 1] [2 1] [1 0] [1 2]}
```

#### `get-generation`
Get the current generation number for a game.

**Parameters**: `game-id` (string/keyword)  
**Returns**: Integer generation count

```clojure
(game/get-generation :my-game)
;; => 42
```

#### `get-score`
Calculate score based on board complexity and generation.

**Parameters**: `game-id` (string/keyword)  
**Returns**: Integer score (higher complexity + sustained life = higher score)

```clojure
(game/get-score :my-game)
;; => 1337
```

#### `list-saved-games`
List all saved game states.

**Returns**: Vector of maps with `:id`, `:name`, `:generation`, `:score`

```clojure
(game/list-saved-games)
;; => [{:id "game-1"
;;      :name "Glider Gun"
;;      :generation 100
;;      :score 5000}
;;     {:id "game-2"
;;      :name "Pulsar"
;;      :generation 50
;;      :score 3200}]
```

### Command Operations (Side Effects)

#### `create-game!`
Create a new game with initial board state.

**Parameters**:
- `game-id` - Unique identifier for the game
- `initial-board` (optional) - Set of `[x y]` coordinates, or nil for random

```clojure
;; Random initial state
(game/create-game! :my-game)

;; Glider pattern
(game/create-game! :glider-game #{[1 0] [2 1] [0 2] [1 2] [2 2]})

;; Blinker (oscillator)
(game/create-game! :blinker #{[1 0] [1 1] [1 2]})
```

#### `evolve!`
Evolve the game board one generation using Conway's rules.

**Parameters**: `game-id`  
**Returns**: New board state (set of coordinates)

```clojure
(game/evolve! :my-game)
;; => #{[0 1] [1 1] [2 1] [1 0] [1 2]} (new generation)
```

#### `add-cells!`
Add living cells to the board.

**Parameters**:
- `game-id` - Game identifier
- `cells` - Set of `[x y]` coordinates to add

```clojure
(game/add-cells! :my-game #{[5 5] [5 6] [6 5]})
```

#### `clear-cells!`
Clear specific cells from the board.

**Parameters**:
- `game-id` - Game identifier
- `cells` - Set of `[x y]` coordinates to remove

```clojure
(game/clear-cells! :my-game #{[0 0] [0 1]})
```

#### `save-game!`
Save current game state with a name.

**Parameters**:
- `game-id` - Game to save
- `name` - Human-readable name

**Returns**: Saved game record

```clojure
(game/save-game! :my-game "My Epic Game")
;; => {:id "saved-123"
;;     :name "My Epic Game"
;;     :generation 42
;;     :score 1337
;;     :board #{...}}
```

#### `load-game!`
Load a saved game state.

**Parameters**:
- `saved-game-id` - ID of saved game
- `new-game-id` - ID for the loaded game instance

**Returns**: New game ID

```clojure
(game/load-game! "saved-123" :loaded-game)
;; => :loaded-game
```

#### `delete-game!`
Delete a saved game state.

**Parameters**: `saved-game-id`

```clojure
(game/delete-game! "saved-123")
```

### Musical Integration

#### `get-musical-triggers`
Analyze board and return musical trigger events.

Maps emergent patterns to audio parameters:
- Oscillator density → harmonic complexity
- Cell births/deaths → envelope triggers
- Stable patterns → sustained drones
- Chaos → noise bursts

**Parameters**: `game-id`  
**Returns**: Vector of `{:trigger :params}` maps

```clojure
(game/get-musical-triggers :my-game)
;; => [{:trigger :oscillator
;;      :params {:frequency 440
;;               :density 0.7}}
;;     {:trigger :envelope
;;      :params {:attack 0.01
;;               :decay 0.5}}]
```

#### `get-pattern-analysis`
Analyze board for known patterns.

**Parameters**: `game-id`  
**Returns**: Map of `{:pattern-type :count :locations}`

```clojure
(game/get-pattern-analysis :my-game)
;; => {:still-lifes {:count 3
;;                   :locations [[5 5] [10 10] [15 15]]}
;;     :oscillators {:count 2
;;                   :locations [[0 0] [20 20]]}
;;     :spaceships {:count 1
;;                  :locations [[25 25]]}}
```

## Usage Examples

### Basic Game Loop

```clojure
(require '[cc.mindward.game.interface :as game])

;; Initialize engine
(game/initialize!)

;; Create a game with a glider
(game/create-game! :my-game #{[1 0] [2 1] [0 2] [1 2] [2 2]})

;; Evolve 10 generations
(dotimes [n 10]
  (println "Generation" n ":" (game/get-generation :my-game))
  (game/evolve! :my-game))

;; Check final score
(println "Final score:" (game/get-score :my-game))
```

### Musical Game of Life

```clojure
(require '[cc.mindward.game.interface :as game])

;; Create random initial state
(game/create-game! :musical-game)

;; Game loop with audio triggers
(loop [generation 0]
  (when (< generation 100)
    ;; Evolve
    (game/evolve! :musical-game)
    
    ;; Get musical triggers
    (let [triggers (game/get-musical-triggers :musical-game)]
      (doseq [trigger triggers]
        (println "Audio trigger:" trigger)))
    
    ;; Get pattern analysis
    (let [patterns (game/get-pattern-analysis :musical-game)]
      (println "Patterns found:" patterns))
    
    (recur (inc generation))))
```

### Save and Load Games

```clojure
;; Create and evolve a game
(game/create-game! :temp-game)
(dotimes [_ 50] (game/evolve! :temp-game))

;; Save it
(game/save-game! :temp-game "50 Generations")

;; List saved games
(doseq [saved (game/list-saved-games)]
  (println "Saved:" (:name saved) "- Score:" (:score saved)))

;; Load a saved game
(game/load-game! "saved-123" :restored-game)
(println "Loaded generation:" (game/get-generation :restored-game))
```

## Conway's Game of Life Rules

The game follows standard Conway's rules:

1. **Birth**: A dead cell with exactly 3 live neighbors becomes alive
2. **Survival**: A live cell with 2 or 3 live neighbors stays alive
3. **Death**: A live cell with fewer than 2 or more than 3 neighbors dies

Neighbors are counted in 8 directions (Moore neighborhood).

## Pattern Recognition

The component recognizes several classic patterns:

### Still Lifes (stable, never change)
- **Block**: 2x2 square
- **Beehive**: 6 cells in hexagonal shape
- **Loaf**: 7 cells
- **Boat**: 5 cells

### Oscillators (repeat with period)
- **Blinker**: Period 2, 3 cells in a line
- **Toad**: Period 2, 6 cells
- **Beacon**: Period 2, 6 cells
- **Pulsar**: Period 3, 48 cells

### Spaceships (move across board)
- **Glider**: Period 4, travels diagonally
- **Lightweight spaceship (LWSS)**: Period 4, travels horizontally

## Musical Mapping

Pattern emergence maps to audio parameters:

| Pattern Type | Audio Parameter | Effect |
|-------------|-----------------|---------|
| Still Lifes | Sustained Drones | Harmonic stability |
| Oscillators | Rhythmic Pulses | Periodic triggers |
| Spaceships | Panning Sweeps | Spatial movement |
| High Density | Harmonic Complexity | Rich overtones |
| Births/Deaths | Envelope Triggers | Attack/decay events |
| Chaos | Noise Bursts | Textural variation |

## Performance Characteristics

- **Board Representation**: Sets (O(1) lookup, efficient for sparse boards)
- **Evolution**: O(n) where n = number of living cells + neighbors
- **Pattern Recognition**: O(n) for each pattern type
- **Typical Performance**: 60+ FPS for boards up to 50x50 cells

## Testing

```bash
# Run game component tests
clojure -M:poly test brick:game

# Expected: 146 assertions, all passing
```

## See Also

- [User Component](../user/README.md) - High score persistence
- [UI Component](../ui/README.md) - Game visualization
- [Web Server](../../bases/web-server/README.md) - HTTP endpoints

---

**Location**: `components/game/src/cc/mindward/game/`  
**Tests**: `components/game/test/cc/mindward/game/`  
**Lines**: ~500 (interface + implementation)
