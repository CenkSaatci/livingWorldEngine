/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
// envDir: '..' -> liest root .env (enthält VITE_API_URL / VITE_WS_URL sowie Backend-Variablen).
// Vite exponiert ausschließlich VITE_-prefixierte Variablen an den Client.
export default defineConfig({
  plugins: [react()],
  envDir: '..',
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': '/src',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    // Playwright-Specs laufen über `npm run test:e2e`, nicht über Vitest.
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
  },
});