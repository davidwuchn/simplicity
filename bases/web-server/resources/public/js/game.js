const canvas = document.getElementById('gameCanvas');
canvas.style.cursor = 'default'; // Show cursor initially
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');

// === CONFIGURATION (Hard Limits) ===
const MAX_ENEMIES = 35;
const MAX_BULLETS = 50;
const MAX_MISSILES = 12;
const MAX_PARTICLES = 80;
const MAX_ENEMY_BULLETS = 40;
const MAX_POWERUPS = 5;

// === Global State ===
let gameStarted = false;
let score = 0; 
let gameOver = false; 
let powerUpActive = false; 
let powerUpTimer = 0;
let weaponLevel = 1; 
let lastBossSpawnScore = 0;
let invincible = false;
let invincibilityEnd = 0;
let invincibilityCooldown = 0;
const INVINCIBILITY_DURATION = 3000; // 3 seconds
const INVINCIBILITY_COOLDOWN = 10000; // 10 seconds cooldown
let screenShake = 0; // Screen shake intensity
let shakeX = 0;
let shakeY = 0;
let combo = 0; // Current combo count
let lastKillTime = 0; // Time of last kill
const COMBO_WINDOW = 2000; // 2 seconds to maintain combo
let comboDisplay = 0; // For fade animation
let topScores = []; // Top 10 scores from leaderboard
let lastComboMilestone = 0; // Track last milestone for sound

// === Piano Particles ===
let pianoParticles = [];
const MAX_PIANO_PARTICLES = 20;
let lastPianoSpawn = 0;
const pianoNotes = [
    261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25,  // C4-C5
    587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50, 1174.66  // D5-D6
];

// === Audio State ===
let audioCtx = null;
let masterGain = null;
let compressor = null;
let reverbNode = null;
let reverbGain = null;
let noiseBuffer = null;
let beatKick = 0;
let beatSnare = 0;
let beatHat = 0;
let currentStyle = 0;
let lastBeatTime = 0;
let beatStep = 0;
let lastSoundTime = {};
let melodyQueue = [];
let filterLFO = 0;
// Sidechain ducking state
let sidechainGain = null;
let sidechainAmount = 0;
// Delay effect nodes
let delayNode = null;
let delayGain = null;
let delayFilter = null;
// Vinyl crackle
let crackleSource = null;
let crackleGain = null;
// Engine thrust sound
let engineOsc = null;
let engineGain = null;
let engineFilter = null;

// === Music Styles (Japanese Game-Inspired) ===
const musicStyles = [
    { 
        name: 'MEGA BUSTER', bpm: 145, // Mega Man style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [1,3,5,7],
        bass: [65.41, 82.41, 73.42, 82.41], // E2-F#2-D#2-F#2 (energetic)
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 523.25, 587.33, 659.25], // E4 Phrygian
        arp: [0, 2, 4, 7, 4, 2], // Fast rising arpeggio (Capcom style)
        melody: [0, 2, 4, 2, 0, 4, 7, 4], // Catchy hook
        chords: [[0,2,4], [1,3,5], [2,4,6], [0,2,4]], // Power chords
        vibe: 'heroic',
        kickDecay: 0.25, snareDecay: 0.08, bassType: 'square',
        melodyOct: 1, // Melody octave multiplier
        // MEGA MAN / CAPCOM: Fast, energetic, memorable melodies
        // - Square wave bass (NES/Famicom sound)
        // - Fast arpeggios (technical, energetic)
        // - Driving 4/4 beat
        // - Catchy melodic hooks
    },
    { 
        name: 'GRADIUS CORE', bpm: 160, // Gradius/Konami style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [0,2,4,6],
        bass: [55.00, 73.42, 82.41, 73.42], // A1-D#2-F#2-D#2
        scale: [220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25], // A Minor
        arp: [0, 4, 7, 4, 0, 4, 7, 4], // Octave jumps (intense)
        melody: [7, 5, 4, 2, 4, 5, 7, 0], // Descending heroic
        chords: [[0,4,7], [2,5,0], [4,7,2], [0,4,7]], // Minor triads
        vibe: 'intense',
        kickDecay: 0.22, snareDecay: 0.07, bassType: 'square',
        melodyOct: 2,
        // GRADIUS / KONAMI: Intense, technical, fast-paced
        // - High-energy driving rhythm
        // - Dramatic octave jumps
        // - Minor key intensity
        // - Relentless forward motion
    },
    { 
        name: 'BUBBLE SYSTEM', bpm: 130, // Bubble Bobble/Taito style
        kick: [0, 4], snare: [2, 6], 
        hat: [0,2,4,6], hatAccent: [2,6],
        bass: [130.81, 146.83, 164.81, 130.81], // C3-D3-E3-C3
        scale: [261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25], // C Major
        arp: [0, 2, 4, 5, 4, 2], // Bouncy arpeggio
        melody: [0, 2, 4, 5, 4, 2, 0, 4], // Cheerful melody
        chords: [[0,2,4], [3,5,7], [0,2,4], [1,3,5]], // Major progressions
        vibe: 'cheerful',
        kickDecay: 0.35, snareDecay: 0.12, bassType: 'sine',
        melodyOct: 1,
        // BUBBLE BOBBLE / TAITO: Cute, bouncy, catchy
        // - Major key brightness
        // - Cheerful melodies
        // - Moderate tempo
        // - Playful atmosphere
    },
    { 
        name: 'CASTLEVANIA', bpm: 140, // Castlevania style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [1,3,5,7], hatAccent: [3,7],
        bass: [55.00, 61.74, 65.41, 55.00], // A1-B1-C2-A1
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 349.23, 415.30], // A Harmonic Minor
        arp: [0, 2, 4, 6, 4, 2], // Gothic arpeggio
        melody: [0, 6, 5, 4, 2, 0, 4, 2], // Dramatic descending
        chords: [[0,2,4], [5,0,2], [4,6,1], [0,2,4]], // Harmonic minor
        vibe: 'gothic',
        kickDecay: 0.3, snareDecay: 0.1, bassType: 'sawtooth',
        melodyOct: 1,
        // CASTLEVANIA / KONAMI: Dark, dramatic, epic
        // - Harmonic minor scale (gothic feel)
        // - Dramatic melodies
        // - Heavy rhythm
        // - Epic atmosphere
    },
    { 
        name: 'STREET FIGHTER', bpm: 155, // Street Fighter style
        kick: [0, 3, 4, 7], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [1,5],
        bass: [82.41, 98.00, 82.41, 73.42], // F#2-G2-F#2-D#2
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 523.25, 587.33], // E Phrygian Dominant
        arp: [0, 3, 5, 7, 5, 3], // Fighting spirit
        melody: [7, 5, 3, 0, 3, 5, 7, 0], // Battle cry
        chords: [[0,3,5], [2,5,0], [4,7,2], [0,3,5]], // Power progressions
        vibe: 'battle',
        kickDecay: 0.28, snareDecay: 0.09, bassType: 'square',
        melodyOct: 1,
        // STREET FIGHTER / CAPCOM: Energetic, fighting spirit
        // - Syncopated rhythms
        // - Phrygian dominant (exotic/intense)
        // - Powerful driving beat
        // - Competitive energy
    },
    { 
        name: 'SONIC SPEED', bpm: 170, // Sonic the Hedgehog style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [0,2,4,6],
        bass: [110.00, 123.47, 130.81, 110.00], // A2-B2-C3-A2
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 392.00, 440.00], // A Natural Minor
        arp: [0, 2, 4, 5, 7, 5, 4, 2], // Fast runs (Sonic speed)
        melody: [0, 4, 7, 5, 4, 2, 0, 7], // Uplifting
        chords: [[0,2,4], [3,5,7], [5,0,2], [0,2,4]], // Natural minor
        vibe: 'speed',
        kickDecay: 0.2, snareDecay: 0.06, bassType: 'sawtooth',
        melodyOct: 2,
        // SONIC / SEGA: Fast, upbeat, adventurous
        // - Very fast tempo (170 BPM)
        // - Continuous hi-hats (speed sensation)
        // - Uplifting melodies
        // - Natural minor (adventurous)
    },
    { 
        name: 'R-TYPE FORCE', bpm: 148, // R-Type style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [1,3,5,7],
        bass: [55.00, 65.41, 73.42, 65.41], // A1-C2-D#2-C2
        scale: [220.00, 246.94, 293.66, 329.63, 369.99, 415.30, 440.00], // A Aeolian
        arp: [0, 4, 7, 4, 0, 2, 5, 2], // Sci-fi arpeggio
        melody: [0, 2, 4, 7, 5, 4, 2, 0], // Space opera
        chords: [[0,4,7], [2,5,0], [4,7,2], [5,0,4]], // Sci-fi progression
        vibe: 'scifi',
        kickDecay: 0.25, snareDecay: 0.08, bassType: 'square',
        melodyOct: 1,
        // R-TYPE / IREM: Sci-fi, atmospheric, technical
        // - Space shooter intensity
        // - Technical precision
        // - Sci-fi atmosphere
        // - Relentless energy
    },
    { 
        name: 'TOUHOU PROJECT', bpm: 165, // Touhou/ZUN style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [0,4],
        bass: [65.41, 73.42, 82.41, 73.42], // C2-D#2-F#2-D#2
        scale: [261.63, 293.66, 329.63, 392.00, 440.00, 493.88, 523.25, 587.33, 659.25], // C Major extended
        arp: [0, 2, 4, 7, 8, 7, 4, 2], // Dense arpeggio runs
        melody: [0, 4, 7, 8, 7, 4, 2, 0], // Intricate melody
        chords: [[0,2,4], [2,4,6], [4,6,8], [0,2,4]], // Dense progressions
        vibe: 'bullet-hell',
        kickDecay: 0.23, snareDecay: 0.07, bassType: 'square',
        melodyOct: 2,
        // TOUHOU / ZUN: Dense, intricate, bullet-hell intensity
        // - Very fast melodic runs
        // - Dense harmonic layering
        // - High energy
        // - Extended scales
    },
    { 
        name: 'FINAL FANTASY', bpm: 125, // Final Fantasy battle style
        kick: [0, 4], snare: [2, 6], 
        hat: [0,2,4,6], hatAccent: [2,6],
        bass: [110.00, 130.81, 146.83, 110.00], // A2-C3-D3-A2
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 392.00, 440.00, 493.88], // A Minor
        arp: [0, 2, 4, 5, 7, 5, 4, 2], // Epic arpeggio
        melody: [7, 5, 4, 2, 0, 2, 4, 5], // Heroic theme
        chords: [[0,2,4], [5,7,2], [4,6,0], [0,2,4]], // Epic progressions
        vibe: 'epic',
        kickDecay: 0.4, snareDecay: 0.15, bassType: 'sawtooth',
        melodyOct: 1,
        // FINAL FANTASY / SQUARE: Epic, orchestral-inspired, heroic
        // - Moderate tempo (battle feel)
        // - Epic melodic themes
        // - Rich harmonic progressions
        // - Heroic atmosphere
    },
    { 
        name: 'METAL SLUG', bpm: 152, // Metal Slug style
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [1,3,5,7],
        bass: [82.41, 98.00, 110.00, 82.41], // F#2-G2-A2-F#2
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 587.33], // E Mixolydian
        arp: [0, 2, 4, 5, 4, 2], // Military march
        melody: [0, 2, 4, 0, 5, 4, 2, 0], // Action theme
        chords: [[0,2,4], [3,5,0], [0,2,4], [1,3,5]], // Rock progressions
        vibe: 'military',
        kickDecay: 0.27, snareDecay: 0.09, bassType: 'square',
        melodyOct: 1,
        // METAL SLUG / SNK: Action-packed, military, energetic
        // - Fast military rhythm
        // - Mixolydian mode (heroic/bright)
        // - Action-oriented
        // - Arcade intensity
    },
];
currentStyle = 0; // Start with MEGA BUSTER (index 0)

// === Entities ===
const player = { x: 400, y: 500, size: 30, speed: 5, missileCooldown: 0, lastShotTime: 0, trail: [] };
let bullets = [], enemies = [], enemyBullets = [], powerUps = [], missiles = [], particles = [];
let stars = [], debris = [];
// Parallax background layers
let nebulaClouds = [], distantStars = [], spaceDust = [];
const keys = {};

// === Initialize Background ===
function initBackground() {
    stars = []; debris = [];
    nebulaClouds = []; distantStars = []; spaceDust = [];
    
    // Layer 1: Nebula clouds (slowest, farthest - large glowing blobs)
    for (let i = 0; i < 8; i++) {
        nebulaClouds.push({
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            speed: 0.05 + Math.random() * 0.1, // Very slow
            size: 80 + Math.random() * 120, // Large
            color: ['#1e3a8a', '#312e81', '#581c87', '#701a75'][Math.floor(Math.random() * 4)], // Deep blue/purple
            alpha: 0.15 + Math.random() * 0.15 // Semi-transparent
        });
    }
    
    // Layer 2: Distant stars (medium speed - tiny bright dots)
    for (let i = 0; i < 120; i++) {
        distantStars.push({
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            speed: 0.2 + Math.random() * 0.3,
            size: 0.5 + Math.random() * 1, // Tiny
            twinkle: Math.random() * Math.PI * 2 // For twinkling effect
        });
    }
    
    // Layer 3: Space dust (fastest, closest - medium particles)
    for (let i = 0; i < 30; i++) {
        spaceDust.push({
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            speed: 1.0 + Math.random() * 2.0, // Fast
            size: 1 + Math.random() * 2,
            alpha: 0.3 + Math.random() * 0.4
        });
    }
    
    // Existing stars (keep for compatibility)
    for (let i = 0; i < 50; i++) {
        stars.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, speed: 0.5 + Math.random() * 1.5, size: Math.random() * 2 });
    }
    for (let i = 0; i < 8; i++) {
        debris.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, speed: 0.2 + Math.random() * 0.3, size: 5 + Math.random() * 15, rotation: Math.random() * Math.PI * 2, rotSpeed: (Math.random() - 0.5) * 0.02, shape: Math.random() > 0.5 ? 'beam' : 'panel' });
    }
}

function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    player.x = canvas.width / 2;
    player.y = canvas.height - 60;
    initBackground();
}
window.addEventListener('resize', resize);
resize();

// === Audio System (Enhanced with reverb, compression, better sound design) ===
function initAudio() {
    if (audioCtx) {
        if (audioCtx.state === 'suspended') audioCtx.resume();
        return;
    }
    try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        
        // Master chain: source -> compressor -> masterGain -> destination
        compressor = audioCtx.createDynamicsCompressor();
        compressor.threshold.value = -12;
        compressor.knee.value = 10;
        compressor.ratio.value = 8;
        compressor.attack.value = 0.005;
        compressor.release.value = 0.1;
        
        masterGain = audioCtx.createGain();
        masterGain.gain.value = 0.5;
        
        // Simple reverb using convolver with generated impulse
        reverbNode = audioCtx.createConvolver();
        reverbGain = audioCtx.createGain();
        reverbGain.gain.value = 0.15;
        
        // Generate impulse response for reverb (0.8 seconds)
        const reverbTime = 0.8;
        const reverbLength = audioCtx.sampleRate * reverbTime;
        const impulse = audioCtx.createBuffer(2, reverbLength, audioCtx.sampleRate);
        for (let ch = 0; ch < 2; ch++) {
            const data = impulse.getChannelData(ch);
            for (let i = 0; i < reverbLength; i++) {
                data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / reverbLength, 2.5);
            }
        }
        reverbNode.buffer = impulse;
        
        // Sidechain gain node (for ducking non-kick elements)
        sidechainGain = audioCtx.createGain();
        sidechainGain.gain.value = 1.0;
        
        // Ping-pong style delay with filtering (simulates pitch-shift via detune)
        delayNode = audioCtx.createDelay(1.0);
        delayNode.delayTime.value = 0.375; // Dotted eighth at ~80bpm, adjusts per style
        delayGain = audioCtx.createGain();
        delayGain.gain.value = 0.25; // Feedback/wet amount
        delayFilter = audioCtx.createBiquadFilter();
        delayFilter.type = 'lowpass';
        delayFilter.frequency.value = 2000; // Darker repeats
        
        // Routing: sources -> compressor -> sidechainGain -> masterGain -> destination
        // Kick bypasses sidechain, other elements go through it
        compressor.connect(sidechainGain);
        sidechainGain.connect(masterGain);
        masterGain.connect(audioCtx.destination);
        masterGain.connect(reverbNode);
        reverbNode.connect(reverbGain);
        reverbGain.connect(audioCtx.destination);
        
        // Delay send from sidechain (affected elements get delay)
        sidechainGain.connect(delayNode);
        delayNode.connect(delayFilter);
        delayFilter.connect(delayGain);
        delayGain.connect(masterGain);
        // Feedback loop (filtered)
        delayGain.connect(delayNode);
        
        // Create noise buffer for snare/hats/explosion
        const bufferSize = audioCtx.sampleRate * 2;
        noiseBuffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
        const output = noiseBuffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) output[i] = Math.random() * 2 - 1;
        
        // Vinyl crackle texture (continuous, very subtle)
        startCrackle();
        
    } catch (e) { console.error('Audio init failed', e); }
}

