// === Musical Game of Life ===
// Cells play piano notes, eat food, and grow!

const canvas = document.getElementById('bgCanvas');
const ctx = canvas.getContext('2d');

// === Configuration ===
const CELL_SIZE = 6;  // Even smaller = LOTS more cells!
const MAX_ENERGY = 12;
const FOOD_SPAWN_RATE = 0.08;
const MAX_FOOD = 400;
const BIRTH_ENERGY = 4;
const EAT_ENERGY_GAIN = 3;
const TICK_MS = 50;  // Faster!
const INITIAL_CELL_DENSITY = 0.18;  // Dense initial population
const MAX_CELLS = 3000;  // LOTS of cells!
const ENEMY_SPAWN_RATE = 0.03;  // Enemy (red) spawns
const MISSILE_SPAWN_RATE = 0.02;  // Missiles that hunt enemies

// === State ===
let width, height, cols, rows;
let cells = [];  // Array of {x, y, energy, type, age}
let food = [];   // Array of {x, y}
let missiles = [];  // Array of {x, y, vx, vy, target}
let explosions = [];  // Array of {x, y, radius, life}
let audioCtx = null;
let masterGain = null;
let reverbNode = null;
let reverbGain = null;
let lastNoteTime = {};
let audioInitialized = false;

// Stats for intensity
let killCount = 0;
let birthCount = 0;
let intensity = 0;  // 0-1 based on activity

// === Musical Scales (pentatonic for pleasant sounds) ===
const SCALES = {
    major: [0, 2, 4, 7, 9],      // C major pentatonic
    minor: [0, 3, 5, 7, 10],     // C minor pentatonic
    japanese: [0, 2, 5, 7, 9],   // Japanese scale
    blues: [0, 3, 5, 6, 7, 10],  // Blues scale
    chromatic: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11], // All notes
    wholetone: [0, 2, 4, 6, 8, 10], // Dreamy whole tone
};
let currentScale = SCALES.major;
const BASE_FREQ = 130.81; // C3

// === Rhythm & Beat System ===
let beatStep = 0;
let lastBeatTime = 0;
const BPM = 120;
const BEAT_INTERVAL = 60000 / BPM / 4; // 16th notes

// === Polyphony Control ===
const MAX_SIMULTANEOUS_NOTES = 12;
let activeNotes = 0;
let noteQueue = [];  // Queue notes to play on beat

// === Audio System ===
let compressor = null;
let delayNode = null;
let delayGain = null;
let bassGain = null;
let padOsc = null;
let padGain = null;

function initAudio() {
    if (audioCtx) {
        if (audioCtx.state === 'suspended') audioCtx.resume();
        return;
    }
    try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        
        // Compressor for glue
        compressor = audioCtx.createDynamicsCompressor();
        compressor.threshold.value = -20;
        compressor.ratio.value = 4;
        compressor.attack.value = 0.003;
        compressor.release.value = 0.1;
        
        // Master gain
        masterGain = audioCtx.createGain();
        masterGain.gain.value = 0.4;
        
        // Bass channel
        bassGain = audioCtx.createGain();
        bassGain.gain.value = 0.5;
        
        // Delay effect (rhythmic echoes)
        delayNode = audioCtx.createDelay(1.0);
        delayNode.delayTime.value = 60 / BPM * 0.75; // Dotted eighth
        delayGain = audioCtx.createGain();
        delayGain.gain.value = 0.3;
        
        // Reverb for ambient feel
        reverbNode = audioCtx.createConvolver();
        reverbGain = audioCtx.createGain();
        reverbGain.gain.value = 0.5;
        
        // Generate impulse response (2 second reverb)
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
        
        // Routing
        compressor.connect(masterGain);
        masterGain.connect(audioCtx.destination);
        
        // Delay send
        compressor.connect(delayNode);
        delayNode.connect(delayGain);
        delayGain.connect(masterGain);
        delayGain.connect(delayNode); // Feedback
        
        // Reverb send
        masterGain.connect(reverbNode);
        reverbNode.connect(reverbGain);
        reverbGain.connect(audioCtx.destination);
        
        // Bass direct
        bassGain.connect(masterGain);
        
        // Start ambient pad
        startAmbientPad();
        
        audioInitialized = true;
    } catch (e) {
        console.error('Audio init failed:', e);
    }
}

// Ambient drone that evolves with cell count
function startAmbientPad() {
    if (!audioCtx) return;
    
    padGain = audioCtx.createGain();
    padGain.gain.value = 0;
    
    const padFilter = audioCtx.createBiquadFilter();
    padFilter.type = 'lowpass';
    padFilter.frequency.value = 400;
    padFilter.Q.value = 2;
    
    // Create multiple detuned oscillators for rich pad
    const oscs = [];
    const detunes = [-12, -5, 0, 7, 12]; // Spread
    for (const detune of detunes) {
        const osc = audioCtx.createOscillator();
        osc.type = 'sine';
        osc.frequency.value = BASE_FREQ;
        osc.detune.value = detune;
        osc.connect(padFilter);
        osc.start();
        oscs.push(osc);
    }
    
    padFilter.connect(padGain);
    padGain.connect(reverbNode);
    
    padOsc = oscs;
}

