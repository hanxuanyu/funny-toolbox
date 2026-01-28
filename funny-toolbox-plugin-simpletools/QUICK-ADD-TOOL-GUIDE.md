# 新增工具改动点总结

本文档简明扼要地列出了在 Funny Toolbox 项目中新增一个工具所需的全部改动点。

---

## 📝 改动清单（7 个步骤）

### ✅ 步骤 1：安装依赖（可选）

如果工具需要第三方库，安装相关依赖：

```bash
npm install <package-name>
npm install --save-dev @types/<package-name>  # 如果有 TypeScript 类型
```

**示例（加解密工具）：**
```bash
npm install crypto-js
npm install --save-dev @types/crypto-js
```

---

### ✅ 步骤 2：创建工具目录和文件

创建以下文件结构：

```
src/tools/<tool-name>/
├── <ToolName>App.vue      # 主组件
├── main.ts                # 入口文件
└── assets/
    └── favicon.svg        # 工具图标
```

**示例（加解密工具）：**
```
src/tools/crypto-tool/
├── CryptoToolApp.vue
├── main.ts
└── assets/
    └── favicon.svg
```

---

### ✅ 步骤 3：创建主组件

**文件：** `src/tools/<tool-name>/<ToolName>App.vue`

**必要导入：**
```vue
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'
// 其他必要的导入...
</script>
```

**要点：**
- 实现核心功能逻辑
- 错误处理和用户反馈（使用 toast）
- 响应式布局设计

---

### ✅ 步骤 4：创建入口文件

**文件：** `src/tools/<tool-name>/main.ts`

```typescript
import { createApp } from 'vue'
import '../../style.css'
import <ToolName>App from './<ToolName>App.vue'

createApp(<ToolName>App).mount('#app')
```

**示例（加解密工具）：**
```typescript
import { createApp } from 'vue'
import '../../style.css'
import CryptoToolApp from './CryptoToolApp.vue'

createApp(CryptoToolApp).mount('#app')
```

---

### ✅ 步骤 5：创建 HTML 模板

**文件：** `public/tool-<tool-name>.html`

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="./assets/favicon.svg" />
    <title>工具名称 - Funny Toolbox</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  </head>
  <body class="min-h-screen bg-background text-foreground">
    <div id="app"></div>
    <script type="module" src="/src/tools/<tool-name>/main.ts"></script>
  </body>
</html>
```

**示例（加解密工具）：**
```html
<!-- public/tool-crypto-tool.html -->
<script type="module" src="/src/tools/crypto-tool/main.ts"></script>
```

---

### ✅ 步骤 6：创建工具图标

**文件：** `src/tools/<tool-name>/assets/favicon.svg`

```svg
<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#颜色代码"/>
  <!-- 你的图标设计 -->
