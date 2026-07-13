/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      // LWE Design Tokens — siehe docs/UI-UX.md Abschnitt 10
      colors: {
        bg: {
          primary: '#0F1419',
          surface: '#1A2128',
          elevated: '#232C36',
        },
        text: {
          primary: '#E8EDF2',
          secondary: '#8B98A5',
        },
        accent: '#5BB8C5',
        danger: '#E0556B',
        success: '#7AC784',
        warning: '#F2A65A',
      },
      fontFamily: {
        heading: ['Inter', 'system-ui', 'sans-serif'],
        body: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      spacing: {
        1: '4px',
        2: '8px',
        3: '12px',
        4: '16px',
        6: '24px',
        8: '32px',
      },
    },
  },
  plugins: [],
};