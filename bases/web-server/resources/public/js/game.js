const canvas = document.getElementById('gameCanvas');
canvas.style.cursor = 'none'; 
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');

// === CONFIGURATION (Hard Limits) ===
const MAX_ENEMIES = 20;
const MAX_BULLETS = 50;
const MAX_MISSILES = 12;
const MAX_PARTICLES = 80;
const MAX_ENEMY_BULLETS = 25;
const MAX_POWERUPS = 5;

// === Global State ===
let gameStarted = false;
let score = 0; 
let gameOver = false; 
let powerUpActive = false; 
let powerUpTimer = 0;
let weaponLevel = 1; 
let lastBossSpawnScore = 0;

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

// === Music Styles (Enhanced with hi-hat patterns and arpeggios) ===
const musicStyles = [
    { 
        name: 'HIP-HOP', bpm: 90, 
        kick: [0, 6], snare: [4], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [2, 6], // continuous hats, accents on offbeats
        bass: [55, 55, 65, 55], 
        scale: [261.63, 293.66, 329.63, 392.00, 440.00],
        arp: [0, 2, 4, 2], // scale indices for arpeggio
        vibe: 'chill'
    },
    { 
        name: 'TECHNO', bpm: 128, 
        kick: [0, 2, 4, 6], snare: [4], 
        hat: [1, 3, 5, 7], hatAccent: [3, 7], // offbeat hats
        bass: [55, 55, 110, 110], 
        scale: [220.00, 261.63, 293.66, 329.63, 392.00],
        arp: [0, 0, 2, 4],
        vibe: 'drive'
    },
    { 
        name: 'D&B', bpm: 174, 
        kick: [0, 5], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [1, 3, 5, 7],
        bass: [55, 55, 82, 65], 
        scale: [174.61, 196.00, 220.00, 261.63, 293.66],
        arp: [0, 2, 0, 3],
        vibe: 'intense'
    },
    { 
        name: 'SYNTHWAVE', bpm: 105, 
        kick: [0, 4], snare: [2, 6], 
        hat: [0,2,4,6], hatAccent: [2, 6],
        bass: [65, 65, 82, 98], 
        scale: [261.63, 293.66, 329.63, 392.00, 523.25],
        arp: [0, 2, 4, 3],
        vibe: 'retro'
    },
    { 
        name: 'INDUSTRIAL', bpm: 125, 
        kick: [0, 2, 3, 5, 6], snare: [4], 
        hat: [1, 3, 5, 7], hatAccent: [1, 5],
        bass: [41, 41, 41, 44], 
        scale: [146.83, 155.56, 174.61, 196.00, 207.65],
        arp: [0, 1, 0, 1],
        vibe: 'dark'
    },
    { 
        name: 'TRANCE', bpm: 138, 
        kick: [0, 2, 4, 6], snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7], hatAccent: [0,2,4,6],
        bass: [110, 98, 82, 98], 
        scale: [329.63, 392.00, 440.00, 523.25, 587.33],
        arp: [0, 2, 4, 2],
        vibe: 'euphoric'
    }
];
currentStyle = Math.floor(Math.random() * musicStyles.length);

// === Entities ===
const player = { x: 400, y: 500, size: 30, speed: 5, missileCooldown: 0, lastShotTime: 0 };
let bullets = [], enemies = [], enemyBullets = [], powerUps = [], missiles = [], particles = [];
let stars = [], debris = [];
const keys = {};

// === Initialize Background ===
function initBackground() {
    stars = []; debris = [];
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
    } catch (e) {}
}

