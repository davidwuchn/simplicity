/**
 * Music Configuration for Japanese Game-Inspired Soundtrack
 * 
 * Each music style contains:
 * - name: Display name
 * - bpm: Beats per minute
 * - kick/snare/hat: Drum patterns (8-step sequencer, 0-7)
 * - bass: 4-note bassline (frequencies in Hz)
 * - scale: Musical scale (frequencies in Hz)
 * - arp: Arpeggio pattern (indices into scale array)
 * - melody: Melodic hook pattern (indices into scale array)
 * - chords: Chord progressions (arrays of scale indices)
 * - vibe: Character/mood descriptor
 * - kickDecay/snareDecay: Drum envelope decay times
 * - bassType: Oscillator waveform for bass ('sine', 'square', 'sawtooth')
 * - melodyOct: Melody octave multiplier (1 or 2)
 */

const MUSIC_STYLES = [
    { 
        name: 'MEGA BUSTER',
        bpm: 145,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [1,3,5,7],
        bass: [65.41, 82.41, 73.42, 82.41], // E2-F#2-D#2-F#2
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 523.25, 587.33, 659.25], // E4 Phrygian
        arp: [0, 2, 4, 7, 4, 2],
        melody: [0, 2, 4, 2, 0, 4, 7, 4],
        chords: [[0,2,4], [1,3,5], [2,4,6], [0,2,4]],
        vibe: 'heroic',
        kickDecay: 0.25,
        snareDecay: 0.08,
        bassType: 'square',
        melodyOct: 1,
        description: 'Mega Man/Capcom: Fast, energetic, memorable melodies with NES/Famicom sound'
    },
    { 
        name: 'GRADIUS CORE',
        bpm: 160,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [0,2,4,6],
        bass: [55.00, 73.42, 82.41, 73.42], // A1-D#2-F#2-D#2
        scale: [220.00, 261.63, 293.66, 329.63, 392.00, 440.00, 523.25], // A Minor
        arp: [0, 4, 7, 4, 0, 4, 7, 4],
        melody: [7, 5, 4, 2, 4, 5, 7, 0],
        chords: [[0,4,7], [2,5,0], [4,7,2], [0,4,7]],
        vibe: 'intense',
        kickDecay: 0.22,
        snareDecay: 0.07,
        bassType: 'square',
        melodyOct: 2,
        description: 'Gradius/Konami: Intense, technical, fast-paced with dramatic octave jumps'
    },
    { 
        name: 'BUBBLE SYSTEM',
        bpm: 130,
        kick: [0, 4],
        snare: [2, 6], 
        hat: [0,2,4,6],
        hatAccent: [2,6],
        bass: [130.81, 146.83, 164.81, 130.81], // C3-D3-E3-C3
        scale: [261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25], // C Major
        arp: [0, 2, 4, 5, 4, 2],
        melody: [0, 2, 4, 5, 4, 2, 0, 4],
        chords: [[0,2,4], [3,5,7], [0,2,4], [1,3,5]],
        vibe: 'cheerful',
        kickDecay: 0.35,
        snareDecay: 0.12,
        bassType: 'sine',
        melodyOct: 1,
        description: 'Bubble Bobble/Taito: Cute, bouncy, catchy with playful atmosphere'
    },
    { 
        name: 'CASTLEVANIA',
        bpm: 140,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [1,3,5,7],
        hatAccent: [3,7],
        bass: [55.00, 61.74, 65.41, 55.00], // A1-B1-C2-A1
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 349.23, 415.30], // A Harmonic Minor
        arp: [0, 2, 4, 6, 4, 2],
        melody: [0, 6, 5, 4, 2, 0, 4, 2],
        chords: [[0,2,4], [5,0,2], [4,6,1], [0,2,4]],
        vibe: 'gothic',
        kickDecay: 0.3,
        snareDecay: 0.1,
        bassType: 'sawtooth',
        melodyOct: 1,
        description: 'Castlevania/Konami: Dark, dramatic, epic with harmonic minor gothic feel'
    },
    { 
        name: 'STREET FIGHTER',
        bpm: 155,
        kick: [0, 3, 4, 7],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [1,5],
        bass: [82.41, 98.00, 82.41, 73.42], // F#2-G2-F#2-D#2
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 523.25, 587.33], // E Phrygian Dominant
        arp: [0, 3, 5, 7, 5, 3],
        melody: [7, 5, 3, 0, 3, 5, 7, 0],
        chords: [[0,3,5], [2,5,0], [4,7,2], [0,3,5]],
        vibe: 'battle',
        kickDecay: 0.28,
        snareDecay: 0.09,
        bassType: 'square',
        melodyOct: 1,
        description: 'Street Fighter/Capcom: Energetic fighting spirit with syncopated rhythms'
    },
    { 
        name: 'SONIC SPEED',
        bpm: 170,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [0,2,4,6],
        bass: [110.00, 123.47, 130.81, 110.00], // A2-B2-C3-A2
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 392.00, 440.00], // A Natural Minor
        arp: [0, 2, 4, 5, 7, 5, 4, 2],
        melody: [0, 4, 7, 5, 4, 2, 0, 7],
        chords: [[0,2,4], [3,5,7], [5,0,2], [0,2,4]],
        vibe: 'speed',
        kickDecay: 0.2,
        snareDecay: 0.06,
        bassType: 'sawtooth',
        melodyOct: 2,
        description: 'Sonic/Sega: Very fast, upbeat, adventurous with speed sensation'
    },
    { 
        name: 'R-TYPE FORCE',
        bpm: 148,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [1,3,5,7],
        bass: [55.00, 65.41, 73.42, 65.41], // A1-C2-D#2-C2
        scale: [220.00, 246.94, 293.66, 329.63, 369.99, 415.30, 440.00], // A Aeolian
        arp: [0, 4, 7, 4, 0, 2, 5, 2],
        melody: [0, 2, 4, 7, 5, 4, 2, 0],
        chords: [[0,4,7], [2,5,0], [4,7,2], [5,0,4]],
        vibe: 'scifi',
        kickDecay: 0.25,
        snareDecay: 0.08,
        bassType: 'square',
        melodyOct: 1,
        description: 'R-Type/Irem: Sci-fi atmospheric with technical precision'
    },
    { 
        name: 'TOUHOU PROJECT',
        bpm: 165,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [0,4],
        bass: [65.41, 73.42, 82.41, 73.42], // C2-D#2-F#2-D#2
        scale: [261.63, 293.66, 329.63, 392.00, 440.00, 493.88, 523.25, 587.33, 659.25], // C Major extended
        arp: [0, 2, 4, 7, 8, 7, 4, 2],
        melody: [0, 4, 7, 8, 7, 4, 2, 0],
        chords: [[0,2,4], [2,4,6], [4,6,8], [0,2,4]],
        vibe: 'bullet-hell',
        kickDecay: 0.23,
        snareDecay: 0.07,
        bassType: 'square',
        melodyOct: 2,
        description: 'Touhou/ZUN: Dense, intricate bullet-hell intensity with fast melodic runs'
    },
    { 
        name: 'FINAL FANTASY',
        bpm: 125,
        kick: [0, 4],
        snare: [2, 6], 
        hat: [0,2,4,6],
        hatAccent: [2,6],
        bass: [110.00, 130.81, 146.83, 110.00], // A2-C3-D3-A2
        scale: [220.00, 246.94, 261.63, 293.66, 329.63, 392.00, 440.00, 493.88], // A Minor
        arp: [0, 2, 4, 5, 7, 5, 4, 2],
        melody: [7, 5, 4, 2, 0, 2, 4, 5],
        chords: [[0,2,4], [5,7,2], [4,6,0], [0,2,4]],
        vibe: 'epic',
        kickDecay: 0.4,
        snareDecay: 0.15,
        bassType: 'sawtooth',
        melodyOct: 1,
        description: 'Final Fantasy/Square: Epic, orchestral-inspired, heroic battle themes'
    },
    { 
        name: 'METAL SLUG',
        bpm: 152,
        kick: [0, 2, 4, 6],
        snare: [2, 6], 
        hat: [0,1,2,3,4,5,6,7],
        hatAccent: [1,3,5,7],
        bass: [82.41, 98.00, 110.00, 82.41], // F#2-G2-A2-F#2
        scale: [329.63, 369.99, 392.00, 440.00, 493.88, 587.33], // E Mixolydian
        arp: [0, 2, 4, 5, 4, 2],
        melody: [0, 2, 4, 0, 5, 4, 2, 0],
        chords: [[0,2,4], [3,5,0], [0,2,4], [1,3,5]],
        vibe: 'military',
        kickDecay: 0.27,
        snareDecay: 0.09,
        bassType: 'square',
        melodyOct: 1,
        description: 'Metal Slug/SNK: Action-packed military arcade intensity'
    }
];

// Piano note frequencies (C4 to D6)
const PIANO_NOTES = [
    261.63, 293.66, 329.63, 349.23, 392.00, 440.00, 493.88, 523.25,  // C4-C5
    587.33, 659.25, 698.46, 783.99, 880.00, 987.77, 1046.50, 1174.66  // D5-D6
];

// Export for use in game.js
if (typeof module !== 'undefined' && module.exports) {
    module.exports = { MUSIC_STYLES, PIANO_NOTES };
}
