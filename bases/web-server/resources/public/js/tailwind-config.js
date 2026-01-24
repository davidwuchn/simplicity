tailwind.config = {
  theme: {
    extend: {
      colors: {
        "cyber-yellow": "#fcee0a",
        "cyber-cyan": "#00f0ff",
        "cyber-red": "#ff003c",
        "success-green": "#00ff00",
        "background": "#050505",
        "foreground": "#e2e8f0",
        "black": "#000",
        "gray-dark": "#1a1a1a",
        "gray-medium": "#2a2a2a",
        "gray-100": "#f5f5f5",
        "gray-200": "#e0e0e0",
        "gray-400": "#9ca3af",
        "gray-500": "#6b7280",
        "gray-700": "#333333",
        "gray-800": "#1f2937",
        "gray-900": "#111827",
        "zinc-800": "#27272a",
        "zinc-900": "#18181b"
      },
      fontFamily: {
        sans: ["Orbitron", "sans-serif"]
      },
      keyframes: {
        glitch: {
          "0%": { textShadow: "2px 0 #ff003c, -2px 0 #00f0ff" },
          "25%": { textShadow: "-2px 0 #ff003c, 2px 0 #00f0ff" },
          "50%": { textShadow: "2px 0 #00f0ff, -2px 0 #fcee0a" },
          "100%": { textShadow: "-2px 0 #00f0ff, 2px 0 #ff003c" }
        },
        spin: {
          to: { transform: "rotate(360deg)" }
        }
      },
      animation: {
        glitch: "glitch 1s infinite alternate-reverse",
        spin: "spin 0.6s linear infinite"
      }
    }
  }
};