// Throttled sound player with enhanced sound design
function playSound(type) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    
    const now = Date.now();
    const minInterval = { shoot: 80, missile: 150, explosion: 120, powerup: 100, 'boss-shoot': 180, hit: 50 }[type] || 100;
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
            // Rising sweep with sub
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
            // Layered explosion: noise + low sine thump
            const src = audioCtx.createBufferSource();
            const noiseGain = audioCtx.createGain();
            const filter = audioCtx.createBiquadFilter();
            
            src.buffer = noiseBuffer;
            filter.type = 'lowpass';
            filter.frequency.setValueAtTime(2000, t);
            filter.frequency.exponentialRampToValueAtTime(80, t + 0.4);
            noiseGain.gain.setValueAtTime(0.2, t);
            noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);
            src.connect(filter).connect(noiseGain).connect(compressor);
            src.start(t); src.stop(t + 0.4);
            
            // Sub thump
            const sub = audioCtx.createOscillator();
            const subGain = audioCtx.createGain();
            sub.type = 'sine';
            sub.frequency.setValueAtTime(80, t);
            sub.frequency.exponentialRampToValueAtTime(30, t + 0.3);
            subGain.gain.setValueAtTime(0.25, t);
            subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
            sub.connect(subGain).connect(compressor);
            sub.start(t); sub.stop(t + 0.3);
            
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
        
        // === BASS LINE with filter sweep ===
        const bassOsc = audioCtx.createOscillator();
        const bassGain = audioCtx.createGain();
        const bassFilter = audioCtx.createBiquadFilter();
        const bassNote = style.bass[Math.floor(beatStep / 2) % 4];
        
        bassOsc.type = 'sawtooth';
        bassOsc.frequency.setValueAtTime(bassNote, t);
        bassFilter.type = 'lowpass';
        // Filter sweeps based on LFO and intensity (enhanced automation)
        const intensity = Math.min(score / 500, 1); // 0-1 based on score
        const filterFreq = 150 + filterLFO * 300 + (enemies.length * 10) + intensity * 200;
        bassFilter.frequency.setValueAtTime(filterFreq, t);
        bassFilter.Q.value = 4 + intensity * 4; // More resonance as intensity grows
        bassGain.gain.setValueAtTime(0.12, t);
        bassGain.gain.exponentialRampToValueAtTime(0.001, t + 0.12);
        bassOsc.connect(bassFilter).connect(bassGain).connect(compressor);
        bassOsc.start(t); bassOsc.stop(t + 0.12);
        
        // === SUB-BASS LAYER (pure sine, one octave below) ===
        const subOsc = audioCtx.createOscillator();
        const subGain = audioCtx.createGain();
        subOsc.type = 'sine';
        subOsc.frequency.setValueAtTime(bassNote / 2, t); // One octave below
        subGain.gain.setValueAtTime(0.18, t);
        subGain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
        subOsc.connect(subGain).connect(compressor); // Goes through sidechain
        subOsc.start(t); subOsc.stop(t + 0.15);
        
        // === CHORD STAB on beat 1 (every 8 steps) ===
        if (step === 0) {
            const chordNotes = [style.scale[0], style.scale[2], style.scale[4]]; // Root, third, fifth
            for (let i = 0; i < chordNotes.length; i++) {
                const chordOsc = audioCtx.createOscillator();
                const chordGain = audioCtx.createGain();
                const chordFilter = audioCtx.createBiquadFilter();
                
                chordOsc.type = style.vibe === 'retro' ? 'square' : 'sawtooth';
                chordOsc.frequency.setValueAtTime(chordNotes[i], t);
                
                chordFilter.type = 'lowpass';
                chordFilter.frequency.setValueAtTime(2000 + intensity * 1000, t);
                chordFilter.frequency.exponentialRampToValueAtTime(400, t + 0.4);
                
                chordGain.gain.setValueAtTime(0.05, t);
                chordGain.gain.linearRampToValueAtTime(0.04, t + 0.05);
                chordGain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
                
                chordOsc.connect(chordFilter).connect(chordGain).connect(compressor);
                chordOsc.start(t); chordOsc.stop(t + 0.5);
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
            kickOsc.type = 'sine';
            kickOsc.frequency.setValueAtTime(160, t);
            kickOsc.frequency.exponentialRampToValueAtTime(35, t + 0.15);
            kickGain.gain.setValueAtTime(0.6, t);
            kickGain.gain.setValueAtTime(0.5, t + 0.01);
            kickGain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
            kickOsc.connect(kickGain).connect(masterGain); // Direct to master (no duck)
            kickOsc.start(t); kickOsc.stop(t + 0.35);
            
            // Click transient
            const click = audioCtx.createOscillator();
            const clickGain = audioCtx.createGain();
            click.type = 'square';
            click.frequency.value = 1200;
            clickGain.gain.setValueAtTime(0.1, t);
            clickGain.gain.exponentialRampToValueAtTime(0.001, t + 0.015);
            click.connect(clickGain).connect(masterGain); // Direct to master
            click.start(t); click.stop(t + 0.015);
        }
        
        // === SNARE DRUM ===
        if (style.snare.includes(step) && noiseBuffer) {
            beatSnare = 1.0;
            
            // Noise body
            const snare = audioCtx.createBufferSource();
            const snareGain = audioCtx.createGain();
            const snareFilter = audioCtx.createBiquadFilter();
            snare.buffer = noiseBuffer;
            snareFilter.type = 'bandpass';
            snareFilter.frequency.value = 3000;
            snareFilter.Q.value = 1;
            snareGain.gain.setValueAtTime(0.18, t);
            snareGain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
            snare.connect(snareFilter).connect(snareGain).connect(compressor);
            snare.start(t); snare.stop(t + 0.15);
            
            // Tone body
            const snareOsc = audioCtx.createOscillator();
            const snareOscGain = audioCtx.createGain();
            snareOsc.type = 'triangle';
            snareOsc.frequency.setValueAtTime(200, t);
            snareOsc.frequency.exponentialRampToValueAtTime(120, t + 0.05);
            snareOscGain.gain.setValueAtTime(0.12, t);
            snareOscGain.gain.exponentialRampToValueAtTime(0.001, t + 0.08);
            snareOsc.connect(snareOscGain).connect(compressor);
            snareOsc.start(t); snareOsc.stop(t + 0.08);
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
    initAudio();
}

window.addEventListener('keydown', e => {
    if (!gameStarted) startGame();
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space', 'KeyR'].includes(e.code)) e.preventDefault();
    keys[e.code] = true;
    
    if (e.code === 'Space' && !e.repeat) {
        weaponLevel = (weaponLevel % 3) + 1;
        currentStyle = (currentStyle + 1) % musicStyles.length;
        playSound('transition');
        beatStep = 0; // Reset beat for clean transition
    }
    
    if (e.code === 'KeyR' && gameOver) {
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; missiles = []; particles = [];
        gameOver = false; lastBossSpawnScore = 0;
        player.x = canvas.width / 2; player.y = canvas.height - 60;
        weaponLevel = 1; powerUpActive = false; player.missileCooldown = 0;
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
            x: canvas.width / 2, y: -100, vx: 0, vy: 1, size: 80, color: '#fcee0a',
            hp: 20, isBoss: true, faction, behavior: 'boss', lastShot: 0, hitFlash: 0, rotation: 0
        });
    } else {
        const rand = Math.random();
        let hp = 1, size = 25, color = '#a855f7', behavior = 'strafing';
        
        if (rand < 0.25) { hp = 1; size = 18; color = faction === 'zerg' ? '#d97706' : '#facc15'; behavior = 'kamikaze'; }
        else if (rand < 0.4) { hp = 3; size = 35; color = faction === 'zerg' ? '#7f1d1d' : '#0ea5e9'; behavior = 'hover'; }
        else { color = faction === 'zerg' ? '#a855f7' : '#eab308'; }
        
        enemies.push({
            x, y: -50, vx: 0, vy: 0, size, color, hp, isBoss: false, faction, behavior,
            lastShot: 0, hitFlash: 0, rotation: 0, targetX: x, lastTurn: Date.now()
        });
    }
}

