import path from 'node:path'
import fs from 'node:fs'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { viteSingleFile } from 'vite-plugin-singlefile'

// 单独为每个工具构建的配置（支持 vite-plugin-singlefile）
export default defineConfig(({ mode }) => {
  const toolName = process.env.TOOL_NAME || 'encoding-tool'
  
  return {
    base: './',
    plugins: [
      vue(),
      tailwindcss(),
      viteSingleFile({
        removeViteModuleLoader: true,
      }),
      // 自定义插件：复制 favicon
      {
        name: 'copy-favicon',
        closeBundle() {
          const srcFavicon = path.resolve(__dirname, `src/tools/${toolName}/assets/favicon.svg`)
          const destDir = path.resolve(__dirname, `dist/tools/${toolName}/assets`)
          const destFavicon = path.resolve(destDir, 'favicon.svg')
          
          if (fs.existsSync(srcFavicon)) {
            if (!fs.existsSync(destDir)) {
              fs.mkdirSync(destDir, { recursive: true })
            }
            fs.copyFileSync(srcFavicon, destFavicon)
            console.log(`✓ Copied favicon for ${toolName}`)
          }
        },
      },
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, './src'),
      },
    },
    build: {
      outDir: `dist/tools/${toolName}`,
      emptyOutDir: true,
      rollupOptions: {
        input: path.resolve(__dirname, `public/tool-${toolName}.html`),
        output: {
          // 直接输出为 index.html，避免嵌套 public 目录
          entryFileNames: 'index.js',
          assetFileNames: 'assets/[name].[ext]',
        },
      },
    },
  }
})
