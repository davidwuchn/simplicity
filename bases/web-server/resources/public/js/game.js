const canvas = document.getElementById('gameCanvas');
canvas.style.cursor = 'none'; // Hide cursor over canvas
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');
let score = 0; let gameOver = false; 
let powerUpActive = false; let powerUpTimer = 0;
let weaponLevel = 1; // 1: Blaster, 2: Tri-Shot, 3: Penta-Spread
const player = { x: canvas.width / 2, y: canvas.height - 50, size: 30, speed: 5, color: '#94a3b8', missileCooldown: 0 };
const keys = {};

window.addEventListener('keydown', e => {
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space', 'KeyR'].includes(e.code)) {
        e.preventDefault();
    }
    keys[e.code] = true;
    if (e.code === 'KeyR' && gameOver) { 
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; missiles = [];
        gameOver = false; lastBossSpawnScore = 0; 
        player.x = canvas.width / 2; player.y = canvas.height - 50; 
        weaponLevel = 1; powerUpActive = false; player.missileCooldown = 0;
    }
    initAudio();
});
window.addEventListener('keyup', e => keys[e.code] = false);

let bullets = []; let enemies = []; let powerUps = []; let particles = []; let enemyBullets = []; let missiles = [];
let stars = []; let debris = [];
let lastBossSpawnScore = 0;
let audioCtx;

// Init Background
for(let i=0; i<80; i++) {
    stars.push({ x: Math.random() * canvas.width, y: Math.random() * canvas.height, speed: 0.5 + Math.random() * 2, size: Math.random() * 2 });
}
// Init Debris (Space Ruins)
for(let i=0; i<15; i++) {
    debris.push({ 
        x: Math.random() * canvas.width, 
        y: Math.random() * canvas.height, 
        speed: 0.2 + Math.random() * 0.5, 
        size: 5 + Math.random() * 20, 
        rotation: Math.random() * Math.PI * 2,
        rotSpeed: (Math.random() - 0.5) * 0.02,
        shape: Math.random() > 0.5 ? 'beam' : 'panel'
    });
}

