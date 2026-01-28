# Vite 配置示例

如果后端服务运行在不同的端口（例如 http://localhost:8080），您可以在 `vite.config.ts` 中配置代理：

```typescript
import path from 'node:path'
import { defineConfig } from 'vite'
import tailwindcss from '@tailwindcss/vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  // 添加代理配置
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端服务地址
        changeOrigin: true,
        // 如果后端 API 路径不包含 /api 前缀，可以使用 rewrite
        // rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
})
```

## 配置说明

- `target`: 后端服务的地址
- `changeOrigin`: 改变请求头中的 origin 字段
- `rewrite`: 可选，重写请求路径

## 使用场景

1. **后端在不同端口**: 例如前端 `http://localhost:5173`，后端 `http://localhost:8080`
2. **开发环境跨域**: 避免浏览器的跨域限制

## 注意事项

- 代理配置仅在开发环境有效
- 生产环境需要通过 Nginx 等反向代理或后端 CORS 配置解决跨域问题
- 确保后端服务已启动并可访问