function startCrackle() {
    if (!audioCtx || !noiseBuffer) return;
    try {
        // Create crackle source (looping noise)
        crackleSource = audioCtx.createBufferSource();
        crackleSource.buffer = noiseBuffer;
        crackleSource.loop = true;
        
        // Heavy filtering for vinyl character
        const crackleHP = audioCtx.createBiquadFilter();
        crackleHP.type = 'highpass';
        crackleHP.frequency.value = 1000;
        
        const crackleLP = audioCtx.createBiquadFilter();
        crackleLP.type = 'lowpass';
        crackleLP.frequency.value = 4000;
        
        crackleGain = audioCtx.createGain();
        crackleGain.gain.value = 0.015; // Very subtle
        
        crackleSource.connect(crackleHP);
        crackleHP.connect(crackleLP);
        crackleLP.connect(crackleGain);
        crackleGain.connect(masterGain);
        
        crackleSource.start();
        
        // Engine thrust sound (continuous low rumble)
        engineOsc = audioCtx.createOscillator();
        engineFilter = audioCtx.createBiquadFilter();
        engineGain = audioCtx.createGain();
        
        engineOsc.type = 'sawtooth';
        engineOsc.frequency.value = 60; // Low rumble
        
        engineFilter.type = 'lowpass';
        engineFilter.frequency.value = 200;
        engineFilter.Q.value = 2;
        
        engineGain.gain.value = 0; // Start silent, will modulate based on movement
        
        engineOsc.connect(engineFilter);
        engineFilter.connect(engineGain);
        engineGain.connect(masterGain);
        
        engineOsc.start();
    } catch (e) {}
}

// Throttled sound player with enhanced sound design
function playSound(type) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    const now = Date.now();
    const minInterval = { 
        shoot: 80, missile: 150, explosion: 120, powerup: 100, 'boss-shoot': 180, hit: 50,
        'zerg-death': 100, 'protoss-death': 100, 'tank-explosion': 120, 'boss-death': 500
    }[type] || 100;
    if (lastSoundTime[type] && now - lastSoundTime[type] < minInterval) return;
    lastSoundTime[type] = now;
    
    try {
        const t = audioCtx.currentTime;
        const style = musicStyles[currentStyle];
        
        if (type === 'shoot') {
            // Layered shot: sine + noise transient
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            const filter = audioCtx.createBiquadFilter();
            
            // Harmonized to scale
            const note = style.scale[style.arp[beatStep % 4]];
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(note * 2, t);
            osc.frequency.exponentialRampToValueAtTime(note, t + 0.06);
            filter.type = 'bandpass';
            filter.frequency.value = note * 4;
            filter.Q.value = 2;
            gain.gain.setValueAtTime(0.07, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.06);
            osc.connect(filter).connect(gain).connect(compressor);
            osc.start(t); osc.stop(t + 0.06);
            
            // Add melody note to queue occasionally
            if (Math.random() < 0.15 && melodyQueue.length < 4) {
                melodyQueue.push(note);
            }
        } 
        else if (type === 'missile') {
            // Enhanced missile: rising sweep with whoosh + sub
            // Whoosh (wind-like noise sweep)
            if (noiseBuffer) {
                const whoosh = audioCtx.createBufferSource();
                const whooshGain = audioCtx.createGain();
                const whooshFilter = audioCtx.createBiquadFilter();
                
                whoosh.buffer = noiseBuffer;
                whooshFilter.type = 'highpass';
                whooshFilter.frequency.setValueAtTime(1000, t);
                whooshFilter.frequency.exponentialRampToValueAtTime(4000, t + 0.3);
                whooshGain.gain.setValueAtTime(0.08, t);
                whooshGain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
                
                whoosh.connect(whooshFilter).connect(whooshGain).connect(compressor);
                whoosh.start(t);
                whoosh.stop(t + 0.3);
            }
            
            // Rising sweep
            const osc1 = audioCtx.createOscillator();
            const osc2 = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            
            osc1.type = 'sawtooth';
            osc1.frequency.setValueAtTime(style.bass[0], t);
            osc1.frequency.exponentialRampToValueAtTime(style.scale[2], t + 0.25);
            
            osc2.type = 'sine';
            osc2.frequency.setValueAtTime(style.bass[0] / 2, t);
            
            gain.gain.setValueAtTime(0.06, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
            
            osc1.connect(gain);
            osc2.connect(gain);
            gain.connect(compressor);
            osc1.start(t); osc1.stop(t + 0.25);
            osc2.start(t); osc2.stop(t + 0.25);
        } 
        else if (type === 'explosion' && noiseBuffer) {
            // ENHANCED MASSIVE EXPLOSION: multi-layered with impact
            
            // Layer 1: White noise burst (high-frequency shrapnel)
            const src = audioCtx.createBufferSource();
            const noiseGain = audioCtx.createGain();
            const filter = audioCtx.createBiquadFilter();
            
            src.buffer = noiseBuffer;
            filter.type = 'lowpass';
            filter.frequency.setValueAtTime(3000, t);
            filter.frequency.exponentialRampToValueAtTime(60, t + 0.5);
            noiseGain.gain.setValueAtTime(0.3, t);
            noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
            src.connect(filter).connect(noiseGain).connect(compressor);
            src.start(t); src.stop(t + 0.5);
            
            // Layer 2: Sub bass thump (impact)
            const sub = audioCtx.createOscillator();
            const subGain = audioCtx.createGain();
            sub.type = 'sine';
            sub.frequency.setValueAtTime(100, t);
            sub.frequency.exponentialRampToValueAtTime(25, t + 0.4);
            subGain.gain.setValueAtTime(0.4, t);
            subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
            sub.connect(subGain).connect(masterGain); // Direct to master for max impact
            sub.start(t); sub.stop(t + 0.4);
            
            // Layer 3: Mid-range crunch (debris)
            const crunch = audioCtx.createOscillator();
            const crunchGain = audioCtx.createGain();
            const crunchFilter = audioCtx.createBiquadFilter();
            crunch.type = 'square';
            crunch.frequency.setValueAtTime(200, t);
            crunch.frequency.exponentialRampToValueAtTime(50, t + 0.3);
            crunchFilter.type = 'bandpass';
            crunchFilter.frequency.value = 400;
            crunchFilter.Q.value = 3;
            crunchGain.gain.setValueAtTime(0.2, t);
            crunchGain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
            crunch.connect(crunchFilter).connect(crunchGain).connect(compressor);
            crunch.start(t); crunch.stop(t + 0.3);
            
            // Layer 4: Click transient (initial impact)
            const click = audioCtx.createOscillator();
            const clickGain = audioCtx.createGain();
            click.type = 'square';
            click.frequency.value = 1500;
            clickGain.gain.setValueAtTime(0.25, t);
            clickGain.gain.exponentialRampToValueAtTime(0.001, t + 0.02);
            click.connect(clickGain).connect(compressor);
            click.start(t); click.stop(t + 0.02);
            
            // Trigger arpeggio on kill
            if (melodyQueue.length < 6) {
                const arp = style.arp;
                melodyQueue.push(style.scale[arp[0]], style.scale[arp[1]], style.scale[arp[2]]);
            }
        } 
        else if (type === 'powerup') {
            // Rising arpeggio
            for (let i = 0; i < 4; i++) {
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                const noteTime = t + i * 0.08;
                osc.type = 'sine';
                osc.frequency.setValueAtTime(style.scale[i % style.scale.length], noteTime);
                gain.gain.setValueAtTime(0, noteTime);
                gain.gain.linearRampToValueAtTime(0.08, noteTime + 0.02);
                gain.gain.exponentialRampToValueAtTime(0.001, noteTime + 0.15);
                osc.connect(gain).connect(compressor);
                osc.start(noteTime); osc.stop(noteTime + 0.15);
            }
        } 
        else if (type === 'boss-shoot') {
            // Menacing low growl
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            const filter = audioCtx.createBiquadFilter();
            
            osc.type = 'sawtooth';
            osc.frequency.setValueAtTime(110, t);
            osc.frequency.exponentialRampToValueAtTime(55, t + 0.2);
            filter.type = 'lowpass';
            filter.frequency.setValueAtTime(800, t);
            filter.frequency.exponentialRampToValueAtTime(200, t + 0.2);
            gain.gain.setValueAtTime(0.08, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
            osc.connect(filter).connect(gain).connect(compressor);
            osc.start(t); osc.stop(t + 0.2);
        }
        else if (type === 'hit') {
            // Short click for bullet hits
            const osc = audioCtx.createOscillator();
            const gain = audioCtx.createGain();
            osc.type = 'square';
            osc.frequency.setValueAtTime(style.scale[Math.floor(Math.random() * 3)] * 2, t);
            gain.gain.setValueAtTime(0.04, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.03);
            osc.connect(gain).connect(compressor);
            osc.start(t); osc.stop(t + 0.03);
        }
        else if (type === 'transition') {
            // Style change transition: sweep + impact
            // Rising sweep
            const sweepOsc = audioCtx.createOscillator();
            const sweepGain = audioCtx.createGain();
            const sweepFilter = audioCtx.createBiquadFilter();
            
            sweepOsc.type = 'sawtooth';
            sweepOsc.frequency.setValueAtTime(100, t);
            sweepOsc.frequency.exponentialRampToValueAtTime(2000, t + 0.3);
            
            sweepFilter.type = 'lowpass';
            sweepFilter.frequency.setValueAtTime(200, t);
            sweepFilter.frequency.exponentialRampToValueAtTime(4000, t + 0.3);
            sweepFilter.Q.value = 8;
            
            sweepGain.gain.setValueAtTime(0.1, t);
            sweepGain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
            
            sweepOsc.connect(sweepFilter).connect(sweepGain).connect(compressor);
            sweepOsc.start(t); sweepOsc.stop(t + 0.35);
            
            // White noise burst at peak
            if (noiseBuffer) {
                const noiseSrc = audioCtx.createBufferSource();
                const noiseGain = audioCtx.createGain();
                const noiseFilter = audioCtx.createBiquadFilter();
                
                noiseSrc.buffer = noiseBuffer;
                noiseFilter.type = 'bandpass';
                noiseFilter.frequency.value = 3000;
                noiseFilter.Q.value = 1;
                
                noiseGain.gain.setValueAtTime(0, t);
                noiseGain.gain.linearRampToValueAtTime(0.15, t + 0.25);
                noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
                
                noiseSrc.connect(noiseFilter).connect(noiseGain).connect(compressor);
                noiseSrc.start(t); noiseSrc.stop(t + 0.4);
            }
            
            // Sub drop after sweep
            const subDrop = audioCtx.createOscillator();
            const subDropGain = audioCtx.createGain();
            subDrop.type = 'sine';
            subDrop.frequency.setValueAtTime(120, t + 0.3);
            subDrop.frequency.exponentialRampToValueAtTime(30, t + 0.6);
            subDropGain.gain.setValueAtTime(0.3, t + 0.3);
            subDropGain.gain.exponentialRampToValueAtTime(0.001, t + 0.6);
            subDrop.connect(subDropGain).connect(masterGain);
            subDrop.start(t + 0.3); subDrop.stop(t + 0.6);
        }
        else if (type === 'combo-3x') {
            // 3x combo: Rising cheerful chord
            const frequencies = [style.scale[0], style.scale[2], style.scale[4]]; // Triad
            frequencies.forEach((freq, i) => {
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, t + i * 0.05);
                gain.gain.setValueAtTime(0, t + i * 0.05);
                gain.gain.linearRampToValueAtTime(0.15, t + i * 0.05 + 0.02);
                gain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.05 + 0.3);
                osc.connect(gain).connect(compressor);
                osc.start(t + i * 0.05);
                osc.stop(t + i * 0.05 + 0.3);
            });
        }
        else if (type === 'combo-5x') {
            // 5x combo: Major chord + shimmer
            const frequencies = [style.scale[0], style.scale[2], style.scale[4], style.scale[6] || style.scale[4]];
            frequencies.forEach((freq, i) => {
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                const filter = audioCtx.createBiquadFilter();
                filter.type = 'bandpass';
                filter.frequency.value = freq * 2;
                filter.Q.value = 3;
                osc.type = 'sine';
                osc.frequency.setValueAtTime(freq, t + i * 0.04);
                gain.gain.setValueAtTime(0, t + i * 0.04);
                gain.gain.linearRampToValueAtTime(0.18, t + i * 0.04 + 0.02);
                gain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.04 + 0.4);
                osc.connect(filter).connect(gain).connect(compressor);
                osc.start(t + i * 0.04);
                osc.stop(t + i * 0.04 + 0.4);
            });
            // Bright shimmer on top
            const shimmer = audioCtx.createOscillator();
            const shimmerGain = audioCtx.createGain();
            shimmer.type = 'sine';
            shimmer.frequency.setValueAtTime(style.scale[style.scale.length - 1] * 2, t);
            shimmerGain.gain.setValueAtTime(0.1, t);
            shimmerGain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
            shimmer.connect(shimmerGain).connect(compressor);
            shimmer.start(t);
            shimmer.stop(t + 0.5);
        }
        else if (type === 'combo-10x') {
            // 10x combo: Epic fanfare with rising arp + sub bass
            // Sub bass hit
            const sub = audioCtx.createOscillator();
            const subGain = audioCtx.createGain();
            sub.type = 'sine';
            sub.frequency.setValueAtTime(style.bass[0] / 2, t);
            subGain.gain.setValueAtTime(0.35, t);
            subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.6);
            sub.connect(subGain).connect(compressor);
            sub.start(t);
            sub.stop(t + 0.6);
            
            // Rising arpeggio
            for (let i = 0; i < style.scale.length; i++) {
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                const filter = audioCtx.createBiquadFilter();
                filter.type = 'bandpass';
                filter.frequency.value = style.scale[i] * 3;
                filter.Q.value = 5;
                osc.type = 'sawtooth';
                osc.frequency.setValueAtTime(style.scale[i], t + i * 0.06);
                gain.gain.setValueAtTime(0, t + i * 0.06);
                gain.gain.linearRampToValueAtTime(0.2, t + i * 0.06 + 0.01);
                gain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.06 + 0.4);
                osc.connect(filter).connect(gain).connect(compressor);
                osc.start(t + i * 0.06);
                osc.stop(t + i * 0.06 + 0.4);
            }
        }
        else if (type === 'combo-15x' || type === 'combo-20x') {
            // 15x/20x combo: ULTRA fanfare - massive impact
            // Massive sub drop
            const sub = audioCtx.createOscillator();
            const subGain = audioCtx.createGain();
            sub.type = 'sine';
            sub.frequency.setValueAtTime(style.bass[0] / 2, t);
            sub.frequency.exponentialRampToValueAtTime(style.bass[0] / 4, t + 0.8);
            subGain.gain.setValueAtTime(0.5, t);
            subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.8);
            sub.connect(subGain).connect(masterGain);
            sub.start(t);
            sub.stop(t + 0.8);
            
            // Explosion of notes (all scale notes at once)
            style.scale.forEach((freq, i) => {
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                const filter = audioCtx.createBiquadFilter();
                filter.type = 'bandpass';
                filter.frequency.setValueAtTime(freq * 4, t);
                filter.frequency.exponentialRampToValueAtTime(freq * 2, t + 0.6);
                filter.Q.value = 8;
                osc.type = type === 'combo-20x' ? 'square' : 'sawtooth';
                osc.frequency.setValueAtTime(freq * (1 + i * 0.002), t); // Slight detune for richness
                gain.gain.setValueAtTime(0.15, t);
                gain.gain.exponentialRampToValueAtTime(0.001, t + 0.7);
                osc.connect(filter).connect(gain).connect(compressor);
                osc.start(t);
                osc.stop(t + 0.7);
            });
            
            // White noise burst
            if (noiseBuffer) {
                const noiseSrc = audioCtx.createBufferSource();
                const noiseGain = audioCtx.createGain();
                const noiseFilter = audioCtx.createBiquadFilter();
                noiseFilter.type = 'highpass';
                noiseFilter.frequency.value = 2000;
                noiseSrc.buffer = noiseBuffer;
                noiseGain.gain.setValueAtTime(0.3, t);
                noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
                noiseSrc.connect(noiseFilter).connect(noiseGain).connect(compressor);
                noiseSrc.start(t);
                noiseSrc.stop(t + 0.3);
            }
        }
        else if (type === 'boss-warning') {
            // Boss warning: ominous siren-like sound
            for (let i = 0; i < 3; i++) {
                const siren = audioCtx.createOscillator();
                const sirenGain = audioCtx.createGain();
                const sirenFilter = audioCtx.createBiquadFilter();
                
                siren.type = 'sawtooth';
                siren.frequency.setValueAtTime(440, t + i * 0.4);
                siren.frequency.linearRampToValueAtTime(220, t + i * 0.4 + 0.2);
                siren.frequency.linearRampToValueAtTime(440, t + i * 0.4 + 0.4);
                
                sirenFilter.type = 'bandpass';
                sirenFilter.frequency.value = 800;
                sirenFilter.Q.value = 5;
                
                sirenGain.gain.setValueAtTime(0.2, t + i * 0.4);
                sirenGain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.4 + 0.4);
                
                siren.connect(sirenFilter).connect(sirenGain).connect(compressor);
                siren.start(t + i * 0.4);
                siren.stop(t + i * 0.4 + 0.4);
            }
        }
        else if (type === 'game-over') {
            // Game over: descending "wah wah" sound
            const gameOverOsc = audioCtx.createOscillator();
            const gameOverGain = audioCtx.createGain();
            const gameOverFilter = audioCtx.createBiquadFilter();
            
            gameOverOsc.type = 'sawtooth';
            gameOverOsc.frequency.setValueAtTime(440, t);
            gameOverOsc.frequency.exponentialRampToValueAtTime(110, t + 1.5);
            
            gameOverFilter.type = 'lowpass';
            gameOverFilter.frequency.setValueAtTime(2000, t);
            gameOverFilter.frequency.exponentialRampToValueAtTime(200, t + 1.5);
            gameOverFilter.Q.value = 8;
            
            gameOverGain.gain.setValueAtTime(0.25, t);
            gameOverGain.gain.setValueAtTime(0.2, t + 0.5);
            gameOverGain.gain.exponentialRampToValueAtTime(0.001, t + 1.5);
            
            gameOverOsc.connect(gameOverFilter).connect(gameOverGain).connect(compressor);
            gameOverOsc.start(t);
            gameOverOsc.stop(t + 1.5);
        }
        else if (type === 'whoosh') {
            // Generic whoosh sound (for power-up drops, etc.)
            if (noiseBuffer) {
                const whoosh = audioCtx.createBufferSource();
                const whooshGain = audioCtx.createGain();
                const whooshFilter = audioCtx.createBiquadFilter();
                
                whoosh.buffer = noiseBuffer;
                whooshFilter.type = 'bandpass';
                whooshFilter.frequency.setValueAtTime(800, t);
                whooshFilter.frequency.exponentialRampToValueAtTime(2000, t + 0.2);
                whooshFilter.Q.value = 3;
                whooshGain.gain.setValueAtTime(0.06, t);
                whooshGain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
                
                whoosh.connect(whooshFilter).connect(whooshGain).connect(compressor);
                whoosh.start(t);
                whoosh.stop(t + 0.2);
            }
        }
        else if (type === 'zerg-death' && noiseBuffer) {
            // Zerg death: Wet splatter + organic squelch
            const splat = audioCtx.createBufferSource();
            const splatGain = audioCtx.createGain();
            const splatFilter = audioCtx.createBiquadFilter();
            
            splat.buffer = noiseBuffer;
            splatFilter.type = 'lowpass';
            splatFilter.frequency.setValueAtTime(800, t);
            splatFilter.frequency.exponentialRampToValueAtTime(150, t + 0.25);
            splatFilter.Q.value = 5;
            splatGain.gain.setValueAtTime(0.25, t);
            splatGain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
            splat.connect(splatFilter).connect(splatGain).connect(compressor);
            splat.start(t); splat.stop(t + 0.25);
            
            // Organic squelch (low frequency wobble)
            const squelch = audioCtx.createOscillator();
            const squelchGain = audioCtx.createGain();
            squelch.type = 'sawtooth';
            squelch.frequency.setValueAtTime(180, t);
            squelch.frequency.exponentialRampToValueAtTime(40, t + 0.3);
            squelchGain.gain.setValueAtTime(0.2, t);
            squelchGain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
            squelch.connect(squelchGain).connect(compressor);
            squelch.start(t); squelch.stop(t + 0.3);
        }
        else if (type === 'protoss-death') {
            // Protoss death: Crystalline shatter + energy dissipation
            // Crystal shatter (high-frequency burst)
            for (let i = 0; i < 5; i++) {
                const crystal = audioCtx.createOscillator();
                const crystalGain = audioCtx.createGain();
                const freq = 800 + Math.random() * 1200;
                crystal.type = 'sine';
                crystal.frequency.setValueAtTime(freq, t + i * 0.02);
                crystal.frequency.exponentialRampToValueAtTime(freq * 0.5, t + i * 0.02 + 0.15);
                crystalGain.gain.setValueAtTime(0.08, t + i * 0.02);
                crystalGain.gain.exponentialRampToValueAtTime(0.001, t + i * 0.02 + 0.15);
                crystal.connect(crystalGain).connect(compressor);
                crystal.start(t + i * 0.02);
                crystal.stop(t + i * 0.02 + 0.15);
            }
            
            // Energy dissipation (descending harmonic)
            const energy = audioCtx.createOscillator();
            const energyGain = audioCtx.createGain();
            energy.type = 'triangle';
            energy.frequency.setValueAtTime(440, t);
            energy.frequency.exponentialRampToValueAtTime(110, t + 0.4);
            energyGain.gain.setValueAtTime(0.15, t);
            energyGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
            energy.connect(energyGain).connect(compressor);
            energy.start(t); energy.stop(t + 0.4);
        }
        else if (type === 'tank-explosion' && noiseBuffer) {
            // Tank explosion: Massive metallic crash + low boom
            const crash = audioCtx.createBufferSource();
            const crashGain = audioCtx.createGain();
            const crashFilter = audioCtx.createBiquadFilter();
            
            crash.buffer = noiseBuffer;
            crashFilter.type = 'lowpass';
            crashFilter.frequency.setValueAtTime(4000, t);
            crashFilter.frequency.exponentialRampToValueAtTime(100, t + 0.6);
            crashGain.gain.setValueAtTime(0.35, t);
            crashGain.gain.exponentialRampToValueAtTime(0.001, t + 0.6);
            crash.connect(crashFilter).connect(crashGain).connect(compressor);
            crash.start(t); crash.stop(t + 0.6);
            
            // Deep boom (sub bass)
            const boom = audioCtx.createOscillator();
            const boomGain = audioCtx.createGain();
            boom.type = 'sine';
            boom.frequency.setValueAtTime(80, t);
            boom.frequency.exponentialRampToValueAtTime(20, t + 0.5);
            boomGain.gain.setValueAtTime(0.5, t);
            boomGain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
            boom.connect(boomGain).connect(masterGain);
            boom.start(t); boom.stop(t + 0.5);
            
            // Metallic ring
            const ring = audioCtx.createOscillator();
            const ringGain = audioCtx.createGain();
            ring.type = 'sine';
            ring.frequency.value = 300;
            ringGain.gain.setValueAtTime(0.15, t);
            ringGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
            ring.connect(ringGain).connect(compressor);
            ring.start(t); ring.stop(t + 0.4);
        }
        else if (type === 'boss-death' && noiseBuffer) {
            // Boss death: ULTIMATE multi-layered explosion sequence
            // Massive noise burst
            const noise = audioCtx.createBufferSource();
            const noiseGain = audioCtx.createGain();
            const noiseFilter = audioCtx.createBiquadFilter();
            
            noise.buffer = noiseBuffer;
            noiseFilter.type = 'lowpass';
            noiseFilter.frequency.setValueAtTime(5000, t);
            noiseFilter.frequency.exponentialRampToValueAtTime(50, t + 1.0);
            noiseGain.gain.setValueAtTime(0.4, t);
            noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 1.0);
            noise.connect(noiseFilter).connect(noiseGain).connect(compressor);
            noise.start(t); noise.stop(t + 1.0);
            
            // Massive sub bass impact
            const subBass = audioCtx.createOscillator();
            const subGain = audioCtx.createGain();
            subBass.type = 'sine';
            subBass.frequency.setValueAtTime(120, t);
            subBass.frequency.exponentialRampToValueAtTime(15, t + 0.8);
            subGain.gain.setValueAtTime(0.6, t);
            subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.8);
            subBass.connect(subGain).connect(masterGain);
            subBass.start(t); subBass.stop(t + 0.8);
            
            // Distorted mid-range crunch
            const distortion = audioCtx.createOscillator();
            const distGain = audioCtx.createGain();
            distortion.type = 'square';
            distortion.frequency.setValueAtTime(400, t);
            distortion.frequency.exponentialRampToValueAtTime(80, t + 0.6);
            distGain.gain.setValueAtTime(0.3, t);
            distGain.gain.exponentialRampToValueAtTime(0.001, t + 0.6);
            distortion.connect(distGain).connect(compressor);
            distortion.start(t); distortion.stop(t + 0.6);
            
            // Descending energy wail (demonic)
            const wail = audioCtx.createOscillator();
            const wailGain = audioCtx.createGain();
            wail.type = 'sawtooth';
            wail.frequency.setValueAtTime(660, t + 0.2);
            wail.frequency.exponentialRampToValueAtTime(110, t + 1.2);
            wailGain.gain.setValueAtTime(0.25, t + 0.2);
            wailGain.gain.exponentialRampToValueAtTime(0.001, t + 1.2);
            wail.connect(wailGain).connect(compressor);
            wail.start(t + 0.2); wail.stop(t + 1.2);
        }
    } catch (e) {}
}

