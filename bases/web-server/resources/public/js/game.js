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
let audioCtx = null;
let masterGain = null;
let bgmInterval = null;
let beatKick = 0;
let beatSnare = 0;
let currentStyle = 0;
let melodyQueue = []; 
let noiseBuffer = null; 

// --- Music Data (Reverted to "Good" Style) ---
const musicStyles = [
    { name: 'HIP-HOP', bpm: 250, kick: [0, 5], snare: [2], notes: [82.41, 98.00, 123.47, 110.00], scale: [261.63, 293.66, 311.13, 349.23, 392.00] }, 
    { name: 'TECHNO', bpm: 130, kick: [0, 2, 4, 6], snare: [4], notes: [55, 55, 110, 110], scale: [220.00, 246.94, 261.63, 293.66, 329.63] }, 
    { name: 'D&B', bpm: 85, kick: [0, 5], snare: [2, 6], notes: [55, 55, 110, 82], scale: [174.61, 196.00, 220.00, 261.63, 293.66] },
    { name: 'RETROWAVE', bpm: 180, kick: [0, 4], snare: [2, 6], notes: [65.41, 65.41, 73.42, 87.31], scale: [261.63, 293.66, 329.63, 349.23, 392.00] },
    { name: 'INDUSTRIAL', bpm: 120, kick: [0, 2, 3, 5, 6], snare: [4], notes: [36.71, 36.71, 36.71, 38.89], scale: [146.83, 155.56, 174.61, 196.00, 207.65] },
    { name: 'TRANCE', bpm: 140, kick: [0, 2, 4, 6], snare: [2, 6], notes: [146.83, 130.81, 110.00, 130.81], scale: [293.66, 329.63, 369.99, 440.00, 523.25] }
];

// Start with random style
currentStyle = Math.floor(Math.random() * musicStyles.length);

// --- Entities ---
const player = { 
    x: canvas.width / 2, y: canvas.height - 50, size: 30, speed: 5, color: '#94a3b8', 
    missileCooldown: 0, lastShotTime: 0, lastSwitch: 0 
};

let bullets = [], enemies = [], enemyBullets = [], powerUps = [], missiles = [], particles = [], stars = [], debris = [];
const keys = {};

// Init BG
for(let i=0; i<80; i++) stars.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.5+Math.random()*2, size: Math.random()*2 });
for(let i=0; i<15; i++) debris.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.2+Math.random()*0.5, size: 5+Math.random()*20, rotation: Math.random()*Math.PI*2, rotSpeed: (Math.random()-0.5)*0.02, shape: Math.random()>0.5?'beam':'panel' });

// --- Audio System ---

function initAudio() { 
    if (audioCtx) {
        if (audioCtx.state === 'suspended') audioCtx.resume();
        return;
    }
    try {
        const AudioContext = window.AudioContext || window.webkitAudioContext;
        audioCtx = new AudioContext();
        masterGain = audioCtx.createGain();
        masterGain.connect(audioCtx.destination);
        masterGain.gain.value = 0.5;

        // Noise Buffer
        const bufferSize = audioCtx.sampleRate * 2;
        noiseBuffer = audioCtx.createBuffer(1, bufferSize, audioCtx.sampleRate);
        const output = noiseBuffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) output[i] = Math.random() * 2 - 1;

        // Compressor
        const compressor = audioCtx.createDynamicsCompressor();
        compressor.threshold.setValueAtTime(-10, audioCtx.currentTime);
        compressor.ratio.setValueAtTime(12, audioCtx.currentTime);
        compressor.connect(masterGain);
        audioCtx.masterOut = compressor;

        startBGM(); 
        
        ['click', 'keydown', 'touchstart'].forEach(evt => {
            document.addEventListener(evt, () => {
                if (audioCtx && audioCtx.state === 'suspended') audioCtx.resume();
            }, { once: true });
        });
    } catch (e) { console.error("Audio init failed", e); }
}

