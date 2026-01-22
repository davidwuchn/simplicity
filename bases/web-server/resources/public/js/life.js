// === Real Conway's Game of Life with Backend Sync ===
// Syncs with backend game engine, renders locally

const canvas = document.getElementById('bgCanvas');
const ctx = canvas.getContext('2d');

// === Configuration ===
const CELL_SIZE = 8;
const TICK_MS = 150; // Backend evolution rate

// Backend API endpoints
const API_BASE = '/api';
const csrfToken = document.getElementById('csrf-token') ? document.getElementById('csrf-token').value : '';

// === State ===
let width, height, cols, rows;
let board = new Set(); // Set of "x,y" strings from backend
let generation = 0;
let score = 0;
let musicalTriggers = [];
let audioCtx = null;
let masterGain = null;
let reverbNode = null;
let reverbGain = null;
let lastNoteTime = {};
let audioInitialized = false;

// === Musical System (simplified from original) ===
const SCALES = {
    major: [0, 2, 4, 7, 9],
    minor: [0, 3, 5, 7, 10],
    japanese: [0, 2, 5, 7, 9],
    blues: [0, 3, 5, 6, 7, 10],
    chromatic: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11],
    wholetone: [0, 2, 4, 6, 8, 10],
};
let currentScale = SCALES.major;
const BASE_FREQ = 130.81;

let beatStep = 0;
let lastBeatTime = 0;
const BPM = 120;
const BEAT_INTERVAL = 60000 / BPM / 4;

const MAX_SIMULTANEOUS_NOTES = 12;
let activeNotes = 0;
let noteQueue = [];

let compressor = null;
let delayNode = null;
let delayGain = null;
let bassGain = null;
let padOsc = null;
let padGain = null;

// === Backend API Calls ===
async function apiCall(action, data = {}) {
    try {
        const response = await fetch(`${API_BASE}/game`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'x-csrf-token': csrfToken
            },
            body: new URLSearchParams({
                action: action,
                ...data
            })
        });
        return await response.json();
    } catch (e) {
        console.error('API call failed:', e);
        return { error: e.message };
    }
}

// Initialize or update board from backend
async function syncGame(cellsToAdd = [], cellsToRemove = []) {
    if (cellsToAdd.length > 0 || cellsToRemove.length > 0) {
        // Manipulate board
        const result = await apiCall('manipulate', {
            cells: JSON.stringify(cellsToAdd),
            remove: JSON.stringify(cellsToRemove)
        });
        if (result.board) {
            board = new Set(result.board.map(([x, y]) => `${x},${y}`));
            generation = result.generation;
            score = result.score;
        }
    } else {
        // Just get current state
        const result = await apiCall('create', {
            cells: JSON.stringify([])
        });
        if (result.board) {
            board = new Set(result.board.map(([x, y]) => `${x},${y}`));
            generation = result.generation;
            score = result.score;
        }
    }
}

// Evolve one generation via backend
async function evolveGame() {
    const result = await apiCall('evolve');
    if (result.board) {
        board = new Set(result.board.map(([x, y]) => `${x},${y}`));
        generation = result.generation;
        score = result.score;
        musicalTriggers = result.triggers || [];
        processMusicalTriggers();
    }
}

// === Audio System (simplified from original) ===
function initAudio() {
    if (audioCtx) {
        if (audioCtx.state === 'suspended') audioCtx.resume();
        return;
    }
    try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        
        compressor = audioCtx.createDynamicsCompressor();
        compressor.threshold.value = -20;
        compressor.ratio.value = 4;
        compressor.attack.value = 0.003;
        compressor.release.value = 0.1;
        
        masterGain = audioCtx.createGain();
        masterGain.gain.value = 0.3;
        
        bassGain = audioCtx.createGain();
        bassGain.gain.value = 0.4;
        
        delayNode = audioCtx.createDelay(1.0);
        delayNode.delayTime.value = 60 / BPM * 0.75;
        delayGain = audioCtx.createGain();
        delayGain.gain.value = 0.2;
        
        reverbNode = audioCtx.createConvolver();
        reverbGain = audioCtx.createGain();
        reverbGain.gain.value = 0.4;
        
        const reverbTime = 2.0;
        const reverbLength = audioCtx.sampleRate * reverbTime;
        const impulse = audioCtx.createBuffer(2, reverbLength, audioCtx.sampleRate);
        for (let ch = 0; ch < 2; ch++) {
            const data = impulse.getChannelData(ch);
            for (let i = 0; i < reverbLength; i++) {
                data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / reverbLength, 1.8);
            }
        }
        reverbNode.buffer = impulse;
        
        compressor.connect(masterGain);
        masterGain.connect(audioCtx.destination);
        compressor.connect(delayNode);
        delayNode.connect(delayGain);
        delayGain.connect(masterGain);
        delayGain.connect(delayNode);
        masterGain.connect(reverbNode);
        reverbNode.connect(reverbGain);
        reverbGain.connect(audioCtx.destination);
        bassGain.connect(masterGain);
        
        audioInitialized = true;
    } catch (e) {
        console.error('Audio init failed:', e);
    }
}