function initAudio() { if (!audioCtx) { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); startBGM(); } }
function playSound(type) {
    if (!audioCtx) return;
    const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
    if (type === 'shoot') { osc.type = 'square'; osc.frequency.setValueAtTime(880, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(110, audioCtx.currentTime + 0.1); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
    else if (type === 'missile') { osc.type = 'sawtooth'; osc.frequency.setValueAtTime(220, audioCtx.currentTime); osc.frequency.linearRampToValueAtTime(880, audioCtx.currentTime + 0.3); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
    else if (type === 'explosion') { osc.type = 'sawtooth'; osc.frequency.setValueAtTime(100, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5); gain.gain.setValueAtTime(0.1, audioCtx.currentTime); }
    else if (type === 'powerup') { osc.type = 'sine'; osc.frequency.setValueAtTime(440, audioCtx.currentTime); osc.frequency.linearRampToValueAtTime(880, audioCtx.currentTime + 0.3); gain.gain.setValueAtTime(0.1, audioCtx.currentTime); }
    else if (type === 'boss-shoot') { osc.type = 'sine'; osc.frequency.setValueAtTime(440, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(220, audioCtx.currentTime + 0.2); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
    osc.connect(gain); gain.connect(audioCtx.destination);
    osc.start(); osc.stop(audioCtx.currentTime + (type === 'explosion' ? 0.5 : (type === 'powerup' ? 0.3 : 0.1)));
}

function startBGM() {
    const notes = [110, 123.47, 130.81, 146.83]; let noteIndex = 0;
    setInterval(() => {
        if (gameOver || !audioCtx) return;
        const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
        osc.type = 'triangle'; osc.frequency.setValueAtTime(notes[noteIndex], audioCtx.currentTime);
        gain.gain.setValueAtTime(0.03, audioCtx.currentTime); gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.4);
        osc.connect(gain); gain.connect(audioCtx.destination);
        osc.start(); osc.stop(audioCtx.currentTime + 0.4); noteIndex = (noteIndex + 1) % notes.length;
    }, 500);
}

function spawnEnemy(isBoss = false) {
    if (gameOver) return;
    
    // Improved Spawning Logic
    let x = Math.random() * (canvas.width - 60) + 30;
    let y = -50;
    let vx = 0; 
    let vy = 0;
    
    let type = 'basic';
    let hp = 1; let size = 20; let color = '#ff003c';
    let faction = Math.random() > 0.5 ? 'zerg' : 'protoss'; 
    let behavior = 'strafing'; // Default behavior

    if (isBoss) { 
        type = 'boss'; x = canvas.width / 2; y = -100; vx = 0; vy = 1; hp = 20; size = 80; color = '#fcee0a';
    } else {
        const rand = Math.random();
        size = 15 + Math.random() * 25; 
        
        if (rand < 0.3) { 
            type = 'fast'; hp = 1; color = faction === 'zerg' ? '#d97706' : '#facc15'; 
            behavior = 'kamikaze'; // Fast interceptors
        } else if (rand < 0.5) { 
            type = 'heavy'; hp = 4; size += 10; color = faction === 'zerg' ? '#7f1d1d' : '#0ea5e9'; 
            behavior = 'hover'; // Heavy gunships
        } else {
            color = faction === 'zerg' ? '#a855f7' : '#eab308';
            behavior = 'strafing'; // Standard fighters
        }
    }

    enemies.push({ 
        x, y, vx, vy, size, color, hp, type, isBoss, faction, behavior,
        lastShot: 0, lastTurn: Date.now(), 
        turnRate: 500 + Math.random() * 1000, 
        hitFlash: 0,
        rotation: 0, rotSpeed: 0,
        targetX: x, targetY: y // For AI maneuvering
    });
}

function update() {
    if (gameOver) return;
    
    // Background Update
    stars.forEach(s => {
        s.y += s.speed;
        if (s.y > canvas.height) { s.y = 0; s.x = Math.random() * canvas.width; }
    });
    debris.forEach(d => {
        d.y += d.speed;
        d.rotation += d.rotSpeed;
        if (d.y > canvas.height + 50) { 
            d.y = -50; d.x = Math.random() * canvas.width; 
            d.shape = Math.random() > 0.5 ? 'beam' : 'panel';
        }
    });

    // Player Movement
    if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
    if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
    if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
    if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
    
    // Shooting
    if (keys['Space']) {
        initAudio(); 
        const cooldown = powerUpActive ? 50 : 150;
        if (!player.lastShotTime || Date.now() - player.lastShotTime > cooldown) { 
            playSound('shoot');
            // Weapon Logic
            const bulletSpeed = -12;
            bullets.push({ x: player.x + player.size/2, y: player.y, vy: bulletSpeed, vx: 0, damage: 2 }); // Center
            if (weaponLevel >= 2) {
                bullets.push({ x: player.x, y: player.y + 10, vy: bulletSpeed * 0.9, vx: -2, damage: 1.5 });
                bullets.push({ x: player.x + player.size, y: player.y + 10, vy: bulletSpeed * 0.9, vx: 2, damage: 1.5 });
            }
            if (weaponLevel >= 3) {
                bullets.push({ x: player.x - 10, y: player.y + 20, vy: bulletSpeed * 0.8, vx: -4, damage: 1 });
                bullets.push({ x: player.x + player.size + 10, y: player.y + 20, vy: bulletSpeed * 0.8, vx: 4, damage: 1 });
            }
            player.lastShotTime = Date.now();
        }

        // Missile Logic (Fire every 1000ms while holding Space)
        if (Date.now() > player.missileCooldown) {
            playSound('missile');
            // Find target
            let target = null;
            let minDist = Infinity;
            enemies.forEach(e => {
                const dist = Math.hypot(e.x - player.x, e.y - player.y);
                if (dist < minDist) { minDist = dist; target = e; }
            });

            missiles.push({
                x: player.x + player.size/2, y: player.y,
                vx: 0, vy: -5,
                target: target,
                damage: 5,
                speed: 6,
                angle: -Math.PI / 2
            });
            player.missileCooldown = Date.now() + 1000;
        }
    }

    if (powerUpActive && Date.now() > powerUpTimer) powerUpActive = false;

    // Update Bullets
    bullets = bullets.filter(b => {
        b.x += b.vx; b.y += b.vy;
        return b.y > -50 && b.x > -50 && b.x < canvas.width + 50;
    });

    // Update Missiles
    missiles = missiles.filter(m => {
        if (m.target && !enemies.includes(m.target)) m.target = null; // Lost target
        
        if (m.target) {
            const angleToTarget = Math.atan2(m.target.y - m.y, m.target.x - m.x);
            // Smooth turning
            const diff = angleToTarget - m.angle;
            const turnSpeed = 0.1;
            // Normalize angle difference
            let turn = Math.atan2(Math.sin(diff), Math.cos(diff));
            if (turn > turnSpeed) turn = turnSpeed;
            if (turn < -turnSpeed) turn = -turnSpeed;
            m.angle += turn;
        }

        m.vx = Math.cos(m.angle) * m.speed;
        m.vy = Math.sin(m.angle) * m.speed;
        m.x += m.vx; m.y += m.vy;

        // Trail
        if (Math.random() < 0.5) {
            particles.push({ x: m.x, y: m.y, vx: 0, vy: 0, life: 10, color: 'gray' });
        }

        return m.y > -50 && m.x > -50 && m.x < canvas.width + 50 && m.y < canvas.height + 50;
    });

    // Update Particles
    particles = particles.filter(p => {
        p.life--;
        return p.life > 0;
    });

    // Update Enemy Bullets
    enemyBullets = enemyBullets.filter(b => {
        b.x += b.vx; b.y += b.vy;
        const dx = b.x - (player.x + player.size/2), dy = b.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < 5 + player.size/2) { gameOver = true; saveScore(); }
        return b.y < canvas.height && b.y > 0 && b.x > 0 && b.x < canvas.width;
    });

    // Update Enemies
    enemies.forEach((e, ei) => {
        if (e.hitFlash > 0) e.hitFlash--;
        
        if (e.isBoss) {
            // Boss AI (unchanged)
            const madnessFactor = 1 + (20 - e.hp) / 5;
            if (e.y < 100) e.y += e.vy;
            else { e.x += Math.sin(Date.now()/(500/madnessFactor)) * (2 * madnessFactor); }
            const shootInterval = 2500 / madnessFactor;
            if (Date.now() - e.lastShot > shootInterval) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * (4 * madnessFactor), vy: Math.sin(angle) * (4 * madnessFactor) });
                e.lastShot = Date.now(); playSound('boss-shoot');
            }
        } else {
            // Spacecraft Shooting
            const fireRate = e.type === 'heavy' ? 2000 : (e.type === 'fast' ? 1500 : 3000);
            if (Date.now() - e.lastShot > fireRate + Math.random() * 1000) {
                // Only shoot if roughly facing player or in front
                if (e.y < player.y - 100) {
                    if (e.faction === 'zerg') {
                        // Zerg: Spawn "Scourge" (Small suicide bombers)
                        enemies.push({ 
                            x: e.x, y: e.y + e.size, 
                            vx: 0, vy: 0, // Will be set by logic
                            size: 10, color: '#a855f7', hp: 1, 
                            type: 'fast', faction: 'zerg', behavior: 'kamikaze',
                            lastShot: Date.now(), lastTurn: Date.now(), turnRate: 0, hitFlash: 0,
                            rotation: 0, rotSpeed: 0, isMinion: true
                        });
                        e.lastShot = Date.now();
                        playSound('shoot'); // Squishy sound ideally
                    } else {
                        // Protoss: Energy Bolts
                        const angle = Math.atan2(player.y - e.y, player.x - e.x);
                        const speed = 4;
                        enemyBullets.push({ 
                            x: e.x, y: e.y, 
                            vx: Math.cos(angle) * speed, 
                            vy: Math.sin(angle) * speed 
                        });
                        e.lastShot = Date.now();
                    }
                }
            }

            // Advanced Spacecraft AI
            if (e.y < 50) { 
                e.y += 3; // Entrance speed
            } else {
                if (e.behavior === 'kamikaze') {
                    // Dive at player
                    const angle = Math.atan2(player.y - e.y, player.x - e.x);
                    e.vx = Math.cos(angle) * 4;
                    e.vy = Math.sin(angle) * 4;
                    e.rotation = angle + Math.PI/2;
                } else if (e.behavior === 'hover') {
                    // Hover and slowly advance
                    e.vy = 0.5;
                    e.vx = Math.sin(Date.now() / 1000 + ei) * 2;
                    e.rotation = e.vx * 0.1;
                } else { // Strafing (Standard)
                    if (Date.now() - e.lastTurn > e.turnRate) {
                        e.targetX = Math.random() * canvas.width;
                        e.lastTurn = Date.now();
                    }
                    // Smooth move to target X
                    const dx = e.targetX - e.x;
                    e.vx = dx * 0.02;
                    e.vy = 1.5; // Constant forward
                    e.rotation = e.vx * 0.2; // Bank into turns
                }
                
                e.x += e.vx;
                e.y += e.vy;
            }
        }

        // Collision: Enemy <-> Player
        const dx = e.x - (player.x + player.size/2), dy = e.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < e.size/2 + player.size/2) { gameOver = true; saveScore(); }

        // Collision: Enemy <-> Player Bullets
        bullets.forEach((b, bi) => {
            const bdx = b.x - e.x, bdy = b.y - e.y;
            if (Math.sqrt(bdx*bdx + bdy*bdy) < e.size/2) {
                bullets.splice(bi, 1); 
                e.hp -= (b.damage || 1); // Use damage value or default to 1
                e.hitFlash = 5;
                if (e.hp <= 0) {
                    killEnemy(e, ei);
                }
            }
        });

        // Collision: Enemy <-> Missiles
        missiles.forEach((m, mi) => {
            const mdx = m.x - e.x, mdy = m.y - e.y;
            if (Math.sqrt(mdx*mdx + mdy*mdy) < e.size/2 + 5) {
                missiles.splice(mi, 1);
                e.hp -= m.damage;
                e.hitFlash = 10;
                playSound('explosion');
                if (e.hp <= 0) {
                    killEnemy(e, ei);
                }
            }
        });
    });

    function killEnemy(e, ei) {
        if (enemies[ei] !== e) return; // Already killed (e.g. by simultaneous hit)
        playSound('explosion'); 
        enemies.splice(ei, 1); 
        score += e.isBoss ? 100 : (e.type === 'heavy' ? 30 : 10);
        // Drop Powerup chance
        if (Math.random() < 0.15) {
            const pType = Math.random() < 0.6 ? 'upgrade' : 'rapid';
            powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15, type: pType });
        }
    }

    // Update PowerUps
    powerUps = powerUps.filter(p => {
        p.y += 2; const dx = p.x - (player.x + player.size/2), dy = p.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < p.size + player.size/2) { 
            playSound('powerup');
            if (p.type === 'rapid') {
                powerUpActive = true; powerUpTimer = Date.now() + 5000; 
            } else {
                weaponLevel = Math.min(weaponLevel + 1, 3);
            }
            score += 50; 
            return false; 
        }
        return p.y < canvas.height;
    });

    enemies = enemies.filter(e => e.y < canvas.height + 100 && e.x > -100 && e.x < canvas.width + 100);
    
    // Spawning
    if (Math.random() < 0.03) spawnEnemy(); // Slightly increased spawn rate
    if (score >= lastBossSpawnScore + 500 && !enemies.some(e => e.isBoss)) { spawnEnemy(true); lastBossSpawnScore = score; }
}