function playSound(type) {
    if (!audioCtx || audioCtx.state !== 'running') return;
    try {
        const t = audioCtx.currentTime;
        const gain = audioCtx.createGain();
        gain.connect(audioCtx.masterOut);

        if (type === 'shoot') { 
            const osc = audioCtx.createOscillator();
            const style = musicStyles[currentStyle];
            const note = style.scale[Math.floor(Math.random() * style.scale.length)];
            osc.type = 'triangle';
            osc.frequency.setValueAtTime(note, t); 
            osc.frequency.exponentialRampToValueAtTime(note/2, t + 0.1); 
            gain.gain.setValueAtTime(0.05, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.1);
            osc.connect(gain); osc.start(t); osc.stop(t + 0.1);
        }
        else if (type === 'missile') { 
            const osc = audioCtx.createOscillator();
            osc.type = 'sawtooth';
            const style = musicStyles[currentStyle];
            const baseNote = style.scale[0] / 2;
            osc.frequency.setValueAtTime(baseNote, t); 
            osc.frequency.linearRampToValueAtTime(baseNote * 4, t + 0.3); 
            gain.gain.setValueAtTime(0.05, t);
            gain.gain.linearRampToValueAtTime(0.001, t + 0.3);
            osc.connect(gain); osc.start(t); osc.stop(t + 0.3);
        }
        else if (type === 'explosion' && noiseBuffer) { 
            const src = audioCtx.createBufferSource();
            src.buffer = noiseBuffer;
            const filter = audioCtx.createBiquadFilter();
            filter.type = 'lowpass';
            filter.frequency.setValueAtTime(1000, t);
            filter.frequency.exponentialRampToValueAtTime(100, t + 0.5);
            gain.gain.setValueAtTime(0.2, t);
            gain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
            src.connect(filter); filter.connect(gain);
            src.start(t); src.stop(t + 0.5);
        }
        else if (type === 'powerup') { 
            const osc = audioCtx.createOscillator();
            osc.type = 'sine'; 
            osc.frequency.setValueAtTime(440, t); 
            osc.frequency.linearRampToValueAtTime(880, t + 0.3); 
            gain.gain.setValueAtTime(0.05, t);
            gain.gain.linearRampToValueAtTime(0.001, t + 0.3);
            osc.connect(gain); osc.start(t); osc.stop(t + 0.3);
        }
        else if (type === 'boss-shoot') { 
            const osc = audioCtx.createOscillator();
            osc.type = 'sine'; 
            osc.frequency.setValueAtTime(440, t); 
            osc.frequency.exponentialRampToValueAtTime(220, t + 0.2); 
            gain.gain.setValueAtTime(0.03, t);
            gain.gain.linearRampToValueAtTime(0.001, t + 0.2);
            osc.connect(gain); osc.start(t); osc.stop(t + 0.2);
        }
    } catch(e) { console.error(e); }
}

