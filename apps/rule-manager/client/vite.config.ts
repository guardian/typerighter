import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react-swc';
import checker from 'vite-plugin-checker';

export default defineConfig({
	plugins: [
		react(),
		checker({
			typescript: true,
		}),
	],
	resolve: {
		alias: [
			{
				find: '@elastic/eui/src/',
				replacement: '@elastic/eui/es/',
			},
		],
	},
	build: {
		manifest: true,
		rollupOptions: {
			input: '/src/ts/index.tsx',
			output: {
				entryFileNames: `build/[name].js`,
				chunkFileNames: `build/[name].js`,
				assetFileNames: `build/[name].[ext]`,
			},
		},
		outDir: '../public/',
	},
	css: {
		preprocessorOptions: {
			// Silence deprecation warnings for EUI stylesheets
			quietDeps: true,
		},
	},
	server: {
		origin: 'http://localhost:5173',
		// We depend upon this port number in a few places, so fail fast if we cannot allocate it.
		strictPort: true,
		fs: {
			allow: ['../public/fonts', './'],
		},
	},
});