function drawDrone(ctx, x, y, size, color, rotation, faction) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rotation);
    ctx.fillStyle = color;
    
    if (faction === 'zerg') {
        if (size < 15) { 
            // Minion / Scourge Look
            ctx.fillStyle = '#facc15'; 
            ctx.beginPath();
            ctx.arc(0, 0, size/2, 0, Math.PI*2);
            ctx.fill();
            // Tail
            ctx.strokeStyle = color;
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(0, 0);
            const wiggle = Math.sin(Date.now() / 50) * 3;
            ctx.lineTo(wiggle, size * 1.5);
            ctx.stroke();
            ctx.restore();
            return;
        }

        // Alien/Biomechanical Organism
        ctx.shadowBlur = 10;
        ctx.shadowColor = '#a855f7'; // Purple bio-glow

        // Main Carapace (Pulsating)
        ctx.fillStyle = '#4c1d95'; // Deep Purple
        ctx.beginPath();
        // Head
        ctx.arc(0, -size/3, size/3, Math.PI, 0); 
        // Body (Tapered)
        ctx.bezierCurveTo(size/2, 0, size/4, size/2, 0, size); 
        ctx.bezierCurveTo(-size/4, size/2, -size/2, 0, -size/3, -size/3);
        ctx.fill();

        // Tentacles/Limbs
        ctx.strokeStyle = '#d8b4fe'; // Pale purple
        ctx.lineWidth = 2;
        const wiggle = Math.sin(Date.now() / 200) * 5;
        
        ctx.beginPath();
        // Left Tentacle
        ctx.moveTo(-size/4, 0);
        ctx.quadraticCurveTo(-size, size/2, -size/2 + wiggle, size + 10);
        // Right Tentacle
        ctx.moveTo(size/4, 0);
        ctx.quadraticCurveTo(size, size/2, size/2 - wiggle, size + 10);
        ctx.stroke();

        // Bio-luminescent Sacs
        ctx.fillStyle = '#facc15'; // Yellow pus/energy
        ctx.beginPath();
        ctx.arc(-size/4, -size/4, 3, 0, Math.PI*2);
        ctx.arc(size/4, -size/4, 3, 0, Math.PI*2);
        ctx.fill();

        ctx.shadowBlur = 0;
    } else {
        // Protoss Probe/Interceptor (Mechanical/Elegant)
        ctx.shadowBlur = 10;
        ctx.shadowColor = color; // Energy shield glow
        
        // Main Core
        ctx.beginPath();
        ctx.arc(0, 0, size/3, 0, Math.PI*2);
        ctx.fill();

        // Energy Wings/Blades
        ctx.fillStyle = '#e2e8f0'; // Metallic
        ctx.beginPath();
        ctx.moveTo(0, size/2);
        ctx.lineTo(size/1.2, -size/2);
        ctx.lineTo(0, -size/4);
        ctx.lineTo(-size/1.2, -size/2);
        ctx.closePath();
        ctx.fill();

        // Psy-Crystal
        ctx.fillStyle = '#38bdf8'; // Cyan Crystal
        ctx.beginPath();
        ctx.moveTo(0, -size/4);
        ctx.lineTo(size/6, 0);
        ctx.lineTo(0, size/4);
        ctx.lineTo(-size/6, 0);
        ctx.fill();
    }

    ctx.restore();
}

