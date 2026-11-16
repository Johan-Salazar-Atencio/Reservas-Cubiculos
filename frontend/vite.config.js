import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

// La web corre en el puerto 5195 y reenvía /api a Spring (8095), así no hace falta CORS en desarrollo.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5195,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://localhost:8095', changeOrigin: true },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.js',
    css: false,
  },
})
