import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
  plugins: [react()],
  build: {
    manifest: true,
    rollupOptions: {
      input: '/src/ts/index.tsx',
    },
  },
  server: {
    origin: 'http://localhost:5173',
    fs: {
      allow: ['../public', '/']
    }
  }
})
