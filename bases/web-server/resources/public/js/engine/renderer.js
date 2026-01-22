import { COLORS } from './config.js';

export function drawDrone(ctx, x, y, size, color, rotation, faction) {
    ctx.save();
    ctx.translate(x, y);
    ctx.rotate(rotation);
    ctx.fillStyle = color;
    
    if (!faction) faction = 'protoss'; 

    if (faction === 'zerg') {
        if (size < 15) { // Minion
            ctx.fillStyle = '#facc15'; 
            ctx.beginPath(); 
            ctx.arc(0, 0, size/2, 0, Math.PI*2); 
            ctx.fill(); 
            ctx.strokeStyle = color; 
            ctx.lineWidth = 2; 
            ctx.beginPath(); 
            ctx.moveTo(0, 0); 
            ctx.lineTo(Math.sin(Date.now()/50)*3, size*1.5); 
            ctx.stroke(); 
        } else { // Drone
            ctx.shadowBlur = 10; 
            ctx.shadowColor = COLORS.ZERG_GLOW; 
            ctx.fillStyle = COLORS.ZERG_CORE; 
            ctx.beginPath(); 
            ctx.arc(0, -size/3, size/3, Math.PI, 0); 
            ctx.bezierCurveTo(size/2, 0, size/4, size/2, 0, size); 
            ctx.bezierCurveTo(-size/4, size/2, -size/2, 0, -size/3, -size/3); 
            ctx.fill(); 
            
            ctx.strokeStyle = '#d8b4fe'; 
            ctx.lineWidth = 2; 
            const wiggle = Math.sin(Date.now()/200)*5; 
            ctx.beginPath(); 
            ctx.moveTo(-size/4, 0); ctx.quadraticCurveTo(-size, size/2, -size/2+wiggle, size+10); 
            ctx.moveTo(size/4, 0); ctx.quadraticCurveTo(size, size/2, size/2-wiggle, size+10); 
            ctx.stroke(); 
            
            ctx.fillStyle = COLORS.ZERG_EYE; 
            ctx.beginPath(); 
            ctx.arc(-size/4, -size/4, 3, 0, Math.PI*2); 
            ctx.arc(size/4, -size/4, 3, 0, Math.PI*2); 
            ctx.fill(); 
            ctx.shadowBlur = 0; 
        }
    } else { // Protoss
        ctx.shadowBlur = 10; 
        ctx.shadowColor = color; 
        ctx.beginPath(); 
        ctx.arc(0, 0, size/3, 0, Math.PI*2); 
        ctx.fill(); 
        
        ctx.fillStyle = COLORS.PROTOSS_CORE; 
        ctx.beginPath(); 
        ctx.moveTo(0, size/2); 
        ctx.lineTo(size/1.2, -size/2); 
        ctx.lineTo(0, -size/4); 
        ctx.lineTo(-size/1.2, -size/2); 
        ctx.closePath(); 
        ctx.fill(); 
        
        ctx.fillStyle = COLORS.PROTOSS_CRYSTAL; 
        ctx.beginPath(); 
        ctx.moveTo(0, -size/4); 
        ctx.lineTo(size/6, 0); 
        ctx.lineTo(0, size/4); 
        ctx.lineTo(-size/6, 0); 
        ctx.fill(); 
    }
    ctx.restore();
}

export function drawBoss(ctx, e, beatKick, beatSnare) {
    ctx.save(); 
    ctx.translate(e.x, e.y); 
    const bossScale = 1.0 + beatKick * 0.05; 
    ctx.scale(bossScale, bossScale);
    
    ctx.fillStyle = (e.hp < 10 && Math.floor(Date.now()/100)%2===0) ? COLORS.ENEMY_DEFAULT : '#2a2a2a'; 
    ctx.strokeStyle = '#fcee0a'; 
    ctx.lineWidth = 3;
    
    ctx.beginPath(); 
    ctx.moveTo(0, e.size/2); 
    ctx.lineTo(e.size/2, -e.size/4); 
    ctx.lineTo(e.size/1.5, -e.size/2); 
    ctx.lineTo(0, -e.size/2+10); 
    ctx.lineTo(-e.size/1.5, -e.size/2); 
    ctx.lineTo(-e.size/2, -e.size/4); 
    ctx.closePath(); 
    ctx.fill(); 
    ctx.stroke();
    
    ctx.fillStyle = COLORS.ENEMY_DEFAULT; 
    ctx.shadowBlur = 20 + beatSnare*20; 
    ctx.shadowColor = COLORS.ENEMY_DEFAULT; 
    ctx.beginPath(); 
    ctx.arc(0, 0, e.size/4, 0, Math.PI*2); 
    ctx.fill(); 
    ctx.shadowBlur = 0;
    
    ctx.fillStyle = COLORS.LIFE; 
    ctx.fillRect(-e.size/3, -e.size/2 - 10, 10, 20); 
    ctx.fillRect(e.size/3 - 10, -e.size/2 - 10, 10, 20); 
    
    ctx.restore();
    ctx.fillStyle = '#fcee0a'; 
    ctx.font = 'bold 16px Orbitron, Arial'; 
    ctx.fillText('DREADNOUGHT HP: ' + e.hp, e.x - 60, e.y - e.size/2 - 20);
}
