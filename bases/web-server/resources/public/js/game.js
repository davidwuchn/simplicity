const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');
const highScoreEl = document.getElementById('high-score');
let score = 0; let gameOver = false; let powerUpActive = false; let powerUpTimer = 0;
const player = { x: canvas.width / 2, y: canvas.height - 50, size: 30, speed: 5, color: '#00f0ff' };
const keys = {};

window.addEventListener('keydown', e => {
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space', 'KeyR'].includes(e.code)) {
        e.preventDefault();
    }
    keys[e.code] = true;
    if (e.code === 'KeyR' && gameOver) { 
        score = 0; enemies = []; bullets = []; enemyBullets = []; powerUps = []; 
        gameOver = false; lastBossSpawnScore = 0; player.x = canvas.width / 2; player.y = canvas.height - 50; 
    }
    initAudio();
});
window.addEventListener('keyup', e => keys[e.code] = false);

let bullets = []; let enemies = []; let powerUps = []; let particles = []; let enemyBullets = [];
let lastBossSpawnScore = 0;
let audioCtx;
function initAudio() { if (!audioCtx) { audioCtx = new (window.AudioContext || window.webkitAudioContext)(); startBGM(); } }
function playSound(type) {
    if (!audioCtx) return;
    const osc = audioCtx.createOscillator(); const gain = audioCtx.createGain();
    if (type === 'shoot') { osc.type = 'square'; osc.frequency.setValueAtTime(880, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(110, audioCtx.currentTime + 0.1); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
    else if (type === 'explosion') { osc.type = 'sawtooth'; osc.frequency.setValueAtTime(100, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.5); gain.gain.setValueAtTime(0.1, audioCtx.currentTime); }
    else if (type === 'boss-shoot') { osc.type = 'sine'; osc.frequency.setValueAtTime(440, audioCtx.currentTime); osc.frequency.exponentialRampToValueAtTime(220, audioCtx.currentTime + 0.2); gain.gain.setValueAtTime(0.05, audioCtx.currentTime); }
    osc.connect(gain); gain.connect(audioCtx.destination);
    osc.start(); osc.stop(audioCtx.currentTime + (type === 'explosion' ? 0.5 : 0.1));
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
    const side = Math.random(); let x, y, vx, vy;
    if (side < 0.5) { x = Math.random() * canvas.width; y = -30; vx = (Math.random() - 0.5) * 2; vy = 2 + Math.random() * 2; }
    else if (side < 0.75) { x = -30; y = Math.random() * (canvas.height / 2); vx = 2 + Math.random() * 2; vy = (Math.random() - 0.5) * 2; }
    else { x = canvas.width + 30; y = Math.random() * (canvas.height / 2); vx = -(2 + Math.random() * 2); vy = (Math.random() - 0.5) * 2; }
    if (isBoss) { x = canvas.width / 2; y = -100; vx = 0; vy = 1; }
    enemies.push({ 
        x, y, vx, vy, size: isBoss ? 80 : 20 + Math.random() * 10, color: isBoss ? '#fcee0a' : '#ff003c', 
        hp: isBoss ? 15 : 1, isBoss, lastShot: 0, lastTurn: Date.now(), turnRate: 1000 + Math.random() * 2000, hitFlash: 0
    });
}
function update() {
    if (gameOver) return;
    if (keys['ArrowUp'] && player.y > 0) player.y -= player.speed;
    if (keys['ArrowDown'] && player.y < canvas.height - player.size) player.y += player.speed;
    if (keys['ArrowLeft'] && player.x > 0) player.x -= player.speed;
    if (keys['ArrowRight'] && player.x < canvas.width - player.size) player.x += player.speed;
    if (keys['Space']) {
        initAudio(); const cooldown = powerUpActive ? 100 : 250;
        if (!player.lastShotTime || Date.now() - player.lastShotTime > cooldown) { 
            bullets.push({ x: player.x + player.size/2, y: player.y, vy: -7 }); player.lastShotTime = Date.now(); playSound('shoot');
        }
    }
    if (powerUpActive && Date.now() > powerUpTimer) powerUpActive = false;
    bullets = bullets.filter(b => (b.y -= 7) > 0);
    enemyBullets = enemyBullets.filter(b => {
        b.x += b.vx; b.y += b.vy;
        const dx = b.x - (player.x + player.size/2), dy = b.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < 5 + player.size/2) { gameOver = true; saveScore(); }
        return b.y < canvas.height && b.y > 0 && b.x > 0 && b.x < canvas.width;
    });
    enemies.forEach((e, ei) => {
        if (e.hitFlash > 0) e.hitFlash--;
        if (e.isBoss) {
            const madnessFactor = 1 + (15 - e.hp) / 5;
            if (e.y < 100) e.y += e.vy;
            else { e.x += Math.sin(Date.now()/(500/madnessFactor)) * (2 * madnessFactor); }
            const shootInterval = 1200 / madnessFactor;
            if (Date.now() - e.lastShot > shootInterval) {
                const angle = Math.atan2(player.y - e.y, player.x - e.x);
                enemyBullets.push({ x: e.x, y: e.y, vx: Math.cos(angle) * (4 * madnessFactor), vy: Math.sin(angle) * (4 * madnessFactor) });
                e.lastShot = Date.now(); playSound('boss-shoot');
            }
        } else {
            if (Date.now() - e.lastTurn > e.turnRate) { e.vx += (Math.random() - 0.5) * 2; e.vy += (Math.random() - 0.5) * 1; e.lastTurn = Date.now(); }
            e.x += e.vx; e.y += e.vy;
        }
        const dx = e.x - (player.x + player.size/2), dy = e.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < e.size/2 + player.size/2) { gameOver = true; saveScore(); }
        bullets.forEach((b, bi) => {
            const bdx = b.x - e.x, bdy = b.y - e.y;
            if (Math.sqrt(bdx*bdx + bdy*bdy) < e.size/2) {
                bullets.splice(bi, 1); e.hp--; e.hitFlash = 5;
                if (e.hp <= 0) {
                    playSound('explosion'); enemies.splice(ei, 1); score += e.isBoss ? 100 : 10;
                    if (Math.random() < 0.1) powerUps.push({ x: e.x, y: e.y, vy: 2, size: 15 });
                }
            }
        });
    });
    powerUps = powerUps.filter(p => {
        p.y += 2; const dx = p.x - (player.x + player.size/2), dy = p.y - (player.y + player.size/2);
        if (Math.sqrt(dx*dx + dy*dy) < p.size + player.size/2) { powerUpActive = true; powerUpTimer = Date.now() + 5000; score += 50; return false; }
        return p.y < canvas.height;
    });
    enemies = enemies.filter(e => e.y < canvas.height + 100 && e.x > -100 && e.x < canvas.width + 100);
    if (Math.random() < 0.02) spawnEnemy();
    if (score >= lastBossSpawnScore + 300 && !enemies.some(e => e.isBoss)) { spawnEnemy(true); lastBossSpawnScore = score; }
}
function draw() {
    ctx.fillStyle = 'black'; ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = 'white'; for(let i=0; i<50; i++) ctx.fillRect((Math.sin(i*777) + 1) * canvas.width / 2, (Date.now() * 0.1 + i*100) % canvas.height, 1, 1);
    ctx.fillStyle = powerUpActive ? '#fbbf24' : player.color; ctx.beginPath(); ctx.moveTo(player.x + player.size/2, player.y); ctx.lineTo(player.x, player.y + player.size); ctx.lineTo(player.x + player.size, player.y + player.size); ctx.fill();
    ctx.fillStyle = '#60a5fa'; bullets.forEach(b => ctx.fillRect(b.x-2, b.y, 4, 10));
    ctx.fillStyle = '#f87171'; enemyBullets.forEach(b => { ctx.beginPath(); ctx.arc(b.x, b.y, 6, 0, Math.PI*2); ctx.fill(); });
    enemies.forEach(e => {
        ctx.fillStyle = e.hitFlash > 0 ? 'white' : e.color; 
        if (e.isBoss && e.hp < 5) ctx.fillStyle = (Math.floor(Date.now()/100) % 2 === 0) ? '#ff003c' : '#fcee0a';
        ctx.beginPath();
        if (e.isBoss) { ctx.rect(e.x - e.size/2, e.y - e.size/2, e.size, e.size); ctx.fill(); ctx.fillStyle='black'; ctx.font = 'bold 16px Orbitron, Arial'; ctx.fillText('BOSS HP: ' + e.hp, e.x - 40, e.y - 50); }
        else { ctx.rect(e.x - e.size/2, e.y - e.size/2, e.size, e.size); ctx.fill(); }
    });
    powerUps.forEach(p => { ctx.fillStyle = '#fbbf24'; ctx.fillRect(p.x - p.size/2, p.y - p.size/2, p.size, p.size); });
    ctx.fillStyle = 'white'; ctx.font = '20px Orbitron, Arial'; ctx.fillText(`Score: ${score}`, 20, 30);
    if (powerUpActive) { ctx.fillStyle = '#fcee0a'; ctx.fillText('RAPID FIRE ACTIVE!', 20, 60); }
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
