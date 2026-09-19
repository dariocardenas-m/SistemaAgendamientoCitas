/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#00A887',
          dark: '#008F73',
          light: '#E6F7F3'
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
