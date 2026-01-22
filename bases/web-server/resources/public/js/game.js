import { CONFIG, COLORS } from './engine/config.js';
import { AudioSystem } from './engine/audio.js';
import { drawDrone, drawBoss } from './engine/renderer.js';

const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');

// --- Initialization ---
function resize() {
    canvas.width = window.innerWidth; // Config updates
    canvas.height = window.innerHeight;
    CONFIG.CANVAS_WIDTH = canvas.width;
    CONFIG.CANVAS_HEIGHT = canvas.height;
}
window.addEventListener('resize', resize);
resize();

const audio = new AudioSystem();

// --- Game State ---
const State = {
    started: false,
    score: 0,
    gameOver: false,
    powerUpActive: false,
    powerUpTimer: 0,
    weaponLevel: 1,
    lastBossScore: 0,
    lastShot: 0,
    missileCooldown: 0
};

// --- Entities ---
const player = { x: 0, y: 0, size: 30, speed: 5 };
// Center player
player.x = canvas.width/2; 
player.y = canvas.height-50;

let entities = {
    bullets: [],
    enemies: [],
    enemyBullets: [],
    missiles: [],
    particles: [],
    powerUps: [],
    stars: [],
    debris: []
};

const keys = {};

// Init Background
for(let i=0; i<80; i++) entities.stars.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.5+Math.random()*2, size: Math.random()*2 });
for(let i=0; i<15; i++) entities.debris.push({ x: Math.random()*canvas.width, y: Math.random()*canvas.height, speed: 0.2+Math.random()*0.5, size: 5+Math.random()*20, rotation: Math.random()*Math.PI*2, rotSpeed: (Math.random()-0.5)*0.02, shape: Math.random()>0.5?'beam':'panel' });

// --- Inputs ---
window.addEventListener('keydown', e => {
    if(!State.started) { State.started=true; audio.init(); }
    if(['ArrowUp','ArrowDown','ArrowLeft','ArrowRight','Space','KeyR'].includes(e.code)) e.preventDefault();
    keys[e.code] = true;
    
    if(e.code === 'Space') {
        State.weaponLevel = (State.weaponLevel % 3) + 1;
        audio.cycleStyle();
    }
    if(e.code === 'KeyR' && State.gameOver) resetGame();
    
    audio.init(); // Retry init
});
window.addEventListener('keyup', e => keys[e.code] = false);
window.addEventListener('click', () => { if(!State.started){State.started=true; audio.init();} });

function resetGame() {
    State.score = 0; State.gameOver = false; State.lastBossScore = 0;
    entities.enemies = []; entities.bullets = []; entities.enemyBullets = []; entities.missiles = []; entities.powerUps = []; entities.particles = [];
    player.x = canvas.width/2; player.y = canvas.height-50;
    State.weaponLevel = 1;
}

// --- Logic ---

function spawnEnemy(isBoss=false) {
    if(State.gameOver) return;
    let x = Math.random() * (canvas.width - 60) + 30;
    let y = -50;
    let vx=0, vy=0, hp=1, size=20, color=COLORS.ENEMY_DEFAULT;
    let faction = Math.random()>0.5 ? 'zerg' : 'protoss';
    let behavior = 'strafing';

    if(isBoss) {
        x = canvas.width/2; y = -100; hp = 20; size = 80; color = '#fcee0a';
    } else {
        size = 15 + Math.random() * 25; 
        const r = Math.random();
        if(r<0.3) { behavior='kamikaze'; hp=1; color=faction==='zerg'?'#d97706':'#facc15'; }
        else if(r<0.5) { behavior='hover'; hp=4; size+=10; color=faction==='zerg'?'#7f1d1d':'#0ea5e9'; }
        else { behavior='strafing'; color=faction==='zerg'?'#a855f7':'#eab308'; }
    }

    entities.enemies.push({ x, y, vx, vy, size, color, hp, isBoss, faction, behavior, lastShot:0, lastTurn:Date.now(), turnRate:500+Math.random()*1000, hitFlash:0, rotation:0, targetX:x });
    
    // Spawn Music
    if(faction==='zerg' && audio.melodyQueue.length<5) audio.melodyQueue.push(audio.style.scale[Math.floor(Math.random()*2)]/2);
    else if(audio.melodyQueue.length<5) audio.melodyQueue.push(audio.style.scale[Math.floor(Math.random()*5)]*4);
}