</svg>
```

**要求：**
- 尺寸：32x32 像素
- 简洁、识别度高
- 与工具功能相关

---

### ✅ 步骤 7：更新配置文件（3 个文件）

#### 7.1 更新 `package.json`

**位置：** `scripts` 字段

**修改前：**
```json
"scripts": {
  "build": "npm run build:encoding-tool && npm run build:json-formatter && npm run build:index && node scripts/reorganize-dist.mjs",
  "build:encoding-tool": "...",
  "build:json-formatter": "..."
}
```

**修改后：**
```json
"scripts": {
  "build": "npm run build:encoding-tool && npm run build:json-formatter && npm run build:<tool-name> && npm run build:index && node scripts/reorganize-dist.mjs",
  "build:encoding-tool": "...",
  "build:json-formatter": "...",
  "build:<tool-name>": "cross-env TOOL_NAME=<tool-name> vite build --config vite.config.tools.ts"
}
```

**实例（加解密工具）：**
```json
"build": "... && npm run build:crypto-tool && ...",
"build:crypto-tool": "cross-env TOOL_NAME=crypto-tool vite build --config vite.config.tools.ts"
```

---

#### 7.2 更新 `vite.config.ts`

**位置：** `build.rollupOptions.input` 对象

**修改前：**
```typescript
build: {
  rollupOptions: {
    input: {
      index: path.resolve(__dirname, 'index.html'),
      'tool-encoding-tool': path.resolve(__dirname, 'public/tool-encoding-tool.html'),
      'tool-json-formatter': path.resolve(__dirname, 'public/tool-json-formatter.html'),
    },
  },
}
```

**修改后：**
```typescript
build: {
  rollupOptions: {
    input: {
      index: path.resolve(__dirname, 'index.html'),
      'tool-encoding-tool': path.resolve(__dirname, 'public/tool-encoding-tool.html'),
      'tool-json-formatter': path.resolve(__dirname, 'public/tool-json-formatter.html'),
      'tool-<tool-name>': path.resolve(__dirname, 'public/tool-<tool-name>.html'),
    },
  },
}
```

**实例（加解密工具）：**
```typescript
'tool-crypto-tool': path.resolve(__dirname, 'public/tool-crypto-tool.html'),
```

---

#### 7.3 更新 `src/App.vue`

**位置：** `tools` 数组

**修改前：**
```typescript
const tools = [
  {
    id: 'encoding-tool',
    name: '编码工具',
    description: '...',
    href: getToolHref('encoding-tool'),
  },
  {
    id: 'json-formatter',
    name: 'JSON 工具箱',
    description: '...',
    href: getToolHref('json-formatter'),
  },
]
```

**修改后：**
```typescript
const tools = [
  {
    id: 'encoding-tool',
    name: '编码工具',
    description: '...',
    href: getToolHref('encoding-tool'),
  },
  {
    id: 'json-formatter',
    name: 'JSON 工具箱',
    description: '...',
    href: getToolHref('json-formatter'),
  },
  {
    id: '<tool-name>',
    name: '工具名称',
    description: '工具描述信息',
    href: getToolHref('<tool-name>'),
  },
]
```

**实例（加解密工具）：**
```typescript
{
  id: 'crypto-tool',
  name: '加解密工具',
  description: '支持 AES、DES、3DES、RC4、Rabbit 等对称加密，以及 MD5、SHA、HMAC 等哈希算法。',
  href: getToolHref('crypto-tool'),
},
```

---

## 🎯 快速检查清单

在提交代码前，确保完成以下检查：

- [ ] **文件创建**
  - [ ] `src/tools/<tool-name>/<ToolName>App.vue` 
  - [ ] `src/tools/<tool-name>/main.ts`
  - [ ] `src/tools/<tool-name>/assets/favicon.svg`
  - [ ] `public/tool-<tool-name>.html`

- [ ] **配置更新**
  - [ ] `package.json` - 添加 `build:<tool-name>` 脚本
  - [ ] `package.json` - 在 `build` 脚本中添加新工具
  - [ ] `vite.config.ts` - 在 `input` 中添加新工具入口
  - [ ] `src/App.vue` - 在 `tools` 数组中添加新工具

- [ ] **测试验证**
  - [ ] `npm run dev` - 开发环境运行正常
  - [ ] `npm run build:<tool-name>` - 单独构建成功
  - [ ] `npm run build` - 完整构建成功
  - [ ] 访问导航页能看到新工具
  - [ ] 点击能正常打开新工具页面
  - [ ] 工具功能运行正常

---

## 📂 改动文件汇总

每次新增工具需要修改/创建的文件：

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `src/tools/<tool-name>/<ToolName>App.vue` | ✨ 创建 | 主组件 |
| `src/tools/<tool-name>/main.ts` | ✨ 创建 | 入口文件 |
| `src/tools/<tool-name>/assets/favicon.svg` | ✨ 创建 | 工具图标 |
| `public/tool-<tool-name>.html` | ✨ 创建 | HTML 模板 |
| `package.json` | ✏️ 修改 | 添加构建脚本 |
| `vite.config.ts` | ✏️ 修改 | 添加入口配置 |
| `src/App.vue` | ✏️ 修改 | 添加导航链接 |

**总计：** 4 个新建文件 + 3 个修改文件 = 7 个文件操作

---

## 🚀 实战示例：加解密工具

### 改动点对照表

| 步骤 | 文件/操作 | 实际内容 |
|-----|----------|---------|
| 1. 安装依赖 | 命令行 | `npm install crypto-js @types/crypto-js` |
| 2. 创建目录 | 目录结构 | `src/tools/crypto-tool/` |
| 3. 主组件 | Vue 文件 | `CryptoToolApp.vue` (支持15+种算法) |
| 4. 入口文件 | TS 文件 | `main.ts` |
| 5. HTML模板 | HTML 文件 | `public/tool-crypto-tool.html` |
| 6. 图标 | SVG 文件 | `assets/favicon.svg` (紫色锁图标) |
| 7.1 package.json | scripts | 添加 `build:crypto-tool` |
| 7.2 vite.config.ts | input | 添加 `'tool-crypto-tool': ...` |
| 7.3 App.vue | tools 数组 | 添加加解密工具对象 |

### 构建结果

```bash
✓ dist/tools/crypto-tool/index.html  341.51 kB │ gzip: 112.03 kB
```

---

## 💡 小贴士

1. **命名一致性**：工具 ID、文件名、目录名保持统一（kebab-case）
2. **组件命名**：Vue 组件使用 PascalCase（如 `CryptoToolApp`）
3. **图标设计**：使用在线工具如 [SVG Editor](https://boxy-svg.com/) 创建图标
4. **测试优先**：先在开发环境验证，再进行构建
5. **依赖控制**：谨慎添加大型依赖，注意打包体积
6. **错误处理**：使用 try-catch 和 toast 提供友好的用户反馈

---

## 📚 相关文档

- 📖 [完整开发指南](./HOW-TO-ADD-NEW-TOOL.md) - 详细的步骤说明和最佳实践
- 📖 [项目开发文档](./DEVELOPMENT.md) - 项目架构和技术栈说明
- 📖 [README](./README.md) - 项目概览

---

**最后更新：** 2025年12月5日  
**版本：** 1.0.0
