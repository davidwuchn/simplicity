/**
 * UI Utilities (Toasts, Loading States)
 * Extracted from layout.clj to reduce slop.
 */

/* === Toast Notifications === */
function showToast(message, type = 'info', duration = 3000) {
  const container = document.getElementById('toast-container');
  if (!container) return;
  
  const toast = document.createElement('div');
  toast.className = `toast ${type} fade-in`;
  toast.textContent = message;
  toast.setAttribute('role', 'status');
  container.appendChild(toast);
  
  setTimeout(() => {
    toast.style.animation = 'slideIn 0.3s ease-out reverse';
    setTimeout(() => toast.remove(), 300);
  }, duration);
}

/* === Form Loading State === */
document.addEventListener('DOMContentLoaded', () => {
  const forms = document.querySelectorAll('form');
  forms.forEach(form => {
    form.addEventListener('submit', (e) => {
      // Check if the form is valid (if browser validation is active)
      if (!form.checkValidity()) return;
      
      const btn = form.querySelector('button[type=submit]');
      if (btn) {
        btn.classList.add('loading');
        btn.disabled = true;
      }
    });
  });
});
