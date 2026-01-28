# Funny Toolbox UI

这是 Funny Toolbox 平台的前端项目，一个可扩展的工具平台。

## 功能特性

### 三个主要页面

1. **插件展示页面** (`/`)
   - 平台首页，展示所有已启用的插件
   - 卡片式布局，支持点击打开插件
   - 响应式设计，移动端友好
   - 支持在模态框中通过 iframe 展示插件页面
   - 支持在新窗口中打开插件页面

2. **登录页面** (`/login`)
   - 用户名和密码登录
   - 表单验证
   - 错误提示
   - 响应式布局

3. **后台管理页面** (`/admin`)
   - 需要登录认证
   - 插件列表展示（表格形式）
   - 插件管理操作：
     - 启用/禁用插件
     - 卸载插件
     - 重新加载插件
     - 上传并安装新插件
   - 响应式表格设计

## 技术栈

- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **路由**: Vue Router 4
- **HTTP 客户端**: Axios
- **UI 组件**: shadcn-vue (基于 Reka UI)
- **样式**: Tailwind CSS 4

## 项目结构

```
src/
├── api/                 # API 服务层
│   ├── types.ts        # 类型定义
│   ├── request.ts      # Axios 实例和拦截器
│   ├── auth.ts         # 认证相关 API
│   ├── plugins.ts      # 插件管理 API
│   └── index.ts        # API 统一导出
├── components/         # 组件目录
│   ├── PluginModal.vue # 插件模态框组件
│   └── ui/             # shadcn-vue UI 组件
├── views/              # 页面组件
│   ├── LoginView.vue   # 登录页面
│   ├── AdminView.vue   # 后台管理页面
│   └── PluginsView.vue # 插件展示页面
├── router/             # 路由配置
│   └── index.ts        # 路由定义和守卫
├── lib/                # 工具函数
│   └── utils.ts        # 通用工具函数
├── App.vue             # 根组件
└── main.ts             # 应用入口
```

## API 接口说明

### 无需认证的接口
- `GET /api/platform/plugins` - 获取插件列表
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/logout` - 用户登出

### 需要认证的接口
- `POST /api/platform/plugins/{id}/enable` - 启用插件
- `POST /api/platform/plugins/{id}/disable` - 禁用插件
- `DELETE /api/platform/plugins/{id}` - 卸载插件
- `POST /api/platform/plugins/{id}/reload` - 重新加载插件
- `POST /api/platform/plugins/install` - 上传并安装插件

## 开发说明

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173

### 构建生产版本

**独立部署模式**（前后端分离）：
```bash
npm run build
```
构建输出到 `dist` 目录

**嵌入式部署模式**（打包到 Spring Boot）：
```bash
npm run build:embedded
```
构建输出到 `dist-embedded` 目录

### 预览生产版本

```bash
npm run preview
```

## 部署模式

项目支持两种部署模式，通过环境变量配置：

### 1. 独立部署模式（standalone）

前端和后端分别部署，前端通过完整的 URL 调用后端 API。

**环境配置** (`.env.production`):
```env
VITE_API_BASE_URL=http://your-backend-server:8080
VITE_DEPLOYMENT_MODE=standalone
```

**部署步骤**:
1. 构建：`npm run build`
2. 将 `dist` 目录部署到 Nginx、Apache 等静态服务器
3. 配置反向代理到后端 API（可选）

**Nginx 配置示例**:
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /path/to/dist;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 可选：代理后端 API
    location /api/ {
        proxy_pass http://backend-server:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 2. 嵌入式部署模式（embedded）

前端打包后嵌入到 Spring Boot 应用，作为 JAR 包的一部分发布。

**环境配置** (`.env.production.embedded`):
```env
VITE_API_BASE_URL=
VITE_DEPLOYMENT_MODE=embedded
```

**部署步骤**:

1. 构建前端：
```bash
npm run build:embedded
```

2. 将 `dist-embedded` 目录下的所有文件复制到 Spring Boot 项目的 `src/main/resources/static` 目录：

Windows:
```bash
xcopy /E /I /Y dist-embedded\* path\to\springboot\src\main\resources\static
```

Linux/Mac:
```bash
cp -r dist-embedded/* path/to/springboot/src/main/resources/static/
```

3. Spring Boot 会自动提供静态资源服务，访问 `http://localhost:8080/` 即可

4. 打包并运行 Spring Boot 应用：
```bash
mvn clean package
java -jar target/your-app.jar
```

## 环境变量配置

| 变量名 | 说明 | 默认值 | 示例 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | 后端 API 基础地址（不含 /api 路径） | `http://localhost:8080` | `http://api.example.com` |
| `VITE_DEPLOYMENT_MODE` | 部署模式 | `standalone` | `standalone` 或 `embedded` |

**环境配置文件**:
- `.env.development` - 开发环境
- `.env.production` - 生产环境（独立部署）
- `.env.production.embedded` - 生产环境（嵌入式部署）

## 认证机制

- 使用 `localStorage` 存储认证状态
- Axios 请求拦截器自动携带 cookies（后端基于 Session）
- 路由守卫保护需要认证的页面
- 401 响应自动跳转到登录页

## 响应式设计

项目使用 Tailwind CSS 实现响应式布局：

- **移动端** (< 640px): 单列布局
- **平板** (640px - 1024px): 双列布局
- **桌面** (> 1024px): 三列布局

所有页面都针对不同屏幕尺寸进行了优化。

## 组件说明

### PluginModal

可复用的插件模态框组件，支持：
- iframe 内嵌展示插件页面
- 在新窗口中打开插件页面
- 响应式尺寸调整

**Props:**
- `plugin`: 插件对象
- `open`: 控制模态框显示/隐藏

**Events:**
- `update:open`: 更新模态框状态

## 注意事项

1. **部署模式配置**: 
   - 独立部署使用 `npm run build`
   - 嵌入式部署使用 `npm run build:embedded`
   
2. **后端地址配置**: 
   - 开发环境默认 `http://localhost:8080`
   - 生产环境需在对应的 `.env` 文件中配置
   
3. **跨域问题**: 
   - 开发环境已配置 Vite 代理自动处理
   - 独立部署模式需配置 Nginx 反向代理或后端 CORS
   - 嵌入式部署无跨域问题
   
4. **认证状态**: 当前使用 localStorage + Session，建议生产环境增强安全性

5. **插件页面**: 插件的 `frontendEntry` 字段应该提供可访问的页面 URL

6. **文件上传**: 上传插件时仅支持 `.jar` 文件

## 后续优化建议

1. 添加 loading 状态的统一管理
2. 实现更完善的错误处理和用户提示
3. 添加插件搜索和过滤功能
4. 实现插件的分页加载
5. 添加用户权限管理
6. 优化移动端的交互体验
7. 添加国际化支持

## License

MIT
