/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: 'rgb(var(--brand-color-rgb, 0 168 135) / <alpha-value>)',
          dark: 'rgb(var(--brand-color-dark-rgb, 0 143 115) / <alpha-value>)',
          light: 'rgb(var(--brand-color-light-rgb, 230 247 243) / <alpha-value>)'
        },
        page: '#F4F7F6'
      },
      boxShadow: {
        card: '0 8px 30px rgba(15, 23, 42, 0.06)'
      },
      borderRadius: {
        '2xl': '1.25rem'
      }
    }
  },
  plugins: []
}
