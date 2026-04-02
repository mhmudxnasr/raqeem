/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        void: '#080808',
        base: '#0F0F0F',
        surface: '#161616',
        elevated: '#1E1E1E',
        subtle: '#242424',
        overlay: '#2C2C2C',
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
        negative: '#F87171',
        warning: '#FBBF24',
      },
      fontFamily: {
        sans: ['Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      borderRadius: {
        DEFAULT: '8px',
        lg: '12px',
        xl: '16px',
        '2xl': '20px',
      },
      boxShadow: {
        focus: '0 0 0 3px rgba(139, 92, 246, 0.12)',
      },
      animation: {
        fadeSlide: 'fadeSlide 300ms cubic-bezier(0.16, 1, 0.3, 1)',
      },
      keyframes: {
        fadeSlide: {
          '0%': { opacity: '0', transform: 'translateY(8px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};