// Play piano note based on Y position (for particles)
function playPianoNote(yPosition, intensity = 0.03) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    try {
        const t = audioCtx.currentTime;
        
        // Map Y position to piano note (top = high notes, bottom = low notes)
        const noteIndex = Math.floor((1 - yPosition / canvas.height) * (pianoNotes.length - 1));
        const freq = pianoNotes[Math.max(0, Math.min(noteIndex, pianoNotes.length - 1))];
        
        // Piano-like sound: triangle wave with quick attack and natural decay
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        const filter = audioCtx.createBiquadFilter();
        
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(freq, t);
        
        // Gentle lowpass filter for warmth
        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(freq * 4, t);
        filter.Q.value = 1;
        
        // Piano envelope: quick attack, natural decay
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(intensity, t + 0.01);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.8);
        
        osc.connect(filter).connect(gain).connect(reverbGain); // Send to reverb for space
        osc.start(t);
        osc.stop(t + 0.8);
    } catch (e) {}
}

// BGM tick - called from game loop with full drum machine + melody
function tickBGM() {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    const style = musicStyles[currentStyle];
    const beatInterval = 60000 / style.bpm / 2; // 8th notes
    const now = Date.now();
    
    if (now - lastBeatTime < beatInterval) return;
    lastBeatTime = now;
    
    // Update filter LFO
    filterLFO = Math.sin(beatStep * 0.2) * 0.5 + 0.5;
    
    // Sync delay time to BPM (dotted eighth note = 3/4 of a beat)
    if (delayNode) {
        const beatDuration = 60 / style.bpm;
        delayNode.delayTime.setValueAtTime(beatDuration * 0.75, audioCtx.currentTime);
    }
    
    try {
        const t = audioCtx.currentTime;
        const step = beatStep % 8;
        
        // === BASS LINE with filter sweep (style-specific waveform) ===
        const bassOsc = audioCtx.createOscillator();
        const bassGain = audioCtx.createGain();
        const bassFilter = audioCtx.createBiquadFilter();
        const bassNote = style.bass[Math.floor(beatStep / 2) % 4];
        
        bassOsc.type = style.bassType || 'sawtooth'; // Use style-specific bass type
        bassOsc.frequency.setValueAtTime(bassNote, t);
        bassFilter.type = 'lowpass';
        // Filter sweeps based on LFO and intensity (enhanced automation)
        const intensity = Math.min(score / 500, 1); // 0-1 based on score
        const filterFreq = 150 + filterLFO * 300 + (enemies.length * 10) + intensity * 200;
        bassFilter.frequency.setValueAtTime(filterFreq, t);
        bassFilter.Q.value = 4 + intensity * 4; // More resonance as intensity grows
        bassGain.gain.setValueAtTime(0.14, t);
        bassGain.gain.exponentialRampToValueAtTime(0.001, t + 0.14);
        bassOsc.connect(bassFilter).connect(bassGain).connect(compressor);
        bassOsc.start(t); bassOsc.stop(t + 0.14);
        
        // === SUB-BASS LAYER (pure sine, one octave below) ===
        const subOsc = audioCtx.createOscillator();
        const subGain = audioCtx.createGain();
        subOsc.type = 'sine';
        subOsc.frequency.setValueAtTime(bassNote / 2, t); // One octave below
        subGain.gain.setValueAtTime(0.18, t);
        subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
        subOsc.connect(subGain).connect(compressor); // Goes through sidechain
        subOsc.start(t); subOsc.stop(t + 0.15);
        
        // === MELODY SYSTEM (Japanese game-style catchy hooks) ===
        // Play melody on every 2 beats for catchiness (step % 4 === 0)
        if (step % 4 === 0 && style.melody) {
            const melodyIndex = Math.floor(beatStep / 4) % style.melody.length;
            const melodyNote = style.melody[melodyIndex];
            const freq = style.scale[melodyNote] * (style.melodyOct || 1);
            
            const melody = audioCtx.createOscillator();
            const melodyGain = audioCtx.createGain();
            const melodyFilter = audioCtx.createBiquadFilter();
            
            // Square wave for authentic retro Japanese game sound
            melody.type = 'square';
            melody.frequency.setValueAtTime(freq, t);
            
            melodyFilter.type = 'lowpass';
            melodyFilter.frequency.setValueAtTime(freq * 4, t);
            melodyFilter.frequency.exponentialRampToValueAtTime(freq * 2, t + 0.2);
            melodyFilter.Q.value = 2;
            
            melodyGain.gain.setValueAtTime(0.12, t);
            melodyGain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
            
            melody.connect(melodyFilter).connect(melodyGain).connect(compressor);
            melody.start(t);
            melody.stop(t + 0.25);
        }
        
        // === CHORD PROGRESSION SYSTEM (Japanese game harmony) ===
        // Play chords on beat 1 (every 8 steps) - uses custom chord arrays
        if (step === 0 && style.chords) {
            const chordIndex = Math.floor(beatStep / 8) % style.chords.length;
            const chord = style.chords[chordIndex];
            
            for (let i = 0; i < chord.length; i++) {
                const chordOsc = audioCtx.createOscillator();
                const chordGain = audioCtx.createGain();
                const chordFilter = audioCtx.createBiquadFilter();
                
                const freq = style.scale[chord[i]];
                
                // Sawtooth for rich harmonic content (Japanese game style)
                chordOsc.type = 'sawtooth';
                chordOsc.frequency.setValueAtTime(freq, t);
                
                chordFilter.type = 'lowpass';
                chordFilter.frequency.setValueAtTime(2000 + intensity * 1000, t);
                chordFilter.frequency.exponentialRampToValueAtTime(400, t + 0.4);
                
                chordGain.gain.setValueAtTime(0.08, t);
                chordGain.gain.linearRampToValueAtTime(0.06, t + 0.05);
                chordGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
                
                chordOsc.connect(chordFilter).connect(chordGain).connect(compressor);
                chordOsc.start(t); chordOsc.stop(t + 0.4);
            }
        }
        
        // === KICK DRUM (808 style) with sidechain trigger ===
        if (style.kick.includes(step)) {
            beatKick = 1.0;
            sidechainAmount = 1.0; // Trigger sidechain
            
            // Sidechain ducking envelope on other elements
            if (sidechainGain) {
                sidechainGain.gain.cancelScheduledValues(t);
                sidechainGain.gain.setValueAtTime(0.3, t); // Duck to 30%
                sidechainGain.gain.linearRampToValueAtTime(1.0, t + 0.12); // Release over 120ms
            }
            
            // Pitch envelope - kick goes direct to masterGain (bypasses sidechain)
            const kickOsc = audioCtx.createOscillator();
            const kickGain = audioCtx.createGain();
            const kickDecay = style.kickDecay || 0.35; // Use style-specific decay
            kickOsc.type = 'sine';
            kickOsc.frequency.setValueAtTime(160, t);
            kickOsc.frequency.exponentialRampToValueAtTime(35, t + kickDecay * 0.5);
            kickGain.gain.setValueAtTime(0.65, t);
            kickGain.gain.setValueAtTime(0.55, t + 0.01);
            kickGain.gain.exponentialRampToValueAtTime(0.001, t + kickDecay);
            kickOsc.connect(kickGain).connect(masterGain); // Direct to master (no duck)
            kickOsc.start(t); kickOsc.stop(t + kickDecay);
            
            // Click transient (tighter for faster genres)
            const click = audioCtx.createOscillator();
            const clickGain = audioCtx.createGain();
            click.type = 'square';
            click.frequency.value = 1200;
            clickGain.gain.setValueAtTime(0.12, t);
            clickGain.gain.exponentialRampToValueAtTime(0.001, t + 0.015);
            click.connect(clickGain).connect(masterGain); // Direct to master
            click.start(t); click.stop(t + 0.015);
        }
        
        // === SNARE DRUM ===
        if (style.snare.includes(step) && noiseBuffer) {
            beatSnare = 1.0;
            const snareDecay = style.snareDecay || 0.15; // Use style-specific decay
            
            // Noise body
            const snare = audioCtx.createBufferSource();
            const snareGain = audioCtx.createGain();
            const snareFilter = audioCtx.createBiquadFilter();
            snare.buffer = noiseBuffer;
            snareFilter.type = 'bandpass';
            snareFilter.frequency.value = 3000;
            snareFilter.Q.value = 1;
            snareGain.gain.setValueAtTime(0.2, t);
            snareGain.gain.exponentialRampToValueAtTime(0.001, t + snareDecay);
            snare.connect(snareFilter).connect(snareGain).connect(compressor);
            snare.start(t); snare.stop(t + snareDecay);
            
            // Tone body (punchier for tight snares)
            const snareOsc = audioCtx.createOscillator();
            const snareOscGain = audioCtx.createGain();
            snareOsc.type = 'triangle';
            snareOsc.frequency.setValueAtTime(200, t);
            snareOsc.frequency.exponentialRampToValueAtTime(120, t + snareDecay * 0.4);
            snareOscGain.gain.setValueAtTime(0.14, t);
            snareOscGain.gain.exponentialRampToValueAtTime(0.001, t + snareDecay * 0.6);
            snareOsc.connect(snareOscGain).connect(compressor);
            snareOsc.start(t); snareOsc.stop(t + snareDecay * 0.6);
        }
        
        // === HI-HATS ===
        if (style.hat.includes(step) && noiseBuffer) {
            const isAccent = style.hatAccent.includes(step);
            beatHat = isAccent ? 0.8 : 0.4;
            
            const hat = audioCtx.createBufferSource();
            const hatGain = audioCtx.createGain();
            const hatFilter = audioCtx.createBiquadFilter();
            const hatHP = audioCtx.createBiquadFilter();
            
            hat.buffer = noiseBuffer;
            hatFilter.type = 'bandpass';
            hatFilter.frequency.value = isAccent ? 10000 : 8000;
            hatFilter.Q.value = 2;
            hatHP.type = 'highpass';
            hatHP.frequency.value = 7000;
            
            const vol = isAccent ? 0.08 : 0.04;
            const decay = isAccent ? 0.08 : 0.04;
            hatGain.gain.setValueAtTime(vol, t);
            hatGain.gain.exponentialRampToValueAtTime(0.001, t + decay);
            
            hat.connect(hatHP).connect(hatFilter).connect(hatGain).connect(compressor);
            hat.start(t); hat.stop(t + decay);
        }
        
        // === MELODY from queue (triggered by game events) ===
        if (melodyQueue.length > 0 && step % 2 === 0) {
            const noteFreq = melodyQueue.shift();
            const melOsc = audioCtx.createOscillator();
            const melGain = audioCtx.createGain();
            const melFilter = audioCtx.createBiquadFilter();
            
            // Different synth character per style
            melOsc.type = style.vibe === 'retro' ? 'square' : (style.vibe === 'dark' ? 'sawtooth' : 'sine');
            melOsc.frequency.setValueAtTime(noteFreq, t);
            
            melFilter.type = 'lowpass';
            melFilter.frequency.setValueAtTime(noteFreq * 6, t);
            melFilter.frequency.exponentialRampToValueAtTime(noteFreq * 2, t + 0.3);
            
            melGain.gain.setValueAtTime(0, t);
            melGain.gain.linearRampToValueAtTime(0.1, t + 0.02);
            melGain.gain.setValueAtTime(0.08, t + 0.1);
            melGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
            
            melOsc.connect(melFilter).connect(melGain).connect(compressor);
            melOsc.start(t); melOsc.stop(t + 0.4);
        }
        
        beatStep++;
    } catch (e) {}
}

