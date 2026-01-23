/**
 * Conway's Game of Life - Background Animation
 * 
 * A beautiful, musical cellular automaton for the landing page.
 * Runs entirely client-side (no backend API required).
 * 
 * Features:
 * - Classic Conway's Game of Life rules
 * - Musical feedback based on cell activity
 * - Interactive: click to add cells, keyboard to change scales
 */

// =============================================================================
// CONFIGURATION
// =============================================================================

const CONFIG = {
    cellSize: 8,
    tickMs: 150,
    initialDensity: 0.15,  // Initial random fill percentage
    audio: {
        baseFreq: 130.81,
        bpm: 120,
        maxSimultaneousNotes: 12,
        masterVolume: 0.3,
        reverbTime: 2.0
    }
};

const SCALES = {
    major: [0, 2, 4, 7, 9],
    minor: [0, 3, 5, 7, 10],
    japanese: [0, 2, 5, 7, 9],
    blues: [0, 3, 5, 6, 7, 10],
    chromatic: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
    wholetone: [0, 2, 4, 6, 8, 10]
};

// =============================================================================
// STATE
// =============================================================================

const state = {
    // Canvas
    canvas: null,
    ctx: null,
    width: 0,
    height: 0,
    cols: 0,
    rows: 0,

    // Game - using Set of "x,y" strings for O(1) lookup
    board: new Set(),
    generation: 0,
    lastUpdate: 0,

    // Control state
    isPlaying: false,
    playInterval: null,

    // Audio
    audioCtx: null,
    audioInitialized: false,
    masterGain: null,
    compressor: null,
    reverbNode: null,
    reverbGain: null,
    delayNode: null,
    delayGain: null,
    activeNotes: 0,
    lastNoteTime: {},
    currentScale: SCALES.major
};

// =============================================================================
// INITIALIZATION
// =============================================================================

function init() {
    state.canvas = document.getElementById('lifeCanvas');
    if (!state.canvas) {
        console.error('Canvas element not found');
        return;
    }
    
    state.ctx = state.canvas.getContext('2d');
    
    resize();
    initializeRandomBoard();
    setupEventListeners();
    draw();
    updateHud();
    startGameLoop();
}

function resize() {
    state.width = state.canvas.width = window.innerWidth;
    state.height = state.canvas.height = window.innerHeight;
    state.cols = Math.floor(state.width / CONFIG.cellSize);
    state.rows = Math.floor(state.height / CONFIG.cellSize);
}

function initializeRandomBoard() {
    state.board.clear();
    state.generation = 0;
    
    for (let x = 0; x < state.cols; x++) {
        for (let y = 0; y < state.rows; y++) {
            if (Math.random() < CONFIG.initialDensity) {
                state.board.add(`${x},${y}`);
            }
        }
    }
}

function setupEventListeners() {
    window.addEventListener('resize', () => {
        resize();
        draw();
    });
    
    state.canvas.addEventListener('click', handleClick);
    window.addEventListener('keydown', handleKeydown);
}

// =============================================================================
// GAME LOOP
// =============================================================================

function startGameLoop() {
    requestAnimationFrame(gameLoop);
}

function gameLoop() {
    const now = Date.now();
    
    if (now - state.lastUpdate > CONFIG.tickMs) {
        try {
            evolve();
            draw();
        } catch (e) {
            console.error('Game loop error:', e);
        }
        state.lastUpdate = now;
    }
    
    requestAnimationFrame(gameLoop);
}

// =============================================================================
// CONWAY'S GAME OF LIFE LOGIC
// =============================================================================