function startBGM() {
    if (bgmInterval) clearInterval(bgmInterval);
    let step = 0;
    
    bgmInterval = setInterval(() => {
        if (!audioCtx || audioCtx.state !== 'running') return;
        const style = musicStyles[currentStyle];
        const t = audioCtx.currentTime;
        
        try {
            // Melody
            if (melodyQueue.length > 0) {
                const noteFreq = melodyQueue.shift();
                const pOsc = audioCtx.createOscillator();
                const pGain = audioCtx.createGain();
                pOsc.type = 'sine'; 
                pOsc.frequency.setValueAtTime(noteFreq, t);
                const delay = audioCtx.createDelay(); delay.delayTime.value = 0.25;
                const delayGain = audioCtx.createGain(); delayGain.gain.value = 0.3;
                pGain.gain.setValueAtTime(0, t);
                pGain.gain.linearRampToValueAtTime(0.1, t + 0.02);
                pGain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
                pOsc.connect(pGain); pGain.connect(audioCtx.masterOut);
                pGain.connect(delay); delay.connect(delayGain); delayGain.connect(audioCtx.masterOut);
                pOsc.start(t); pOsc.stop(t + 0.5);
            }

            // Bass
            const bass = audioCtx.createOscillator();
            const bassGain = audioCtx.createGain();
            bass.type = 'sawtooth';
            bass.frequency.setValueAtTime(style.notes[Math.floor(step/4) % 4], t);
            const filter = audioCtx.createBiquadFilter();
            filter.type = 'lowpass'; filter.frequency.value = 400;
            bassGain.gain.setValueAtTime(0.1, t);
            bassGain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
            bass.connect(filter); filter.connect(bassGain); bassGain.connect(audioCtx.masterOut);
            bass.start(t); bass.stop(t + 0.2);

            // Kick
            if (style.kick.includes(step % 8)) {
                beatKick = 1.0;
                const osc = audioCtx.createOscillator();
                const gain = audioCtx.createGain();
                osc.frequency.setValueAtTime(150, t);
                osc.frequency.exponentialRampToValueAtTime(0.01, t + 0.5);
                gain.gain.setValueAtTime(0.5, t);
                gain.gain.exponentialRampToValueAtTime(0.001, t + 0.5);
                osc.connect(gain); gain.connect(audioCtx.masterOut);
                osc.start(t); osc.stop(t + 0.5);
            }
            
            // Snare
            if (style.snare.includes(step % 8) && noiseBuffer) {
                beatSnare = 1.0;
                const src = audioCtx.createBufferSource();
                src.buffer = noiseBuffer;
                const gain = audioCtx.createGain();
                const filter = audioCtx.createBiquadFilter();
                filter.type = 'highpass'; filter.frequency.value = 800;
                gain.gain.setValueAtTime(0.2, t);
                gain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
                src.connect(filter); filter.connect(gain); gain.connect(audioCtx.masterOut);
                src.start(t); src.stop(t + 0.2);
            }
            
            step++;
        } catch(e) {}
    }, musicStyles[currentStyle].bpm);
}

// --- Input Handling ---

function startGame() {
    if (gameStarted) return;
    gameStarted = true;
    initAudio();
}

window.addEventListener('keydown', e => {
    if (!gameStarted) startGame();
    
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space', 'KeyR'].includes(e.code)) e.preventDefault();
    keys[e.code] = true;
    
    if (e.code === 'Space') {
        weaponLevel = (weaponLevel % 3) + 1;
        currentStyle = (currentStyle + 1) % musicStyles.length;
        startBGM(); 
        if (audioCtx && audioCtx.state === 'running') playSound('powerup');
    }
    
    if (e.code === 'KeyR' && gameOver) { 
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; missiles = []; particles = [];
        gameOver = false; lastBossSpawnScore = 0; 
        player.x = canvas.width/2; player.y = canvas.height-50; 
        weaponLevel = 1; powerUpActive = false; player.missileCooldown = 0;
        player.lastSwitch = 0;
    }
});
window.addEventListener('keyup', e => keys[e.code] = false);
window.addEventListener('click', startGame);
window.addEventListener('touchstart', startGame);

// --- Core Game Logic ---

function spawnEnemy(isBoss = false) {
    if (gameOver) return;
    let x = Math.random() * (canvas.width - 60) + 30;
    let y = -50;
    let vx = 0, vy = 0, hp = 1, size = 20, color = '#ff003c';
    let faction = Math.random() > 0.5 ? 'zerg' : 'protoss';
    let behavior = 'strafing';
    
    if (isBoss) { 
        type = 'boss'; x = canvas.width / 2; y = -100; vx = 0; vy = 1; hp = 20; size = 80; color = '#fcee0a';
    } else {
        size = 15 + Math.random() * 25; 
        const rand = Math.random();
        if (rand < 0.3) { type = 'fast'; hp = 1; color = faction==='zerg'?'#d97706':'#facc15'; behavior = 'kamikaze'; }
        else if (rand < 0.5) { type = 'heavy'; hp = 4; size += 10; color = faction==='zerg'?'#7f1d1d':'#0ea5e9'; behavior = 'hover'; }
        else { color = faction==='zerg'?'#a855f7':'#eab308'; behavior = 'strafing'; }
    }

    enemies.push({ x, y, vx, vy, size, color, hp, type: isBoss?'boss':'basic', isBoss, faction, behavior, lastShot: 0, lastTurn: Date.now(), turnRate: 500+Math.random()*1000, hitFlash: 0, rotation: 0, rotSpeed: 0, targetX: x });
    
    if (faction === 'zerg' && melodyQueue.length < 5) {
        melodyQueue.push(musicStyles[currentStyle].scale[Math.floor(Math.random()*2)]/2);
    } else if (melodyQueue.length < 5) {
        melodyQueue.push(musicStyles[currentStyle].scale[Math.floor(Math.random()*5)]*4);
    }
}