// === Input Handling ===
function startGame() {
    if (gameStarted) return;
    gameStarted = true;
    canvas.style.cursor = 'none'; // Hide cursor when game starts
    initAudio();
}

window.addEventListener('keydown', e => {
    if (!gameStarted) startGame();
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space', 'KeyR'].includes(e.code)) e.preventDefault();
    keys[e.code] = true;
    
    if (e.code === 'Space' && !e.repeat) {
        // Change weapon level and music style (original functionality)
        weaponLevel = (weaponLevel % 3) + 1;
        currentStyle = (currentStyle + 1) % musicStyles.length;
        playSound('transition');
        beatStep = 0; // Reset beat for clean transition
    }
    
    if (e.code === 'ShiftLeft' || e.code === 'ShiftRight') {
        // Activate invincibility shield if available (not on cooldown)
        const now = Date.now();
        if (!invincible && now >= invincibilityCooldown && !e.repeat) {
            invincible = true;
            invincibilityEnd = now + INVINCIBILITY_DURATION;
            invincibilityCooldown = now + INVINCIBILITY_DURATION + INVINCIBILITY_COOLDOWN;
            playSound('powerup');
        }
    }
    
    if (e.code === 'KeyR' && gameOver) {
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; missiles = []; particles = [];
        gameOver = false; lastBossSpawnScore = 0;
        player.x = canvas.width / 2; player.y = canvas.height - 60;
        weaponLevel = 1; powerUpActive = false; player.missileCooldown = 0;
        invincible = false; invincibilityEnd = 0; invincibilityCooldown = 0;
        combo = 0; lastKillTime = 0; comboDisplay = 0; lastComboMilestone = 0;
        canvas.style.cursor = 'none'; // Hide cursor when restarting
    }
});
window.addEventListener('keyup', e => keys[e.code] = false);
window.addEventListener('click', startGame);
window.addEventListener('touchstart', startGame);

// === Game Logic ===
function spawnEnemy(isBoss = false) {
    if (gameOver || enemies.length >= MAX_ENEMIES) return;
    
    const x = Math.random() * (canvas.width - 60) + 30;
    const faction = Math.random() > 0.5 ? 'zerg' : 'protoss';
    
    if (isBoss) {
        enemies.push({
            x: canvas.width / 2, y: -100, vx: 0, vy: 1, size: 120, color: '#fcee0a',
            hp: 50, isBoss: true, faction, behavior: 'boss', lastShot: 0, hitFlash: 0, rotation: 0, trail: []
        });
    } else {
        const rand = Math.random();
        let hp = 1, size = 25, color = '#a855f7', behavior = 'strafing';
        
        if (rand < 0.15) { hp = 1; size = 18; color = faction === 'zerg' ? '#d97706' : '#facc15'; behavior = 'kamikaze'; }
        else if (rand < 0.30) { hp = 3; size = 35; color = faction === 'zerg' ? '#7f1d1d' : '#0ea5e9'; behavior = 'hover'; }
        else if (rand < 0.50) { hp = 5; size = 65; color = faction === 'zerg' ? '#991b1b' : '#0c4a6e'; behavior = 'tank'; }
        else { color = faction === 'zerg' ? '#a855f7' : '#eab308'; }
        
        enemies.push({
            x, y: -50, vx: 0, vy: 0, size, color, hp, isBoss: false, faction, behavior,
            lastShot: 0, hitFlash: 0, rotation: 0, targetX: x, lastTurn: Date.now(), trail: []
        });
    }
}

function killEnemy(e) {
    e.dead = true;
    
    // Faction-specific explosion sounds
    if (e.isBoss) {
        playSound('boss-death');
    } else if (e.behavior === 'tank') {
        playSound('tank-explosion');
    } else if (e.faction === 'zerg') {
        playSound('zerg-death');
    } else {
        playSound('protoss-death');
    }
    
    // Combo system
    const now = Date.now();
    if (now - lastKillTime < COMBO_WINDOW) {
        combo++;
    } else {
        combo = 1; // Reset combo
        lastComboMilestone = 0; // Reset milestone tracker
    }
    lastKillTime = now;
    comboDisplay = combo; // For fade animation
    
    // Play combo milestone sounds
    if (combo === 3 && lastComboMilestone < 3) {
        playSound('combo-3x');
        lastComboMilestone = 3;
    } else if (combo === 5 && lastComboMilestone < 5) {
        playSound('combo-5x');
        lastComboMilestone = 5;
    } else if (combo === 10 && lastComboMilestone < 10) {
        playSound('combo-10x');
        lastComboMilestone = 10;
    } else if (combo === 15 && lastComboMilestone < 15) {
        playSound('combo-15x');
        lastComboMilestone = 15;
    } else if (combo === 20 && lastComboMilestone < 20) {
        playSound('combo-20x');
        lastComboMilestone = 20;
    }
    
    // Base score with combo multiplier
    let baseScore = e.isBoss ? 500 : (e.hp >= 5 ? 50 : (e.hp >= 3 ? 30 : 10));
    let comboBonus = 0;
    
    if (combo >= 3) {
        // 2x at 3 combo, 3x at 5 combo, 4x at 10 combo, etc.
        const multiplier = Math.min(1 + Math.floor(combo / 2), 10);
        comboBonus = baseScore * (multiplier - 1);
    }
    
    score += baseScore + comboBonus;
    
    // === FACTION-SPECIFIC DEATH EFFECTS ===
    const count = e.isBoss ? 50 : Math.min(Math.floor(e.size / 2.5), 20);
    
    if (e.isBoss) {
        // BOSS DEATH: Ultimate explosion sequence
        for (let i = 0; i < count && particles.length < MAX_PARTICLES; i++) {
            const angle = (Math.PI * 2 * i) / count;
            const speed = 3 + Math.random() * 5;
            const particleType = Math.random();
            
            if (particleType < 0.3) {
                // Massive fire burst
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 1.5,
                    vy: Math.sin(angle) * speed * 1.5,
                    life: 60 + Math.random() * 40,
                    maxLife: 100,
                    size: 8 + Math.random() * 8,
                    color: ['#ff0000', '#ff6600', '#ffaa00', '#fef08a'][Math.floor(Math.random() * 4)],
                    type: 'fire'
                });
            } else if (particleType < 0.6) {
                // Yellow/gold energy shards (boss aura color)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 2,
                    vy: Math.sin(angle) * speed * 2,
                    life: 50 + Math.random() * 50,
                    maxLife: 100,
                    size: 4 + Math.random() * 6,
                    color: '#fcee0a',
                    type: 'debris',
                    rotation: Math.random() * Math.PI * 2,
                    rotSpeed: (Math.random() - 0.5) * 0.4
                });
            } else {
                // Dark smoke (demonic essence)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 0.8,
                    vy: Math.sin(angle) * speed * 0.8,
                    life: 70 + Math.random() * 60,
                    maxLife: 130,
                    size: 10 + Math.random() * 12,
                    color: ['#0a0a0a', '#1a1a1a', '#450a0a'][Math.floor(Math.random() * 3)],
                    type: 'smoke'
                });
            }
        }
    } else if (e.behavior === 'tank') {
        // TANK DEATH: Metal debris and explosive shockwave
        for (let i = 0; i < count && particles.length < MAX_PARTICLES; i++) {
            const angle = (Math.PI * 2 * i) / count;
            const speed = 2 + Math.random() * 4;
            const particleType = Math.random();
            
            if (particleType < 0.5) {
                // Metal debris (angular chunks)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 1.5,
                    vy: Math.sin(angle) * speed * 1.5,
                    life: 50 + Math.random() * 40,
                    maxLife: 90,
                    size: 3 + Math.random() * 5,
                    color: e.faction === 'zerg' ? '#7f1d1d' : '#0c4a6e', // Tank colors
                    type: 'debris',
                    rotation: Math.random() * Math.PI * 2,
                    rotSpeed: (Math.random() - 0.5) * 0.5
                });
            } else if (particleType < 0.8) {
                // Explosive fire
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    life: 35 + Math.random() * 35,
                    maxLife: 70,
                    size: 5 + Math.random() * 6,
                    color: ['#ff6600', '#fbbf24', '#f59e0b'][Math.floor(Math.random() * 3)],
                    type: 'fire'
                });
            } else {
                // Black smoke (burning metal)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 0.6,
                    vy: Math.sin(angle) * speed * 0.6,
                    life: 60 + Math.random() * 50,
                    maxLife: 110,
                    size: 6 + Math.random() * 8,
                    color: '#1f2937',
                    type: 'smoke'
                });
            }
        }
    } else if (e.faction === 'zerg') {
        // ZERG DEATH: Bio splatter, organic particles
        for (let i = 0; i < count && particles.length < MAX_PARTICLES; i++) {
            const angle = (Math.PI * 2 * i) / count + (Math.random() - 0.5) * 0.5;
            const speed = 1.5 + Math.random() * 3;
            const particleType = Math.random();
            
            if (particleType < 0.6) {
                // Bio matter (purple/red organic goo)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed + 0.5, // Slight downward (gravity effect)
                    life: 40 + Math.random() * 40,
                    maxLife: 80,
                    size: 3 + Math.random() * 5,
                    color: ['#a855f7', '#9333ea', '#7c3aed', '#d97706', '#dc2626'][Math.floor(Math.random() * 5)],
                    type: 'fire' // Use fire for glow effect
                });
            } else if (particleType < 0.85) {
                // Organic chunks (darker, angular)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 1.3,
                    vy: Math.sin(angle) * speed * 1.3,
                    life: 45 + Math.random() * 45,
                    maxLife: 90,
                    size: 2 + Math.random() * 4,
                    color: ['#581c87', '#7f1d1d', '#450a0a'][Math.floor(Math.random() * 3)],
                    type: 'debris',
                    rotation: Math.random() * Math.PI * 2,
                    rotSpeed: (Math.random() - 0.5) * 0.4
                });
            } else {
                // Green toxic gas
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 0.5,
                    vy: Math.sin(angle) * speed * 0.5 - 0.3, // Float upward
                    life: 50 + Math.random() * 50,
                    maxLife: 100,
                    size: 5 + Math.random() * 7,
                    color: ['#16a34a', '#15803d', '#166534'][Math.floor(Math.random() * 3)],
                    type: 'smoke'
                });
            }
        }
    } else {
        // PROTOSS DEATH: Energy burst, crystalline shards
        for (let i = 0; i < count && particles.length < MAX_PARTICLES; i++) {
            const angle = (Math.PI * 2 * i) / count;
            const speed = 2.5 + Math.random() * 4;
            const particleType = Math.random();
            
            if (particleType < 0.5) {
                // Crystalline shards (angular, bright)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 1.5,
                    vy: Math.sin(angle) * speed * 1.5,
                    life: 45 + Math.random() * 45,
                    maxLife: 90,
                    size: 3 + Math.random() * 5,
                    color: ['#38bdf8', '#0ea5e9', '#06b6d4', '#67e8f9'][Math.floor(Math.random() * 4)],
                    type: 'debris',
                    rotation: Math.random() * Math.PI * 2,
                    rotSpeed: (Math.random() - 0.5) * 0.6
                });
            } else if (particleType < 0.8) {
                // Energy burst (glowing cyan/blue)
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed,
                    vy: Math.sin(angle) * speed,
                    life: 35 + Math.random() * 35,
                    maxLife: 70,
                    size: 4 + Math.random() * 6,
                    color: ['#0ea5e9', '#38bdf8', '#7dd3fc'][Math.floor(Math.random() * 3)],
                    type: 'fire' // Use fire for glow
                });
            } else {
                // Blue plasma vapor
                particles.push({
                    x: e.x, y: e.y,
                    vx: Math.cos(angle) * speed * 0.7,
                    vy: Math.sin(angle) * speed * 0.7,
                    life: 55 + Math.random() * 55,
                    maxLife: 110,
                    size: 6 + Math.random() * 8,
                    color: ['#0c4a6e', '#075985', '#0369a1'][Math.floor(Math.random() * 3)],
                    type: 'smoke'
                });
            }
        }
    }
    
    // PowerUp drop
    if (Math.random() < 0.12 && powerUps.length < MAX_POWERUPS) {
        powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15, type: Math.random() < 0.5 ? 'upgrade' : 'rapid' });
        playSound('whoosh');
    }
}

