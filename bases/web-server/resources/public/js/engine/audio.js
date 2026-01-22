import { NOTES } from './config.js';

export const MusicStyles = [
    { 
        name: 'DRILL', bpm: 140, 
        tick: 60000 / 140 / 4, 
        kick:  [1,0,0,0, 0,0,0,1, 0,0,1,0, 0,0,0,0], 
        snare: [0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0], 
        hat:   [0,0,1,0, 0,0,1,0, 0,0,1,0, 0,0,1,0], 
        bass:  [NOTES.A1,0,0,0, NOTES.A1,0,0,0, 0,0,NOTES.A1,0, NOTES.A1,0,0,0], 
        scale: [146.83, 174.61, 196.00, 220.00, 261.63] // D Minor
    },
    { 
        name: 'WEST-COAST', bpm: 95, 
        tick: 60000 / 95 / 4, 
        kick:  [1,0,0,0, 0,0,1,0, 0,0,0,0, 0,0,1,0], 
        snare: [0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0], 
        hat:   [1,0,1,0, 1,0,1,0, 1,0,1,0, 1,0,1,0], 
        bass:  [43.65,0,0,0, 0,0,0,0, 0,0,0,0, 0,0,0,0], // F1
        scale: [174.61, 207.65, 233.08, 261.63, 311.13] // F Minor
    },
    { 
        name: 'MEMPHIS', bpm: 130, 
        tick: 60000 / 130 / 4, 
        kick:  [1,0,1,0, 0,0,1,0, 1,0,0,0, 0,0,1,0], 
        snare: [0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0], 
        hat:   [1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1], 
        bass:  [NOTES.C2,0,0,0, NOTES.C2,0,0,0, NOTES.D2,0,0,0, NOTES.C2,0,0,0], 
        scale: [261.63, 277.18, 311.13, 349.23, 392.00] // Phrygian
    },
    { 
        name: 'OLD-SCHOOL', bpm: 90, 
        tick: 60000 / 90 / 4, 
        kick:  [1,0,0,1, 0,0,0,0, 0,1,0,0, 0,0,1,0], 
        snare: [0,0,0,0, 1,0,0,0, 0,0,0,0, 1,0,0,0], 
        hat:   [1,0,1,0, 1,0,1,0, 1,0,1,0, 1,0,1,0], 
        bass:  [49.00,0,0,0, 0,0,0,0, NOTES.A1,0,0,0, 0,0,0,0], 
        scale: [196.00, 233.08, 261.63, 293.66, 349.23] 
    }
];

export class AudioSystem {
    constructor() {
        this.ctx = null;
        this.masterOut = null;
        this.noiseBuffer = null;
        this.bgmInterval = null;
        this.currentStyleIdx = Math.floor(Math.random() * MusicStyles.length);
        this.melodyQueue = [];
        this.beatState = { kick: 0, snare: 0 };
    }

    init() {
        if (this.ctx) {
            if (this.ctx.state === 'suspended') this.ctx.resume();
            return;
        }
        try {
            const AudioContext = window.AudioContext || window.webkitAudioContext;
            this.ctx = new AudioContext();
            
            // Compressor
            const compressor = this.ctx.createDynamicsCompressor();
            compressor.threshold.value = -10;
            compressor.ratio.value = 12;
            compressor.connect(this.ctx.destination);
            
            // Master Gain
            this.masterOut = this.ctx.createGain();
            this.masterOut.gain.value = 0.5;
            this.masterOut.connect(compressor);

            // Noise Buffer
            const bufferSize = this.ctx.sampleRate * 2;
            this.noiseBuffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
            const output = this.noiseBuffer.getChannelData(0);
            for (let i = 0; i < bufferSize; i++) output[i] = Math.random() * 2 - 1;

            this.startBGM();
            
            // Unlocker
            const unlock = () => {
                if (this.ctx && this.ctx.state === 'suspended') this.ctx.resume();
            };
            ['click', 'keydown', 'touchstart'].forEach(evt => document.addEventListener(evt, unlock, { once: true }));
            
        } catch (e) { console.error("Audio Init Failed", e); }
    }

    get style() { return MusicStyles[this.currentStyleIdx]; }

    cycleStyle() {
        this.currentStyleIdx = (this.currentStyleIdx + 1) % MusicStyles.length;
        this.startBGM();
        this.playSFX('powerup');
    }

    startBGM() {
        if (this.bgmInterval) clearInterval(this.bgmInterval);
        if (!this.ctx) return;

        let step = 0;
        this.bgmInterval = setInterval(() => {
            if (this.ctx.state !== 'running') return;
            const t = this.ctx.currentTime;
            const style = this.style;
            const beat = step % 16;

            try {
                this.playSequencer(style, beat, t);
                step++;
            } catch(e) {}
        }, this.style.tick);
    }