function getFrequency(x, y) {
    const scaleIndex = x % currentScale.length;
    const octave = Math.floor(y / (rows / 4));
    const semitones = currentScale[scaleIndex] + (octave * 12);
    return BASE_FREQ * Math.pow(2, semitones / 12);
}

function playNote(freq, volume = 0.1, duration = 0.3) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    if (activeNotes >= MAX_SIMULTANEOUS_NOTES) return;
    
    const key = Math.round(freq);
    const now = Date.now();
    if (lastNoteTime[key] && now - lastNoteTime[key] < 30) return;
    lastNoteTime[key] = now;
    
    activeNotes++;
    
    try {
        const t = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        const filter = audioCtx.createBiquadFilter();
        
        osc.type = 'sine';
        osc.frequency.value = freq;
        
        filter.type = 'lowpass';
        filter.frequency.value = freq * 4;
        filter.Q.value = 1;
        
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(volume, t + 0.008);
        gain.gain.exponentialRampToValueAtTime(volume * 0.5, t + 0.08);
        gain.gain.exponentialRampToValueAtTime(0.001, t + duration);
        
        osc.connect(filter);
        filter.connect(gain);
        gain.connect(compressor);
        
        osc.start(t);
        osc.stop(t + duration);
        
        setTimeout(() => { activeNotes = Math.max(0, activeNotes - 1); }, duration * 1000);
    } catch (e) {}
}

function processMusicalTriggers() {
    if (!musicalTriggers || musicalTriggers.length === 0) return;
    
    for (const trigger of musicalTriggers) {
        switch (trigger.trigger) {
            case 'density-high':
            case 'density-mid':
                const pitch = (trigger.params && trigger.params.frequency) || 220;
                playNote(pitch, 0.08, 0.2);
                break;
            case 'life-pulse':
                const intensity = (trigger.params && trigger.params.intensity) || 0.5;
                const rate = (trigger.params && trigger.params.rate) || 0.1;
                if (Math.random() < rate * 10) {
                    playNote(200 + (intensity * 400), rate * 0.1, 0.3);
                }
                break;
            default:
                break;
        }
    }
}

function playChord(baseFreq, type = 'major') {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    const intervals = type === 'major' ? [0, 4, 7, 12] : [0, 3, 7, 12];
    intervals.forEach((semitone, i) => {
        setTimeout(() => {
            playNote(baseFreq * Math.pow(2, semitone / 12), 0.07, 0.4);
        }, i * 50);
    });
}

// === Grid Helpers ===
function resize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
    cols = Math.floor(width / CELL_SIZE);
    rows = Math.floor(height / CELL_SIZE);
}

function coordsToString(x, y) {
    return `${x},${y}`;
}

function stringToCoords(s) {
    return s.split(',').map(Number);
}

// === Drawing ===
function draw() {
    const bgBrightness = Math.floor(5 + (board.size / 1000) * 10);
    ctx.fillStyle = `rgb(${bgBrightness}, ${bgBrightness}, ${Math.floor(bgBrightness * 1.5)})`;
    ctx.fillRect(0, 0, width, height);
    
    // Draw cells
    ctx.fillStyle = '#00f0ff';
    ctx.globalAlpha = 0.8;
    
    for (const cellKey of board) {
        const [x, y] = stringToCoords(cellKey);
        ctx.fillRect(
            x * CELL_SIZE + 1,
            y * CELL_SIZE + 1,
            CELL_SIZE - 2,
            CELL_SIZE - 2
        );
    }
    
    ctx.globalAlpha = 1.0;
    
    // Stats
    ctx.fillStyle = '#666';
    ctx.font = '12px monospace';
    ctx.fillText(`GEN: ${generation} | CELLS: ${board.size} | SCORE: ${score}`, 10, height - 10);
    
    const scaleName = Object.entries(SCALES).find(([k, v]) => v === currentScale)?.[0] || 'major';
    ctx.fillText(`SCALE: ${scaleName.toUpperCase()}`, 10, height - 28);
}

// === Game Loop ===
let lastUpdate = 0;