function update() {
    if (gameOver) return;
    
    const now = Date.now();
    
    // Check invincibility timer
    if (invincible && now >= invincibilityEnd) {
        invincible = false;
    }
    
    // Check combo timeout
    if (combo > 0 && now - lastKillTime > COMBO_WINDOW) {
        combo = 0;
        comboDisplay = 0;
    }
    
    // Fade combo display
    if (comboDisplay > 0 && combo === 0) {
        comboDisplay = Math.max(0, comboDisplay - 0.05);
    }
    
    // Background
    // Update background layers (parallax scrolling)
    for (let s of stars) { s.y += s.speed; if (s.y > canvas.height) { s.y = 0; s.x = Math.random() * canvas.width; } }
    for (let d of debris) { d.y += d.speed; d.rotation += d.rotSpeed; if (d.y > canvas.height + 50) { d.y = -50; d.x = Math.random() * canvas.width; } }
    
    // Parallax layer 1: Nebula clouds (slowest)
    for (let n of nebulaClouds) {
        n.y += n.speed;
        if (n.y > canvas.height + n.size) {
            n.y = -n.size;
            n.x = Math.random() * canvas.width;
        }
    }
    
    // Parallax layer 2: Distant stars (medium speed with twinkling)
    for (let s of distantStars) {
        s.y += s.speed;
        s.twinkle += 0.05; // Twinkle animation
        if (s.y > canvas.height) {
            s.y = 0;
            s.x = Math.random() * canvas.width;
        }
    }
    
    // Parallax layer 3: Space dust (fastest)
    for (let d of spaceDust) {
        d.y += d.speed;
        if (d.y > canvas.height) {
            d.y = 0;
            d.x = Math.random() * canvas.width;
        }
    }
    
    // Piano Particles - spawn new ones periodically
    if (now - lastPianoSpawn > 500 && pianoParticles.length < MAX_PIANO_PARTICLES) {
        const newParticle = {
            x: Math.random() * canvas.width,
            y: Math.random() * canvas.height,
            vx: (Math.random() - 0.5) * 2,
            vy: (Math.random() - 0.5) * 2,
            size: 3 + Math.random() * 4,
            color: `hsl(${Math.random() * 60 + 180}, 70%, 60%)`, // Cyan to blue
            life: 200 + Math.random() * 100,
            lastNote: 0,
            trail: []
        };
        pianoParticles.push(newParticle);
        playPianoNote(newParticle.y, 0.02); // Spawn sound
        lastPianoSpawn = now;
    }
    
    // Update piano particles
    pianoParticles = pianoParticles.filter(p => {
        // Move particle
        p.x += p.vx;
        p.y += p.vy;
        
        // Bounce off edges (with piano note on bounce)
        if (p.x < 0 || p.x > canvas.width) {
            p.vx *= -1;
            if (now - p.lastNote > 200) {
                playPianoNote(p.y, 0.025);
                p.lastNote = now;
            }
        }
        if (p.y < 0 || p.y > canvas.height) {
            p.vy *= -1;
            if (now - p.lastNote > 200) {
                playPianoNote(p.y, 0.025);
                p.lastNote = now;
            }
        }
        
        // Keep in bounds
        p.x = Math.max(0, Math.min(canvas.width, p.x));
        p.y = Math.max(0, Math.min(canvas.height, p.y));
        
        // Add trail
        p.trail.push({ x: p.x, y: p.y, alpha: 1 });
        if (p.trail.length > 8) p.trail.shift();
        
        // Fade trail
        p.trail.forEach(t => t.alpha *= 0.9);
        
        // Particle interactions - play note when particles get close
        for (let other of pianoParticles) {
            if (other === p) continue;
            const dx = other.x - p.x;
            const dy = other.y - p.y;
            const dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < 30 && dist > 0 && now - p.lastNote > 300) {
                // Play chord when particles collide
                playPianoNote(p.y, 0.015);
                playPianoNote(other.y, 0.015);
                p.lastNote = now;
                other.lastNote = now;
                
                // Gentle repulsion
                const force = 0.5;
                p.vx -= (dx / dist) * force;
                p.vy -= (dy / dist) * force;
                other.vx += (dx / dist) * force;
                other.vy += (dy / dist) * force;
            }
        }
        
        p.life--;
        return p.life > 0;
    });
    
    // Player movement
    if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
    if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
    if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
    if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
    
    // Update player trail (twin engine exhaust from back of ship)
    const playerCenterX = player.x + player.size / 2;
    const playerBackY = player.y + player.size / 3; // Back of ship
    const engineOffset = player.size / 5; // Distance between twin engines
    player.trail.push(
        { x: playerCenterX - engineOffset, y: playerBackY, alpha: 1.0, age: 0 },  // Left engine
        { x: playerCenterX + engineOffset, y: playerBackY, alpha: 1.0, age: 0 }   // Right engine
    );
    // Keep last 16 trail points (8 frames of twin trails, since we add 2 per frame)
    while (player.trail.length > 16) {
        player.trail.shift();
    }
    // Age and fade trails
    for (let t of player.trail) {
        t.age++;
        t.alpha = Math.max(0, 1.0 - (t.age / 8)); // Clamp alpha to >= 0
    }
    
    // Engine sound modulation (based on movement)
    if (engineGain && audioCtx) {
        const isMoving = keys['ArrowUp'] || keys['ArrowDown'] || keys['ArrowLeft'] || keys['ArrowRight'];
        const targetVolume = isMoving ? 0.08 : 0.02; // Louder when moving, quieter when idle
        const targetFreq = isMoving ? 80 : 60; // Higher pitch when moving
        engineGain.gain.linearRampToValueAtTime(targetVolume, audioCtx.currentTime + 0.1);
        if (engineOsc) {
            engineOsc.frequency.linearRampToValueAtTime(targetFreq, audioCtx.currentTime + 0.1);
        }
    }
    
    // Player shooting
    const cooldown = powerUpActive ? 120 : 200;
    if (now - player.lastShotTime > cooldown && bullets.length < MAX_BULLETS) {
        playSound('shoot');
        bullets.push({ x: player.x + player.size / 2, y: player.y, vx: 0, vy: -12, damage: 2, trail: [] });
        if (weaponLevel >= 2) {
            bullets.push({ x: player.x, y: player.y + 10, vx: -1.5, vy: -10, damage: 1.5, trail: [] });
            bullets.push({ x: player.x + player.size, y: player.y + 10, vx: 1.5, vy: -10, damage: 1.5, trail: [] });
        }
        if (weaponLevel >= 3) {
            bullets.push({ x: player.x - 10, y: player.y + 15, vx: -3, vy: -9, damage: 1, trail: [] });
            bullets.push({ x: player.x + player.size + 10, y: player.y + 15, vx: 3, vy: -9, damage: 1, trail: [] });
        }
        player.lastShotTime = now;
    }
    
    // Missiles
    if (now > player.missileCooldown && enemies.length > 0 && missiles.length < MAX_MISSILES) {
        const target = enemies.find(e => !e.dead);
        if (target) {
            playSound('missile');
            for (let i = 0; i < 2; i++) {
                missiles.push({
                    x: player.x + player.size / 2, y: player.y,
                    target, damage: 5, speed: 7, angle: -Math.PI / 2 + (Math.random() - 0.5) * 0.4,
                    trail: []
                });
            }
            player.missileCooldown = now + 1500;
        }
    }
    
    // PowerUp timer
    if (powerUpActive && now > powerUpTimer) powerUpActive = false;
    
    // Update bullets
    bullets = bullets.filter(b => {
        if (b.dead) return false;
        b.x += b.vx; b.y += b.vy;
        
        // Update bullet trail (fading projectile trail)
        b.trail.push({ x: b.x, y: b.y, alpha: 1.0, age: 0 });
        if (b.trail.length > 5) b.trail.shift(); // Keep last 5 positions
        for (let t of b.trail) {
            t.age++;
            t.alpha = Math.max(0, 1.0 - (t.age / 5)); // Clamp alpha to >= 0
        }
        
        return b.y > -20 && b.y < canvas.height + 20 && b.x > -20 && b.x < canvas.width + 20;
    });
    
    // Update missiles
    missiles = missiles.filter(m => {
        if (m.dead) return false;
        if (m.target && m.target.dead) m.target = enemies.find(e => !e.dead) || null;
        if (m.target) {
            const angle = Math.atan2(m.target.y - m.y, m.target.x - m.x);
            let diff = angle - m.angle;
            while (diff > Math.PI) diff -= Math.PI * 2;
            while (diff < -Math.PI) diff += Math.PI * 2;
            m.angle += Math.max(-0.12, Math.min(0.12, diff));
        }
        m.x += Math.cos(m.angle) * m.speed;
        m.y += Math.sin(m.angle) * m.speed;
        
        // Update missile trail (smoke/fire trail)
        m.trail.push({ x: m.x, y: m.y, alpha: 1.0, age: 0 });
        if (m.trail.length > 10) m.trail.shift(); // Longer trail for missiles
        for (let t of m.trail) {
            t.age++;
            t.alpha = Math.max(0, 1.0 - (t.age / 10)); // Clamp alpha to >= 0
        }
        
        return m.y > -50 && m.y < canvas.height + 50 && m.x > -50 && m.x < canvas.width + 50;
    });
    
    // Update particles with type-specific physics
    particles = particles.filter(p => {
        p.x += p.vx;
        p.y += p.vy;
        p.life--;
        
        // Type-specific behavior
        if (p.type === 'fire') {
            p.vy -= 0.15; // Fire rises
            p.vx *= 0.98; // Friction
        } else if (p.type === 'debris') {
            p.vy += 0.2; // Gravity
            if (p.rotation !== undefined) p.rotation += p.rotSpeed;
        } else if (p.type === 'smoke') {
            p.vy -= 0.05; // Slight upward drift
            p.vx *= 0.95; // More friction
            p.size += 0.1; // Expand over time
        }
        
        return p.life > 0;
    });
    
    // Update enemy bullets
    enemyBullets = enemyBullets.filter(b => {
        b.x += b.vx; b.y += b.vy;
        // Only check collision if not invincible
        if (!invincible) {
            const dx = b.x - player.x - player.size / 2;
            const dy = b.y - player.y - player.size / 2;
            const bulletRadius = b.isExplosive ? (b.size || 8) : 5;
            if (Math.sqrt(dx * dx + dy * dy) < player.size / 2 + bulletRadius) {
                playSound('game-over');
                gameOver = true;
                canvas.style.cursor = 'default'; // Show cursor on game over
                saveScore();
                return false;
            }
        }
        return b.y > 0 && b.y < canvas.height && b.x > 0 && b.x < canvas.width;
    });
    
    // Update powerups
    powerUps = powerUps.filter(p => {
        p.y += p.vy;
        const dx = p.x - player.x - player.size / 2;
        const dy = p.y - player.y - player.size / 2;
        if (Math.sqrt(dx * dx + dy * dy) < player.size / 2 + p.size / 2) {
            playSound('powerup');
            if (p.type === 'upgrade') weaponLevel = Math.min(weaponLevel + 1, 3);
            else { powerUpActive = true; powerUpTimer = now + 5000; }
            score += 50;
            return false;
        }
        return p.y < canvas.height + 50;
    });
    
    // Update enemies
    for (let e of enemies) {
        if (e.dead) continue;
        if (e.hitFlash > 0) e.hitFlash--;
        
        if (e.isBoss) {
            const rage = 1 + (50 - e.hp) / 20;
            if (e.y < 120) e.y += 1.5;
            else e.x += Math.sin(now / (500 / rage)) * 3 * rage;
            
            if (now - e.lastShot > 1000 / rage && enemyBullets.length < MAX_ENEMY_BULLETS - 5) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                // Shoot 5 bullets in a spread pattern
                for (let i = -2; i <= 2; i++) {
                    const spreadAngle = angle + (i * 0.15);
                    enemyBullets.push({ 
                        x: e.x, 
                        y: e.y, 
                        vx: Math.cos(spreadAngle) * 5 * rage, 
                        vy: Math.sin(spreadAngle) * 5 * rage 
                    });
                }
                e.lastShot = now;
                playSound('boss-shoot');
            }
        } else {
            if (e.y < 50) e.y += 2;
            else {
                if (e.behavior === 'kamikaze') {
                    const angle = Math.atan2(player.y - e.y, player.x - e.x);
                    e.vx = Math.cos(angle) * 3.5;
                    e.vy = Math.sin(angle) * 3.5;
                    e.rotation = angle + Math.PI / 2;
                } else if (e.behavior === 'hover') {
                    e.vy = 0.4;
                    e.vx = Math.sin(now / 800) * 2;
                } else if (e.behavior === 'tank') {
                    // Slow moving tank - moves straight down
                    e.vx = 0;
                    e.vy = 0.8;
                } else {
                    if (now - e.lastTurn > 1200) { e.targetX = Math.random() * canvas.width; e.lastTurn = now; }
                    e.vx = (e.targetX - e.x) * 0.02;
                    e.vy = 1.2;
                }
                e.x += e.vx; e.y += e.vy;
                
                // Update enemy trail (only for fast-moving kamikazes)
                if (e.behavior === 'kamikaze') {
                    e.trail.push({ x: e.x, y: e.y, alpha: 1.0, age: 0 });
                    if (e.trail.length > 8) e.trail.shift();
                    for (let t of e.trail) {
                        t.age++;
                        t.alpha = Math.max(0, 1.0 - (t.age / 8)); // Clamp alpha to >= 0
                    }
                }
            }
            
            // Enemy shooting - Tanks shoot explosive projectiles, others shoot regular bullets
            if (e.behavior === 'tank' && now - e.lastShot > 1500 && enemyBullets.length < MAX_ENEMY_BULLETS - 1 && e.y < player.y - 80) {
                // Tank shoots large explosive projectile from bottom cannon
                // Cannon muzzle position (at bottom of tank, accounting for turret + barrel length)
                const cannonLength = e.size * 0.65;
                const turretHeight = e.size * 0.25;
                const muzzleY = e.y + e.size / 2 + turretHeight + cannonLength;
                
                const angle = Math.atan2(player.y - muzzleY, player.x - e.x);
                enemyBullets.push({ 
                    x: e.x, 
                    y: muzzleY, 
                    vx: Math.cos(angle) * 2.5, 
                    vy: Math.sin(angle) * 2.5,
                    isExplosive: true,
                    size: 8
                });
                e.lastShot = now;
                playSound('explosion');
            } else if (e.faction === 'protoss' && e.behavior !== 'tank' && now - e.lastShot > 2500 && enemyBullets.length < MAX_ENEMY_BULLETS && e.y < player.y - 80) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 3, vy: Math.sin(angle) * 3 });
                e.lastShot = now;
            }
        }
        
        // Enemy collision with player
        if (!invincible) {
            const dx = e.x - player.x - player.size / 2;
            const dy = e.y - player.y - player.size / 2;
            if (Math.sqrt(dx * dx + dy * dy) < e.size / 2 + player.size / 2) {
                playSound('game-over');
                gameOver = true;
                canvas.style.cursor = 'default'; // Show cursor on game over
                saveScore();
            }
        }
    }
    
    // Bullet vs Enemy collision
    for (let b of bullets) {
        if (b.dead) continue;
        for (let e of enemies) {
            if (e.dead) continue;
            const dx = b.x - e.x, dy = b.y - e.y;
            if (Math.sqrt(dx * dx + dy * dy) < e.size / 2) {
                b.dead = true;
                e.hp -= b.damage || 1;
                e.hitFlash = 5;
                playSound('hit');
                // Screen shake on boss hit
                if (e.isBoss) {
                    screenShake = Math.min(screenShake + 3, 12);
                }
                if (e.hp <= 0) killEnemy(e);
                break;
            }
        }
    }
    
    // Missile vs Enemy collision
    for (let m of missiles) {
        if (m.dead) continue;
        for (let e of enemies) {
            if (e.dead) continue;
            const dx = m.x - e.x, dy = m.y - e.y;
            if (Math.sqrt(dx * dx + dy * dy) < e.size / 2 + 8) {
                m.dead = true;
                e.hp -= m.damage;
                e.hitFlash = 10;
                // Screen shake on boss hit (missiles shake more)
                if (e.isBoss) {
                    screenShake = Math.min(screenShake + 5, 15);
                }
                if (e.hp <= 0) killEnemy(e);
                break;
            }
        }
    }
    
    // Remove dead enemies
    enemies = enemies.filter(e => {
        if (e.dead) return false;
        if (e.y > canvas.height + 100 || e.x < -100 || e.x > canvas.width + 100) return false;
        return true;
    });
    
    // Spawn enemies
    if (Math.random() < 0.035 && enemies.length < MAX_ENEMIES - 2) spawnEnemy();
    if (score >= lastBossSpawnScore + 500 && !enemies.some(e => e.isBoss)) {
        playSound('boss-warning');
        spawnEnemy(true);
        lastBossSpawnScore = score;
    }
    
    // Beat decay
    beatKick *= 0.9;
    beatSnare *= 0.88;
    beatHat *= 0.85;
    
    // Screen shake decay
    if (screenShake > 0) {
        shakeX = (Math.random() - 0.5) * screenShake;
        shakeY = (Math.random() - 0.5) * screenShake;
        screenShake *= 0.85; // Decay
        if (screenShake < 0.1) screenShake = 0;
    } else {
        shakeX = 0;
        shakeY = 0;
    }
    
    // Cap melody queue
    if (melodyQueue.length > 8) melodyQueue.length = 8;
    
    // Tick BGM (safe, throttled)
    tickBGM();
}