function killEnemy(e) {
    e.dead = true;
    playSound('explosion');
    score += e.isBoss ? 100 : (e.hp >= 3 ? 30 : 10);
    
    // Limited particles
    const count = Math.min(Math.floor(e.size / 5), 6);
    for (let i = 0; i < count && particles.length < MAX_PARTICLES; i++) {
        particles.push({
            x: e.x + (Math.random() - 0.5) * e.size,
            y: e.y + (Math.random() - 0.5) * e.size,
            vx: (Math.random() - 0.5) * 4,
            vy: (Math.random() - 0.5) * 4,
            life: 40 + Math.random() * 20,
            color: '#00f0ff'
        });
    }
    
    // PowerUp drop
    if (Math.random() < 0.12 && powerUps.length < MAX_POWERUPS) {
        powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15, type: Math.random() < 0.5 ? 'upgrade' : 'rapid' });
    }
}

function update() {
    if (gameOver) return;
    
    const now = Date.now();
    
    // Background
    for (let s of stars) { s.y += s.speed; if (s.y > canvas.height) { s.y = 0; s.x = Math.random() * canvas.width; } }
    for (let d of debris) { d.y += d.speed; d.rotation += d.rotSpeed; if (d.y > canvas.height + 50) { d.y = -50; d.x = Math.random() * canvas.width; } }
    
    // Player movement
    if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
    if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
    if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
    if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
    
    // Player shooting
    const cooldown = powerUpActive ? 120 : 200;
    if (now - player.lastShotTime > cooldown && bullets.length < MAX_BULLETS) {
        playSound('shoot');
        bullets.push({ x: player.x + player.size / 2, y: player.y, vx: 0, vy: -12, damage: 2 });
        if (weaponLevel >= 2) {
            bullets.push({ x: player.x, y: player.y + 10, vx: -1.5, vy: -10, damage: 1.5 });
            bullets.push({ x: player.x + player.size, y: player.y + 10, vx: 1.5, vy: -10, damage: 1.5 });
        }
        if (weaponLevel >= 3) {
            bullets.push({ x: player.x - 10, y: player.y + 15, vx: -3, vy: -9, damage: 1 });
            bullets.push({ x: player.x + player.size + 10, y: player.y + 15, vx: 3, vy: -9, damage: 1 });
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
                    target, damage: 5, speed: 7, angle: -Math.PI / 2 + (Math.random() - 0.5) * 0.4
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
        return m.y > -50 && m.y < canvas.height + 50 && m.x > -50 && m.x < canvas.width + 50;
    });
    
    // Update particles
    particles = particles.filter(p => { p.x += p.vx; p.y += p.vy; p.life--; return p.life > 0; });
    
    // Update enemy bullets
    enemyBullets = enemyBullets.filter(b => {
        b.x += b.vx; b.y += b.vy;
        const dx = b.x - player.x - player.size / 2;
        const dy = b.y - player.y - player.size / 2;
        if (Math.sqrt(dx * dx + dy * dy) < player.size / 2 + 5) {
            gameOver = true; saveScore(); return false;
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
            const rage = 1 + (20 - e.hp) / 10;
            if (e.y < 100) e.y += 1;
            else e.x += Math.sin(now / (600 / rage)) * 2 * rage;
            
            if (now - e.lastShot > 2000 / rage && enemyBullets.length < MAX_ENEMY_BULLETS) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 4 * rage, vy: Math.sin(angle) * 4 * rage });
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
                } else {
                    if (now - e.lastTurn > 1200) { e.targetX = Math.random() * canvas.width; e.lastTurn = now; }
                    e.vx = (e.targetX - e.x) * 0.02;
                    e.vy = 1.2;
                }
                e.x += e.vx; e.y += e.vy;
            }
            
            // Enemy shooting (Protoss only, Zerg don't shoot)
            if (e.faction === 'protoss' && now - e.lastShot > 2500 && enemyBullets.length < MAX_ENEMY_BULLETS && e.y < player.y - 80) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 3, vy: Math.sin(angle) * 3 });
                e.lastShot = now;
            }
        }
        
        // Enemy collision with player
        const dx = e.x - player.x - player.size / 2;
        const dy = e.y - player.y - player.size / 2;
        if (Math.sqrt(dx * dx + dy * dy) < e.size / 2 + player.size / 2) {
            gameOver = true; saveScore();
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
    if (Math.random() < 0.018 && enemies.length < MAX_ENEMIES - 2) spawnEnemy();
    if (score >= lastBossSpawnScore + 500 && !enemies.some(e => e.isBoss)) {
        spawnEnemy(true);
        lastBossSpawnScore = score;
    }
    
    // Beat decay
    beatKick *= 0.9;
    beatSnare *= 0.88;
    beatHat *= 0.85;
    
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
    ctx.fillStyle = color;
    
    if (faction === 'zerg') {
        // Organic zerg shape
        ctx.beginPath();
        ctx.arc(0, -size / 4, size / 3, Math.PI, 0);
        ctx.quadraticCurveTo(size / 2, size / 4, 0, size / 2);
        ctx.quadraticCurveTo(-size / 2, size / 4, -size / 3, -size / 4);
        ctx.fill();
        ctx.fillStyle = '#facc15';
        ctx.beginPath();
        ctx.arc(-size / 5, -size / 5, 2, 0, Math.PI * 2);
        ctx.arc(size / 5, -size / 5, 2, 0, Math.PI * 2);
        ctx.fill();
    } else {
        // Angular protoss shape
        ctx.beginPath();
        ctx.moveTo(0, -size / 2);
        ctx.lineTo(size / 2, size / 4);
        ctx.lineTo(0, size / 2);
        ctx.lineTo(-size / 2, size / 4);
        ctx.closePath();
        ctx.fill();
        ctx.fillStyle = '#38bdf8';
        ctx.beginPath();
        ctx.arc(0, 0, size / 5, 0, Math.PI * 2);
        ctx.fill();
    }
    ctx.restore();
}