function evolve() {
    const { board, cols, rows } = state;
    const newBoard = new Set();
    const checked = new Set();
    
    // Track births/deaths for audio feedback
    let births = 0;
    let deaths = 0;
    
    // Check all living cells and their neighbors
    for (const cellKey of board) {
        const [x, y] = cellKey.split(',').map(Number);
        
        // Check this cell and all its neighbors
        for (let dx = -1; dx <= 1; dx++) {
            for (let dy = -1; dy <= 1; dy++) {
                const nx = (x + dx + cols) % cols;
                const ny = (y + dy + rows) % rows;
                const key = `${nx},${ny}`;
                
                if (checked.has(key)) continue;
                checked.add(key);
                
                const neighbors = countNeighbors(nx, ny);
                const isAlive = board.has(key);
                
                // Conway's rules:
                // 1. Live cell with 2-3 neighbors survives
                // 2. Dead cell with exactly 3 neighbors becomes alive
                if (isAlive) {
                    if (neighbors === 2 || neighbors === 3) {
                        newBoard.add(key);
                    } else {
                        deaths++;
                    }
                } else {
                    if (neighbors === 3) {
                        newBoard.add(key);
                        births++;
                    }
                }
            }
        }
    }
    
    state.board = newBoard;
    state.generation++;
    
    // Musical feedback based on activity
    if (state.audioInitialized) {
        processActivitySound(births, deaths);
    }
    
    // Auto-respawn if population dies out
    if (newBoard.size < 50) {
        addRandomCells(100);
    }
}

function countNeighbors(x, y) {
    const { board, cols, rows } = state;
    let count = 0;
    
    for (let dx = -1; dx <= 1; dx++) {
        for (let dy = -1; dy <= 1; dy++) {
            if (dx === 0 && dy === 0) continue;
            
            const nx = (x + dx + cols) % cols;
            const ny = (y + dy + rows) % rows;
            
            if (board.has(`${nx},${ny}`)) {
                count++;
            }
        }
    }
    
    return count;
}

function addRandomCells(count) {
    for (let i = 0; i < count; i++) {
        const x = Math.floor(Math.random() * state.cols);
        const y = Math.floor(Math.random() * state.rows);
        state.board.add(`${x},${y}`);
    }
}

function addCellCluster(centerX, centerY, radius = 1) {
    for (let dx = -radius; dx <= radius; dx++) {
        for (let dy = -radius; dy <= radius; dy++) {
            const x = (centerX + dx + state.cols) % state.cols;
            const y = (centerY + dy + state.rows) % state.rows;
            state.board.add(`${x},${y}`);
        }
    }
}

// =============================================================================
// RENDERING
// =============================================================================

function draw() {
    const { ctx, width, height, board } = state;
    
    // Background - subtle brightness based on population
    const bgBrightness = Math.floor(5 + Math.min(board.size / 500, 1) * 10);
    ctx.fillStyle = `rgb(${bgBrightness}, ${bgBrightness}, ${Math.floor(bgBrightness * 1.5)})`;
    ctx.fillRect(0, 0, width, height);
    
    // Draw cells
    ctx.fillStyle = '#00f0ff';
    ctx.globalAlpha = 0.8;
    
    for (const cellKey of board) {
        const [x, y] = cellKey.split(',').map(Number);
        ctx.fillRect(
            x * CONFIG.cellSize + 1,
            y * CONFIG.cellSize + 1,
            CONFIG.cellSize - 2,
            CONFIG.cellSize - 2
        );
    }
    
    ctx.globalAlpha = 1.0;
    
    // Stats overlay
    drawStats();
}

function drawStats() {
    const { ctx, height, board, generation, currentScale } = state;
    
    ctx.fillStyle = '#666';
    ctx.font = '12px monospace';
    ctx.fillText(
        `GEN: ${generation} | CELLS: ${board.size}`,
        10, height - 10
    );
    
    const scaleName = Object.entries(SCALES)
        .find(([_, v]) => v === currentScale)?.[0] || 'major';
    ctx.fillText(`SCALE: ${scaleName.toUpperCase()} | CLICK to add cells | 1-6 change scale`, 10, height - 28);
}

// =============================================================================
// INPUT HANDLERS
// =============================================================================

function handleClick(e) {
    e.preventDefault();
    initAudio();
    
    const col = Math.floor(e.clientX / CONFIG.cellSize);
    const row = Math.floor(e.clientY / CONFIG.cellSize);
    
    // Add 3x3 cluster
    addCellCluster(col, row, 1);
    
    playChord(getFrequency(col, row), 'major');
    draw();
}