// === Drawing ===
function drawDrone(x, y, size, color, rotation, faction) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rotation || 0);
    
    if (faction === 'zerg') {
        // SCARY ORGANIC ZERG - Grotesque bio-horror
        
        // Pulsating outer membrane (breathing effect)
        const pulse = Math.sin(Date.now() / 200) * 0.1;
        ctx.fillStyle = color;
        ctx.globalAlpha = 0.6;
        ctx.beginPath();
        ctx.arc(0, 0, size / 2 * (1 + pulse), 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1.0;
        
        // Main grotesque body
        ctx.fillStyle = color;
        ctx.beginPath();
        // Irregular organic shape with tentacle-like appendages
        for (let i = 0; i < 8; i++) {
            const angle = (i / 8) * Math.PI * 2;
            const radius = size / 2 + (i % 2 ? size / 6 : -size / 8);
            const x = Math.cos(angle) * radius;
            const y = Math.sin(angle) * radius;
            if (i === 0) ctx.moveTo(x, y);
            else ctx.lineTo(x, y);
        }
        ctx.closePath();
        ctx.fill();
        
        // Disgusting veins/tendrils
        ctx.strokeStyle = '#7c2d12';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(-size / 3, -size / 4);
        ctx.quadraticCurveTo(0, 0, size / 3, size / 4);
        ctx.moveTo(size / 3, -size / 4);
        ctx.quadraticCurveTo(0, 0, -size / 3, size / 4);
        ctx.stroke();
        
        // Multiple creepy glowing eyes (irregular placement)
        const eyePositions = [
            [-size / 4, -size / 6],
            [size / 4, -size / 6],
            [0, size / 8],
            [-size / 5, size / 5],
            [size / 5, size / 5]
        ];
        
        ctx.shadowBlur = 8;
        ctx.shadowColor = '#fef08a';
        for (let [ex, ey] of eyePositions.slice(0, 3 + Math.floor(Math.random() * 3))) {
            // Eye socket (dark)
            ctx.fillStyle = '#1c0a00';
            ctx.beginPath();
            ctx.arc(ex, ey, 4, 0, Math.PI * 2);
            ctx.fill();
            
            // Glowing yellow eye
            ctx.fillStyle = '#fef08a';
            ctx.beginPath();
            ctx.arc(ex + 1, ey, 2.5, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.shadowBlur = 0;
        
        // Grotesque mouth/maw
        ctx.fillStyle = '#450a0a';
        ctx.beginPath();
        ctx.arc(0, size / 4, size / 6, 0, Math.PI);
        ctx.fill();
        
        // Teeth/spines
        ctx.strokeStyle = '#fbbf24';
        ctx.lineWidth = 1.5;
        for (let i = 0; i < 5; i++) {
            const tx = -size / 6 + (i * size / 15);
            ctx.beginPath();
            ctx.moveTo(tx, size / 4);
            ctx.lineTo(tx, size / 4 - 4);
            ctx.stroke();
        }
        
    } else {
        // SCARY PROTOSS - Alien biomechanical horror
        
        // Ominous dark core
        ctx.fillStyle = '#0a0a0a';
        ctx.beginPath();
        ctx.arc(0, 0, size / 2.5, 0, Math.PI * 2);
        ctx.fill();
        
        // Sharp angular crystalline body
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.moveTo(0, -size / 2);
        ctx.lineTo(size / 2.2, -size / 6);
        ctx.lineTo(size / 2, size / 3);
        ctx.lineTo(size / 6, size / 2);
        ctx.lineTo(0, size / 3);
        ctx.lineTo(-size / 6, size / 2);
        ctx.lineTo(-size / 2, size / 3);
        ctx.lineTo(-size / 2.2, -size / 6);
        ctx.closePath();
        ctx.fill();
        
        // Energy veins (pulsing)
        const energyPulse = Math.sin(Date.now() / 150) * 0.5 + 0.5;
        ctx.strokeStyle = `rgba(56, 189, 248, ${0.6 + energyPulse * 0.4})`;
        ctx.lineWidth = 2;
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#38bdf8';
        ctx.beginPath();
        ctx.moveTo(0, -size / 2);
        ctx.lineTo(0, size / 3);
        ctx.moveTo(-size / 3, 0);
        ctx.lineTo(size / 3, 0);
        ctx.stroke();
        ctx.shadowBlur = 0;
        
        // Menacing glowing core (pulsing)
        const gradient = ctx.createRadialGradient(0, 0, 0, 0, 0, size / 4);
        gradient.addColorStop(0, '#38bdf8');
        gradient.addColorStop(0.5, '#0ea5e9');
        gradient.addColorStop(1, '#0c4a6e');
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(0, 0, size / 4 * (0.8 + energyPulse * 0.3), 0, Math.PI * 2);
        ctx.fill();
        
        // Sharp blade-like protrusions
        ctx.fillStyle = '#0369a1';
        const blades = 6;
        for (let i = 0; i < blades; i++) {
            const angle = (i / blades) * Math.PI * 2;
            ctx.save();
            ctx.rotate(angle);
            ctx.beginPath();
            ctx.moveTo(0, -size / 2.5);
            ctx.lineTo(size / 8, -size / 2 - 6);
            ctx.lineTo(0, -size / 2 - 10);
            ctx.lineTo(-size / 8, -size / 2 - 6);
            ctx.closePath();
            ctx.fill();
            ctx.restore();
        }
        
        // Alien eye-like sensors (menacing red glow)
        ctx.fillStyle = '#dc2626';
        ctx.shadowBlur = 8;
        ctx.shadowColor = '#dc2626';
        ctx.beginPath();
        ctx.arc(-size / 6, -size / 8, 3, 0, Math.PI * 2);
        ctx.arc(size / 6, -size / 8, 3, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
    }
    ctx.restore();
}

function drawTank(x, y, size, color, faction) {
    ctx.save();
    ctx.translate(x, y);
    
    // StarCraft 2 Siege Tank - UPSIDE DOWN (cannon pointing down)
    const baseWidth = size * 0.85;
    const baseHeight = size * 0.4;
    
    if (faction === 'zerg') {
        // === INFESTED TERRAN SIEGE TANK (Zerg) - UPSIDE DOWN ===
        
        // Deployed stabilizer legs extending upward (tank is flipped)
        ctx.fillStyle = '#451a03';
        ctx.strokeStyle = '#7c2d12';
        ctx.lineWidth = 2;
        // Left leg (extending up)
        ctx.beginPath();
        ctx.moveTo(-baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(-baseWidth / 1.5, -baseHeight);
        ctx.lineTo(-baseWidth / 1.8, -baseHeight - 4);
        ctx.stroke();
        // Right leg (extending up)
        ctx.beginPath();
        ctx.moveTo(baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(baseWidth / 1.5, -baseHeight);
        ctx.lineTo(baseWidth / 1.8, -baseHeight - 4);
        ctx.stroke();
        
        // Main tank base/chassis (flipped)
        ctx.fillStyle = color;
        ctx.strokeStyle = '#7c2d12';
        ctx.lineWidth = 2;
        ctx.fillRect(-baseWidth / 2, -baseHeight / 2, baseWidth, baseHeight);
        ctx.strokeRect(-baseWidth / 2, -baseHeight / 2, baseWidth, baseHeight);
        
        // Organic infestation details
        ctx.fillStyle = '#78350f';
        for (let i = 0; i < 4; i++) {
            const blobX = -baseWidth / 3 + (i * baseWidth / 5);
            const blobY = -baseHeight / 4 + (Math.random() - 0.5) * baseHeight / 3;
            ctx.beginPath();
            ctx.arc(blobX, blobY, 3 + Math.random() * 2, 0, Math.PI * 2);
            ctx.fill();
        }
        
        // Turret base (infested) - now on bottom
        const turretBaseWidth = size * 0.45;
        const turretBaseHeight = size * 0.25;
        ctx.fillStyle = '#7f1d1d';
        ctx.strokeStyle = '#7c2d12';
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.rect(-turretBaseWidth / 2, baseHeight / 2, turretBaseWidth, turretBaseHeight);
        ctx.fill();
        ctx.stroke();
        
        // Organic spines on turret
        ctx.fillStyle = '#fef08a';
        for (let i = 0; i < 6; i++) {
            const spineX = -turretBaseWidth / 3 + (i * turretBaseWidth / 7);
            ctx.beginPath();
            ctx.moveTo(spineX, baseHeight / 2 + turretBaseHeight);
            ctx.lineTo(spineX - 1.5, baseHeight / 2 + turretBaseHeight + 4);
            ctx.lineTo(spineX + 1.5, baseHeight / 2 + turretBaseHeight + 4);
            ctx.fill();
        }
        
        // SINGLE CANNON pointing DOWN (center bottom)
        const cannonWidth = size * 0.18;
        const cannonLength = size * 0.65;
        
        // Cannon barrel
        ctx.fillStyle = '#991b1b';
        ctx.strokeStyle = '#450a0a';
        ctx.lineWidth = 2;
        ctx.fillRect(-cannonWidth / 2, baseHeight / 2 + turretBaseHeight, cannonWidth, cannonLength);
        ctx.strokeRect(-cannonWidth / 2, baseHeight / 2 + turretBaseHeight, cannonWidth, cannonLength);
        
        // Cannon muzzle (glowing at bottom)
        ctx.fillStyle = '#fbbf24';
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#fbbf24';
        ctx.beginPath();
        ctx.arc(0, baseHeight / 2 + turretBaseHeight + cannonLength, cannonWidth / 2 + 2, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
        
        // Infested biomass details
        ctx.fillStyle = '#a16207';
        ctx.globalAlpha = 0.6;
        ctx.beginPath();
        ctx.arc(-baseWidth / 4, 0, 4, 0, Math.PI * 2);
        ctx.arc(baseWidth / 4, 0, 3, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1.0;
        
    } else {
        // === TERRAN SIEGE TANK (Protoss - Advanced Tech) - UPSIDE DOWN ===
        
        // Deployed stabilizer legs extending upward
        ctx.strokeStyle = '#0c4a6e';
        ctx.lineWidth = 2.5;
        // Left leg (extending up)
        ctx.beginPath();
        ctx.moveTo(-baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(-baseWidth / 1.5, -baseHeight);
        ctx.lineTo(-baseWidth / 1.8, -baseHeight - 5);
        ctx.stroke();
        // Right leg (extending up)
        ctx.beginPath();
        ctx.moveTo(baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(baseWidth / 1.5, -baseHeight);
        ctx.lineTo(baseWidth / 1.8, -baseHeight - 5);
        ctx.stroke();
        
        // Main tank base/chassis with angular design
        ctx.fillStyle = color;
        ctx.strokeStyle = '#0c4a6e';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        // Angular chassis
        ctx.moveTo(-baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(baseWidth / 2, -baseHeight / 2);
        ctx.lineTo(baseWidth / 2 + baseWidth / 10, 0);
        ctx.lineTo(baseWidth / 2, baseHeight / 2);
        ctx.lineTo(-baseWidth / 2, baseHeight / 2);
        ctx.lineTo(-baseWidth / 2 - baseWidth / 10, 0);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        
        // Tech panels/plating details
        ctx.strokeStyle = '#075985';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(-baseWidth / 3, -baseHeight / 2);
        ctx.lineTo(-baseWidth / 3, baseHeight / 2);
        ctx.moveTo(baseWidth / 3, -baseHeight / 2);
        ctx.lineTo(baseWidth / 3, baseHeight / 2);
        ctx.stroke();
        
        // Turret base (armored and angular) - now on bottom
        const turretBaseWidth = size * 0.5;
        const turretBaseHeight = size * 0.28;
        ctx.fillStyle = '#075985';
        ctx.strokeStyle = '#0c4a6e';
        ctx.lineWidth = 2.5;
        ctx.beginPath();
        // Hexagonal turret base (flipped)
        ctx.moveTo(-turretBaseWidth / 2, baseHeight / 2);
        ctx.lineTo(-turretBaseWidth / 3, baseHeight / 2 + turretBaseHeight / 2);
        ctx.lineTo(turretBaseWidth / 3, baseHeight / 2 + turretBaseHeight / 2);
        ctx.lineTo(turretBaseWidth / 2, baseHeight / 2);
        ctx.lineTo(turretBaseWidth / 2, baseHeight / 2 + turretBaseHeight);
        ctx.lineTo(-turretBaseWidth / 2, baseHeight / 2 + turretBaseHeight);
        ctx.closePath();
        ctx.fill();
        ctx.stroke();
        
        // Energy core (glowing blue) in turret
        ctx.fillStyle = '#38bdf8';
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#38bdf8';
        ctx.beginPath();
        ctx.arc(0, baseHeight / 2 + turretBaseHeight / 2, size / 10, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
        
        // SINGLE CANNON pointing DOWN - pristine tech
        const cannonWidth = size * 0.2;
        const cannonLength = size * 0.7;
        
        ctx.fillStyle = '#0c4a6e';
        ctx.strokeStyle = '#075985';
        ctx.lineWidth = 2;
        
        // Cannon barrel segments (3 sections pointing down)
        const segments = 3;
        for (let s = 0; s < segments; s++) {
            const segmentLength = cannonLength / segments;
            const segmentY = baseHeight / 2 + turretBaseHeight + (s * segmentLength);
            ctx.fillRect(-cannonWidth / 2, segmentY, cannonWidth, segmentLength - 2);
            ctx.strokeRect(-cannonWidth / 2, segmentY, cannonWidth, segmentLength - 2);
        }
        
        // Cannon muzzle (glowing cyan energy at bottom)
        ctx.fillStyle = '#06b6d4';
        ctx.shadowBlur = 14;
        ctx.shadowColor = '#06b6d4';
        ctx.beginPath();
        ctx.arc(0, baseHeight / 2 + turretBaseHeight + cannonLength, cannonWidth / 2 + 2, 0, Math.PI * 2);
        ctx.fill();
        
        // Inner glow
        ctx.fillStyle = '#67e8f9';
        ctx.beginPath();
        ctx.arc(0, baseHeight / 2 + turretBaseHeight + cannonLength, cannonWidth / 3, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
        
        // Energy conduit from core to cannon
        ctx.strokeStyle = '#38bdf8';
        ctx.lineWidth = 2;
        ctx.shadowBlur = 5;
        ctx.shadowColor = '#38bdf8';
        ctx.beginPath();
        ctx.moveTo(0, baseHeight / 2 + turretBaseHeight / 2);
        ctx.lineTo(0, baseHeight / 2 + turretBaseHeight);
        ctx.stroke();
        ctx.shadowBlur = 0;
    }
    
    ctx.restore();
}


function draw() {
    // Clear
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Beat-reactive camera + screen shake
    ctx.save();
    const scale = 1.0 + beatKick * 0.018;
    const beatShakeX = (Math.random() - 0.5) * beatSnare * 6;
    const beatShakeY = (Math.random() - 0.5) * beatSnare * 6;
    // Combine beat shake with damage shake
    ctx.translate(canvas.width / 2 + beatShakeX + shakeX, canvas.height / 2 + beatShakeY + shakeY);
    ctx.scale(scale, scale);
    ctx.translate(-canvas.width / 2, -canvas.height / 2);
    
    // Snare flash (white)
    if (beatSnare > 0.1) {
        ctx.fillStyle = `rgba(255,255,255,${beatSnare * 0.06})`;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
    }
    
    // Hat flash (cyan tint on accents)
    if (beatHat > 0.5) {
        ctx.fillStyle = `rgba(0,240,255,${(beatHat - 0.5) * 0.04})`;
        ctx.fillRect(0, 0, canvas.width, canvas.height);
    }
    
    // === PARALLAX BACKGROUND LAYERS (farthest to closest) ===
    
    // Layer 1: Nebula clouds (slowest, farthest - glowing fog)
    for (let n of nebulaClouds) {
        ctx.globalAlpha = n.alpha;
        
        // Create radial gradient for nebula glow
        const gradient = ctx.createRadialGradient(n.x, n.y, 0, n.x, n.y, n.size);
        gradient.addColorStop(0, n.color.replace(')', ', 0.4)').replace('rgb', 'rgba'));
        gradient.addColorStop(0.5, n.color.replace(')', ', 0.2)').replace('rgb', 'rgba'));
        gradient.addColorStop(1, n.color.replace(')', ', 0)').replace('rgb', 'rgba'));
        
        ctx.fillStyle = gradient;
        ctx.beginPath();
        ctx.arc(n.x, n.y, n.size, 0, Math.PI * 2);
        ctx.fill();
    }
    ctx.globalAlpha = 1.0;
    
    // Layer 2: Distant stars (medium speed - twinkling tiny dots)
    for (let s of distantStars) {
        // Twinkling effect using sine wave
        const twinkleAlpha = 0.3 + Math.sin(s.twinkle) * 0.3;
        ctx.globalAlpha = twinkleAlpha;
        
        // Beat-reactive brightness
        const brightness = 180 + beatHat * 40;
        ctx.fillStyle = `rgb(${brightness},${brightness},${brightness})`;
        ctx.fillRect(s.x, s.y, s.size, s.size);
    }
    ctx.globalAlpha = 1.0;
    
    // Layer 3: Space dust (fastest, closest - visible particles)
    for (let d of spaceDust) {
        ctx.globalAlpha = d.alpha;
        ctx.fillStyle = '#64748b';
        ctx.fillRect(d.x - d.size / 2, d.y - d.size / 2, d.size, d.size);
    }
    ctx.globalAlpha = 1.0;
    
    // Debris
    ctx.fillStyle = '#334155';
    for (let d of debris) {
        ctx.save();
        ctx.translate(d.x, d.y);
        ctx.rotate(d.rotation);
        if (d.shape === 'beam') ctx.fillRect(-d.size / 2, -2, d.size, 4);
        else ctx.fillRect(-d.size / 2, -d.size / 2, d.size, d.size);
        ctx.restore();
    }
    
    // Stars (beat reactive - pulse on kick, shimmer on hat)
    for (let s of stars) {
        const brightness = 200 + beatHat * 55;
        ctx.fillStyle = `rgb(${brightness},${brightness},${brightness})`;
        const starSize = s.size + beatKick * 2 + beatHat * 0.5;
        ctx.fillRect(s.x, s.y, starSize, starSize);
    }
    
    // Piano Particles (musical background layer)
    for (let p of pianoParticles) {
        // Draw trail
        for (let i = 0; i < p.trail.length; i++) {
            const t = p.trail[i];
            const trailAlpha = t.alpha * 0.3;
            ctx.fillStyle = p.color.replace('60%', `60%, ${trailAlpha}`).replace(')', ', ' + trailAlpha + ')');
            ctx.beginPath();
            ctx.arc(t.x, t.y, p.size * 0.5, 0, Math.PI * 2);
            ctx.fill();
        }
        
        // Draw particle with glow
        ctx.shadowBlur = 10;
        ctx.shadowColor = p.color;
        ctx.fillStyle = p.color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fill();
        ctx.shadowBlur = 0;
        
        // Inner bright core
        ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * 0.4, 0, Math.PI * 2);
        ctx.fill();
    }
    
    // Particles with enhanced rendering
    for (let p of particles) {
        const lifeRatio = p.maxLife ? p.life / p.maxLife : 1;
        ctx.globalAlpha = lifeRatio;
        
        if (p.type === 'fire') {
            // Fire particles with glow
            ctx.shadowBlur = 10;
            ctx.shadowColor = p.color;
            ctx.fillStyle = p.color;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
            ctx.shadowBlur = 0;
        } else if (p.type === 'debris') {
            // Rotating debris
            ctx.save();
            ctx.translate(p.x, p.y);
            if (p.rotation) ctx.rotate(p.rotation);
            ctx.fillStyle = p.color;
            ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
            ctx.restore();
        } else if (p.type === 'smoke') {
            // Smoke with fade
            ctx.fillStyle = p.color;
            ctx.beginPath();
            ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
            ctx.fill();
        } else {
            // Default simple particle
            ctx.fillStyle = p.color;
            ctx.fillRect(p.x - 2, p.y - 2, 4, 4);
        }
    }
    ctx.globalAlpha = 1.0;
    
    // === TRAILS (render before entities so they appear behind) ===
    
    // Player engine trails (twin cyan/white exhaust)
    for (let i = 0; i < player.trail.length; i++) {
        const t = player.trail[i];
        if (t.alpha <= 0) continue; // Skip invisible trails
        ctx.globalAlpha = t.alpha * 0.7;
        
        // Outer glow (cyan)
        ctx.fillStyle = '#38bdf8';
        ctx.shadowBlur = 8;
        ctx.shadowColor = '#38bdf8';
        ctx.beginPath();
        ctx.arc(t.x, t.y, 3 * t.alpha, 0, Math.PI * 2);
        ctx.fill();
        
        // Inner core (white)
        ctx.shadowBlur = 0;
        ctx.fillStyle = '#ffffff';
        ctx.beginPath();
        ctx.arc(t.x, t.y, 1.5 * t.alpha, 0, Math.PI * 2);
        ctx.fill();
    }
    ctx.globalAlpha = 1.0;
    ctx.shadowBlur = 0;
    
    // Bullet trails (fading blue streaks)
    for (let b of bullets) {
        if (b.dead || !b.trail) continue;
        for (let i = 0; i < b.trail.length; i++) {
            const t = b.trail[i];
            if (t.alpha <= 0) continue; // Skip invisible trails
            ctx.globalAlpha = t.alpha * 0.5;
            ctx.fillStyle = '#60a5fa';
            ctx.fillRect(t.x - 1, t.y - 2, 2, 4);
        }
    }
    ctx.globalAlpha = 1.0;
    
    // Missile trails (smoke/fire)
    for (let m of missiles) {
        if (m.dead || !m.trail) continue;
        for (let i = 0; i < m.trail.length; i++) {
            const t = m.trail[i];
            if (t.alpha <= 0) continue; // Skip invisible trails
            ctx.globalAlpha = t.alpha * 0.6;
            
            // Smoke (gray)
            ctx.fillStyle = i % 2 === 0 ? '#6b7280' : '#9ca3af';
            ctx.beginPath();
            ctx.arc(t.x, t.y, 2.5 * t.alpha, 0, Math.PI * 2);
            ctx.fill();
            
            // Fire core (orange/red) for fresh trails
            if (i >= m.trail.length - 3) {
                ctx.fillStyle = '#fb923c';
                ctx.shadowBlur = 5;
                ctx.shadowColor = '#fb923c';
                ctx.beginPath();
                ctx.arc(t.x, t.y, 1.5 * t.alpha, 0, Math.PI * 2);
                ctx.fill();
            }
        }
    }
    ctx.globalAlpha = 1.0;
    ctx.shadowBlur = 0;
    
    // Player (F-35 Lightning II)
    ctx.save();
    ctx.translate(player.x + player.size / 2, player.y + player.size / 2);
    
    // Invincibility shield effect (pulsing cyan shield)
    if (invincible) {
        const pulseSize = Math.sin(Date.now() / 100) * 5 + player.size * 0.8;
        ctx.strokeStyle = '#00f0ff';
        ctx.lineWidth = 3;
        ctx.shadowBlur = 15;
        ctx.shadowColor = '#00f0ff';
        ctx.beginPath();
        ctx.arc(0, 0, pulseSize, 0, Math.PI * 2);
        ctx.stroke();
        
        // Inner shield glow
        ctx.strokeStyle = '#38bdf8';
        ctx.lineWidth = 2;
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#38bdf8';
        ctx.beginPath();
        ctx.arc(0, 0, pulseSize - 5, 0, Math.PI * 2);
        ctx.stroke();
        ctx.shadowBlur = 0;
    }
    
    // Main fuselage (body)
    ctx.fillStyle = powerUpActive ? '#fbbf24' : '#64748b';
    ctx.beginPath();
    ctx.moveTo(0, -player.size);  // Nose
    ctx.lineTo(player.size / 4, -player.size / 3);  // Right side of cockpit
    ctx.lineTo(player.size / 4, player.size / 2);   // Right side tail
    ctx.lineTo(0, player.size / 3);                  // Tail center
    ctx.lineTo(-player.size / 4, player.size / 2);  // Left side tail
    ctx.lineTo(-player.size / 4, -player.size / 3); // Left side of cockpit
    ctx.closePath();
    ctx.fill();
    
    // Delta wings (swept back)
    ctx.fillStyle = powerUpActive ? '#fcd34d' : '#475569';
    ctx.beginPath();
    // Right wing
    ctx.moveTo(player.size / 5, -player.size / 4);
    ctx.lineTo(player.size / 1.3, player.size / 8);
    ctx.lineTo(player.size / 2, player.size / 4);
    ctx.lineTo(player.size / 4, player.size / 6);
    ctx.closePath();
    ctx.fill();
    
    ctx.beginPath();
    // Left wing
    ctx.moveTo(-player.size / 5, -player.size / 4);
    ctx.lineTo(-player.size / 1.3, player.size / 8);
    ctx.lineTo(-player.size / 2, player.size / 4);
    ctx.lineTo(-player.size / 4, player.size / 6);
    ctx.closePath();
    ctx.fill();
    
    // Cockpit canopy (glowing blue)
    ctx.fillStyle = '#00f0ff';
    ctx.beginPath();
    ctx.ellipse(0, -player.size / 2, player.size / 6, player.size / 4, 0, 0, Math.PI * 2);
    ctx.fill();
    
    // Cockpit detail (highlight)
    ctx.fillStyle = '#38bdf8';
    ctx.beginPath();
    ctx.ellipse(0, -player.size / 1.8, player.size / 10, player.size / 8, 0, 0, Math.PI * 2);
    ctx.fill();
    
    // Twin vertical stabilizers (tail fins)
    ctx.fillStyle = powerUpActive ? '#fbbf24' : '#334155';
    // Right stabilizer
    ctx.beginPath();
    ctx.moveTo(player.size / 6, player.size / 4);
    ctx.lineTo(player.size / 5, player.size / 2);
    ctx.lineTo(player.size / 8, player.size / 2);
    ctx.fill();
    
    // Left stabilizer
    ctx.beginPath();
    ctx.moveTo(-player.size / 6, player.size / 4);
    ctx.lineTo(-player.size / 5, player.size / 2);
    ctx.lineTo(-player.size / 8, player.size / 2);
    ctx.fill();
    
    // Engine exhaust glow (beat reactive)
    ctx.fillStyle = '#f59e0b';
    ctx.shadowBlur = 10;
    ctx.shadowColor = '#f59e0b';
    ctx.beginPath();
    ctx.arc(-player.size / 8, player.size / 2.5, 3 + beatKick * 3, 0, Math.PI * 2);
    ctx.arc(player.size / 8, player.size / 2.5, 3 + beatKick * 3, 0, Math.PI * 2);
    ctx.fill();
    ctx.shadowBlur = 0;
    
    ctx.restore();
    
    // Bullets
    ctx.fillStyle = '#60a5fa';
    for (let b of bullets) if (!b.dead) ctx.fillRect(b.x - 2, b.y, 4, 8);
    
    // Missiles
    for (let m of missiles) {
        if (m.dead) continue;
        ctx.save();
        ctx.translate(m.x, m.y);
        ctx.rotate(m.angle + Math.PI / 2);
        ctx.fillStyle = '#ef4444';
        ctx.beginPath();
        ctx.moveTo(0, -8);
        ctx.lineTo(4, 4);
        ctx.lineTo(-4, 4);
        ctx.fill();
        ctx.restore();
    }
    
    // Enemy bullets
    for (let b of enemyBullets) {
        if (b.isExplosive) {
            // Tank explosive projectiles - larger, glowing, yellow
            ctx.fillStyle = '#fbbf24';
            ctx.beginPath();
            ctx.arc(b.x, b.y, b.size || 8, 0, Math.PI * 2);
            ctx.fill();
            // Glow effect
            ctx.strokeStyle = '#f59e0b';
            ctx.lineWidth = 2;
            ctx.stroke();
        } else {
            // Regular enemy bullets - small, red
            ctx.fillStyle = '#f87171';
            ctx.beginPath();
            ctx.arc(b.x, b.y, 5, 0, Math.PI * 2);
            ctx.fill();
        }
    }
    
    // Enemy trails (for kamikazes)
    for (let e of enemies) {
        if (e.dead || !e.trail || e.behavior !== 'kamikaze') continue;
        for (let i = 0; i < e.trail.length; i++) {
            const t = e.trail[i];
            if (t.alpha <= 0) continue; // Skip invisible trails
            ctx.globalAlpha = t.alpha * 0.4;
            
            // Faction-colored trail
            const trailColor = e.faction === 'zerg' ? '#d97706' : '#facc15';
            ctx.fillStyle = trailColor;
            ctx.shadowBlur = 5;
            ctx.shadowColor = trailColor;
            ctx.beginPath();
            ctx.arc(t.x, t.y, e.size / 3 * t.alpha, 0, Math.PI * 2);
            ctx.fill();
        }
    }
    ctx.globalAlpha = 1.0;
    ctx.shadowBlur = 0;
    
    // Enemies
    for (let e of enemies) {
        if (e.dead) continue;
        if (e.isBoss) {
            // TERRIFYING BOSS - Ultimate Bio-Mechanical Horror
            ctx.save();
            ctx.translate(e.x, e.y);
            ctx.scale(1 + beatKick * 0.03, 1 + beatKick * 0.03);
            
            // Ominous pulsating aura
            const bossPulse = Math.sin(Date.now() / 100) * 0.15;
            ctx.fillStyle = 'rgba(252, 238, 10, 0.15)';
            ctx.beginPath();
            ctx.arc(0, 0, e.size / 1.5 * (1 + bossPulse), 0, Math.PI * 2);
            ctx.fill();
            
            // Dark menacing core
            ctx.fillStyle = (e.hp < 15 && Math.floor(Date.now() / 100) % 2 === 0) ? '#ff003c' : '#0a0a0a';
            ctx.beginPath();
            ctx.arc(0, 0, e.size / 2.5, 0, Math.PI * 2);
            ctx.fill();
            
            // Main grotesque body - sharp skull-like shape
            ctx.fillStyle = (e.hp < 15 && Math.floor(Date.now() / 100) % 2 === 0) ? '#dc2626' : '#1a1a1a';
            ctx.strokeStyle = '#fcee0a';
            ctx.lineWidth = 4;
            ctx.beginPath();
            // Skull-like shape with horns
            ctx.moveTo(0, e.size / 2);  // Bottom jaw
            ctx.lineTo(e.size / 2.5, e.size / 4);
            ctx.lineTo(e.size / 1.8, -e.size / 6);  // Right horn
            ctx.lineTo(e.size / 1.4, -e.size / 2.2);
            ctx.lineTo(e.size / 2, -e.size / 1.8);  // Horn tip
            ctx.lineTo(e.size / 3, -e.size / 2.5);
            ctx.lineTo(0, -e.size / 2 + 5);  // Top of skull
            ctx.lineTo(-e.size / 3, -e.size / 2.5);
            ctx.lineTo(-e.size / 2, -e.size / 1.8);  // Left horn tip
            ctx.lineTo(-e.size / 1.4, -e.size / 2.2);
            ctx.lineTo(-e.size / 1.8, -e.size / 6);  // Left horn
            ctx.lineTo(-e.size / 2.5, e.size / 4);
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
            
            // Multiple demonic eyes (glowing red)
            ctx.shadowBlur = 15;
            ctx.shadowColor = '#dc2626';
            ctx.fillStyle = '#dc2626';
            const eyeGlow = 0.7 + Math.sin(Date.now() / 120) * 0.3;
            ctx.globalAlpha = eyeGlow;
            
            // Main eyes
            ctx.beginPath();
            ctx.arc(-e.size / 6, -e.size / 8, 8, 0, Math.PI * 2);
            ctx.arc(e.size / 6, -e.size / 8, 8, 0, Math.PI * 2);
            ctx.fill();
            
            // Third eye (forehead)
            ctx.beginPath();
            ctx.arc(0, -e.size / 3, 6, 0, Math.PI * 2);
            ctx.fill();
            
            ctx.globalAlpha = 1.0;
            ctx.shadowBlur = 0;
            
            // Terrifying mouth/maw
            ctx.fillStyle = '#450a0a';
            ctx.beginPath();
            ctx.ellipse(0, e.size / 6, e.size / 4, e.size / 8, 0, 0, Math.PI * 2);
            ctx.fill();
            
            // Sharp teeth
            ctx.fillStyle = '#fef08a';
            for (let i = 0; i < 8; i++) {
                const tx = -e.size / 5 + (i * e.size / 20);
                ctx.beginPath();
                ctx.moveTo(tx, e.size / 6 - e.size / 8);
                ctx.lineTo(tx - 2, e.size / 6);
                ctx.lineTo(tx + 2, e.size / 6);
                ctx.fill();
            }
            
            // Energy core (pulsating)
            const coreGradient = ctx.createRadialGradient(0, 0, 0, 0, 0, e.size / 5);
            coreGradient.addColorStop(0, '#ff003c');
            coreGradient.addColorStop(0.5, '#fcee0a');
            coreGradient.addColorStop(1, 'rgba(252, 238, 10, 0)');
            ctx.fillStyle = coreGradient;
            ctx.beginPath();
            ctx.arc(0, 0, e.size / 5 * (0.9 + bossPulse), 0, Math.PI * 2);
            ctx.fill();
            
            // Spikes/protrusions around body
            ctx.fillStyle = '#fcee0a';
            const spikes = 12;
            for (let i = 0; i < spikes; i++) {
                const angle = (i / spikes) * Math.PI * 2;
                ctx.save();
                ctx.rotate(angle);
                ctx.beginPath();
                ctx.moveTo(0, -e.size / 2.2);
                ctx.lineTo(e.size / 12, -e.size / 2 - 8);
                ctx.lineTo(0, -e.size / 2 - 15);
                ctx.lineTo(-e.size / 12, -e.size / 2 - 8);
                ctx.closePath();
                ctx.fill();
                ctx.restore();
            }
            
            ctx.restore();
            
            // Boss HP bar (menacing)
            ctx.fillStyle = '#fcee0a';
            ctx.font = 'bold 16px Arial';
            ctx.shadowBlur = 5;
            ctx.shadowColor = '#fcee0a';
            ctx.fillText('☠ BOSS HP: ' + e.hp + ' ☠', e.x - 60, e.y - e.size / 2 - 15);
            ctx.shadowBlur = 0;
        } else {
            if (e.behavior === 'tank') {
                drawTank(e.x, e.y, e.size, e.hitFlash > 0 ? 'white' : e.color, e.faction);
            } else {
                drawDrone(e.x, e.y, e.size, e.hitFlash > 0 ? 'white' : e.color, e.rotation, e.faction);
            }
        }
    }
    
    // PowerUps
    for (let p of powerUps) {
        ctx.fillStyle = p.type === 'rapid' ? '#fbbf24' : '#00f0ff';
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size / 2 + beatKick, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = 'black';
        ctx.font = '10px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(p.type === 'rapid' ? 'R' : 'W', p.x, p.y + 3);
    }
    
    ctx.restore(); // End beat-reactive camera
    
    // UI (outside camera transform)
    ctx.textAlign = 'left';
    ctx.fillStyle = 'white';
    ctx.font = '20px Arial';
    const style = musicStyles[currentStyle];
    ctx.fillText('Score: ' + score, 20, 30);
    ctx.fillText('Weapon: LVL ' + weaponLevel, 20, 55);
    ctx.fillText('Style: ' + style.name + ' [' + style.vibe.toUpperCase() + ']', 20, 80);
    
    // Combo display (right side, large and flashy)
    if (combo >= 2 || comboDisplay >= 2) {
        const displayCombo = Math.max(combo, Math.floor(comboDisplay));
        const multiplier = Math.min(1 + Math.floor(displayCombo / 2), 10);
        const comboAlpha = combo > 0 ? 1.0 : Math.max(0, comboDisplay / 2);
        
        ctx.save();
        ctx.globalAlpha = comboAlpha;
        ctx.textAlign = 'right';
        ctx.font = 'bold 48px Arial';
        ctx.fillStyle = displayCombo >= 10 ? '#ff00ff' : (displayCombo >= 5 ? '#ff6600' : '#fcee0a');
        ctx.shadowBlur = 20;
        ctx.shadowColor = ctx.fillStyle;
        ctx.fillText(displayCombo + 'x COMBO!', canvas.width - 20, 60);
        
        ctx.font = '20px Arial';
        ctx.fillStyle = '#00f0ff';
        ctx.shadowBlur = 10;
        ctx.fillText('×' + multiplier + ' MULTIPLIER', canvas.width - 20, 90);
        ctx.shadowBlur = 0;
        ctx.restore();
    }
    
    if (!gameStarted) {
        ctx.fillStyle = '#fcee0a';
        ctx.font = '40px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('PRESS ANY KEY TO START', canvas.width / 2, canvas.height / 2);
    } else if (powerUpActive) {
        ctx.fillStyle = '#fcee0a';
        ctx.textAlign = 'left';
        ctx.font = '18px Arial';
        ctx.fillText('RAPID FIRE!', 20, 105);
    }
    
    const missileReady = Date.now() > player.missileCooldown;
    ctx.fillStyle = missileReady ? '#00f0ff' : '#475569';
    ctx.textAlign = 'left';
    ctx.font = '14px Arial';
    ctx.fillText(missileReady ? 'MISSILE: READY' : 'MISSILE: RELOADING', 20, 125);
    
    // Invincibility status
    const now = Date.now();
    if (invincible) {
        const timeLeft = Math.ceil((invincibilityEnd - now) / 1000);
        ctx.fillStyle = '#00f0ff';
        ctx.font = 'bold 16px Arial';
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#00f0ff';
        ctx.fillText('⚡ INVINCIBLE: ' + timeLeft + 's ⚡', 20, 150);
        ctx.shadowBlur = 0;
    } else if (now < invincibilityCooldown) {
        const cooldownLeft = Math.ceil((invincibilityCooldown - now) / 1000);
        ctx.fillStyle = '#475569';
        ctx.font = '14px Arial';
        ctx.fillText('SHIELD COOLDOWN: ' + cooldownLeft + 's', 20, 150);
    } else {
        ctx.fillStyle = '#00f0ff';
        ctx.font = '14px Arial';
        ctx.fillText('SHIELD: READY [SHIFT]', 20, 150);
    }
    
    // Debug info
    ctx.fillStyle = '#555';
    ctx.font = '11px monospace';
    ctx.fillText(`E:${enemies.length} B:${bullets.length} M:${missiles.length} P:${particles.length}`, 20, canvas.height - 10);
    
    // Game Over
    if (gameOver) {
        ctx.fillStyle = 'rgba(0,0,0,0.9)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fcee0a';
        ctx.font = '50px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('GAME OVER', canvas.width / 2, 80);
        ctx.font = '25px Arial';
        ctx.fillStyle = '#00f0ff';
        ctx.fillText('Score: ' + score, canvas.width / 2, 130);
        
        // Display top 10 leaderboard
        if (topScores && topScores.length > 0) {
            ctx.fillStyle = '#fcee0a';
            ctx.font = 'bold 24px Arial';
            ctx.fillText('TOP 10 NETRUNNERS', canvas.width / 2, 180);
            
            ctx.textAlign = 'left';
            ctx.font = '16px monospace';
            const startY = 215;
            const lineHeight = 25;
            
            for (let i = 0; i < Math.min(10, topScores.length); i++) {
                const entry = topScores[i];
                const rank = i + 1;
                const isCurrentUser = typeof CURRENT_USERNAME !== 'undefined' && entry.username === CURRENT_USERNAME;
                
                // Highlight top 3 with different colors
                if (rank === 1) ctx.fillStyle = '#ffd700'; // Gold
                else if (rank === 2) ctx.fillStyle = '#c0c0c0'; // Silver
                else if (rank === 3) ctx.fillStyle = '#cd7f32'; // Bronze
                else if (isCurrentUser) ctx.fillStyle = '#00f0ff'; // Current user
                else ctx.fillStyle = '#94a3b8'; // Regular
                
                const rankStr = (rank + '.').padEnd(4, ' ');
                const nameStr = (entry.name || entry.username).substring(0, 20).padEnd(22, ' ');
                const scoreStr = String(entry.high_score).padStart(8, ' ');
                
                const x = canvas.width / 2 - 200;
                const y = startY + i * lineHeight;
                
                ctx.fillText(rankStr + nameStr + scoreStr, x, y);
            }
        }
        
        ctx.textAlign = 'center';
        ctx.fillStyle = '#00f0ff';
        ctx.font = '20px Arial';
        ctx.fillText('Press R to Restart', canvas.width / 2, canvas.height - 40);
    }
}

function saveScore() {
    const currentHighScore = parseInt(highScoreEl?.innerText || '0');
    
    if (score > currentHighScore) {
        // Update display immediately
        if (highScoreEl) {
            highScoreEl.innerText = score;
        }
        
        // Save to server
        const csrfToken = document.getElementById('csrf-token')?.value;
        if (!csrfToken) {
            console.error('CSRF token not found');
            return;
        }
        
        fetch('/game/score', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
                'x-csrf-token': csrfToken
            },
            body: `score=${score}`
        })
        .then(response => response.json())
        .then(data => {
            console.log('High score saved:', data.highScore);
            // Update with server's confirmed high score
            if (highScoreEl && data.highScore) {
                highScoreEl.innerText = data.highScore;
            }
            // Fetch updated leaderboard
            fetchLeaderboard();
        })
        .catch(error => {
            console.error('Failed to save score:', error);
        });
    } else {
        // Still fetch leaderboard even if not a new high score
        fetchLeaderboard();
    }
}

function fetchLeaderboard() {
    fetch('/api/leaderboard')
        .then(response => response.json())
        .then(data => {
            topScores = data.slice(0, 10); // Top 10
        })
        .catch(error => {
            console.error('Failed to fetch leaderboard:', error);
            topScores = [];
        });
}

function loop() {
    try {
        if (gameStarted) update();
        draw();
    } catch (e) {
        console.error('Loop error:', e);
    }
    requestAnimationFrame(loop);
}

loop();
