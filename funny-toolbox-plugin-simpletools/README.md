# 小工具合集 - Vue + Vite + shadcn-vue

基于 **Vite 7.2.6**、**Vue 3.5.24** 和 **shadcn-vue 2.4.0** 构建的可独立部署的小工具集合。每个工具都可打包为单个 HTML 文件，所有 JS 和 CSS 已内联，便于直接发布。

## ✨ 特性

- 🎨 使用 **shadcn-vue** UI 组件库，界面美观统一
- 📦 使用 **vite-plugin-singlefile** 实现单文件打包，所有资源内联
- 🔔 使用 **vue-sonner** 在右下角展示通知
- 🛠️ 多入口构建配置，每个工具独立打包
- 🚀 零依赖部署，只需一个 HTML 文件
- ⚡ TypeScript 支持
- 🎯 Tailwind CSS 4.x 样式支持

## 📂 项目结构

```
├── src/
│   ├── App.vue                  # 导航页（工具汇总）
│   ├── main.ts                  # 导航页入口
│   ├── components/ui/           # shadcn-vue 组件
│   │   ├── button/
│   │   ├── card/
│   │   ├── label/
│   │   ├── textarea/
│   │   ├── separator/
│   │   └── sonner/              # Toaster 通知组件
│   └── tools/                   # 各工具实现
│       ├── base64-encoder/      # Base64 编码器
│       ├── json-formatter/      # JSON 格式化
│       └── uuid-generator/      # UUID 生成器
├── public/
│   ├── tool-base64-encoder.html
│   ├── tool-json-formatter.html
│   └── tool-uuid-generator.html
├── scripts/
│   └── reorganize-dist.mjs      # 构建后清理脚本
├── vite.config.ts               # 主配置（已弃用）
├── vite.config.tools.ts         # 工具单独构建配置
├── vite.config.index.ts         # 导航页构建配置
└── package.json
```

## 🚀 快速开始

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

### 生产构建

使用专业的 `vite-plugin-singlefile` 进行单文件打包：

```bash
npm run build:tools
```

构建产物结构：

```
dist/
├── index.html                          # 导航页
├── vite.svg
└── tools/
    ├── base64-encoder/
    │   └── index.html                  # ~158KB，所有 JS/CSS 已内联
    ├── json-formatter/
    │   └── index.html                  # ~158KB
    └── uuid-generator/
        └── index.html                  # ~157KB
```

每个 `index.html` 文件都是完全独立的，可以直接部署或分享。

## 🛠️ 当前工具

### 1. 编码工具 (Encoding Tool)
- 功能：支持多种编码格式转换
- 特性：Base64、URL、HTML、Unicode、十六进制、二进制、MD5、SHA-1、SHA-256
- 路径：`dist/tools/encoding-tool/index.html`

### 2. JSON 工具箱 (JSON Formatter)
- 功能：格式化、压缩、验证 JSON
- 特性：JSONPath 查询、结构分析、语法高亮、一键复制
- 路径：`dist/tools/json-formatter/index.html`

### 3. 加解密工具 (Crypto Tool) 🆕
- 功能：支持多种加密算法的加密和解密
- 特性：
  - **对称加密**：AES、DES、TripleDES、RC4、Rabbit
  - **哈希算法**：MD5、SHA-1、SHA-256、SHA-512、SHA-3、RIPEMD-160
  - **消息认证码**：HMAC-MD5、HMAC-SHA1、HMAC-SHA256、HMAC-SHA512
  - 随机密钥生成、输入输出交换、一键复制
- 路径：`dist/tools/crypto-tool/index.html`

## 📝 添加新工具

想要添加新工具？查看详细指南：

- 📘 **[快速添加指南](./QUICK-ADD-TOOL-GUIDE.md)** - 7个步骤清单，快速上手
- 📖 **[完整开发指南](./HOW-TO-ADD-NEW-TOOL.md)** - 详细说明、实战案例、常见问题

### 快速步骤概览

1. 安装依赖（如需要）
2. 创建工具目录：`src/tools/<tool-name>/`
3. 创建 Vue 主组件、入口文件、图标
4. 创建 HTML 模板：`public/tool-<tool-name>.html`
5. 更新 `package.json`、`vite.config.ts`、`src/App.vue`

完整步骤请参考上述文档。

### 工具开发示例

