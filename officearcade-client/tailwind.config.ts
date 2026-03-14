import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      fontFamily: {
        sans: ["Space Grotesk", "Segoe UI", "Tahoma", "sans-serif"],
        display: ["Rajdhani", "Space Grotesk", "Segoe UI", "sans-serif"]
      },
      colors: {
        "oa-bg": "#040612",
        "oa-bg-elevated": "#0b1120",
        "oa-surface": "#11182b",
        "oa-surface-soft": "#171f34",
        "oa-surface-strong": "#202b44",
        "oa-border": "#2d3a58",
        "oa-text": "#f2f6ff",
        "oa-muted": "#9caecc",
        "oa-accent": "#20d4ff",
        "oa-accent-alt": "#c75bff",
        "oa-success": "#2ed39a",
        "oa-warning": "#f8c95b",
        "oa-danger": "#ff6f87",
        "oa-info": "#66b4ff"
      },
      boxShadow: {
        glow: "0 0 52px rgba(32, 212, 255, 0.22)",
        panel: "0 24px 48px -32px rgba(4, 8, 20, 0.9)",
        lift: "0 16px 30px -24px rgba(4, 8, 20, 0.8)"
      }
    }
  },
  plugins: []
} satisfies Config;