function handleKeydown(e) {
    initAudio();

    // Prevent default browser behavior for our keys
    const handledKeys = ['1', '2', '3', '4', '5', '6', ' ', 'r', 'R', 'c', 'C', 's', 'S'];
    if (handledKeys.includes(e.key)) {
        e.preventDefault();
    }

    // Number keys 1-6 change scale
    const scaleKeys = {
        '1': 'major',
        '2': 'minor',
        '3': 'japanese',
        '4': 'blues',
        '5': 'chromatic',
        '6': 'wholetone'
    };

    if (scaleKeys[e.key]) {
        state.currentScale = SCALES[scaleKeys[e.key]];
        const chordType = ['2', '4'].includes(e.key) ? 'minor' : 'major';
        playChord(200, chordType);
        return;
    }

    // Space - add random cells
    if (e.code === 'Space') {
        addRandomCells(50);
        draw();
        return;
    }

    // C - clear board
    if (e.key === 'c' || e.key === 'C') {
        lifeClear();
        return;
    }

    // S - step one generation
    if (e.key === 's' || e.key === 'S') {
        lifeStep();
        return;
    }

    // R - reset board
    if (e.key === 'r' || e.key === 'R') {
        initializeRandomBoard();
        draw();
    }
}

// =============================================================================
// AUDIO SYSTEM
// =============================================================================

function initAudio() {
    if (state.audioCtx) {
        if (state.audioCtx.state === 'suspended') {
            state.audioCtx.resume();
        }
        return;
    }
    
    try {
        state.audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        
        // Compressor
        state.compressor = state.audioCtx.createDynamicsCompressor();
        state.compressor.threshold.value = -20;
        state.compressor.ratio.value = 4;
        state.compressor.attack.value = 0.003;
        state.compressor.release.value = 0.1;
        
        // Master gain
        state.masterGain = state.audioCtx.createGain();
        state.masterGain.gain.value = CONFIG.audio.masterVolume;
        
        // Delay
        state.delayNode = state.audioCtx.createDelay(1.0);
        state.delayNode.delayTime.value = 60 / CONFIG.audio.bpm * 0.75;
        state.delayGain = state.audioCtx.createGain();
        state.delayGain.gain.value = 0.2;
        
        // Reverb
        state.reverbNode = state.audioCtx.createConvolver();
        state.reverbGain = state.audioCtx.createGain();
        state.reverbGain.gain.value = 0.4;
        
        // Create impulse response for reverb
        const reverbLength = state.audioCtx.sampleRate * CONFIG.audio.reverbTime;
        const impulse = state.audioCtx.createBuffer(2, reverbLength, state.audioCtx.sampleRate);
        for (let ch = 0; ch < 2; ch++) {
            const data = impulse.getChannelData(ch);
            for (let i = 0; i < reverbLength; i++) {
                data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / reverbLength, 1.8);
            }
        }
        state.reverbNode.buffer = impulse;
        
        // Connect audio graph
        state.compressor.connect(state.masterGain);
        state.masterGain.connect(state.audioCtx.destination);
        state.compressor.connect(state.delayNode);
        state.delayNode.connect(state.delayGain);
        state.delayGain.connect(state.masterGain);
        state.delayGain.connect(state.delayNode);
        state.masterGain.connect(state.reverbNode);
        state.reverbNode.connect(state.reverbGain);
        state.reverbGain.connect(state.audioCtx.destination);
        
        state.audioInitialized = true;
    } catch (e) {
        console.error('Audio initialization failed:', e);
    }
}

function getFrequency(x, y) {
    const scale = state.currentScale;
    const scaleIndex = x % scale.length;
    const octave = Math.floor(y / (state.rows / 4));
    const semitones = scale[scaleIndex] + (octave * 12);
    return CONFIG.audio.baseFreq * Math.pow(2, semitones / 12);
}

