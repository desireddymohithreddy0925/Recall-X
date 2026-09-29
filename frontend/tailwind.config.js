/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'app-bg': '#080D17',
        'app-secondary': '#0E1624',
        'app-card': '#121C2B',
        'app-elevated': '#172235',
        'app-text-primary': '#F4F7FB',
        'app-text-secondary': '#94A3B8',
        'app-border': '#243247',
        'app-accent': '#38BDF8',
        'app-memory': '#8B5CF6',
        'app-learning': '#2DD4BF',
        'app-warning': '#F59E0B',
        'app-critical': '#F43F5E',
        'app-success': '#34D399',
      }
    },
  },
  plugins: [],
}