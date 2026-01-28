# 快速配置指南

## 配置后端地址

根据您的部署方式选择相应的配置文件：

### 开发环境

编辑 `.env.development`：

```env
# 后端地址（默认）
VITE_API_BASE_URL=http://localhost:8080

# 部署模式
VITE_DEPLOYMENT_MODE=standalone
```

如果您的后端运行在不同的地址/端口，请修改 `VITE_API_BASE_URL`：

```env
# 示例：后端在 3000 端口
VITE_API_BASE_URL=http://localhost:3000

# 示例：后端在远程服务器
VITE_API_BASE_URL=http://192.168.1.100:8080
```

### 独立部署模式（生产环境）

编辑 `.env.production`：

```env
# 修改为您的后端生产地址
VITE_API_BASE_URL=http://your-backend-server.com:8080

# 部署模式
VITE_DEPLOYMENT_MODE=standalone
```

常见配置示例：

```env
# 示例 1：使用域名
VITE_API_BASE_URL=https://api.example.com

# 示例 2：使用 IP
VITE_API_BASE_URL=http://192.168.1.100:8080

# 示例 3：使用相同域名（通过反向代理）
VITE_API_BASE_URL=https://example.com
```

### 嵌入式部署模式（打包到 Spring Boot）

编辑 `.env.production.embedded`：

```env
# 嵌入式部署不需要配置 API 地址
VITE_API_BASE_URL=

# 部署模式必须为 embedded
VITE_DEPLOYMENT_MODE=embedded
```

**注意**：嵌入式部署时，前端会自动使用相对路径 `/api` 访问后端。

## 常见问题

### Q1: 如何更改默认端口 8080？

**A:** 修改对应环境的 `.env` 文件中的 `VITE_API_BASE_URL`：

```env
VITE_API_BASE_URL=http://localhost:9090
```

### Q2: 跨域问题怎么解决？

**A:** 三种解决方案：

1. **开发环境**：已自动配置 Vite 代理，无需额外配置

2. **独立部署 + Nginx**：配置反向代理
   ```nginx
   location /api/ {
       proxy_pass http://backend:8080/api/;
   }
   ```

3. **Spring Boot CORS**：在后端添加 CORS 配置
   ```java
   @Configuration
   public class CorsConfig {
       @Bean
       public WebMvcConfigurer corsConfigurer() {
           return new WebMvcConfigurer() {
               @Override
               public void addCorsMappings(CorsRegistry registry) {
                   registry.addMapping("/api/**")
                           .allowedOrigins("http://localhost:5173")
                           .allowCredentials(true);
               }
           };
       }
   }
   ```

### Q3: 如何修改前端开发端口（默认 5173）？

**A:** 编辑 `vite.config.ts`：

```typescript
export default defineConfig(({ mode }) => {
  return {
    server: {
      port: 3000, // 修改为你想要的端口
      // ... 其他配置
    },
  }
})
```

### Q4: 部署后 API 请求 404？

**A:** 检查以下几点：

1. 确认后端地址配置正确
2. 确认后端 API 路径是 `/api/...`
3. 查看浏览器开发者工具的网络请求
4. 确认部署模式配置正确

### Q5: 嵌入式部署后前端页面空白？

**A:** 可能是路由配置问题，确保 Spring Boot 配置了前端路由支持：

```java
@Controller
public class SpaController {
    @GetMapping(value = {"", "/", "/login", "/admin"})
    public String forward() {
        return "forward:/index.html";
    }
}
```

## 部署前检查清单

### 独立部署

- [ ] 修改 `.env.production` 中的 `VITE_API_BASE_URL`
- [ ] 确认 `VITE_DEPLOYMENT_MODE=standalone`
- [ ] 运行 `npm run build`
- [ ] 部署 `dist` 目录到静态服务器
- [ ] 配置 Nginx 或其他 Web 服务器

### 嵌入式部署

- [ ] 确认 `.env.production.embedded` 中 `VITE_DEPLOYMENT_MODE=embedded`
- [ ] 确认 `VITE_API_BASE_URL` 为空
- [ ] 运行 `npm run build:embedded`
- [ ] 修改部署脚本中的 Spring Boot 项目路径
- [ ] 运行部署脚本
- [ ] 在 Spring Boot 项目中打包

## 环境切换

切换环境时，删除之前的构建产物：

```bash
# Windows
Remove-Item dist, dist-embedded -Recurse -Force -ErrorAction SilentlyContinue

# Linux/Mac
rm -rf dist dist-embedded
```

然后重新构建：

```bash
# 独立部署
npm run build

# 嵌入式部署
npm run build:embedded
```

## 测试配置

启动项目前，可以通过以下方式测试配置：

```bash
# 查看当前环境变量（开发环境）
npm run dev

# 在浏览器中打开控制台，输入：
console.log(import.meta.env)
```

检查输出中的：
- `VITE_API_BASE_URL` - 后端地址
- `VITE_DEPLOYMENT_MODE` - 部署模式
