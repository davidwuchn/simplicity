/**
 * Conway's Game of Life - Background Animation
 * 
 * A beautiful, musical cellular automaton that syncs with the backend.
 * Used as the landing page background.
 * 
 * Features:
 * - Backend-synced game state via API
 * - Musical feedback based on cell density
 * - Interactive: click to add cells, keyboard to change scales
 */

// =============================================================================
// CONFIGURATION
// =============================================================================

const CONFIG = {
    cellSize: 8,
    tickMs: 150,
    apiBase: '/api',
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
    
    // Game
    board: new Set(),
    generation: 0,
    score: 0,
    lastUpdate: 0,
    
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
    currentScale: SCALES.major,
    
    // Misc
    csrfToken: ''
};

// =============================================================================
// INITIALIZATION
// =============================================================================

function init() {
    state.canvas = document.getElementById('bgCanvas');
    if (!state.canvas) {
        console.error('Canvas element not found');
        return;
    }
    
    state.ctx = state.canvas.getContext('2d');
    state.csrfToken = document.getElementById('csrf-token')?.value || '';
    
    resize();
    setupEventListeners();
    startGameLoop();
}

function resize() {
    state.width = state.canvas.width = window.innerWidth;
    state.height = state.canvas.height = window.innerHeight;
    state.cols = Math.floor(state.width / CONFIG.cellSize);
    state.rows = Math.floor(state.height / CONFIG.cellSize);
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

async function gameLoop() {
    const now = Date.now();
    
    if (now - state.lastUpdate > CONFIG.tickMs) {
        try {
            await evolveGame();
            draw();
        } catch (e) {
            console.error('Game loop error:', e);
        }
        state.lastUpdate = now;
    }
    
    requestAnimationFrame(gameLoop);
}

// =============================================================================
// BACKEND API
// =============================================================================

async function apiCall(action, data = {}) {
    try {
        const response = await fetch(`${CONFIG.apiBase}/game`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'x-csrf-token': state.csrfToken
            },
            body: new URLSearchParams({ action, ...data })
        });
        return await response.json();
    } catch (e) {
        console.error('API call failed:', e);
        return { error: e.message };
    }
}

async function syncGame(cellsToAdd = [], cellsToRemove = []) {
    const action = (cellsToAdd.length > 0 || cellsToRemove.length > 0) ? 'manipulate' : 'create';
    const data = action === 'manipulate' 
        ? { cells: JSON.stringify(cellsToAdd), remove: JSON.stringify(cellsToRemove) }
        : { cells: JSON.stringify([]) };
    
    const result = await apiCall(action, data);
    if (result.board) {
        updateBoardFromResult(result);
    }
}

async function evolveGame() {
    const result = await apiCall('evolve');
    if (result.board) {
        updateBoardFromResult(result);
        if (result.triggers) {
            processMusicalTriggers(result.triggers);
        }
    }
}

function updateBoardFromResult(result) {
    state.board = new Set(result.board.map(([x, y]) => `${x},${y}`));
    state.generation = result.generation;
    state.score = result.score;
}

// =============================================================================
// RENDERING
// =============================================================================

function draw() {
    const { ctx, width, height, board } = state;
    
    // Background - subtle brightness based on population
    const bgBrightness = Math.floor(5 + (board.size / 1000) * 10);
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
    const { ctx, height, board, generation, score, currentScale } = state;
    
    ctx.fillStyle = '#666';
    ctx.font = '12px monospace';
    ctx.fillText(
        `GEN: ${generation} | CELLS: ${board.size} | SCORE: ${score}`,
        10, height - 10
    );
    
    const scaleName = Object.entries(SCALES)
        .find(([_, v]) => v === currentScale)?.[0] || 'major';
    ctx.fillText(`SCALE: ${scaleName.toUpperCase()}`, 10, height - 28);
}

// =============================================================================
// INPUT HANDLERS
// =============================================================================

async function handleClick(e) {
    e.preventDefault();
    initAudio();
    
    const col = Math.floor(e.clientX / CONFIG.cellSize);
    const row = Math.floor(e.clientY / CONFIG.cellSize);
    
    // Add 3x3 cluster
    const cellsToAdd = [];
    for (let i = -1; i <= 1; i++) {
        for (let j = -1; j <= 1; j++) {
            const nx = (col + i + state.cols) % state.cols;
            const ny = (row + j + state.rows) % state.rows;
            cellsToAdd.push([nx, ny]);
        }
    }
    
    await syncGame(cellsToAdd, []);
    playChord(getFrequency(col, row), 'major');
    draw();
}

async function handleKeydown(e) {
    initAudio();
    
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
        e.preventDefault();
        const cellsToAdd = [];
        for (let i = 0; i < 20; i++) {
            cellsToAdd.push([
                Math.floor(Math.random() * state.cols),
                Math.floor(Math.random() * state.rows)
            ]);
        }
        await syncGame(cellsToAdd, []);
        draw();
        return;
    }
    
    // R - reset board
    if (e.key === 'r' || e.key === 'R') {
        const cellsToRemove = Array.from(state.board).map(s => s.split(',').map(Number));
        await syncGame([], cellsToRemove);
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

function processMusicalTriggers(triggers) {
    if (!triggers || triggers.length === 0) return;
    
    for (const trigger of triggers) {
        switch (trigger.trigger) {
            case 'density-high':
            case 'density-mid':
                const pitch = trigger.params?.frequency || 220;
                playNote(pitch, 0.08, 0.2);
                break;
            case 'life-pulse':
                const intensity = trigger.params?.intensity || 0.5;
                const rate = trigger.params?.rate || 0.1;
                if (Math.random() < rate * 10) {
                    playNote(200 + (intensity * 400), rate * 0.1, 0.3);
                }
                break;
        }
    }
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