function drawDebris(ctx, d) {
    ctx.save();
    ctx.translate(d.x, d.y);
    ctx.rotate(d.rotation);
    ctx.fillStyle = '#334155'; // Dark slate
    if (d.shape === 'beam') {
        ctx.fillRect(-d.size/2, -2, d.size, 4);
    } else {
        ctx.fillRect(-d.size/2, -d.size/2, d.size, d.size);
        ctx.fillStyle = '#1e293b';
        ctx.fillRect(-d.size/4, -d.size/4, d.size/2, d.size/2);
    }
    ctx.restore();
}

function draw() {
    ctx.fillStyle = '#0f172a'; // Deep space blue
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Draw Debris (Background Layer)
    debris.forEach(d => drawDebris(ctx, d));

    // Stars
    ctx.fillStyle = 'white'; 
    stars.forEach(s => ctx.fillRect(s.x, s.y, s.size, s.size));
    
    // Particles
    ctx.fillStyle = 'gray';
    particles.forEach(p => ctx.fillRect(p.x, p.y, 2, 2));

    // Player (F-35 Style)
    ctx.save();
    ctx.translate(player.x + player.size/2, player.y + player.size/2);
    
    // Body
    ctx.fillStyle = powerUpActive ? '#fbbf24' : '#64748b'; // Slate-500 or Gold
    ctx.beginPath();
    ctx.moveTo(0, -player.size); // Nose
    ctx.lineTo(player.size/2, 0); // Wing joint
    ctx.lineTo(player.size, player.size/2); // Right Wing Tip
    ctx.lineTo(player.size/2, player.size/2); // Right Rear
    ctx.lineTo(player.size/3, player.size); // Right Tail
    ctx.lineTo(-player.size/3, player.size); // Left Tail
    ctx.lineTo(-player.size/2, player.size/2); // Left Rear
    ctx.lineTo(-player.size, player.size/2); // Left Wing Tip
    ctx.lineTo(-player.size/2, 0); // Wing joint
    ctx.closePath();
    ctx.fill();

    // Cockpit
    ctx.fillStyle = '#00f0ff'; // Cyan Glass
    ctx.beginPath();
    ctx.moveTo(0, -player.size/2);
    ctx.lineTo(5, -10);
    ctx.lineTo(0, 0);
    ctx.lineTo(-5, -10);
    ctx.fill();

    // Engine Glow
    ctx.fillStyle = '#f59e0b'; // Amber
    ctx.shadowBlur = 10;
    ctx.shadowColor = '#f59e0b';
    ctx.beginPath();
    ctx.arc(-5, player.size, 3, 0, Math.PI*2);
    ctx.arc(5, player.size, 3, 0, Math.PI*2);
    ctx.fill();
    ctx.shadowBlur = 0;

    ctx.restore();
    
    // Player Bullets
    ctx.fillStyle = '#60a5fa'; bullets.forEach(b => ctx.fillRect(b.x-2, b.y, 4, 10));

    // Missiles
    missiles.forEach(m => {
        ctx.save();
        ctx.translate(m.x, m.y);
        ctx.rotate(m.angle + Math.PI/2);
        ctx.fillStyle = '#ef4444'; // Red
        ctx.beginPath();
        ctx.moveTo(0, -10);
        ctx.lineTo(4, 5);
        ctx.lineTo(-4, 5);
        ctx.fill();
        ctx.restore();
    });
    
    // Enemy Bullets
    ctx.fillStyle = '#f87171'; enemyBullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 6, 0, Math.PI*2); ctx.fill(); });
    
    // Enemies
    enemies.forEach(e => {
        const color = e.hitFlash > 0 ? 'white' : e.color; 
        if (e.isBoss) {
            // Draw Boss Spaceship
            ctx.save();
            ctx.translate(e.x, e.y);
            
            // Main Body (Heavy Armor)
            ctx.fillStyle = (e.hp < 10 && Math.floor(Date.now()/100) % 2 === 0) ? '#ff003c' : '#2a2a2a';
            ctx.strokeStyle = '#fcee0a';
            ctx.lineWidth = 3;
            
            // Core Body
            ctx.beginPath();
            ctx.moveTo(0, e.size/2); // Nose
            ctx.lineTo(e.size/2, -e.size/4); // Right Wing Front
            ctx.lineTo(e.size/1.5, -e.size/2); // Right Wing Tip
            ctx.lineTo(0, -e.size/2 + 10); // Rear Center
            ctx.lineTo(-e.size/1.5, -e.size/2); // Left Wing Tip
            ctx.lineTo(-e.size/2, -e.size/4); // Left Wing Front
            ctx.closePath();
            ctx.fill();
            ctx.stroke();
            
            // Glowing Core
            ctx.fillStyle = '#ff003c';
            ctx.shadowBlur = 20;
            ctx.shadowColor = '#ff003c';
            ctx.beginPath();
            ctx.arc(0, 0, e.size/4, 0, Math.PI*2);
            ctx.fill();
            ctx.shadowBlur = 0; // Reset shadow

            // Engines
            ctx.fillStyle = '#00f0ff';
            ctx.fillRect(-e.size/3, -e.size/2 - 10, 10, 20);
            ctx.fillRect(e.size/3 - 10, -e.size/2 - 10, 10, 20);

            ctx.restore();

            // Boss HP Text
            ctx.fillStyle='#fcee0a'; 
            ctx.font = 'bold 16px Orbitron, Arial'; 
            ctx.fillText('DREADNOUGHT HP: ' + e.hp, e.x - 60, e.y - e.size/2 - 20);
        } else {
            // Draw Drone
            drawDrone(ctx, e.x, e.y, e.size, color, e.rotation, e.faction);
        }
    });
    
    // PowerUps
    powerUps.forEach(p => { 
        ctx.fillStyle = p.type === 'rapid' ? '#fbbf24' : '#00f0ff'; 
        ctx.beginPath(); ctx.arc(p.x, p.y, p.size/2, 0, Math.PI*2); ctx.fill();
        ctx.fillStyle = 'black'; ctx.font = '10px Arial'; ctx.textAlign = 'center'; 
        ctx.fillText(p.type === 'rapid' ? 'R' : 'W', p.x, p.y + 3);
    });
    
    // HUD
    ctx.textAlign = 'left';
    ctx.fillStyle = 'white'; ctx.font = '20px Orbitron, Arial'; ctx.fillText(`Score: ${score}`, 20, 30);
    ctx.fillText(`Weapon: LVL ${weaponLevel}`, 20, 60);
    if (powerUpActive) { ctx.fillStyle = '#fcee0a'; ctx.fillText('RAPID FIRE ACTIVE!', 20, 90); }
    // Missile Indicator
    const missileReady = Date.now() > player.missileCooldown;
    ctx.fillStyle = missileReady ? '#00f0ff' : '#475569';
    ctx.fillText(missileReady ? 'MISSILE: READY' : 'MISSILE: REARMING', 20, 120);
    
    if (gameOver) {
        ctx.fillStyle = 'rgba(0,0,0,0.85)'; ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fcee0a'; ctx.font = '50px Orbitron, Arial'; ctx.textAlign = 'center'; ctx.fillText('SYSTEM FAILURE', canvas.width/2, canvas.height/2);
        ctx.font = '25px Orbitron, Arial'; ctx.fillStyle = '#00f0ff'; ctx.fillText(`Final Score: ${score}`, canvas.width/2, canvas.height/2 + 50); ctx.fillText('Press R to Reboot', canvas.width/2, canvas.height/2 + 100);
    }
}

function saveScore() {
    const token = document.getElementById('csrf-token').value;
    fetch('/game/score', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'x-csrf-token': token }, body: `score=${score}` })
    .then(r => r.json()).then(data => { highScoreEl.innerText = data.highScore; });
}

function loop() { update(); draw(); requestAnimationFrame(loop); }
loop();
