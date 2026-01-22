const canvas = document.getElementById('gameCanvas');
canvas.style.cursor = 'none'; 
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');

// --- Global State ---
let gameStarted = false;
let score = 0; 
let gameOver = false; 
let powerUpActive = false; 
let powerUpTimer = 0;
let weaponLevel = 1; 
let lastBossSpawnScore = 0;
let beatKick = 0;
let beatSnare = 0;

// --- Entities ---
const player = { 
    x: 400, y: 500, size: 30, speed: 5,
    missileCooldown: 0, lastShotTime: 0
};

let bullets = [], enemies = [], enemyBullets = [], powerUps = [], missiles = [], particles = [], stars = [], debris = [];
const keys = {};

// Init BG
function resize() {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
    player.x = canvas.width / 2;
    player.y = canvas.height - 50;
}
window.addEventListener('resize', resize);
resize();

for(let i=0; i<50; i++) stars.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.5+Math.random()*2, size: Math.random()*2 });
for(let i=0; i<10; i++) debris.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.2+Math.random()*0.5, size: 5+Math.random()*20, rotation: Math.random()*Math.PI*2, rotSpeed: (Math.random()-0.5)*0.02, shape: Math.random()>0.5?'beam':'panel' });

// --- Simple Audio (no BGM, just occasional beeps) ---
let audioCtx = null;

function initAudio() {
    if (audioCtx) return;
    try {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    } catch(e) {}
}

let lastBeep = 0;
function beep(freq, dur) {
    if (!audioCtx || Date.now() - lastBeep < 100) return;
    lastBeep = Date.now();
    try {
        const osc = audioCtx.createOscillator();
        const gain = audioCtx.createGain();
        osc.connect(gain);
        gain.connect(audioCtx.destination);
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + dur);
        osc.start();
        osc.stop(audioCtx.currentTime + dur);
    } catch(e) {}
}

// --- Input ---
function startGame() {
    if (gameStarted) return;
    gameStarted = true;
    initAudio();
}

window.addEventListener('keydown', e => {
    if (!gameStarted) startGame();
    if (['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Space','KeyR'].includes(e.code)) e.preventDefault();
    keys[e.code] = true;
    
    if (e.code === 'Space' && !e.repeat) {
        weaponLevel = (weaponLevel % 3) + 1;
        beep(880, 0.1);
    }
    
    if (e.code === 'KeyR' && gameOver) {
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; missiles = []; particles = [];
        gameOver = false; lastBossSpawnScore = 0;
        player.x = canvas.width/2; player.y = canvas.height-50;
        weaponLevel = 1; powerUpActive = false; player.missileCooldown = 0;
    }
});
window.addEventListener('keyup', e => keys[e.code] = false);
window.addEventListener('click', startGame);
window.addEventListener('touchstart', startGame);

// --- Game Logic ---
function saveScore() {
    if (highScoreEl && score > parseInt(highScoreEl.innerText || '0')) {
        highScoreEl.innerText = score;
    }
}

function spawnEnemy(isBoss = false) {
    if (gameOver || enemies.length >= 15) return;
    
    const x = Math.random() * (canvas.width - 60) + 30;
    const y = -50;
    let hp = 1, size = 25, color = '#ff003c', behavior = 'strafing';
    const faction = Math.random() > 0.5 ? 'zerg' : 'protoss';
    
    if (isBoss) {
        enemies.push({ x: canvas.width/2, y: -100, vx: 0, vy: 1, size: 80, color: '#fcee0a', hp: 20, isBoss: true, faction, behavior: 'boss', lastShot: 0, hitFlash: 0, rotation: 0 });
    } else {
        const rand = Math.random();
        if (rand < 0.3) { hp = 1; color = '#d97706'; behavior = 'kamikaze'; size = 20; }
        else if (rand < 0.5) { hp = 3; color = '#7f1d1d'; behavior = 'hover'; size = 35; }
        else { color = '#a855f7'; behavior = 'strafing'; }
        
        enemies.push({ x, y, vx: 0, vy: 0, size, color, hp, isBoss: false, faction, behavior, lastShot: 0, hitFlash: 0, rotation: 0, targetX: x, lastTurn: Date.now() });
    }
}