async function loop() {
    const now = Date.now();
    
    if (now - lastUpdate > TICK_MS) {
        try {
            await evolveGame();
            draw();
        } catch (e) {
            console.error('Loop error:', e);
        }
        lastUpdate = now;
    }
    
    requestAnimationFrame(loop);
}

// === Input ===
window.addEventListener('resize', () => {
    resize();
    draw();
});

// Click to add cells
canvas.addEventListener('click', async (e) => {
    e.preventDefault();
    if (!audioInitialized) initAudio();
    
    const col = Math.floor(e.clientX / CELL_SIZE);
    const row = Math.floor(e.clientY / CELL_SIZE);
    
    // Add 3x3 cluster
    const cellsToAdd = [];
    for (let i = -1; i <= 1; i++) {
        for (let j = -1; j <= 1; j++) {
            const nx = (col + i + cols) % cols;
            const ny = (row + j + rows) % rows;
            cellsToAdd.push([nx, ny]);
        }
    }
    
    await syncGame(cellsToAdd, []);
    playChord(getFrequency(col, row), 'major');
    draw();
});

// Keyboard controls
window.addEventListener('keydown', async (e) => {
    if (!audioInitialized) initAudio();
    
    // Number keys change scale
    if (e.key === '1') { currentScale = SCALES.major; playChord(200, 'major'); }
    if (e.key === '2') { currentScale = SCALES.minor; playChord(200, 'minor'); }
    if (e.key === '3') { currentScale = SCALES.japanese; playChord(200, 'major'); }
    if (e.key === '4') { currentScale = SCALES.blues; playChord(200, 'minor'); }
    if (e.key === '5') { currentScale = SCALES.chromatic; playChord(200, 'major'); }
    if (e.key === '6') { currentScale = SCALES.wholetone; playChord(200, 'major'); }
    
    // Space to add random cells
    if (e.code === 'Space') {
        e.preventDefault();
        const cellsToAdd = [];
        for (let i = 0; i < 20; i++) {
            cellsToAdd.push([
                Math.floor(Math.random() * cols),
                Math.floor(Math.random() * rows)
            ]);
        }
        await syncGame(cellsToAdd, []);
        draw();
    }
    
    // R to reset
    if (e.key === 'r' || e.key === 'R') {
        await syncGame([], Array.from(board).map(stringToCoords));
        draw();
    }
});

// === Start ===
resize();
loop();

// Spawn a missile targeting nearest enemy
function spawnMissile(fromX, fromY) {
    // Find nearest enemy (type 2)
    let nearestEnemy = null;
    let nearestDist = Infinity;
    
    for (const cell of cells) {
        if (cell.type === 2) {
            const dx = cell.x - fromX;
            const dy = cell.y - fromY;
            const dist = dx * dx + dy * dy;
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestEnemy = cell;
            }
        }
    }
    
    if (nearestEnemy && missiles.length < 100) {
        const angle = Math.atan2(nearestEnemy.y - fromY, nearestEnemy.x - fromX);
        missiles.push({
            x: fromX,
            y: fromY,
            vx: Math.cos(angle) * 0.8,
            vy: Math.sin(angle) * 0.8,
            target: nearestEnemy,
            life: 150
        });
        
        if (Math.random() < 0.3) {
            playMissileSound();
        }
    }
}

// Create explosion at location
function createExplosion(x, y, radius = 3) {
    explosions.push({ x, y, radius, life: 15, maxLife: 15 });
    
    // Kill cells in radius
    const killed = [];
    for (const cell of cells) {
        const dx = cell.x - x;
        const dy = cell.y - y;
        if (dx * dx + dy * dy <= radius * radius) {
            killed.push(cell);
        }
    }
    
    for (const cell of killed) {
        const idx = cells.indexOf(cell);
        if (idx >= 0) {
            cells.splice(idx, 1);
            killCount++;
            
            // Spawn food from dead cells
            if (Math.random() < 0.5) {
                food.push({ x: cell.x, y: cell.y });
            }
        }
    }
    
    if (killed.length > 0) {
        playExplosionSound(Math.min(killed.length / 5, 1.5));
        
        // Queue explosion notes
        const freq = getFrequency(x, y);
        queueNote(freq * 0.5, 0.1, 0.2, 'bass');
    }
    
    return killed.length;
}

function spawnFood() {
    if (food.length >= MAX_FOOD) return;
    food.push({
        x: Math.floor(Math.random() * cols),
        y: Math.floor(Math.random() * rows)
    });
}

function getCellAt(x, y) {
    return cells.find(c => c.x === x && c.y === y);
}

function getFoodAt(x, y) {
    return food.findIndex(f => f.x === x && f.y === y);
}