// Update pad based on cell activity
function updateAmbientPad() {
    if (!padGain || !padOsc) return;
    
    const cellRatio = cells.length / MAX_CELLS;
    const targetGain = Math.min(cellRatio * 0.15, 0.1);
    
    // Smooth transition
    padGain.gain.linearRampToValueAtTime(targetGain, audioCtx.currentTime + 0.1);
    
    // Change pad pitch based on dominant cell type
    const type1Count = cells.filter(c => c.type === 1).length;
    const type2Count = cells.filter(c => c.type === 2).length;
    const baseNote = type1Count > type2Count ? BASE_FREQ : BASE_FREQ * 0.75; // Minor feel for red
    
    for (const osc of padOsc) {
        osc.frequency.linearRampToValueAtTime(baseNote, audioCtx.currentTime + 0.5);
    }
}

// Get frequency from grid position
function getFrequency(x, y) {
    const scaleIndex = x % currentScale.length;
    const octave = Math.floor(y / (rows / 4)); // 4 octaves across screen height
    const semitones = currentScale[scaleIndex] + (octave * 12);
    return BASE_FREQ * Math.pow(2, semitones / 12);
}

// Play a piano-like note
function playNote(freq, volume = 0.1, duration = 0.3, type = 'melody') {
    if (!audioCtx || audioCtx.state !== 'running') return;
    if (activeNotes >= MAX_SIMULTANEOUS_NOTES) return;
    
    // Throttle notes by frequency
    const key = Math.round(freq);
    const now = Date.now();
    if (lastNoteTime[key] && now - lastNoteTime[key] < 30) return;
    lastNoteTime[key] = now;
    
    activeNotes++;
    
    try {
        const t = audioCtx.currentTime;
        
        // Different sounds for different event types
        const osc1 = audioCtx.createOscillator();
        const osc2 = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        const filter = audioCtx.createBiquadFilter();
        
        if (type === 'bass') {
            // Deep bass note
            osc1.type = 'sine';
            osc1.frequency.value = freq;
            osc2.type = 'triangle';
            osc2.frequency.value = freq;
            filter.frequency.value = freq * 2;
            volume *= 1.5;
            duration = 0.4;
        } else if (type === 'pluck') {
            // Plucky synth
            osc1.type = 'sawtooth';
            osc1.frequency.value = freq;
            osc2.type = 'square';
            osc2.frequency.value = freq * 1.005; // Slight detune
            filter.frequency.setValueAtTime(freq * 8, t);
            filter.frequency.exponentialRampToValueAtTime(freq * 2, t + 0.1);
        } else if (type === 'bell') {
            // Bell-like
            osc1.type = 'sine';
            osc1.frequency.value = freq;
            osc2.type = 'sine';
            osc2.frequency.value = freq * 2.4; // Inharmonic
            filter.frequency.value = freq * 6;
            duration = 0.6;
        } else {
            // Default piano-like
            osc1.type = 'triangle';
            osc1.frequency.value = freq;
            osc2.type = 'sine';
            osc2.frequency.value = freq * 2;
            filter.frequency.value = freq * 4;
        }
        
        filter.type = 'lowpass';
        filter.Q.value = 1;
        
        // ADSR envelope
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(volume, t + 0.008);
        gain.gain.exponentialRampToValueAtTime(volume * 0.5, t + 0.08);
        gain.gain.exponentialRampToValueAtTime(0.001, t + duration);
        
        // Connect
        osc1.connect(filter);
        osc2.connect(filter);
        filter.connect(gain);
        
        // Route bass differently
        if (type === 'bass') {
            gain.connect(bassGain);
        } else {
            gain.connect(compressor);
        }
        
        // Play
        osc1.start(t);
        osc2.start(t);
        osc1.stop(t + duration);
        osc2.stop(t + duration);
        
        // Decrement active notes after duration
        setTimeout(() => { activeNotes = Math.max(0, activeNotes - 1); }, duration * 1000);
    } catch (e) {}
}

// Queue a note to play on next beat
function queueNote(freq, volume, duration, type) {
    if (noteQueue.length < 16) {
        noteQueue.push({ freq, volume, duration, type });
    }
}

// Process beat - play queued notes rhythmically
function processBeat() {
    const now = Date.now();
    if (now - lastBeatTime < BEAT_INTERVAL) return;
    lastBeatTime = now;
    beatStep++;
    
    // Play queued notes (up to 4 per beat for musicality)
    const notesToPlay = noteQueue.splice(0, 4);
    for (const note of notesToPlay) {
        playNote(note.freq, note.volume, note.duration, note.type);
    }
    
    // Bass note on downbeat (every 4 steps)
    if (beatStep % 4 === 0 && cells.length > 50) {
        const bassNote = currentScale[0];
        const bassFreq = BASE_FREQ * Math.pow(2, bassNote / 12) / 2;
        playNote(bassFreq, 0.15, 0.5, 'bass');
    }
    
    // Rhythmic hi-hat from cell activity
    if (cells.length > 100 && beatStep % 2 === 0) {
        playHiHat(0.03);
    }
}