function playNote(freq, volume = 0.1, duration = 0.3) {
    if (!state.audioCtx || state.audioCtx.state !== 'running') return;
    if (state.activeNotes >= CONFIG.audio.maxSimultaneousNotes) return;
    
    const key = Math.round(freq);
    const now = Date.now();
    if (state.lastNoteTime[key] && now - state.lastNoteTime[key] < 30) return;
    state.lastNoteTime[key] = now;
    
    state.activeNotes++;
    
    try {
        const t = state.audioCtx.currentTime;
        const osc = state.audioCtx.createOscillator();
        const gain = state.audioCtx.createGain();
        const filter = state.audioCtx.createBiquadFilter();
        
        osc.type = 'sine';
        osc.frequency.value = freq;
        
        filter.type = 'lowpass';
        filter.frequency.value = freq * 4;
        filter.Q.value = 1;
        
        // ADSR envelope
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(volume, t + 0.008);
        gain.gain.exponentialRampToValueAtTime(volume * 0.5, t + 0.08);
        gain.gain.exponentialRampToValueAtTime(0.001, t + duration);
        
        osc.connect(filter);
        filter.connect(gain);
        gain.connect(state.compressor);
        
        osc.start(t);
        osc.stop(t + duration);
        
        setTimeout(() => {
            state.activeNotes = Math.max(0, state.activeNotes - 1);
        }, duration * 1000);
    } catch (e) {
        // Silently fail - audio is non-critical
    }
}

function playChord(baseFreq, type = 'major') {
    if (!state.audioCtx || state.audioCtx.state !== 'running') return;
    
    const intervals = type === 'major' ? [0, 4, 7, 12] : [0, 3, 7, 12];
    intervals.forEach((semitone, i) => {
        setTimeout(() => {
            playNote(baseFreq * Math.pow(2, semitone / 12), 0.07, 0.4);
        }, i * 50);
    });
}

function processActivitySound(births, deaths) {
    // Play notes based on activity level
    const activity = births + deaths;
    
    if (activity > 100 && Math.random() < 0.3) {
        // High activity - play chord
        const freq = CONFIG.audio.baseFreq * (1 + Math.random());
        playChord(freq, births > deaths ? 'major' : 'minor');
    } else if (activity > 20 && Math.random() < 0.2) {
        // Medium activity - play note
        const freq = CONFIG.audio.baseFreq * (1 + Math.random() * 2);
        playNote(freq, 0.05, 0.2);
    }
}

// =============================================================================
// PUBLIC CONTROL API
// =============================================================================

/**
 * Toggle play/pause state.
 * Updates button text and starts/stops the game loop.
 */
function toggleLifePlay() {
    state.isPlaying = !state.isPlaying;

    const btn = document.getElementById('life-play-btn');
    if (btn) {
        btn.textContent = state.isPlaying ? 'PAUSE' : 'PLAY';
    }

    if (state.isPlaying) {
        // Start continuous evolution
        state.playInterval = setInterval(() => {
            evolve();
            draw();
            updateHud();
        }, CONFIG.tickMs);
    } else {
        // Stop evolution
        if (state.playInterval) {
            clearInterval(state.playInterval);
            state.playInterval = null;
        }
    }
}

/**
 * Advance the game by exactly one generation.
 * Used by the STEP button.
 */
function lifeStep() {
    evolve();
    draw();
    updateHud();

    // Play a small sound to indicate step
    initAudio();
    if (state.audioInitialized) {
        playNote(CONFIG.audio.baseFreq * 2, 0.05, 0.1);
    }
}

/**
 * Clear all cells from the board.
 * Used by the CLEAR button.
 */
function lifeClear() {
    state.board.clear();
    state.generation = 0;
    draw();
    updateHud();
}

/**
 * Randomize the board with initial density.
 * Used by the RAND button.
 */
function lifeRandom() {
    initializeRandomBoard();
    draw();
    updateHud();
}

/**
 * Update the HUD elements with current game state.
 */
function updateHud() {
    const genEl = document.getElementById('generation');
    const popEl = document.getElementById('population');

    if (genEl) genEl.textContent = state.generation;
    if (popEl) popEl.textContent = state.board.size;
}

// =============================================================================
// START
// =============================================================================

// Initialize when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}

// =============================================================================
// EXPORT TO GLOBAL SCOPE (for onclick handlers)
// =============================================================================

window.toggleLifePlay = toggleLifePlay;
window.lifeStep = lifeStep;
window.lifeClear = lifeClear;
window.lifeRandom = lifeRandom;
window.updateHud = updateHud;
