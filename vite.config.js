import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api vai para o back-end Spring Boot (backend/, porta 8080).
// Outra porta/host: VITE_API_PROXY=http://localhost:9090 npm run dev
const API = process.env.VITE_API_PROXY || 'http://localhost:8080'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: true,
    proxy: { '/api': { target: API, changeOrigin: true } },
  },
  preview: {
    proxy: { '/api': { target: API, changeOrigin: true } },
  },
})