function update() {
    if (gameOver) return;
    
    stars.forEach(s => { s.y += s.speed; if(s.y > canvas.height) { s.y=0; s.x=Math.random()*canvas.width; } });
    debris.forEach(d => { d.y += d.speed; d.rotation += d.rotSpeed; if(d.y > canvas.height+50) { d.y=-50; d.x=Math.random()*canvas.width; } });

    let moving = false;
    if (keys['ArrowUp'] && player.y > 0) { player.y -= player.speed; moving=true; }
    if (keys['ArrowDown'] && player.y < canvas.height-player.size) { player.y += player.speed; moving=true; }
    if (keys['ArrowLeft'] && player.x > 0) { player.x -= player.speed; moving=true; }
    if (keys['ArrowRight'] && player.x < canvas.width-player.size) { player.x += player.speed; moving=true; }
    if (moving && Math.random()<0.5) particles.push({ x: player.x+player.size/2-2, y: player.y+player.size, vx: (Math.random()-0.5), vy: 2, life: 30, color: '#00f0ff', isLife: true });

    const cooldown = powerUpActive ? 50 : 150;
    if (Date.now() - player.lastShotTime > cooldown) { 
        if (audioCtx && audioCtx.state === 'running') playSound('shoot');
        const bSpeed = -12;
        bullets.push({ x: player.x+player.size/2, y: player.y, vy: bSpeed, vx: 0, damage: 2 });
        if (weaponLevel >= 2) { bullets.push({x:player.x, y:player.y+10, vy:bSpeed*0.9, vx:-2, damage:1.5}); bullets.push({x:player.x+player.size, y:player.y+10, vy:bSpeed*0.9, vx:2, damage:1.5}); }
        if (weaponLevel >= 3) { bullets.push({x:player.x-10, y:player.y+20, vy:bSpeed*0.8, vx:-4, damage:1}); bullets.push({x:player.x+player.size+10, y:player.y+20, vy:bSpeed*0.8, vx:4, damage:1}); }
        player.lastShotTime = Date.now();
    }

    if (Date.now() > player.missileCooldown) {
        let target = null, minDist = Infinity;
        enemies.forEach(e => { const d = Math.hypot(e.x-player.x, e.y-player.y); if(d<minDist){ minDist=d; target=e; } });
        if (target) {
            if (audioCtx && audioCtx.state === 'running') playSound('missile');
            for(let i=0; i<4; i++) missiles.push({ x: player.x+player.size/2, y: player.y, vx: 0, vy: -5, target, damage: 5, speed: 6+Math.random()*2, angle: -Math.PI/2+(Math.random()-0.5)*0.5 });
            player.missileCooldown = Date.now() + 1000;
        }
    }
    if (powerUpActive && Date.now() > powerUpTimer) powerUpActive = false;

    bullets = bullets.filter(b => { b.x+=b.vx; b.y+=b.vy; return b.y>-50 && b.x>-50 && b.x<canvas.width+50; });
    missiles = missiles.filter(m => {
        if (m.target && !enemies.includes(m.target)) m.target = null;
        if (m.target) {
            const angle = Math.atan2(m.target.y-m.y, m.target.x-m.x);
            let diff = angle - m.angle;
            while(diff > Math.PI) diff -= Math.PI*2; while(diff < -Math.PI) diff += Math.PI*2;
            m.angle += Math.max(-0.1, Math.min(0.1, diff));
        }
        m.vx = Math.cos(m.angle)*m.speed; m.vy = Math.sin(m.angle)*m.speed;
        m.x+=m.vx; m.y+=m.vy;
        if (Math.random()<0.5) particles.push({ x: m.x, y: m.y, vx: 0, vy: 0, life: 10, color: 'gray' });
        return m.y>-50 && m.x>-50 && m.x<canvas.width+50 && m.y<canvas.height+50;
    });
    if (particles.length > 500) particles.splice(0, particles.length-500);
    particles = particles.filter(p => { p.x+=p.vx; p.y+=p.vy; p.life--; return p.life>0; });
    
    enemyBullets = enemyBullets.filter(b => {
        b.x+=b.vx; b.y+=b.vy;
        if (Math.hypot(b.x-(player.x+player.size/2), b.y-(player.y+player.size/2)) < 5+player.size/2) { gameOver=true; saveScore(); }
        return b.y<canvas.height && b.y>0 && b.x>0 && b.x<canvas.width;
    });

    enemies.forEach(e => {
        if (e.dead) return;
        if (e.hitFlash > 0) e.hitFlash--;
        
        if (e.isBoss) {
            const mf = 1+(20-e.hp)/5;
            if (e.y < 100) e.y += e.vy; else e.x += Math.sin(Date.now()/(500/mf))*(2*mf);
            if (Date.now()-e.lastShot > 2500/mf) {
                const angle = Math.atan2(player.y-e.y, player.x-e.x);
                enemyBullets.push({ x:e.x, y:e.y, vx:Math.cos(angle)*(4*mf), vy:Math.sin(angle)*(4*mf) });
                e.lastShot = Date.now(); playSound('boss-shoot');
            }
        } else {
            const rate = e.type === 'heavy' ? 2000 : (e.type === 'fast' ? 1500 : 3000);
            if (Date.now()-e.lastShot > rate+Math.random()*1000) {
                if (e.y < player.y-100) {
                    if (e.faction === 'zerg') {
                        enemies.push({ x:e.x, y:e.y+e.size, vx:0, vy:0, size:10, color:'#a855f7', hp:1, type:'fast', faction:'zerg', behavior:'kamikaze', lastShot:Date.now(), lastTurn:Date.now(), turnRate:0, hitFlash:0, rotation:0, rotSpeed:0, isMinion:true });
                        e.lastShot = Date.now();
                        if (melodyQueue.length < 5) melodyQueue.push(musicStyles[currentStyle].scale[Math.floor(Math.random()*2)]/2);
                    } else {
                        const angle = Math.atan2(player.y-e.y, player.x-e.x);
                        enemyBullets.push({ x:e.x, y:e.y, vx:Math.cos(angle)*4, vy:Math.sin(angle)*4 });
                        e.lastShot = Date.now();
                    }
                }
            }
            if (e.y < 50) e.y += 3;
            else {
                if (e.behavior === 'kamikaze') { const angle = Math.atan2(player.y-e.y, player.x-e.x); e.vx = Math.cos(angle)*4; e.vy = Math.sin(angle)*4; e.rotation = angle+Math.PI/2; }
                else if (e.behavior === 'hover') { e.vy = 0.5; e.vx = Math.sin(Date.now()/1000)*2; e.rotation = e.vx*0.1; }
                else { if(Date.now()-e.lastTurn>e.turnRate){e.targetX=Math.random()*canvas.width; e.lastTurn=Date.now();} e.vx=(e.targetX-e.x)*0.02; e.vy=1.5; e.rotation=e.vx*0.2; }
                e.x+=e.vx; e.y+=e.vy;
            }
        }

        if (Math.hypot(e.x-(player.x+player.size/2), e.y-(player.y+player.size/2)) < e.size/2+player.size/2) { gameOver=true; saveScore(); }
        
        bullets.forEach(b => { if(b.dead)return; if(Math.hypot(b.x-e.x, b.y-e.y)<e.size/2){ b.dead=true; e.hp-=(b.damage||1); e.hitFlash=5; if(e.hp<=0 && !e.dead) killEnemy(e, false); } });
        missiles.forEach(m => { if(m.dead)return; if(Math.hypot(m.x-e.x, m.y-e.y)<e.size/2+5){ m.dead=true; e.hp-=m.damage; e.hitFlash=10; if(e.hp<=0 && !e.dead) killEnemy(e, false); } });
        particles.forEach(p => { 
            if(p.isLife && !e.dead && Math.hypot(p.x-e.x, p.y-e.y)<e.size/2+(p.size||4)/2) {
                e.hp -= 0.1*((p.size||4)/4); e.hitFlash=2;
                if(!p.fedTimer || Date.now()-p.fedTimer>50) { p.size=Math.min((p.size||4)+1, 40); p.life+=20; p.fedTimer=Date.now(); }
                if(e.hp<=0 && !e.dead) killEnemy(e, true);
            }
        });
    });
    enemies = enemies.filter(e => !e.dead);
    
    if (Math.random()<0.03) spawnEnemy();
    if (score >= lastBossSpawnScore+500 && !enemies.some(e=>e.isBoss)) { spawnEnemy(true); lastBossSpawnScore=score; }
}

