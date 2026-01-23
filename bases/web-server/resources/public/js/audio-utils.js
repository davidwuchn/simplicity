/**
 * Audio System Utilities
 * 
 * Modular audio synthesis functions for game sounds and music.
 */

/**
 * Create a reverb impulse response buffer
 * @param {AudioContext} ctx - Web Audio API context
 * @param {number} duration - Reverb duration in seconds
 * @param {number} decay - Decay rate
 * @returns {AudioBuffer} Impulse response buffer
 */
function createReverbBuffer(ctx, duration = 1.5, decay = 2.0) {
    const sampleRate = ctx.sampleRate;
    const length = sampleRate * duration;
    const buffer = ctx.createBuffer(2, length, sampleRate);
    
    for (let channel = 0; channel < 2; channel++) {
        const channelData = buffer.getChannelData(channel);
        for (let i = 0; i < length; i++) {
            channelData[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / length, decay);
        }
    }
    
    return buffer;
}

/**
 * Create white noise buffer for percussion
 * @param {AudioContext} ctx - Web Audio API context
 * @param {number} duration - Buffer duration in seconds
 * @returns {AudioBuffer} Noise buffer
 */
function createNoiseBuffer(ctx, duration = 1.0) {
    const sampleRate = ctx.sampleRate;
    const length = sampleRate * duration;
    const buffer = ctx.createBuffer(1, length, sampleRate);
    const data = buffer.getChannelData(0);
    
    for (let i = 0; i < length; i++) {
        data[i] = Math.random() * 2 - 1;
    }
    
    return buffer;
}

/**
 * Play a synthesized kick drum
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {masterGain, compressor}
 * @param {number} time - Start time
 * @param {number} decay - Decay time in seconds
 */
function playKick(ctx, nodes, time, decay = 0.35) {
    const kickOsc = ctx.createOscillator();
    const kickGain = ctx.createGain();
    
    kickOsc.type = 'sine';
    kickOsc.frequency.setValueAtTime(160, time);
    kickOsc.frequency.exponentialRampToValueAtTime(35, time + decay * 0.5);
    
    kickGain.gain.setValueAtTime(0.65, time);
    kickGain.gain.setValueAtTime(0.55, time + 0.01);
    kickGain.gain.exponentialRampToValueAtTime(0.001, time + decay);
    
    kickOsc.connect(kickGain).connect(nodes.masterGain);
    kickOsc.start(time);
    kickOsc.stop(time + decay);
    
    // Click transient
    const click = ctx.createOscillator();
    const clickGain = ctx.createGain();
    click.type = 'square';
    click.frequency.value = 1200;
    clickGain.gain.setValueAtTime(0.12, time);
    clickGain.gain.exponentialRampToValueAtTime(0.001, time + 0.015);
    click.connect(clickGain).connect(nodes.masterGain);
    click.start(time);
    click.stop(time + 0.015);
}

/**
 * Play a synthesized snare drum
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {compressor, noiseBuffer}
 * @param {number} time - Start time
 * @param {number} decay - Decay time in seconds
 */
function playSnare(ctx, nodes, time, decay = 0.15) {
    if (!nodes.noiseBuffer) return;
    
    // Noise body
    const snare = ctx.createBufferSource();
    const snareGain = ctx.createGain();
    const snareFilter = ctx.createBiquadFilter();
    
    snare.buffer = nodes.noiseBuffer;
    snareFilter.type = 'bandpass';
    snareFilter.frequency.value = 3000;
    snareFilter.Q.value = 1;
    
    snareGain.gain.setValueAtTime(0.2, time);
    snareGain.gain.exponentialRampToValueAtTime(0.001, time + decay);
    
    snare.connect(snareFilter).connect(snareGain).connect(nodes.compressor);
    snare.start(time);
    snare.stop(time + decay);
    
    // Tone body
    const snareOsc = ctx.createOscillator();
    const snareOscGain = ctx.createGain();
    snareOsc.type = 'triangle';
    snareOsc.frequency.setValueAtTime(200, time);
    snareOsc.frequency.exponentialRampToValueAtTime(120, time + decay * 0.4);
    
    snareOscGain.gain.setValueAtTime(0.14, time);
    snareOscGain.gain.exponentialRampToValueAtTime(0.001, time + decay * 0.6);
    
    snareOsc.connect(snareOscGain).connect(nodes.compressor);
    snareOsc.start(time);
    snareOsc.stop(time + decay * 0.6);
}

/**
 * Play a synthesized hi-hat
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {compressor, noiseBuffer}
 * @param {number} time - Start time
 * @param {boolean} isAccent - Whether this is an accented hit
 */
