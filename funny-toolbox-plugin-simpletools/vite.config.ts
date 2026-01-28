import path from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { viteSingleFile } from 'vite-plugin-singlefile'

// https://vite.dev/config/
export default defineConfig({
  base: './',
  plugins: [
    vue(),
    tailwindcss(),
    viteSingleFile({
      removeViteModuleLoader: true, // 移除 Vite 模块加载器
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      input: {
        index: path.resolve(__dirname, 'index.html'),
        'tool-encoding-tool': path.resolve(
          __dirname,
          'public/tool-encoding-tool.html',
        ),
        'tool-json-formatter': path.resolve(
          __dirname,
          'public/tool-json-formatter.html',
        ),
        'tool-crypto-tool': path.resolve(
          __dirname,
          'public/tool-crypto-tool.html',
        ),
        'tool-ascii-art-tool': path.resolve(
          __dirname,
          'public/tool-ascii-art-tool.html',
        ),
        'tool-datetime-tool': path.resolve(
          __dirname,
          'public/tool-datetime-tool.html',
        ),
        'tool-color-tool': path.resolve(
          __dirname,
          'public/tool-color-tool.html',
        ),
      },
    },
  },
})