function update() {
    if(State.gameOver) return;
    
    // BG
    entities.stars.forEach(s => { s.y += s.speed; if(s.y>canvas.height){s.y=0; s.x=Math.random()*canvas.width;} });
    entities.debris.forEach(d => { d.y += d.speed; d.rotation += d.rotSpeed; if(d.y>canvas.height+50){d.y=-50; d.x=Math.random()*canvas.width;} });

    // Player
    let moving = false;
    if(keys['ArrowUp'] && player.y>0) { player.y-=player.speed; moving=true; }
    if(keys['ArrowDown'] && player.y<canvas.height-player.size) { player.y+=player.speed; moving=true; }
    if(keys['ArrowLeft'] && player.x>0) { player.x-=player.speed; moving=true; }
    if(keys['ArrowRight'] && player.x<canvas.width-player.size) { player.x+=player.speed; moving=true; }
    if(moving && Math.random()<0.5) entities.particles.push({ x:player.x+player.size/2, y:player.y+player.size, vx:(Math.random()-0.5), vy:2, life:30, color:COLORS.LIFE, isLife:true });

    // Shoot
    const cd = State.powerUpActive ? 50 : 150;
    if(Date.now() - State.lastShot > cd) {
        audio.playSFX('shoot');
        const bSpeed = -12;
        entities.bullets.push({ x:player.x+player.size/2, y:player.y, vy:bSpeed, vx:0, damage:2 });
        if(State.weaponLevel >= 2) { 
            entities.bullets.push({x:player.x, y:player.y+10, vy:bSpeed*0.9, vx:-2, damage:1.5}); 
            entities.bullets.push({x:player.x+player.size, y:player.y+10, vy:bSpeed*0.9, vx:2, damage:1.5}); 
        }
        if(State.weaponLevel >= 3) {
            entities.bullets.push({x:player.x-10, y:player.y+20, vy:bSpeed*0.8, vx:-4, damage:1});
            entities.bullets.push({x:player.x+player.size+10, y:player.y+20, vy:bSpeed*0.8, vx:4, damage:1});
        }
        State.lastShot = Date.now();
    }

    // Missiles
    if(Date.now() > State.missileCooldown) {
        let target = null, minDist = Infinity;
        entities.enemies.forEach(e => { const d=Math.hypot(e.x-player.x, e.y-player.y); if(d<minDist){minDist=d; target=e;} });
        if(target) {
            audio.playSFX('missile');
            for(let i=0; i<4; i++) entities.missiles.push({ x:player.x+player.size/2, y:player.y, vx:0, vy:-5, target, damage:5, speed:6+Math.random()*2, angle:-Math.PI/2+(Math.random()-0.5)*0.5 });
            State.missileCooldown = Date.now() + 1000;
        }
    }
    if(State.powerUpActive && Date.now() > State.powerUpTimer) State.powerUpActive = false;

    // Entity Logic
    entities.bullets = entities.bullets.filter(b => { b.x+=b.vx; b.y+=b.vy; return b.y>-50 && b.x>-50 && b.x<canvas.width+50; });
    entities.missiles = entities.missiles.filter(m => {
        if(m.target && !entities.enemies.includes(m.target)) m.target = null;
        if(m.target) {
            const angle = Math.atan2(m.target.y-m.y, m.target.x-m.x);
            let diff = angle - m.angle;
            while(diff > Math.PI) diff -= Math.PI*2; while(diff < -Math.PI) diff += Math.PI*2;
            m.angle += Math.max(-0.1, Math.min(0.1, diff));
        }
        m.vx = Math.cos(m.angle)*m.speed; m.vy = Math.sin(m.angle)*m.speed;
        m.x+=m.vx; m.y+=m.vy;
        if(Math.random()<0.5) entities.particles.push({ x:m.x, y:m.y, vx:0, vy:0, life:10, color:'gray' });
        return m.y>-50 && m.x>-50 && m.x<canvas.width+50 && m.y<canvas.height+50;
    });
    
    if(entities.particles.length > 500) entities.particles.splice(0, entities.particles.length-500);
    entities.particles = entities.particles.filter(p => { p.x+=p.vx; p.y+=p.vy; p.life--; return p.life>0; });

    entities.enemyBullets = entities.enemyBullets.filter(b => {
        b.x+=b.vx; b.y+=b.vy;
        if(Math.hypot(b.x-(player.x+player.size/2), b.y-(player.y+player.size/2)) < 5+player.size/2) { State.gameOver=true; saveScore(); }
        return b.y<canvas.height && b.y>0 && b.x>0 && b.x<canvas.width;
    });

    entities.enemies.forEach(e => {
        if(e.dead) return;
        if(e.hitFlash>0) e.hitFlash--;

        // AI
        if(e.isBoss) {
            const mf = 1+(20-e.hp)/5;
            if(e.y<100) e.y+=e.vy; else e.x += Math.sin(Date.now()/(500/mf))*(2*mf);
            if(Date.now()-e.lastShot > 2500/mf) {
                const angle = Math.atan2(player.y-e.y, player.x-e.x);
                entities.enemyBullets.push({ x:e.x, y:e.y, vx:Math.cos(angle)*(4*mf), vy:Math.sin(angle)*(4*mf) });
                e.lastShot = Date.now(); audio.playSFX('boss-shoot');
            }
        } else {
            const rate = e.type==='heavy'?2000:(e.type==='fast'?1500:3000);
            if(Date.now()-e.lastShot > rate+Math.random()*1000) {
                if(e.y < player.y-100) {
                    if(e.faction==='zerg') {
                        entities.enemies.push({ x:e.x, y:e.y+e.size, vx:0, vy:0, size:10, color:'#a855f7', hp:1, type:'fast', faction:'zerg', behavior:'kamikaze', lastShot:Date.now(), lastTurn:Date.now(), turnRate:0, hitFlash:0, rotation:0, targetX:e.x, isMinion:true });
                        e.lastShot = Date.now();
                        if(audio.melodyQueue.length<5) audio.melodyQueue.push(audio.style.scale[Math.floor(Math.random()*2)]/2);
                    } else {
                        const angle = Math.atan2(player.y-e.y, player.x-e.x);
                        entities.enemyBullets.push({ x:e.x, y:e.y, vx:Math.cos(angle)*4, vy:Math.sin(angle)*4 });
                        e.lastShot = Date.now();
                    }
                }
            }
            if(e.y<50) e.y+=3;
            else {
                if(e.behavior==='kamikaze') { const a = Math.atan2(player.y-e.y, player.x-e.x); e.vx=Math.cos(a)*4; e.vy=Math.sin(a)*4; e.rotation=a+Math.PI/2; }
                else if(e.behavior==='hover') { e.vy=0.5; e.vx=Math.sin(Date.now()/1000)*2; e.rotation=e.vx*0.1; }
                else { if(Date.now()-e.lastTurn>e.turnRate){e.targetX=Math.random()*canvas.width; e.lastTurn=Date.now();} e.vx=(e.targetX-e.x)*0.02; e.vy=1.5; e.rotation=e.vx*0.2; }
                e.x+=e.vx; e.y+=e.vy;
            }
        }

        // Collisions
        if(Math.hypot(e.x-(player.x+player.size/2), e.y-(player.y+player.size/2)) < e.size/2+player.size/2) { State.gameOver=true; saveScore(); }
        entities.bullets.forEach(b => { if(b.dead)return; if(Math.hypot(b.x-e.x, b.y-e.y)<e.size/2){ b.dead=true; e.hp-=(b.damage||1); e.hitFlash=5; if(e.hp<=0 && !e.dead) killEnemy(e, false); } });
        entities.missiles.forEach(m => { if(m.dead)return; if(Math.hypot(m.x-e.x, m.y-e.y)<e.size/2+5){ m.dead=true; e.hp-=m.damage; e.hitFlash=10; if(e.hp<=0 && !e.dead) killEnemy(e, false); } });
        entities.particles.forEach(p => { 
            if(p.isLife && !e.dead && Math.hypot(p.x-e.x, p.y-e.y)<e.size/2+(p.size||4)/2) {
                e.hp -= 0.1*((p.size||4)/4); e.hitFlash=2;
                if(!p.fedTimer || Date.now()-p.fedTimer>50) { p.size=Math.min((p.size||4)+1, 40); p.life+=20; p.fedTimer=Date.now(); }
                if(e.hp<=0 && !e.dead) killEnemy(e, true);
            }
        });
    });
    entities.enemies = entities.enemies.filter(e => !e.dead);

    if(Math.random()<0.03) spawnEnemy();
    if(State.score >= State.lastBossScore+500 && !entities.enemies.some(e=>e.isBoss)) { spawnEnemy(true); State.lastBossScore=State.score; }
}

