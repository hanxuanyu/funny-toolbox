import path from 'node:path'
import { defineConfig, loadEnv } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const isEmbedded = env.VITE_DEPLOYMENT_MODE === 'embedded'
  
  return {
    plugins: [vue(), tailwindcss()],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    server: {
      port: 5173,
      proxy: env.VITE_DEPLOYMENT_MODE === 'standalone' && env.VITE_API_BASE_URL ? {
        '/api': {
          target: env.VITE_API_BASE_URL,
          changeOrigin: true,
          rewrite: (path) => path,
        },
      } : undefined,
    },
    build: {
      outDir: isEmbedded ? 'dist-embedded' : 'dist',
      assetsDir: 'assets',
      // 嵌入式部署时，资源使用相对路径
      base: isEmbedded ? './' : '/',
    },
  }
})
