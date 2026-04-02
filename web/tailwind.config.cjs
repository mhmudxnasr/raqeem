/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        void: '#060606',
        base: '#0A0A0A',
        surface: '#121212',
        elevated: '#1A1A1A',
        subtle: '#222222',
        overlay: '#2A2A2A',
        purple: {
          950: '#0D0520',
          900: '#1A0D33',
          800: '#2D1B52',
          700: '#4C2A8A',
          600: '#6D28D9',
          500: '#7C3AED',
          400: '#8B5CF6',
          300: '#A78BFA',
          200: '#C4B5FD',
          100: '#EDE9FE',
        },
        positive: '#10B981',
        negative: '#EF4444',
        warning: '#F59E0B',
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
        serif: ['DM Serif Display', 'serif'],
      },
      borderRadius: {
        DEFAULT: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '24px',
      },
      boxShadow: {
        focus: '0 0 0 4px rgba(124, 58, 237, 0.15)',
        glass: '0 8px 32px 0 rgba(0, 0, 0, 0.37)',
      },
      animation: {
        pulseSoft: 'pulseSoft 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        fadeSlide: 'fadeSlide 400ms cubic-bezier(0.16, 1, 0.3, 1)',
        glow: 'glow 2s ease-in-out infinite alternate',
      },
      keyframes: {
        pulseSoft: {
          '0%, 100%': { opacity: '1' },
          '50%': { opacity: '.6' },
        },
        fadeSlide: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        glow: {
          'from': { 'box-shadow': '0 0 10px rgba(124, 58, 237, 0.1)' },
          'to': { 'box-shadow': '0 0 20px rgba(124, 58, 237, 0.3)' },
        }
      },
    },
  },
  plugins: [],
};
