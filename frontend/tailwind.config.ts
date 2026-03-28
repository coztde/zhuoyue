import type { Config } from 'tailwindcss'

export default {
  content: ['./index.html', './src/**/*.{vue,ts}'],
  theme: {
    extend: {
      colors: {
        panel: '#0f172a',
        cyanGlow: '#58d8ff',
        neon: '#9eff00',
        ink: '#06111f',
      },
      boxShadow: {
        glow: '0 0 0 1px rgba(88,216,255,0.18), 0 24px 80px rgba(8,15,31,0.45)',
      },
      fontFamily: {
        display: ['"Orbitron"', '"Space Grotesk"', 'sans-serif'],
        sans: ['"Space Grotesk"', '"Microsoft YaHei"', 'sans-serif'],
      },
    },
  },
  plugins: [],
} satisfies Config