function draw() {
    // Clear
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Beat-reactive camera
    ctx.save();
    const scale = 1.0 + beatKick * 0.018;
    const shakeX = (Math.random() - 0.5) * beatSnare * 6;
    const shakeY = (Math.random() - 0.5) * beatSnare * 6;
    ctx.translate(canvas.width / 2 + shakeX, canvas.height / 2 + shakeY);
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
    
    // Particles
    for (let p of particles) {
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x - 2, p.y - 2, 4, 4);
    }
    
    // Player
    ctx.save();
    ctx.translate(player.x + player.size / 2, player.y + player.size / 2);
    ctx.fillStyle = powerUpActive ? '#fbbf24' : '#64748b';
    ctx.beginPath();
    ctx.moveTo(0, -player.size);
    ctx.lineTo(player.size / 2, 0);
    ctx.lineTo(player.size / 3, player.size);
    ctx.lineTo(-player.size / 3, player.size);
    ctx.lineTo(-player.size / 2, 0);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#00f0ff';
    ctx.beginPath();
    ctx.moveTo(0, -player.size / 2);
    ctx.lineTo(5, -10);
    ctx.lineTo(0, 0);
    ctx.lineTo(-5, -10);
    ctx.fill();
    // Engine glow (beat reactive)
    ctx.fillStyle = '#f59e0b';
    ctx.beginPath();
    ctx.arc(-5, player.size, 3 + beatKick * 3, 0, Math.PI * 2);
    ctx.arc(5, player.size, 3 + beatKick * 3, 0, Math.PI * 2);
    ctx.fill();
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
    ctx.fillStyle = '#f87171';
    for (let b of enemyBullets) {
        ctx.beginPath();
        ctx.arc(b.x, b.y, 5, 0, Math.PI * 2);
        ctx.fill();
    }
    
    // Enemies
    for (let e of enemies) {
        if (e.dead) continue;
        if (e.isBoss) {
            ctx.save();
            ctx.translate(e.x, e.y);
            ctx.scale(1 + beatKick * 0.03, 1 + beatKick * 0.03);
            ctx.fillStyle = (e.hp < 10 && Math.floor(Date.now() / 100) % 2 === 0) ? '#ff003c' : '#2a2a2a';
            ctx.strokeStyle = '#fcee0a';
            ctx.lineWidth = 3;
            ctx.beginPath();
            ctx.moveTo(0, e.size / 2);
            ctx.lineTo(e.size / 2, -e.size / 4);
            ctx.lineTo(e.size / 1.5, -e.size / 2);
            ctx.lineTo(0, -e.size / 2 + 10);
            ctx.lineTo(-e.size / 1.5, -e.size / 2);
            ctx.lineTo(-e.size / 2, -e.size / 4);
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
            ctx.fillStyle = '#ff003c';
            ctx.beginPath();
            ctx.arc(0, 0, e.size / 4, 0, Math.PI * 2);
            ctx.fill();
            ctx.restore();
            ctx.fillStyle = '#fcee0a';
            ctx.font = 'bold 14px Arial';
            ctx.fillText('BOSS HP: ' + e.hp, e.x - 40, e.y - e.size / 2 - 10);
        } else {
            drawDrone(e.x, e.y, e.size, e.hitFlash > 0 ? 'white' : e.color, e.rotation, e.faction);
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
    
    // Debug info
    ctx.fillStyle = '#555';
    ctx.font = '11px monospace';
    ctx.fillText(`E:${enemies.length} B:${bullets.length} M:${missiles.length} P:${particles.length}`, 20, canvas.height - 10);
    
    // Game Over
    if (gameOver) {
        ctx.fillStyle = 'rgba(0,0,0,0.85)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fcee0a';
        ctx.font = '50px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('GAME OVER', canvas.width / 2, canvas.height / 2);
        ctx.font = '25px Arial';
        ctx.fillStyle = '#00f0ff';
        ctx.fillText('Score: ' + score, canvas.width / 2, canvas.height / 2 + 50);
        ctx.fillText('Press R to Restart', canvas.width / 2, canvas.height / 2 + 90);
    }
}

function saveScore() {
    if (highScoreEl && score > parseInt(highScoreEl.innerText || '0')) {
        highScoreEl.innerText = score;
    }
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
