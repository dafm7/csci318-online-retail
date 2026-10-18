import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The Spring services have no CORS config, so the dev server proxies each API prefix to its service.
const service = (port: number) => ({ target: `http://localhost:${port}`, changeOrigin: true })

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api/products': service(8081),
      '/api/inventory': service(8081),
      '/api/sales': service(8081),
      '/api/admin': service(8081),
      '/api/users': service(8082),
      '/api/orders': service(8083),
      '/api/payments': service(8084),
      '/api/chat': service(8085),
    },
  },
})