function killEnemy(e, consumed) {
    e.dead = true;
    if (audioCtx && audioCtx.state === 'running') playSound('explosion');
    score += e.isBoss ? 100 : (e.type === 'heavy' ? 30 : 10);
    if (consumed) {
        score += 25;
        const s = musicStyles[currentStyle];
        if (melodyQueue.length < 10) { melodyQueue.push(s.scale[0]); melodyQueue.push(s.scale[2]); melodyQueue.push(s.scale[4]); }
    }
    const count = e.size/2;
    for(let i=0; i<count; i++) particles.push({ x:e.x+(Math.random()-0.5)*e.size, y:e.y+(Math.random()-0.5)*e.size, vx:(Math.random()-0.5)*4, vy:(Math.random()-0.5)*4, life:100+Math.random()*50, color:'#00f0ff', isLife:true });
    if (Math.random()<0.15) powerUps.push({ x:e.x, y:e.y, vy:2, size:15, type:Math.random()<0.6?'upgrade':'rapid' });
}

function draw() {
    ctx.fillStyle = '#0f172a'; ctx.fillRect(0, 0, canvas.width, canvas.height);
    beatKick *= 0.9; beatSnare *= 0.9;
    
    ctx.save();
    const scale = 1.0 + (beatKick * 0.02);
    ctx.translate(canvas.width/2 + (Math.random()-0.5)*beatSnare*5, canvas.height/2 + (Math.random()-0.5)*beatSnare*5);
    ctx.scale(scale, scale);
    ctx.translate(-canvas.width/2, -canvas.height/2);
    
    if (beatSnare > 0.1) { ctx.fillStyle = `rgba(255,255,255,${beatSnare*0.05})`; ctx.fillRect(0,0,canvas.width,canvas.height); }
    
    debris.forEach(d => { ctx.save(); ctx.translate(d.x,d.y); ctx.rotate(d.rotation); ctx.fillStyle='#334155'; if(d.shape==='beam')ctx.fillRect(-d.size/2,-2,d.size,4); else{ctx.fillRect(-d.size/2,-d.size/2,d.size,d.size);ctx.fillStyle='#1e293b';ctx.fillRect(-d.size/4,-d.size/4,d.size/2,d.size/2);} ctx.restore(); });
    ctx.fillStyle='white'; stars.forEach(s => ctx.fillRect(s.x, s.y, s.size+beatKick*2, s.size+beatKick*2));
    particles.forEach(p => { if(p.isLife){ctx.fillStyle=p.color; const s=(p.size||4)+beatKick*2; ctx.fillRect(p.x-s/2, p.y-s/2, s, s);}else{ctx.fillStyle='gray'; ctx.fillRect(p.x, p.y, 2, 2);} });

    ctx.save(); ctx.translate(player.x+player.size/2, player.y+player.size/2);
    ctx.fillStyle = powerUpActive?'#fbbf24':'#64748b'; ctx.beginPath(); ctx.moveTo(0,-player.size); ctx.lineTo(player.size/2,0); ctx.lineTo(player.size,player.size/2); ctx.lineTo(player.size/2,player.size/2); ctx.lineTo(player.size/3,player.size); ctx.lineTo(-player.size/3,player.size); ctx.lineTo(-player.size/2,player.size/2); ctx.lineTo(-player.size,player.size/2); ctx.lineTo(-player.size/2,0); ctx.closePath(); ctx.fill();
    ctx.fillStyle='#00f0ff'; ctx.beginPath(); ctx.moveTo(0,-player.size/2); ctx.lineTo(5,-10); ctx.lineTo(0,0); ctx.lineTo(-5,-10); ctx.fill();
    ctx.fillStyle='#f59e0b'; ctx.shadowBlur=10+beatKick*10; ctx.shadowColor='#f59e0b'; ctx.beginPath(); ctx.arc(-5,player.size,3+beatKick,0,Math.PI*2); ctx.arc(5,player.size,3+beatKick,0,Math.PI*2); ctx.fill(); ctx.shadowBlur=0; ctx.restore();

    ctx.fillStyle='#60a5fa'; bullets.forEach(b => ctx.fillRect(b.x-2, b.y, 4, 10));
    missiles.forEach(m => { ctx.save(); ctx.translate(m.x, m.y); ctx.rotate(m.angle+Math.PI/2); ctx.fillStyle='#ef4444'; ctx.beginPath(); ctx.moveTo(0,-10); ctx.lineTo(4,5); ctx.lineTo(-4,5); ctx.fill(); ctx.restore(); });
    ctx.fillStyle='#f87171'; enemyBullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 6, 0, Math.PI*2); ctx.fill(); });

    enemies.forEach(e => {
        if(e.isBoss) {
            ctx.save(); ctx.translate(e.x, e.y); ctx.scale(1+beatKick*0.05, 1+beatKick*0.05);
            ctx.fillStyle=(e.hp<10 && Math.floor(Date.now()/100)%2===0)?'#ff003c':'#2a2a2a'; ctx.strokeStyle='#fcee0a'; ctx.lineWidth=3;
            ctx.beginPath(); ctx.moveTo(0,e.size/2); ctx.lineTo(e.size/2,-e.size/4); ctx.lineTo(e.size/1.5,-e.size/2); ctx.lineTo(0,-e.size/2+10); ctx.lineTo(-e.size/1.5,-e.size/2); ctx.lineTo(-e.size/2,-e.size/4); ctx.closePath(); ctx.fill(); ctx.stroke();
            ctx.fillStyle='#ff003c'; ctx.shadowBlur=20+beatSnare*20; ctx.shadowColor='#ff003c'; ctx.beginPath(); ctx.arc(0,0,e.size/4,0,Math.PI*2); ctx.fill(); ctx.shadowBlur=0;
            ctx.fillStyle='#00f0ff'; ctx.fillRect(-e.size/3,-e.size/2-10,10,20); ctx.fillRect(e.size/3-10,-e.size/2-10,10,20); ctx.restore();
            ctx.fillStyle='#fcee0a'; ctx.font='bold 16px Orbitron, Arial'; ctx.fillText('DREADNOUGHT HP: '+e.hp, e.x-60, e.y-e.size/2-20);
        } else {
            drawDrone(ctx, e.x, e.y, e.size, e.hitFlash > 0 ? 'white' : e.color, e.rotation, e.faction);
        }
    });
    
    powerUps.forEach(p => { ctx.fillStyle=p.type==='rapid'?'#fbbf24':'#00f0ff'; ctx.beginPath(); ctx.arc(p.x,p.y,p.size/2+beatKick*2,0,Math.PI*2); ctx.fill(); ctx.fillStyle='black'; ctx.font='10px Arial'; ctx.textAlign='center'; ctx.fillText(p.type==='rapid'?'R':'W',p.x,p.y+3); });
    ctx.restore();

    ctx.textAlign='left'; ctx.fillStyle='white'; ctx.font='20px Orbitron, Arial'; ctx.fillText(`Score: ${score}`, 20, 30);
    ctx.fillText(`Weapon: LVL ${weaponLevel}`, 20, 60); ctx.fillText(`Style: ${musicStyles[currentStyle].name}`, 20, 90);
    if(!gameStarted) { ctx.fillStyle='#fcee0a'; ctx.font='40px Orbitron, Arial'; ctx.textAlign='center'; ctx.fillText('PRESS ANY KEY TO START', canvas.width/2, canvas.height/2); }
    else if(powerUpActive){ctx.fillStyle='#fcee0a'; ctx.fillText('RAPID FIRE ACTIVE!', 20, 120);}
    const rdy = Date.now()>player.missileCooldown; ctx.fillStyle=rdy?'#00f0ff':'#475569'; ctx.fillText(rdy?'MISSILE: READY':'MISSILE: REARMING', 20, 150);

    if (gameOver) {
        ctx.fillStyle='rgba(0,0,0,0.85)'; ctx.fillRect(0,0,canvas.width,canvas.height);
        ctx.fillStyle='#fcee0a'; ctx.font='50px Orbitron, Arial'; ctx.textAlign='center'; ctx.fillText('SYSTEM FAILURE', canvas.width/2, canvas.height/2);
        ctx.font='25px Orbitron, Arial'; ctx.fillStyle='#00f0ff'; ctx.fillText(`Final Score: ${score}`, canvas.width/2, canvas.height/2+50); ctx.fillText('Press R to Reboot', canvas.width/2, canvas.height/2+100);
    }
}

