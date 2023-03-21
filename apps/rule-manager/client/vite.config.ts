import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react-swc'

export default defineConfig({
  plugins: [react()],
  build: {
    manifest: true,
    rollupOptions: {
      input: '/src/ts/index.tsx',
      output: {
        entryFileNames: `build/[name].js`,
        chunkFileNames: `build/[name].js`,
        assetFileNames: `build/[name].[ext]`
      }
    },
    outDir: "../public/",
  },
  server: {
    origin: 'http://localhost:5173',
    fs: {
      allow: ['../public', '/']
    }
  },
})