function countNeighbors(x, y) {
    let count = { total: 0, type1: 0, type2: 0, energySum: 0 };
    for (let i = -1; i <= 1; i++) {
        for (let j = -1; j <= 1; j++) {
            if (i === 0 && j === 0) continue;
            const nx = (x + i + cols) % cols;
            const ny = (y + j + rows) % rows;
            const neighbor = getCellAt(nx, ny);
            if (neighbor) {
                count.total++;
                if (neighbor.type === 1) count.type1++;
                else count.type2++;
                count.energySum += neighbor.energy;
            }
        }
    }
    return count;
}

// === Game Logic ===
function update() {
    const births = [];
    const deaths = [];
    const eats = [];
    
    // Process each cell
    for (const cell of cells) {
        cell.age++;
        
        // Try to eat food
        const foodIdx = getFoodAt(cell.x, cell.y);
        if (foodIdx >= 0) {
            cell.energy = Math.min(cell.energy + EAT_ENERGY_GAIN, MAX_ENERGY);
            food.splice(foodIdx, 1);
            eats.push({ x: cell.x, y: cell.y, type: cell.type });
        }
        
        // Lose energy over time (hunger)
        if (cell.age % 5 === 0 && cell.energy > 1) {
            cell.energy--;
        }
        
        // Death conditions
        const neighbors = countNeighbors(cell.x, cell.y);
        if (neighbors.total < 2 || neighbors.total > 3 || cell.energy <= 0) {
            deaths.push(cell);
        }
        
        // Reproduction (if enough energy and space)
        if (cell.energy >= 6 && neighbors.total < 3) {
            // Find empty adjacent cell
            const dirs = [[-1,0],[1,0],[0,-1],[0,1],[-1,-1],[1,-1],[-1,1],[1,1]];
            for (const [dx, dy] of dirs) {
                const nx = (cell.x + dx + cols) % cols;
                const ny = (cell.y + dy + rows) % rows;
                if (!getCellAt(nx, ny) && Math.random() < 0.3) {
                    births.push({
                        x: nx, y: ny,
                        energy: BIRTH_ENERGY,
                        type: cell.type,
                        age: 0,
                        parentEnergy: cell.energy
                    });
                    cell.energy -= 3; // Cost of reproduction
                    break;
                }
            }
        }
    }
    
    // Check for spontaneous birth (standard GoL rules)
    const checked = new Set();
    for (const cell of cells) {
        for (let i = -1; i <= 1; i++) {
            for (let j = -1; j <= 1; j++) {
                const nx = (cell.x + i + cols) % cols;
                const ny = (cell.y + j + rows) % rows;
                const key = `${nx},${ny}`;
                if (checked.has(key)) continue;
                checked.add(key);
                
                if (!getCellAt(nx, ny)) {
                    const neighbors = countNeighbors(nx, ny);
                    if (neighbors.total === 3) {
                        const type = neighbors.type2 > neighbors.type1 ? 2 : 1;
                        births.push({
                            x: nx, y: ny,
                            energy: BIRTH_ENERGY,
                            type: type,
                            age: 0
                        });
                    }
                }
            }
        }
    }
    
    // Apply deaths
    let deathCount = 0;
    for (const dead of deaths) {
        const idx = cells.indexOf(dead);
        if (idx >= 0) {
            cells.splice(idx, 1);
            deathCount++;
            // Queue death sounds (more deaths = more music)
            if (Math.random() < 0.2) {
                playDeathSound(getFrequency(dead.x, dead.y));
            }
        }
    }
    
    // Kick drum on mass death events
    if (deathCount > 20) {
        playKick();
    }
    
    // Apply births (allow more!)
    const maxNewCells = 50;
    let birthCount = 0;
    for (const birth of births) {
        if (birthCount >= maxNewCells) break;
        if (cells.length >= MAX_CELLS) break;
        if (!getCellAt(birth.x, birth.y)) {
            cells.push({
                x: birth.x, y: birth.y,
                energy: birth.energy,
                type: birth.type,
                age: 0
            });
            birthCount++;
            
            // Queue birth notes (more births = more melody!)
            if (Math.random() < 0.4) {
                const freq = getFrequency(birth.x, birth.y);
                queueNote(freq, 0.07, 0.3, Math.random() > 0.7 ? 'bell' : 'melody');
            }
        }
    }
    
    // Play chord on birth burst
    if (birthCount > 15) {
        const avgX = births.slice(0, 10).reduce((sum, b) => sum + b.x, 0) / 10;
        const avgY = births.slice(0, 10).reduce((sum, b) => sum + b.y, 0) / 10;
        playChord(getFrequency(Math.floor(avgX), Math.floor(avgY)), birthCount > 25 ? 'major' : 'minor');
    }
    
    // Queue eat sounds (more eating = more rhythm!)
    for (const eat of eats) {
        if (Math.random() < 0.6) {
            playEatSound(getFrequency(eat.x, eat.y));
        }
    }
    
    // Spawn new food
    if (Math.random() < FOOD_SPAWN_RATE) {
        spawnFood();
    }
    
    // Spawn enemy clusters (red invasion!)
    if (Math.random() < ENEMY_SPAWN_RATE) {
        const cx = Math.floor(Math.random() * cols);
        const cy = Math.floor(Math.random() * rows);
        for (let i = -2; i <= 2; i++) {
            for (let j = -2; j <= 2; j++) {
                if (Math.random() < 0.6 && cells.length < MAX_CELLS) {
                    const x = (cx + i + cols) % cols;
                    const y = (cy + j + rows) % rows;
                    if (!getCellAt(x, y)) {
                        cells.push({
                            x, y,
                            energy: BIRTH_ENERGY + 4,
                            type: 2,  // Enemy!
                            age: 0
                        });
                    }
                }
            }
        }
    }
    
    // Cyan cells launch missiles at enemies!
    if (Math.random() < MISSILE_SPAWN_RATE) {
        const cyanCells = cells.filter(c => c.type === 1 && c.energy >= 5);
        if (cyanCells.length > 0) {
            const launcher = cyanCells[Math.floor(Math.random() * cyanCells.length)];
            spawnMissile(launcher.x, launcher.y);
            launcher.energy -= 1;  // Launching costs energy
        }
    }
    
    // Update missiles
    for (const missile of missiles) {
        missile.life--;
        
        // Re-target if target is dead
        if (!missile.target || !cells.includes(missile.target)) {
            let nearest = null;
            let nearestDist = Infinity;
            for (const cell of cells) {
                if (cell.type === 2) {
                    const dx = cell.x - missile.x;
                    const dy = cell.y - missile.y;
                    const dist = dx * dx + dy * dy;
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = cell;
                    }
                }
            }
            missile.target = nearest;
        }
        
        // Home towards target
        if (missile.target) {
            const dx = missile.target.x - missile.x;
            const dy = missile.target.y - missile.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist > 0.5) {
                const speed = 0.8 + intensity * 0.4;
                missile.vx += (dx / dist) * 0.15;
                missile.vy += (dy / dist) * 0.15;
                const mag = Math.sqrt(missile.vx * missile.vx + missile.vy * missile.vy);
                if (mag > speed) {
                    missile.vx = (missile.vx / mag) * speed;
                    missile.vy = (missile.vy / mag) * speed;
                }
            }
        }
        
        missile.x += missile.vx;
        missile.y += missile.vy;
        
        // Wrap around
        missile.x = (missile.x + cols) % cols;
        missile.y = (missile.y + rows) % rows;
        
        // Check collision with enemies
        const hitX = Math.floor(missile.x);
        const hitY = Math.floor(missile.y);
        const hitCell = getCellAt(hitX, hitY);
        if (hitCell && hitCell.type === 2) {
            createExplosion(hitX, hitY, 2 + Math.floor(intensity * 2));
            missile.life = 0;
        }
    }
    
    // Remove dead missiles
    missiles = missiles.filter(m => m.life > 0);
    
    // Update explosions
    for (const exp of explosions) {
        exp.life--;
    }
    explosions = explosions.filter(e => e.life > 0);
    
    // Auto-spawn cells if population too low
    if (cells.length < 200 && Math.random() < 0.3) {
        // Spawn a cluster (mostly cyan to fight back!)
        const cx = Math.floor(Math.random() * cols);
        const cy = Math.floor(Math.random() * rows);
        for (let i = -3; i <= 3; i++) {
            for (let j = -3; j <= 3; j++) {
                if (Math.random() < 0.5 && cells.length < MAX_CELLS) {
                    const x = (cx + i + cols) % cols;
                    const y = (cy + j + rows) % rows;
                    if (!getCellAt(x, y)) {
                        cells.push({
                            x, y,
                            energy: BIRTH_ENERGY + 3,
                            type: Math.random() > 0.3 ? 1 : 2,
                            age: 0
                        });
                    }
                }
            }
        }
    }
    
    // Limit total cells
    if (cells.length > MAX_CELLS) {
        // Remove weakest cells
        cells.sort((a, b) => a.energy - b.energy);
        cells.splice(0, cells.length - MAX_CELLS + 200);
    }
    
    // Calculate intensity based on activity
    intensity = Math.min(1, (killCount + birthCount) / 100);
    killCount *= 0.95;  // Decay
    birthCount *= 0.95;
    
    // Process beat (play queued notes rhythmically)
    processBeat();
    
    // Update ambient pad
    if (audioInitialized) {
        updateAmbientPad();
    }
}