function saveScore() {
    const token = document.getElementById('csrf-token').value;
    fetch('/game/score', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded', 'x-csrf-token': token }, body: `score=${score}` })
    .then(r => r.json()).then(data => { highScoreEl.innerText = data.highScore; });
}

function drawDrone(ctx, x, y, size, color, rotation, faction) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rotation);
    ctx.fillStyle = color;
    if (!faction) faction = 'protoss'; 
    if (faction === 'zerg') {
        if (size < 15) { ctx.fillStyle = '#facc15'; ctx.beginPath(); ctx.arc(0, 0, size/2, 0, Math.PI*2); ctx.fill(); ctx.strokeStyle = color; ctx.lineWidth = 2; ctx.beginPath(); ctx.moveTo(0, 0); ctx.lineTo(Math.sin(Date.now()/50)*3, size*1.5); ctx.stroke(); }
        else { ctx.shadowBlur = 10; ctx.shadowColor = '#a855f7'; ctx.fillStyle = '#4c1d95'; ctx.beginPath(); ctx.arc(0, -size/3, size/3, Math.PI, 0); ctx.bezierCurveTo(size/2, 0, size/4, size/2, 0, size); ctx.bezierCurveTo(-size/4, size/2, -size/2, 0, -size/3, -size/3); ctx.fill(); ctx.strokeStyle = '#d8b4fe'; ctx.lineWidth = 2; const wiggle = Math.sin(Date.now()/200)*5; ctx.beginPath(); ctx.moveTo(-size/4, 0); ctx.quadraticCurveTo(-size, size/2, -size/2+wiggle, size+10); ctx.moveTo(size/4, 0); ctx.quadraticCurveTo(size, size/2, size/2-wiggle, size+10); ctx.stroke(); ctx.fillStyle = '#facc15'; ctx.beginPath(); ctx.arc(-size/4, -size/4, 3, 0, Math.PI*2); ctx.arc(size/4, -size/4, 3, 0, Math.PI*2); ctx.fill(); ctx.shadowBlur = 0; }
    } else {
        ctx.shadowBlur = 10; ctx.shadowColor = color; ctx.beginPath(); ctx.arc(0, 0, size/3, 0, Math.PI*2); ctx.fill(); ctx.fillStyle = '#e2e8f0'; ctx.beginPath(); ctx.moveTo(0, size/2); ctx.lineTo(size/1.2, -size/2); ctx.lineTo(0, -size/4); ctx.lineTo(-size/1.2, -size/2); ctx.closePath(); ctx.fill(); ctx.fillStyle = '#38bdf8'; ctx.beginPath(); ctx.moveTo(0, -size/4); ctx.lineTo(size/6, 0); ctx.lineTo(0, size/4); ctx.lineTo(-size/6, 0); ctx.fill();
    }
    ctx.restore();
}

function loop() {
    try { 
        if(gameStarted) update(); 
        draw(); 
    } 
    catch(e) { console.error(e); }
    requestAnimationFrame(loop);
}
loop();