function playHiHat(ctx, nodes, time, isAccent = false) {
    if (!nodes.noiseBuffer) return;
    
    const hat = ctx.createBufferSource();
    const hatGain = ctx.createGain();
    const hatFilter = ctx.createBiquadFilter();
    const hatHP = ctx.createBiquadFilter();
    
    hat.buffer = nodes.noiseBuffer;
    hatFilter.type = 'bandpass';
    hatFilter.frequency.value = isAccent ? 10000 : 8000;
    hatFilter.Q.value = 2;
    hatHP.type = 'highpass';
    hatHP.frequency.value = 7000;
    
    const vol = isAccent ? 0.08 : 0.04;
    const decay = isAccent ? 0.08 : 0.04;
    
    hatGain.gain.setValueAtTime(vol, time);
    hatGain.gain.exponentialRampToValueAtTime(0.001, time + decay);
    
    hat.connect(hatHP).connect(hatFilter).connect(hatGain).connect(nodes.compressor);
    hat.start(time);
    hat.stop(time + decay);
}

/**
 * Play a bass note with filter sweep
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {compressor}
 * @param {number} time - Start time
 * @param {number} frequency - Bass frequency in Hz
 * @param {string} waveType - Oscillator type ('sine', 'square', 'sawtooth')
 * @param {number} filterFreq - Filter cutoff frequency
 * @param {number} resonance - Filter Q value
 */
function playBass(ctx, nodes, time, frequency, waveType = 'sawtooth', filterFreq = 400, resonance = 4) {
    // Main bass
    const bassOsc = ctx.createOscillator();
    const bassGain = ctx.createGain();
    const bassFilter = ctx.createBiquadFilter();
    
    bassOsc.type = waveType;
    bassOsc.frequency.setValueAtTime(frequency, time);
    
    bassFilter.type = 'lowpass';
    bassFilter.frequency.setValueAtTime(filterFreq, time);
    bassFilter.Q.value = resonance;
    
    bassGain.gain.setValueAtTime(0.14, time);
    bassGain.gain.exponentialRampToValueAtTime(0.001, time + 0.14);
    
    bassOsc.connect(bassFilter).connect(bassGain).connect(nodes.compressor);
    bassOsc.start(time);
    bassOsc.stop(time + 0.14);
    
    // Sub-bass layer
    const subOsc = ctx.createOscillator();
    const subGain = ctx.createGain();
    subOsc.type = 'sine';
    subOsc.frequency.setValueAtTime(frequency / 2, time);
    
    subGain.gain.setValueAtTime(0.18, time);
    subGain.gain.exponentialRampToValueAtTime(0.001, time + 0.15);
    
    subOsc.connect(subGain).connect(nodes.compressor);
    subOsc.start(time);
    subOsc.stop(time + 0.15);
}

/**
 * Play a melodic note
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {compressor}
 * @param {number} time - Start time
 * @param {number} frequency - Note frequency in Hz
 * @param {string} waveType - Oscillator type
 * @param {number} duration - Note duration in seconds
 * @param {number} volume - Note volume (0-1)
 */
function playMelody(ctx, nodes, time, frequency, waveType = 'square', duration = 0.25, volume = 0.12) {
    const melody = ctx.createOscillator();
    const melodyGain = ctx.createGain();
    const melodyFilter = ctx.createBiquadFilter();
    
    melody.type = waveType;
    melody.frequency.setValueAtTime(frequency, time);
    
    melodyFilter.type = 'lowpass';
    melodyFilter.frequency.setValueAtTime(frequency * 4, time);
    melodyFilter.frequency.exponentialRampToValueAtTime(frequency * 2, time + duration * 0.8);
    melodyFilter.Q.value = 2;
    
    melodyGain.gain.setValueAtTime(volume, time);
    melodyGain.gain.exponentialRampToValueAtTime(0.001, time + duration);
    
    melody.connect(melodyFilter).connect(melodyGain).connect(nodes.compressor);
    melody.start(time);
    melody.stop(time + duration);
}

/**
 * Play a chord (multiple notes simultaneously)
 * @param {AudioContext} ctx - Audio context
 * @param {Object} nodes - Audio nodes {compressor}
 * @param {number} time - Start time
 * @param {Array<number>} frequencies - Array of note frequencies
 * @param {string} waveType - Oscillator type
 * @param {number} duration - Chord duration in seconds
 * @param {number} volume - Chord volume (0-1)
 */
function playChord(ctx, nodes, time, frequencies, waveType = 'sawtooth', duration = 0.4, volume = 0.08) {
    frequencies.forEach(freq => {
        const chordOsc = ctx.createOscillator();
        const chordGain = ctx.createGain();
        const chordFilter = ctx.createBiquadFilter();
        
        chordOsc.type = waveType;
        chordOsc.frequency.setValueAtTime(freq, time);
        
        chordFilter.type = 'lowpass';
        chordFilter.frequency.setValueAtTime(2000, time);
        chordFilter.frequency.exponentialRampToValueAtTime(400, time + duration);
        
        chordGain.gain.setValueAtTime(volume, time);
        chordGain.gain.linearRampToValueAtTime(volume * 0.75, time + 0.05);
        chordGain.gain.exponentialRampToValueAtTime(0.001, time + duration);
        
        chordOsc.connect(chordFilter).connect(chordGain).connect(nodes.compressor);
        chordOsc.start(time);
        chordOsc.stop(time + duration);
    });
}

// Export functions
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        createReverbBuffer,
        createNoiseBuffer,
        playKick,
        playSnare,
        playHiHat,
        playBass,
        playMelody,
        playChord
    };
}