    playSequencer(style, beat, t) {
        // Melody
        if (this.melodyQueue.length > 0) {
            const freq = this.melodyQueue.shift();
            this.tone(freq, t, 0.5, 'sine', 0.15, 0.25); // Add delay
        }

        // Bass
        const bassFreq = style.bass[beat];
        if (bassFreq > 0) {
            const type = (style.name === 'WEST-COAST' || style.name === 'MEMPHIS') ? 'sawtooth' : 'sine';
            this.bassTone(bassFreq, t, type, style.name === 'DRILL');
        }

        // Drums
        if (style.kick[beat]) {
            this.beatState.kick = 1.0;
            this.kick(t);
        }
        if (style.snare[beat]) {
            this.beatState.snare = 1.0;
            this.snare(t);
        }
        if (style.hat[beat]) {
            this.hat(t, style.name === 'DRILL');
        }
    }

    playSFX(type) {
        if (!this.ctx || this.ctx.state !== 'running') return;
        const t = this.ctx.currentTime;
        const scale = this.style.scale;

        if (type === 'shoot') {
            const note = scale[Math.floor(Math.random() * scale.length)];
            this.tone(note, t, 0.1, 'triangle', 0.05);
        } else if (type === 'missile') {
            const note = scale[0] / 2;
            this.rampTone(note, note * 4, t, 0.3, 'sawtooth');
        } else if (type === 'explosion') {
            this.noise(t, 0.5, 1000);
        } else if (type === 'powerup') {
            this.rampTone(440, 880, t, 0.3, 'sine');
        } else if (type === 'boss-shoot') {
            this.rampTone(440, 220, t, 0.2, 'sine');
        }
    }

    // --- Synthesizers ---

    tone(freq, t, dur, type, vol, delayTime = 0) {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(freq, t);
        
        gain.gain.setValueAtTime(0, t);
        gain.gain.linearRampToValueAtTime(vol, t + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.001, t + dur);

        osc.connect(gain);
        gain.connect(this.masterOut);

        if (delayTime > 0) {
            const delay = this.ctx.createDelay();
            delay.delayTime.value = delayTime;
            const dGain = this.ctx.createGain();
            dGain.gain.value = 0.3;
            gain.connect(delay);
            delay.connect(dGain);
            dGain.connect(this.masterOut);
        }

        osc.start(t);
        osc.stop(t + dur);
    }

    bassTone(freq, t, type, glide) {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        const filter = this.ctx.createBiquadFilter();
        
        osc.type = type;
        osc.frequency.setValueAtTime(freq, t);
        if (glide) osc.frequency.exponentialRampToValueAtTime(freq / 2, t + 0.3);

        filter.type = 'lowpass';
        filter.frequency.setValueAtTime(type === 'sawtooth' ? 600 : 200, t);

        gain.gain.setValueAtTime(0.6, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.4);

        osc.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterOut);
        
        osc.start(t);
        osc.stop(t + 0.4);
    }

    rampTone(start, end, t, dur, type) {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(start, t);
        osc.frequency.linearRampToValueAtTime(end, t + dur);
        
        gain.gain.setValueAtTime(0.1, t);
        gain.gain.linearRampToValueAtTime(0.001, t + dur);
        
        osc.connect(gain);
        gain.connect(this.masterOut);
        osc.start(t);
        osc.stop(t + dur);
    }

    kick(t) {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(150, t);
        osc.frequency.exponentialRampToValueAtTime(0.01, t + 0.5);
        gain.gain.setValueAtTime(0.8, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
        osc.connect(gain);
        gain.connect(this.masterOut);
        osc.start(t);
        osc.stop(t + 0.5);
    }

    snare(t) {
        this.noise(t, 0.2, 800, 'highpass', 0.4);
    }

    hat(t, roll) {
        const dur = (roll && Math.random() < 0.2) ? 0.02 : 0.05;
        this.noise(t, dur, 6000, 'highpass', 0.15);
    }

    noise(t, dur, freq, filterType = 'lowpass', vol = 0.2) {
        if (!this.noiseBuffer) return;
        const src = this.ctx.createBufferSource();
        src.buffer = this.noiseBuffer;
        const filter = this.ctx.createBiquadFilter();
        const gain = this.ctx.createGain();
        
        filter.type = filterType;
        filter.frequency.setValueAtTime(freq, t);
        if (filterType === 'lowpass') filter.frequency.exponentialRampToValueAtTime(100, t + dur);
        
        gain.gain.setValueAtTime(vol, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + dur);
        
        src.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterOut);
        src.start(t);
        src.stop(t + dur);
    }
}