// === Drawing ===
function draw() {
    // Clear with dark background (flash on intensity)
    const bgBrightness = Math.floor(5 + intensity * 10);
    ctx.fillStyle = `rgb(${bgBrightness}, ${bgBrightness}, ${Math.floor(bgBrightness * 1.5)})`;
    ctx.fillRect(0, 0, width, height);
    
    // Draw explosions (behind everything)
    for (const exp of explosions) {
        const progress = 1 - exp.life / exp.maxLife;
        const radius = exp.radius * CELL_SIZE * (0.5 + progress * 1.5);
        
        // Outer glow
        const gradient = ctx.createRadialGradient(
            exp.x * CELL_SIZE, exp.y * CELL_SIZE, 0,
            exp.x * CELL_SIZE, exp.y * CELL_SIZE, radius
        );
        gradient.addColorStop(0, `rgba(255, 200, 50, ${0.8 * (1 - progress)})`);
        gradient.addColorStop(0.4, `rgba(255, 100, 50, ${0.5 * (1 - progress)})`);
        gradient.addColorStop(1, 'rgba(255, 0, 0, 0)');
        
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(exp.x * CELL_SIZE, exp.y * CELL_SIZE, radius, 0, Math.PI * 2);
        ctx.fill();
    }
    
    // Draw food (small yellow dots)
    ctx.fillStyle = '#fcee0a';
    ctx.globalAlpha = 0.5;
    for (const f of food) {
        ctx.beginPath();
        ctx.arc(
            f.x * CELL_SIZE + CELL_SIZE / 2,
            f.y * CELL_SIZE + CELL_SIZE / 2,
            1.5, 0, Math.PI * 2
        );
        ctx.fill();
    }
    
    // Draw cells
    for (const cell of cells) {
        // Size based on energy (grows when well-fed)
        const sizeRatio = 0.4 + (cell.energy / MAX_ENERGY) * 0.6;
        const size = (CELL_SIZE - 1) * sizeRatio;
        
        // Color based on type
        if (cell.type === 1) {
            ctx.fillStyle = '#00f0ff'; // Cyan
        } else {
            ctx.fillStyle = '#ff003c'; // Red - enemy!
        }
        
        // Alpha based on energy
        ctx.globalAlpha = 0.4 + (cell.energy / MAX_ENERGY) * 0.5;
        
        // Draw cell (rounded for organic feel)
        const cx = cell.x * CELL_SIZE + CELL_SIZE / 2;
        const cy = cell.y * CELL_SIZE + CELL_SIZE / 2;
        
        ctx.beginPath();
        ctx.arc(cx, cy, size / 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Glow effect for high-energy cells
        if (cell.energy >= 8) {
            ctx.globalAlpha = 0.15;
            ctx.beginPath();
            ctx.arc(cx, cy, size / 2 + 2, 0, Math.PI * 2);
            ctx.fill();
        }
    }
    
    // Draw missiles
    ctx.globalAlpha = 1.0;
    for (const missile of missiles) {
        const mx = missile.x * CELL_SIZE + CELL_SIZE / 2;
        const my = missile.y * CELL_SIZE + CELL_SIZE / 2;
        
        // Missile trail
        const trailLength = 4;
        ctx.strokeStyle = '#00ff88';
        ctx.lineWidth = 2;
        ctx.globalAlpha = 0.6;
        ctx.beginPath();
        ctx.moveTo(mx, my);
        ctx.lineTo(mx - missile.vx * CELL_SIZE * trailLength, my - missile.vy * CELL_SIZE * trailLength);
        ctx.stroke();
        
        // Missile head
        ctx.fillStyle = '#00ff88';
        ctx.globalAlpha = 1.0;
        ctx.beginPath();
        ctx.arc(mx, my, 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Glow
        ctx.fillStyle = '#88ffaa';
        ctx.globalAlpha = 0.3;
        ctx.beginPath();
        ctx.arc(mx, my, 4, 0, Math.PI * 2);
        ctx.fill();
    }
    
    ctx.globalAlpha = 1.0;
    
    // Count factions
    const cyanCount = cells.filter(c => c.type === 1).length;
    const redCount = cells.filter(c => c.type === 2).length;
    
    // Draw stats (bottom left) - more visible
    ctx.fillStyle = '#666';
    ctx.font = '11px monospace';
    const scaleName = Object.entries(SCALES).find(([k, v]) => v === currentScale)?.[0] || 'major';
    ctx.fillText(`CYAN: ${cyanCount} | RED: ${redCount} | Missiles: ${missiles.length} | Food: ${food.length}`, 10, height - 10);
    
    // Scale and intensity
    ctx.fillText(`Scale: ${scaleName.toUpperCase()} | BPM: ${BPM} | Intensity: ${Math.floor(intensity * 100)}%`, 10, height - 25);
    
    // Instructions
    ctx.fillStyle = '#444';
    ctx.fillText('Click: spawn cyan | Right-click: food | M: missile barrage | Space: burst | 1-6: scale | R: reset', 10, height - 40);
    
    // War status
    if (cyanCount > redCount * 2) {
        ctx.fillStyle = '#00f0ff';
        ctx.fillText('CYAN DOMINATING!', width - 150, height - 10);
    } else if (redCount > cyanCount * 2) {
        ctx.fillStyle = '#ff003c';
        ctx.fillText('RED INVASION!', width - 150, height - 10);
    } else if (missiles.length > 20) {
        ctx.fillStyle = '#00ff88';
        ctx.fillText('MISSILE STORM!', width - 150, height - 10);
    }
}

// === Main Loop ===
function loop() {
    try {
        update();
        draw();
    } catch (e) {
        console.error('Loop error:', e);
    }
    setTimeout(() => requestAnimationFrame(loop), TICK_MS);
}

// === Input ===
window.addEventListener('resize', resize);

// Click to spawn cells + missiles
canvas.addEventListener('click', (e) => {
    // Initialize audio on first click (browser policy)
    if (!audioInitialized) {
        initAudio();
    }
    
    const col = Math.floor(e.clientX / CELL_SIZE);
    const row = Math.floor(e.clientY / CELL_SIZE);
    
    // Spawn a bigger cluster of cells
    for (let i = -2; i <= 2; i++) {
        for (let j = -2; j <= 2; j++) {
            const nx = (col + i + cols) % cols;
            const ny = (row + j + rows) % rows;
            if (!getCellAt(nx, ny) && Math.random() < 0.7) {
                cells.push({
                    x: nx, y: ny,
                    energy: BIRTH_ENERGY + 4,
                    type: 1, // User spawns cyan
                    age: 0
                });
            }
        }
    }
    
    // Launch missiles from click point!
    for (let i = 0; i < 3; i++) {
        spawnMissile(col, row);
    }
    
    // Play chord on click
    playChord(getFrequency(col, row), 'major');
});

// Mouse move to draw cells
let isDrawing = false;
canvas.addEventListener('mousedown', () => { isDrawing = true; initAudio(); });
canvas.addEventListener('mouseup', () => { isDrawing = false; });
canvas.addEventListener('mousemove', (e) => {
    if (!isDrawing) return;
    
    const col = Math.floor(e.clientX / CELL_SIZE);
    const row = Math.floor(e.clientY / CELL_SIZE);
    
    if (!getCellAt(col, row)) {
        cells.push({
            x: col, y: row,
            energy: BIRTH_ENERGY + 2,
            type: 1,
            age: 0
        });
        
        // Play note while drawing
        if (Math.random() < 0.3) {
            playNote(getFrequency(col, row), 0.06, 0.2);
        }
    }
});

// Right-click to spawn food
canvas.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    const col = Math.floor(e.clientX / CELL_SIZE);
    const row = Math.floor(e.clientY / CELL_SIZE);
    
    // Spawn food cluster
    for (let i = -2; i <= 2; i++) {
        for (let j = -2; j <= 2; j++) {
            if (Math.random() < 0.6) {
                food.push({
                    x: (col + i + cols) % cols,
                    y: (row + j + rows) % rows
                });
            }
        }
    }
});

