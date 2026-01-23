/**
 * Game Configuration Constants
 * 
 * Centralized configuration for game limits, timing, and mechanics.
 * Adjust these values to balance gameplay difficulty and performance.
 */

// === Entity Limits (Performance) ===
const LIMITS = {
    MAX_ENEMIES: 35,
    MAX_BULLETS: 50,
    MAX_MISSILES: 12,
    MAX_PARTICLES: 80,
    MAX_ENEMY_BULLETS: 40,
    MAX_POWERUPS: 5,
    MAX_PIANO_PARTICLES: 20
};

// === Player Configuration ===
const PLAYER = {
    SIZE: 30,
    SPEED: 5,
    INITIAL_X: 400,
    INITIAL_Y: 500,
    TRAIL_LENGTH: 15  // Number of trail points
};

// === Invincibility Shield ===
const SHIELD = {
    DURATION: 3000,      // 3 seconds active
    COOLDOWN: 10000,     // 10 seconds total cooldown
    FLASH_INTERVAL: 100  // Visual flash interval (ms)
};

// === Combo System ===
const COMBO = {
    WINDOW: 2000,  // 2 seconds to maintain combo
    MILESTONES: [3, 5, 10, 15, 20]  // Sound effect triggers
};

// === Weapon Configuration ===
const WEAPONS = {
    LEVELS: 3,
    MISSILE_COOLDOWN: 500  // ms between missile launches
};

// === Enemy Spawn Configuration ===
const ENEMIES = {
    BOSS_SCORE_INTERVAL: 500,  // Spawn boss every 500 points
    PARTICLE_COUNT_NORMAL: 20,
    PARTICLE_COUNT_BOSS: 50
};

// === Particle Configuration ===
const PARTICLES = {
    PLAYER_TRAIL: {
        COUNT: 15,
        OUTER_COLOR: 'rgba(6, 182, 212, 0.6)',  // Cyan
        INNER_COLOR: 'rgba(255, 255, 255, 0.8)', // White
        SIZE_OUTER: 12,
        SIZE_INNER: 6
    },
    BULLET_TRAIL: {
        COUNT: 5,
        COLOR: 'rgba(59, 130, 246, 0.5)',  // Blue
        SIZE: 3
    },
    MISSILE_TRAIL: {
        COUNT: 10,
        SMOKE_COLOR: 'rgba(150, 150, 150, 0.6)',  // Gray
        FIRE_COLOR: 'rgba(255, 165, 0, 0.7)',     // Orange
        SIZE_SMOKE: 8,
        SIZE_FIRE: 5
    },
    ENEMY_TRAIL: {
        COUNT: 8,
        SIZE: 6
    }
};

// === Background Parallax Layers ===
const BACKGROUND = {
    NEBULA: {
        COUNT: 8,
        SIZE_MIN: 80,
        SIZE_MAX: 200,
        SPEED_MIN: 0.05,
        SPEED_MAX: 0.15,
        ALPHA_MIN: 0.15,
        ALPHA_MAX: 0.3,
        COLORS: ['#1e3a8a', '#312e81', '#581c87', '#701a75']
    },
    STARS: {
        COUNT: 120,
        SIZE_MIN: 0.5,
        SIZE_MAX: 1.5,
        SPEED_MIN: 0.2,
        SPEED_MAX: 0.5
    },
    DUST: {
        COUNT: 30,
        SIZE_MIN: 1,
        SIZE_MAX: 3,
        SPEED_MIN: 1.0,
        SPEED_MAX: 3.0,
        ALPHA_MIN: 0.3,
        ALPHA_MAX: 0.7
    }
};

// === Audio Configuration ===
const AUDIO = {
    MASTER_VOLUME: 1.0,
    COMPRESSOR: {
        THRESHOLD: -12,
        KNEE: 30,
        RATIO: 12,
        ATTACK: 0.003,
        RELEASE: 0.25
    },
    REVERB: {
        DURATION: 1.5,
        DECAY: 2.0,
        VOLUME: 0.05
    },
    SOUND_COOLDOWNS: {
        'shoot': 50,
        'missile': 100,
        'hit': 50,
        'powerup': 100,
        'kill': 30,
        'boss-shoot': 80,
        'combo-milestone': 200,
        'boss-warning': 500,
        'game-over': 1000
    }
};

// === Faction Colors (for death effects) ===
const FACTIONS = {
    ZERG: {
        PRIMARY: '#a855f7',    // Purple
        SECONDARY: '#d97706',  // Orange
        KAMIKAZE: '#d97706'
    },
    PROTOSS: {
        PRIMARY: '#eab308',    // Yellow
        SECONDARY: '#0ea5e9',  // Blue
        KAMIKAZE: '#facc15'
    }
};

// Export for use in game modules
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        LIMITS,
        PLAYER,
        SHIELD,
        COMBO,
        WEAPONS,
        ENEMIES,
        PARTICLES,
        BACKGROUND,
        AUDIO,
        FACTIONS
    };
}
