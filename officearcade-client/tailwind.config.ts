import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        "oa-bg": "#060912",
        "oa-surface": "#111827",
        "oa-surface-soft": "#1a2436",
        "oa-border": "#273449",
        "oa-text": "#dce6f5",
        "oa-muted": "#8ea2c0",
        "oa-accent": "#2bc4a9",
        "oa-danger": "#f9736f"
      },
      boxShadow: {
        glow: "0 0 48px rgba(43, 196, 169, 0.18)"
      }
    }
  },
  plugins: []
} satisfies Config;