// Keyboard controls
window.addEventListener('keydown', (e) => {
    initAudio();
    
    // Number keys change scale
    if (e.key === '1') { currentScale = SCALES.major; playChord(BASE_FREQ * 2, 'major'); }
    if (e.key === '2') { currentScale = SCALES.minor; playChord(BASE_FREQ * 2, 'minor'); }
    if (e.key === '3') { currentScale = SCALES.japanese; playChord(BASE_FREQ * 2, 'major'); }
    if (e.key === '4') { currentScale = SCALES.blues; playChord(BASE_FREQ * 2, 'minor'); }
    if (e.key === '5') { currentScale = SCALES.chromatic; playChord(BASE_FREQ * 2, 'major'); }
    if (e.key === '6') { currentScale = SCALES.wholetone; playChord(BASE_FREQ * 2, 'major'); }
    
    // Space to spawn random cells (BIG burst!)
    if (e.code === 'Space') {
        e.preventDefault();
        playKick();
        
        // Spawn multiple clusters (more cells!)
        for (let c = 0; c < 8; c++) {
            const cx = Math.floor(Math.random() * cols);
            const cy = Math.floor(Math.random() * rows);
            for (let i = -3; i <= 3; i++) {
                for (let j = -3; j <= 3; j++) {
                    if (Math.random() < 0.6 && cells.length < MAX_CELLS) {
                        const x = (cx + i + cols) % cols;
                        const y = (cy + j + rows) % rows;
                        if (!getCellAt(x, y)) {
                            cells.push({
                                x, y,
                                energy: BIRTH_ENERGY + 5,
                                type: Math.random() > 0.4 ? 1 : 2,
                                age: 0
                            });
                        }
                    }
                }
            }
        }
        
        // Spawn lots of food
        for (let i = 0; i < 80; i++) {
            spawnFood();
        }
        
        // Play rising arpeggio
        for (let i = 0; i < 6; i++) {
            setTimeout(() => {
                const freq = BASE_FREQ * Math.pow(2, currentScale[i % currentScale.length] / 12) * (1 + Math.floor(i / currentScale.length));
                playNote(freq, 0.1, 0.4, 'bell');
            }, i * 70);
        }
    }
    
    // M for MISSILE BARRAGE!
    if (e.key === 'm' || e.key === 'M') {
        playKick();
        
        // Launch missiles from all high-energy cyan cells!
        const launchers = cells.filter(c => c.type === 1 && c.energy >= 4);
        let launched = 0;
        for (const launcher of launchers) {
            if (launched >= 30) break;  // Cap per barrage
            if (Math.random() < 0.5) {
                spawnMissile(launcher.x, launcher.y);
                launched++;
            }
        }
        
        // Dramatic sound
        for (let i = 0; i < 5; i++) {
            setTimeout(() => playMissileSound(), i * 50);
        }
    }
    
    // E for ENEMY WAVE!
    if (e.key === 'e' || e.key === 'E') {
        playKick();
        
        // Spawn enemy invasion from edges
        for (let i = 0; i < 10; i++) {
            const edge = Math.floor(Math.random() * 4);
            let cx, cy;
            if (edge === 0) { cx = 0; cy = Math.floor(Math.random() * rows); }
            else if (edge === 1) { cx = cols - 1; cy = Math.floor(Math.random() * rows); }
            else if (edge === 2) { cx = Math.floor(Math.random() * cols); cy = 0; }
            else { cx = Math.floor(Math.random() * cols); cy = rows - 1; }
            
            for (let di = -2; di <= 2; di++) {
                for (let dj = -2; dj <= 2; dj++) {
                    if (Math.random() < 0.7 && cells.length < MAX_CELLS) {
                        const x = (cx + di + cols) % cols;
                        const y = (cy + dj + rows) % rows;
                        if (!getCellAt(x, y)) {
                            cells.push({
                                x, y,
                                energy: BIRTH_ENERGY + 5,
                                type: 2,  // Enemy!
                                age: 0
                            });
                        }
                    }
                }
            }
        }
        
        // Menacing sound
        playChord(BASE_FREQ * 0.5, 'minor');
    }
    
    // B for BIG EXPLOSION!
    if (e.key === 'b' || e.key === 'B') {
        const cx = Math.floor(Math.random() * cols);
        const cy = Math.floor(Math.random() * rows);
        createExplosion(cx, cy, 8);
        createExplosion(cx + 5, cy + 3, 5);
        createExplosion(cx - 4, cy - 2, 5);
    }
    
    // R to reset
    if (e.key === 'r' || e.key === 'R') {
        cells = [];
        food = [];
        missiles = [];
        explosions = [];
        noteQueue = [];
        initializeLife();
        playKick();
    }
});

// Touch support
canvas.addEventListener('touchstart', (e) => {
    e.preventDefault();
    initAudio();
    
    for (const touch of e.touches) {
        const col = Math.floor(touch.clientX / CELL_SIZE);
        const row = Math.floor(touch.clientY / CELL_SIZE);
        
        for (let i = -1; i <= 1; i++) {
            for (let j = -1; j <= 1; j++) {
                const nx = (col + i + cols) % cols;
                const ny = (row + j + rows) % rows;
                if (!getCellAt(nx, ny)) {
                    cells.push({
                        x: nx, y: ny,
                        energy: BIRTH_ENERGY + 3,
                        type: 1,
                        age: 0
                    });
                }
            }
        }
        playChord(getFrequency(col, row), 'major');
    }
});

// === Start ===
resize();
loop();
