const canvas = document.getElementById('bgCanvas');
const ctx = canvas.getContext('2d');

let width, height;
let cols, rows;
const cellSize = 10;
let grid, nextGrid;

function resize() {
    width = canvas.width = window.innerWidth;
    height = canvas.height = window.innerHeight;
    cols = Math.floor(width / cellSize);
    rows = Math.floor(height / cellSize);
    grid = createGrid();
    randomizeGrid();
}

function createGrid() {
    return new Array(cols).fill(null).map(() => new Array(rows).fill(0));
}

function randomizeGrid() {
    for (let i = 0; i < cols; i++) {
        for (let j = 0; j < rows; j++) {
            // 15% chance of being alive
            // State 1: Cyan (Friendly/Standard)
            // State 2: Red (Enemy/Virus)
            let rand = Math.random();
            if (rand < 0.10) grid[i][j] = 1;
            else if (rand < 0.15) grid[i][j] = 2;
            else grid[i][j] = 0;
        }
    }
}

function update() {
    nextGrid = createGrid();
    for (let i = 0; i < cols; i++) {
        for (let j = 0; j < rows; j++) {
            let state = grid[i][j];
            let neighbors1 = countNeighbors(grid, i, j, 1);
            let neighbors2 = countNeighbors(grid, i, j, 2);
            let totalNeighbors = neighbors1 + neighbors2;

            if (state == 0) {
                // Birth
                if (totalNeighbors == 3) {
                    // Majority wins birth
                    nextGrid[i][j] = neighbors2 > neighbors1 ? 2 : 1;
                }
            } else if (state == 1) { // Cyan
                if (totalNeighbors < 2 || totalNeighbors > 3) {
                    nextGrid[i][j] = 0; // Death by isolation or overpopulation
                } else {
                    // Consumption: If surrounded by enemies, convert
                    if (neighbors2 > 1) nextGrid[i][j] = 2; 
                    else nextGrid[i][j] = 1;
                }
            } else if (state == 2) { // Red
                if (totalNeighbors < 2 || totalNeighbors > 3) {
                    nextGrid[i][j] = 0;
                } else {
                    nextGrid[i][j] = 2; // Red is resilient
                }
            }
        }
    }
    grid = nextGrid;
}

function countNeighbors(grid, x, y, type) {
    let sum = 0;
    for (let i = -1; i < 2; i++) {
        for (let j = -1; j < 2; j++) {
            let col = (x + i + cols) % cols;
            let row = (y + j + rows) % rows;
            if (grid[col][row] == type) sum++;
        }
    }
    if (grid[x][y] == type) sum--;
    return sum;
}

function draw() {
    ctx.fillStyle = '#050505'; // Background
    ctx.fillRect(0, 0, width, height);

    for (let i = 0; i < cols; i++) {
        for (let j = 0; j < rows; j++) {
            if (grid[i][j] > 0) {
                ctx.fillStyle = grid[i][j] == 1 ? '#00f0ff' : '#ff003c'; // Cyan vs Red
                ctx.globalAlpha = 0.2;
                ctx.fillRect(i * cellSize, j * cellSize, cellSize - 1, cellSize - 1);
            }
        }
    }
    ctx.globalAlpha = 1.0;
}

function loop() {
    update();
    // Auto-spawn random noise
    if (Math.random() < 0.1) {
        let rx = Math.floor(Math.random() * cols);
        let ry = Math.floor(Math.random() * rows);
        // Randomly spawn Cyan or Red
        grid[rx][ry] = Math.random() > 0.5 ? 1 : 2;
        if (rx+1 < cols) grid[rx+1][ry] = grid[rx][ry];
        if (ry+1 < rows) grid[rx][ry+1] = grid[rx][ry];
    }
    draw();
    setTimeout(() => requestAnimationFrame(loop), 50); 
}

window.addEventListener('resize', resize);
window.addEventListener('mousemove', e => {
    // Add cells on mouse move
    const col = Math.floor(e.clientX / cellSize);
    const row = Math.floor(e.clientY / cellSize);
    if (col >= 0 && col < cols && row >= 0 && row < rows) {
        // User draws Cyan (1)
        grid[col][row] = 1;
        if (col+1 < cols) grid[col+1][row] = 1;
        if (row+1 < rows) grid[col][row+1] = 1;
    }
});

resize();
loop();