function killEnemy(e, consumed) {
    e.dead = true;
    audio.playSFX('explosion');
    State.score += e.isBoss ? 100 : (e.type==='heavy'?30:10);
    if(consumed) {
        State.score += 25;
        if(audio.melodyQueue.length<10) { const s=audio.style.scale; audio.melodyQueue.push(s[0], s[2], s[4]); }
    }
    const count = e.size/2;
    for(let i=0; i<count; i++) entities.particles.push({ x:e.x+(Math.random()-0.5)*e.size, y:e.y+(Math.random()-0.5)*e.size, vx:(Math.random()-0.5)*4, vy:(Math.random()-0.5)*4, life:100+Math.random()*50, color:COLORS.LIFE, isLife:true });
    if(Math.random()<0.15) entities.powerUps.push({ x:e.x, y:e.y, vy:2, size:15, type:Math.random()<0.6?'upgrade':'rapid' });
}

function saveScore() {
    const token = document.getElementById('csrf-token').value;
    fetch('/game/score', { method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded','x-csrf-token':token}, body:`score=${State.score}` })
    .then(r=>r.json()).then(d=>{ highScoreEl.innerText=d.highScore; });
}

// --- Draw ---

function draw() {
    ctx.fillStyle = '#0f172a'; ctx.fillRect(0, 0, canvas.width, canvas.height);
    audio.beatState.kick *= 0.9;
    audio.beatState.snare *= 0.9;
    const kick = audio.beatState.kick;
    const snare = audio.beatState.snare;

    ctx.save();
    const scale = 1.0 + (kick * 0.02);
    ctx.translate(canvas.width/2 + (Math.random()-0.5)*snare*5, canvas.height/2 + (Math.random()-0.5)*snare*5);
    ctx.scale(scale, scale);
    ctx.translate(-canvas.width/2, -canvas.height/2);

    if(snare > 0.1) { ctx.fillStyle = `rgba(255,255,255,${snare*0.05})`; ctx.fillRect(0,0,canvas.width,canvas.height); }

    entities.debris.forEach(d => { ctx.save(); ctx.translate(d.x,d.y); ctx.rotate(d.rotation); ctx.fillStyle='#334155'; if(d.shape==='beam')ctx.fillRect(-d.size/2,-2,d.size,4); else{ctx.fillRect(-d.size/2,-d.size/2,d.size,d.size);ctx.fillStyle='#1e293b';ctx.fillRect(-d.size/4,-d.size/4,d.size/2,d.size/2);} ctx.restore(); });
    ctx.fillStyle='white'; entities.stars.forEach(s => ctx.fillRect(s.x, s.y, s.size+kick*2, s.size+kick*2));
    entities.particles.forEach(p => { if(p.isLife){ctx.fillStyle=p.color; const s=(p.size||4)+kick*2; ctx.fillRect(p.x-s/2, p.y-s/2, s, s);}else{ctx.fillStyle='gray'; ctx.fillRect(p.x, p.y, 2, 2);} });

    ctx.save(); ctx.translate(player.x+player.size/2, player.y+player.size/2);
    ctx.fillStyle = State.powerUpActive?'#fbbf24':COLORS.PLAYER; ctx.beginPath(); ctx.moveTo(0,-player.size); ctx.lineTo(player.size/2,0); ctx.lineTo(player.size,player.size/2); ctx.lineTo(player.size/2,player.size/2); ctx.lineTo(player.size/3,player.size); ctx.lineTo(-player.size/3,player.size); ctx.lineTo(-player.size/2,player.size/2); ctx.lineTo(-player.size,player.size/2); ctx.lineTo(-player.size/2,0); ctx.closePath(); ctx.fill();
    ctx.fillStyle=COLORS.LIFE; ctx.beginPath(); ctx.moveTo(0,-player.size/2); ctx.lineTo(5,-10); ctx.lineTo(0,0); ctx.lineTo(-5,-10); ctx.fill();
    ctx.fillStyle='#f59e0b'; ctx.shadowBlur=10+kick*10; ctx.shadowColor='#f59e0b'; ctx.beginPath(); ctx.arc(-5,player.size,3+kick,0,Math.PI*2); ctx.arc(5,player.size,3+kick,0,Math.PI*2); ctx.fill(); ctx.shadowBlur=0; ctx.restore();

    ctx.fillStyle='#60a5fa'; entities.bullets.forEach(b => ctx.fillRect(b.x-2, b.y, 4, 10));
    entities.missiles.forEach(m => { ctx.save(); ctx.translate(m.x, m.y); ctx.rotate(m.angle+Math.PI/2); ctx.fillStyle='#ef4444'; ctx.beginPath(); ctx.moveTo(0,-10); ctx.lineTo(4,5); ctx.lineTo(-4,5); ctx.fill(); ctx.restore(); });
    ctx.fillStyle='#f87171'; entities.enemyBullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 6, 0, Math.PI*2); ctx.fill(); });

    entities.enemies.forEach(e => {
        if(e.isBoss) drawBoss(ctx, e, kick, snare);
        else drawDrone(ctx, e.x, e.y, e.size, e.hitFlash>0?'white':e.color, e.rotation, e.faction);
    });

    entities.powerUps.forEach(p => { ctx.fillStyle=p.type==='rapid'?'#fbbf24':COLORS.LIFE; ctx.beginPath(); ctx.arc(p.x,p.y,p.size/2+kick*2,0,Math.PI*2); ctx.fill(); ctx.fillStyle='black'; ctx.font='10px Arial'; ctx.textAlign='center'; ctx.fillText(p.type==='rapid'?'R':'W',p.x,p.y+3); });
    ctx.restore();

    ctx.textAlign='left'; ctx.fillStyle='white'; ctx.font='20px Orbitron, Arial'; ctx.fillText(`Score: ${State.score}`, 20, 30);
    ctx.fillText(`Weapon: LVL ${State.weaponLevel}`, 20, 60); ctx.fillText(`Style: ${audio.style.name}`, 20, 90);
    if(!State.started){ ctx.fillStyle='#fcee0a'; ctx.font='40px Orbitron, Arial'; ctx.textAlign='center'; ctx.fillText('PRESS ANY KEY TO START', canvas.width/2, canvas.height/2); }
    else if(State.powerUpActive){ctx.fillStyle='#fcee0a'; ctx.fillText('RAPID FIRE ACTIVE!', 20, 120);}
    
    if(State.gameOver) {
        ctx.fillStyle='rgba(0,0,0,0.85)'; ctx.fillRect(0,0,canvas.width,canvas.height);
        ctx.fillStyle='#fcee0a'; ctx.font='50px Orbitron, Arial'; ctx.textAlign='center'; ctx.fillText('SYSTEM FAILURE', canvas.width/2, canvas.height/2);
        ctx.font='25px Orbitron, Arial'; ctx.fillStyle='#00f0ff'; ctx.fillText(`Final Score: ${State.score}`, canvas.width/2, canvas.height/2+50); ctx.fillText('Press R to Reboot', canvas.width/2, canvas.height/2+100);
    }
}

function loop() {
    try { if(State.started) update(); draw(); } 
    catch(e) { console.error(e); }
    requestAnimationFrame(loop);
}
loop();
