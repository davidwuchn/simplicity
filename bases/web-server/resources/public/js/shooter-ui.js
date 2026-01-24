function showGameOver(score) {
  document.getElementById('final-score').textContent = score;
  const gameOverOverlay = document.getElementById('game-over-overlay');
  if (gameOverOverlay) {
    gameOverOverlay.classList.remove('hidden');
  }
}

// Initialize username from hidden input
window.CURRENT_USERNAME = document.getElementById('current-username')?.value;
