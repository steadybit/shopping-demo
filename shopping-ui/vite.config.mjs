import viteTsconfigPaths from 'vite-tsconfig-paths';
import { defineConfig } from 'vite';
import svgr from 'vite-plugin-svgr';
import react from '@vitejs/plugin-react';

export default defineConfig(() => {
    return {
        server: {
            proxy: {
                '/products': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    xfwd: true
                },
                '/checkout': {
                    target: 'http://localhost:8080',
                    changeOrigin: true,
                    xfwd: true
                }
            }
        },

        css: {
            preprocessorOptions: {
                scss: {
                    loadPaths: ['.'],
                },
            },
        },

        build: {
            outDir: 'build'
        },
        plugins: [
            viteTsconfigPaths(),
            react(),
            svgr({
                svgrOptions: {}, // https://react-svgr.com/docs/options/
                esbuildOptions: {},
                include: '**/*.svg',
                exclude: ''
            })
        ]
    };
});