```vue
<!-- src/tools/<tool-name>/YourToolApp.vue -->
<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'
import { Toaster } from '@/components/ui/sonner'
import { toast } from 'vue-sonner'

const input = ref('')
const output = ref('')

const process = () => {
  try {
    output.value = input.value.toUpperCase()
    toast.success('处理成功')
  } catch (error) {
    toast.error('处理失败')
  }
}
</script>

<template>
  <div class="min-h-screen bg-background">
    <Toaster position="top-center" richColors />
    <Button @click="process">处理</Button>
  </div>
</template>
```
```

### 3. 更新构建脚本

在 `package.json` 中添加：

```json
{
  "scripts": {
    "build:tool:<tool-name>": "cross-env TOOL_NAME=<tool-name> vite build --config vite.config.tools.ts",
    "build:tools": "npm run build:tool:base64 && npm run build:tool:json && npm run build:tool:uuid && npm run build:tool:<tool-name> && npm run build:index && node scripts/reorganize-dist.mjs"
  }
}
```

### 4. 更新导航页

在 `src/App.vue` 的 `tools` 数组中添加：

```typescript
{
  id: '<tool-name>',
  name: '工具名称',
  description: '工具描述',
  href: `/tools/<tool-name>/`
}
```

## 🎨 UI 组件使用

项目使用 **shadcn-vue** 组件库，常用组件：

```vue
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Separator } from '@/components/ui/separator'
import { toast } from 'vue-sonner'

function handleClick() {
  toast.success('操作成功', { description: '详细信息' })
}
</script>

<template>
  <Card class="p-4">
    <Label for="input">标签</Label>
    <Textarea id="input" placeholder="输入..." />
    <Separator />
    <Button @click="handleClick">提交</Button>
  </Card>
</template>
```

### 通知提示（Sonner）

```typescript
import { toast } from 'vue-sonner'

// 成功提示
toast.success('标题', { description: '描述信息' })

// 错误提示
toast.error('错误', { description: '错误详情' })

// 警告提示
toast.warning('警告', { description: '警告信息' })
```

## 🏗️ 构建原理

项目使用 **分离构建** 策略，每个工具独立构建：

1. **工具构建** (`vite.config.tools.ts`)：
   - 通过环境变量 `TOOL_NAME` 指定工具名
   - 使用 `vite-plugin-singlefile` 内联所有 JS/CSS
   - 输出到 `dist/tools/<tool-name>/`

2. **导航页构建** (`vite.config.index.ts`)：
   - 单独构建导航页
   - 同样使用 `vite-plugin-singlefile`
   - 输出到 `dist/index.html`

3. **后处理** (`scripts/reorganize-dist.mjs`)：
   - 移动 HTML 文件到正确位置
   - 清理冗余文件和目录
   - 删除 `tools/index/` 等临时目录

## 📦 核心依赖

### 运行时
- `vue` ^3.5.24
- `vue-sonner` ^1.3.2（Toast 通知）
- `reka-ui` ^2.6.1（shadcn-vue 基础）
- `clsx` + `tailwind-merge`（样式合并）

### 开发工具
- `vite` ^7.2.6
- `@vitejs/plugin-vue` ^6.0.1
- `vite-plugin-singlefile` ^2.0.2（单文件打包）
- `@tailwindcss/vite` ^4.1.17
- `cross-env`（跨平台环境变量）

## 🔧 配置说明

### vite-plugin-singlefile 配置

```typescript
import { viteSingleFile } from 'vite-plugin-singlefile'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
    viteSingleFile({
      removeViteModuleLoader: true, // 移除 Vite 模块加载器
    }),
  ],
  // ...
})
```

### 多入口构建

由于 `vite-plugin-singlefile` 的 `inlineDynamicImports: true` 与多入口构建冲突，采用分离构建策略：

- 每个工具单独运行一次 Vite 构建
- 通过 `TOOL_NAME` 环境变量指定工具
- 使用 `npm-run-all` 或 `&&` 串行执行

## 📋 命令汇总

| 命令 | 说明 |
|------|------|
| `npm run dev` | 开发服务器 |
| `npm run build` | 传统多入口构建（已弃用） |
| `npm run build:tools` | 推荐：完整构建所有工具 + 导航页 |
| `npm run build:tool:base64` | 单独构建 Base64 编码器 |
| `npm run build:tool:json` | 单独构建 JSON 格式化工具 |
| `npm run build:tool:uuid` | 单独构建 UUID 生成器 |
| `npm run build:index` | 单独构建导航页 |
| `npm run preview` | 预览生产构建 |

## 🎯 部署建议

1. **单工具部署**：
   - 直接使用 `dist/tools/<tool-name>/index.html`
   - 无需额外资源文件
   - 可托管在任何静态服务器或 CDN

2. **完整部署**：
   - 部署整个 `dist/` 目录
   - 访问 `index.html` 查看导航页
   - 访问 `tools/<tool-name>/` 使用工具

3. **CDN 加速**：
   ```bash
   # 示例：上传到阿里云 OSS
   ossutil cp -r dist/ oss://your-bucket/tools/
   ```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License