// Play chord (for special events)
function playChord(baseFreq, type = 'major') {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    const intervals = type === 'major' ? [0, 4, 7, 12] : [0, 3, 7, 12];
    intervals.forEach((semitone, i) => {
        setTimeout(() => {
            playNote(baseFreq * Math.pow(2, semitone / 12), 0.07, 0.6, 'bell');
        }, i * 40); // Arpeggio
    });
}

// Hi-hat sound from cell activity
function playHiHat(volume = 0.05) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    try {
        const t = audioCtx.currentTime;
        const bufferSize = audioCtx.sampleRate * 0.05;
        const buffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = Math.random() * 2 - 1;
        }
        
        const noise = audioCtx.createBufferSource();
        noise.buffer = buffer;
        
        const hihatFilter = audioCtx.createBiquadFilter();
        hihatFilter.type = 'highpass';
        hihatFilter.frequency.value = 7000;
        
        const hihatGain = audioCtx.createGain();
        hihatGain.gain.setValueAtTime(volume, t);
        hihatGain.gain.exponentialRampToValueAtTime(0.001, t + 0.05);
        
        noise.connect(hihatFilter);
        hihatFilter.connect(hihatGain);
        hihatGain.connect(compressor);
        
        noise.start(t);
        noise.stop(t + 0.05);
    } catch (e) {}
}

// Kick drum on major events
function playKick() {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    try {
        const t = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        
        osc.type = 'sine';
        osc.frequency.setValueAtTime(150, t);
        osc.frequency.exponentialRampToValueAtTime(40, t + 0.1);
        
        gain.gain.setValueAtTime(0.4, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
        
        osc.connect(gain);
        gain.connect(bassGain);
        
        osc.start(t);
        osc.stop(t + 0.3);
    } catch (e) {}
}

// Play eating sound (queue a plucky note)
function playEatSound(freq) {
    queueNote(freq, 0.08, 0.15, 'pluck');
}

// Missile launch sound
function playMissileSound() {
    if (!audioCtx || audioCtx.state !== 'running') return;
    try {
        const t = audioCtx.currentTime;
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(200, t);
        osc.frequency.exponentialRampToValueAtTime(800, t + 0.15);
        
        gain.gain.setValueAtTime(0.08, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
        
        osc.connect(gain);
        gain.connect(compressor);
        
        osc.start(t);
        osc.stop(t + 0.15);
    } catch (e) {}
}

// Explosion sound
function playExplosionSound(size = 1) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    try {
        const t = audioCtx.currentTime;
        
        // Noise burst
        const bufferSize = audioCtx.sampleRate * 0.3;
        const buffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) {
            data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / bufferSize, 2);
        }
        
        const noise = audioCtx.createBufferSource();
        noise.buffer = buffer;
        
        const filter = audioCtx.createBiquadFilter();
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(3000, t);
        filter.frequency.exponentialRampToValueAtTime(100, t + 0.3);
        
        const gain = audioCtx.createGain();
        gain.gain.setValueAtTime(0.15 * size, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
        
        noise.connect(filter);
        filter.connect(gain);
        gain.connect(compressor);
        
        noise.start(t);
        noise.stop(t + 0.3);
        
        // Sub thump
        const sub = audioCtx.createOscillator();
        const subGain = audioCtx.createGain();
        sub.type = 'sine';
        sub.frequency.setValueAtTime(80, t);
        sub.frequency.exponentialRampToValueAtTime(30, t + 0.25);
        subGain.gain.setValueAtTime(0.2 * size, t);
        subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
        sub.connect(subGain);
        subGain.connect(bassGain);
        sub.start(t);
        sub.stop(t + 0.25);
    } catch (e) {}
}

// Play death sound (queue a descending note)
function playDeathSound(freq) {
    // Deaths create lower, softer notes
    queueNote(freq * 0.5, 0.04, 0.2, 'melody');
}

// === Grid Helpers ===
function resize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
    cols = Math.floor(width / CELL_SIZE);
    rows = Math.floor(height / CELL_SIZE);
    
    // Reset cells
    cells = [];
    food = [];
    initializeLife();
}

function initializeLife() {
    // Spawn initial cells - more density!
    for (let i = 0; i < cols; i++) {
        for (let j = 0; j < rows; j++) {
            const rand = Math.random();
            if (rand < INITIAL_CELL_DENSITY) {
                cells.push({
                    x: i, y: j,
                    energy: BIRTH_ENERGY + Math.floor(Math.random() * 5),
                    type: Math.random() > 0.35 ? 1 : 2, // More enemies!
                    age: 0
                });
            }
        }
    }
    
    // Spawn lots of initial food
    for (let i = 0; i < 150; i++) {
        spawnFood();
    }
    
    // Reset missiles and explosions
    missiles = [];
    explosions = [];
    killCount = 0;
    birthCount = 0;
}

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
