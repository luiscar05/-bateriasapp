import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// El proxy reenvía /api al backend: el navegador cree que todo es el mismo origen,
// así que no hace falta configurar CORS en Spring Boot.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