function killEnemy(e) {
    e.dead = true;
    beep(150, 0.2);
    score += e.isBoss ? 100 : 10;
    
    // Spawn a few particles
    for(let i = 0; i < 3; i++) {
        if (particles.length < 50) {
            particles.push({ x: e.x, y: e.y, vx: (Math.random()-0.5)*4, vy: (Math.random()-0.5)*4, life: 30, color: '#00f0ff' });
        }
    }
    
    // Maybe drop powerup
    if (Math.random() < 0.1 && powerUps.length < 3) {
        powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15, type: Math.random() < 0.5 ? 'upgrade' : 'rapid' });
    }
}

function update() {
    if (gameOver) return;
    
    const now = Date.now();
    
    // Update background
    for (let s of stars) { s.y += s.speed; if (s.y > canvas.height) { s.y = 0; s.x = Math.random() * canvas.width; } }
    for (let d of debris) { d.y += d.speed; d.rotation += d.rotSpeed; if (d.y > canvas.height + 50) { d.y = -50; d.x = Math.random() * canvas.width; } }
    
    // Player movement
    if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
    if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
    if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
    if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
    
    // Player shooting
    const cooldown = powerUpActive ? 150 : 250;
    if (now - player.lastShotTime > cooldown && bullets.length < 30) {
        beep(440, 0.05);
        bullets.push({ x: player.x + player.size/2, y: player.y, vx: 0, vy: -10, damage: 2 });
        if (weaponLevel >= 2) {
            bullets.push({ x: player.x, y: player.y + 10, vx: -1, vy: -9, damage: 1 });
            bullets.push({ x: player.x + player.size, y: player.y + 10, vx: 1, vy: -9, damage: 1 });
        }
        if (weaponLevel >= 3) {
            bullets.push({ x: player.x - 10, y: player.y + 20, vx: -2, vy: -8, damage: 1 });
            bullets.push({ x: player.x + player.size + 10, y: player.y + 20, vx: 2, vy: -8, damage: 1 });
        }
        player.lastShotTime = now;
    }
    
    // Missiles
    if (now > player.missileCooldown && enemies.length > 0 && missiles.length < 8) {
        let target = enemies.find(e => !e.dead);
        if (target) {
            beep(220, 0.1);
            missiles.push({ x: player.x + player.size/2, y: player.y, target, damage: 5, speed: 6, angle: -Math.PI/2 });
            player.missileCooldown = now + 2000;
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
            m.angle += Math.max(-0.15, Math.min(0.15, diff));
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
        // Hit player?
        if (Math.hypot(b.x - player.x - player.size/2, b.y - player.y - player.size/2) < player.size/2 + 5) {
            gameOver = true; saveScore(); return false;
        }
        return b.y > 0 && b.y < canvas.height && b.x > 0 && b.x < canvas.width;
    });
    
    // Update powerups
    powerUps = powerUps.filter(p => {
        p.y += p.vy;
        if (Math.hypot(p.x - player.x - player.size/2, p.y - player.y - player.size/2) < player.size/2 + p.size/2) {
            beep(660, 0.15);
            if (p.type === 'upgrade') weaponLevel = Math.min(weaponLevel + 1, 3);
            else { powerUpActive = true; powerUpTimer = now + 5000; }
            score += 50;
            return false;
        }
        return p.y < canvas.height + 50;
    });
    
    // Update enemies
    for (let i = 0; i < enemies.length; i++) {
        const e = enemies[i];
        if (e.dead) continue;
        if (e.hitFlash > 0) e.hitFlash--;
        
        // AI
        if (e.isBoss) {
            if (e.y < 100) e.y += 1;
            else e.x += Math.sin(now / 500) * 2;
            if (now - e.lastShot > 1500 && enemyBullets.length < 20) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 5, vy: Math.sin(angle) * 5 });
                e.lastShot = now;
            }
        } else {
            if (e.y < 50) e.y += 2;
            else {
                if (e.behavior === 'kamikaze') {
                    const angle = Math.atan2(player.y - e.y, player.x - e.x);
                    e.vx = Math.cos(angle) * 3; e.vy = Math.sin(angle) * 3;
                    e.rotation = angle + Math.PI/2;
                } else if (e.behavior === 'hover') {
                    e.vy = 0.3; e.vx = Math.sin(now / 1000) * 2;
                } else {
                    if (now - e.lastTurn > 1000) { e.targetX = Math.random() * canvas.width; e.lastTurn = now; }
                    e.vx = (e.targetX - e.x) * 0.02; e.vy = 1;
                }
                e.x += e.vx; e.y += e.vy;
            }
            
            // Shoot occasionally
            if (now - e.lastShot > 2000 && enemyBullets.length < 20 && e.y < player.y - 50) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * 3, vy: Math.sin(angle) * 3 });
                e.lastShot = now;
            }
        }
        
        // Enemy hits player?
        if (Math.hypot(e.x - player.x - player.size/2, e.y - player.y - player.size/2) < e.size/2 + player.size/2) {
            gameOver = true; saveScore();
        }
    }
    
    // Collision: bullets vs enemies
    for (let b of bullets) {
        if (b.dead) continue;
        for (let e of enemies) {
            if (e.dead) continue;
            if (Math.hypot(b.x - e.x, b.y - e.y) < e.size/2) {
                b.dead = true;
                e.hp -= b.damage || 1;
                e.hitFlash = 5;
                if (e.hp <= 0) killEnemy(e);
                break;
            }
        }
    }
    
    // Collision: missiles vs enemies
    for (let m of missiles) {
        if (m.dead) continue;
        for (let e of enemies) {
            if (e.dead) continue;
            if (Math.hypot(m.x - e.x, m.y - e.y) < e.size/2 + 10) {
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
    if (Math.random() < 0.015 && enemies.length < 12) spawnEnemy();
    if (score >= lastBossSpawnScore + 500 && !enemies.some(e => e.isBoss)) {
        spawnEnemy(true);
        lastBossSpawnScore = score;
    }
    
    // Beat effect decay
    beatKick *= 0.9;
    beatSnare *= 0.9;
}

function draw() {
    ctx.fillStyle = '#0f172a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    
    // Stars
    ctx.fillStyle = 'white';
    for (let s of stars) ctx.fillRect(s.x, s.y, s.size, s.size);
    
    // Debris
    for (let d of debris) {
        ctx.save();
        ctx.translate(d.x, d.y);
        ctx.rotate(d.rotation);
        ctx.fillStyle = '#334155';
        if (d.shape === 'beam') ctx.fillRect(-d.size/2, -2, d.size, 4);
        else ctx.fillRect(-d.size/2, -d.size/2, d.size, d.size);
        ctx.restore();
    }
    
    // Particles
    for (let p of particles) {
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x - 2, p.y - 2, 4, 4);
    }
    
    // Player
    ctx.save();
    ctx.translate(player.x + player.size/2, player.y + player.size/2);
    ctx.fillStyle = powerUpActive ? '#fbbf24' : '#64748b';
    ctx.beginPath();
    ctx.moveTo(0, -player.size);
    ctx.lineTo(player.size/2, 0);
    ctx.lineTo(player.size/3, player.size);
    ctx.lineTo(-player.size/3, player.size);
    ctx.lineTo(-player.size/2, 0);
    ctx.closePath();
    ctx.fill();
    ctx.fillStyle = '#00f0ff';
    ctx.beginPath();
    ctx.moveTo(0, -player.size/2);
    ctx.lineTo(5, -10);
    ctx.lineTo(0, 0);
    ctx.lineTo(-5, -10);
    ctx.fill();
    ctx.restore();
    
    // Bullets
    ctx.fillStyle = '#60a5fa';
    for (let b of bullets) {
        if (!b.dead) ctx.fillRect(b.x - 2, b.y, 4, 8);
    }
    
    // Missiles
    for (let m of missiles) {
        if (m.dead) continue;
        ctx.save();
        ctx.translate(m.x, m.y);
        ctx.rotate(m.angle + Math.PI/2);
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
        ctx.save();
        ctx.translate(e.x, e.y);
        ctx.rotate(e.rotation || 0);
        ctx.fillStyle = e.hitFlash > 0 ? 'white' : e.color;
        
        if (e.isBoss) {
            ctx.beginPath();
            ctx.moveTo(0, e.size/2);
            ctx.lineTo(e.size/2, -e.size/4);
            ctx.lineTo(e.size/1.5, -e.size/2);
            ctx.lineTo(0, -e.size/2 + 10);
            ctx.lineTo(-e.size/1.5, -e.size/2);
            ctx.lineTo(-e.size/2, -e.size/4);
            ctx.closePath();
            ctx.fill();
            ctx.fillStyle = '#ff003c';
            ctx.beginPath();
            ctx.arc(0, 0, e.size/4, 0, Math.PI * 2);
            ctx.fill();
        } else {
            ctx.beginPath();
            ctx.arc(0, 0, e.size/2, 0, Math.PI * 2);
            ctx.fill();
        }
        ctx.restore();
        
        if (e.isBoss) {
            ctx.fillStyle = '#fcee0a';
            ctx.font = 'bold 14px Arial';
            ctx.fillText('BOSS HP: ' + e.hp, e.x - 40, e.y - e.size/2 - 10);
        }
    }
    
    // PowerUps
    for (let p of powerUps) {
        ctx.fillStyle = p.type === 'rapid' ? '#fbbf24' : '#00f0ff';
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size/2, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = 'black';
        ctx.font = '10px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(p.type === 'rapid' ? 'R' : 'W', p.x, p.y + 3);
    }
    
    // UI
    ctx.textAlign = 'left';
    ctx.fillStyle = 'white';
    ctx.font = '20px Arial';
    ctx.fillText('Score: ' + score, 20, 30);
    ctx.fillText('Weapon: LVL ' + weaponLevel, 20, 55);
    
    if (!gameStarted) {
        ctx.fillStyle = '#fcee0a';
        ctx.font = '40px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('PRESS ANY KEY TO START', canvas.width/2, canvas.height/2);
    } else if (powerUpActive) {
        ctx.fillStyle = '#fcee0a';
        ctx.fillText('RAPID FIRE!', 20, 80);
    }
    
    const rdy = Date.now() > player.missileCooldown;
    ctx.fillStyle = rdy ? '#00f0ff' : '#475569';
    ctx.textAlign = 'left';
    ctx.font = '16px Arial';
    ctx.fillText(rdy ? 'MISSILE: READY' : 'MISSILE: RELOADING', 20, 105);
    
    // Debug
    ctx.fillStyle = '#666';
    ctx.font = '12px monospace';
    ctx.fillText(`E:${enemies.length} B:${bullets.length} M:${missiles.length} P:${particles.length}`, 20, canvas.height - 10);
    
    if (gameOver) {
        ctx.fillStyle = 'rgba(0,0,0,0.85)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);
        ctx.fillStyle = '#fcee0a';
        ctx.font = '50px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('GAME OVER', canvas.width/2, canvas.height/2);
        ctx.font = '25px Arial';
        ctx.fillStyle = '#00f0ff';
        ctx.fillText('Score: ' + score, canvas.width/2, canvas.height/2 + 50);
        ctx.fillText('Press R to Restart', canvas.width/2, canvas.height/2 + 90);
    }
}

function loop() {
    try {
        if (gameStarted) update();
        draw();
    } catch(e) {
        console.error('Loop error:', e);
    }
    requestAnimationFrame(loop);
}

loop();
