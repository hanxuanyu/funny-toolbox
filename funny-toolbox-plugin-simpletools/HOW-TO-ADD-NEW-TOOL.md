# 📝 如何添加新工具 - 完整指南

本文档详细说明了在 Funny Toolbox 项目中添加新工具的完整步骤，以"加解密工具"为例。

---

## 📋 目录

1. [前置准备](#前置准备)
2. [完整步骤清单](#完整步骤清单)
3. [详细步骤说明](#详细步骤说明)
4. [实际案例：加解密工具](#实际案例加解密工具)
5. [验证与测试](#验证与测试)
6. [常见问题](#常见问题)

---

## 🎯 前置准备

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0
- 了解 Vue 3 和 TypeScript 基础
- 熟悉 Tailwind CSS 和 shadcn-vue 组件

### 工具命名规范

- **工具 ID**：使用 kebab-case，如 `crypto-tool`、`image-converter`
- **组件名**：使用 PascalCase，如 `CryptoToolApp`、`ImageConverterApp`
- **文件名**：与工具 ID 保持一致，如 `tool-crypto-tool.html`

---

## ✅ 完整步骤清单

新增一个工具需要完成以下 **7 个步骤**：

- [ ] **步骤 1**：安装所需的依赖包（如有需要）
- [ ] **步骤 2**：创建工具目录结构
- [ ] **步骤 3**：创建 Vue 主组件文件
- [ ] **步骤 4**：创建入口文件 (main.ts)
- [ ] **步骤 5**：创建 HTML 模板文件
- [ ] **步骤 6**：创建工具图标 (favicon.svg)
- [ ] **步骤 7**：更新配置文件
  - [ ] 7.1 更新 `package.json` 添加构建脚本
  - [ ] 7.2 更新 `vite.config.ts` 添加入口配置
  - [ ] 7.3 更新 `src/App.vue` 添加导航链接

---

## 📖 详细步骤说明

### 步骤 1：安装依赖包

如果你的工具需要特定的第三方库，首先安装它们。

```bash
# 例如：安装加密库
npm install crypto-js

# 安装 TypeScript 类型定义（如果有）
npm install --save-dev @types/crypto-js
```

**注意事项：**
- 优先选择体积小、无依赖或依赖少的库
- 检查库的许可证是否兼容
- 考虑打包后的文件大小

---

### 步骤 2：创建工具目录结构

在 `src/tools/` 下创建新工具的目录：

```bash
src/tools/
└── your-tool-name/           # 工具目录（使用 kebab-case）
    ├── YourToolApp.vue       # 主组件（使用 PascalCase）
    ├── main.ts               # 入口文件
    └── assets/               # 资源目录
        └── favicon.svg       # 工具图标
```

**示例（加解密工具）：**
```bash
src/tools/
└── crypto-tool/
    ├── CryptoToolApp.vue
    ├── main.ts
    └── assets/
        └── favicon.svg
```

---

### 步骤 3：创建 Vue 主组件

创建 `src/tools/your-tool-name/YourToolApp.vue` 文件。

**基础模板：**

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

// 状态管理
const inputText = ref('')
const outputText = ref('')

// 核心功能函数
const process = () => {
  try {
    if (!inputText.value) {
      toast.error('请输入内容')
      return
    }
    
    // 你的处理逻辑
    outputText.value = inputText.value.toUpperCase()
    
    toast.success('处理成功')
  } catch (error) {
    toast.error(`处理失败: ${error instanceof Error ? error.message : '未知错误'}`)
  }
}

// 复制结果
const copyOutput = async () => {
  if (!outputText.value) {
    toast.error('没有可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(outputText.value)
    toast.success('已复制到剪贴板')
  } catch (error) {
    toast.error('复制失败')
  }
}

// 清空
const clear = () => {
  inputText.value = ''
  outputText.value = ''
  toast.info('已清空')
}
</script>

<template>
  <div class="min-h-screen w-full bg-gradient-to-br from-background via-muted/20 to-background">
    <Toaster position="top-center" richColors />
    
    <div class="container mx-auto p-4 md:p-8 max-w-7xl">
      <!-- 标题区域 -->
      <div class="mb-8 text-center">
        <h1 class="text-4xl md:text-5xl font-bold mb-3 bg-gradient-to-r from-primary to-primary/60 bg-clip-text text-transparent">
          工具名称
        </h1>
        <p class="text-muted-foreground text-lg">
          工具描述信息
        </p>
      </div>

      <!-- 主要内容区域 -->
      <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <!-- 输入区域 -->
        <Card class="p-6 shadow-lg">
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <Label class="text-lg font-semibold">输入</Label>
              <span class="text-sm text-muted-foreground">
                {{ inputText.length }} 字符
              </span>
            </div>
            <Textarea
              v-model="inputText"
              placeholder="请输入内容..."
              class="min-h-[300px] font-mono text-sm resize-none"
            />
            <div class="flex gap-2 flex-wrap">
              <Button @click="process" class="flex-1" size="lg">
                处理
              </Button>
              <Button @click="clear" variant="outline" size="lg">
                清空
              </Button>
            </div>
          </div>
        </Card>

        <!-- 输出区域 -->
        <Card class="p-6 shadow-lg">
          <div class="space-y-4">
            <div class="flex items-center justify-between">
              <Label class="text-lg font-semibold">输出</Label>
              <span class="text-sm text-muted-foreground">
                {{ outputText.length }} 字符
              </span>
            </div>
            <Textarea
              v-model="outputText"
              placeholder="处理结果将显示在这里..."
              class="min-h-[300px] font-mono text-sm resize-none bg-muted/50"
              readonly
            />
            <Button @click="copyOutput" variant="outline" class="w-full" size="lg">
              📋 复制结果
            </Button>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>
```

**关键点：**
- 使用 `<script setup lang="ts">` 语法
- 导入必要的 UI 组件
- 实现核心功能和错误处理
- 使用 `toast` 提供用户反馈
- 响应式布局设计

---

### 步骤 4：创建入口文件

创建 `src/tools/your-tool-name/main.ts`：

```typescript
import { createApp } from 'vue'
import '../../style.css'
import YourToolApp from './YourToolApp.vue'

createApp(YourToolApp).mount('#app')
```

**注意：**
- 导入全局样式 `../../style.css`
- 组件名与 Vue 文件保持一致

---

### 步骤 5：创建 HTML 模板

创建 `public/tool-your-tool-name.html`：

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
    <script type="module" src="/src/tools/your-tool-name/main.ts"></script>
  </body>
</html>
```

**关键点：**
- 文件名格式：`tool-{工具id}.html`
- 修改 `<title>` 为你的工具名称
- 修改 `<script>` 的 `src` 路径指向你的 main.ts

---

### 步骤 6：创建工具图标

创建 `src/tools/your-tool-name/assets/favicon.svg`：

```svg
<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#3b82f6"/>
  <text x="16" y="21" font-size="14" font-weight="bold" text-anchor="middle" fill="white">工</text>
  <circle cx="8" cy="8" r="2" fill="white" opacity="0.8"/>
  <circle cx="16" cy="8" r="2" fill="white" opacity="0.8"/>
  <circle cx="24" cy="8" r="2" fill="white" opacity="0.8"/>
</svg>
```

**设计建议：**
- 尺寸：32x32 像素
- 使用简单、识别性强的图标
- 选择与工具功能相关的颜色
- 保持与其他工具图标风格一致

**加解密工具图标示例：**
```svg
<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#8b5cf6"/>
  <g transform="translate(16, 16)">
    <!-- 锁的图标 -->
    <rect x="-5" y="-2" width="10" height="8" rx="1" fill="white" opacity="0.9"/>
    <path d="M -3 -2 L -3 -5 A 3 3 0 0 1 3 -5 L 3 -2" stroke="white" stroke-width="2" fill="none" opacity="0.9"/>
    <circle cx="0" cy="2" r="1.5" fill="#8b5cf6"/>
  </g>
  <circle cx="6" cy="6" r="1.5" fill="white" opacity="0.6"/>
  <circle cx="26" cy="6" r="1.5" fill="white" opacity="0.6"/>
</svg>
```

---

### 步骤 7：更新配置文件

#### 7.1 更新 `package.json`

在 `scripts` 中添加新工具的构建命令：

```json
{
  "scripts": {
    "dev": "vite",
    "build": "npm run build:encoding-tool && npm run build:json-formatter && npm run build:crypto-tool && npm run build:index && node scripts/reorganize-dist.mjs",
    "build:encoding-tool": "cross-env TOOL_NAME=encoding-tool vite build --config vite.config.tools.ts",
    "build:json-formatter": "cross-env TOOL_NAME=json-formatter vite build --config vite.config.tools.ts",
    "build:crypto-tool": "cross-env TOOL_NAME=crypto-tool vite build --config vite.config.tools.ts",
    "build:index": "vite build --config vite.config.index.ts",
    "preview": "vite preview"
  }
}
```

**修改点：**
1. 在 `build` 脚本中添加 `npm run build:your-tool-name &&`
2. 新增 `build:your-tool-name` 脚本，使用 `TOOL_NAME=your-tool-name`

---

#### 7.2 更新 `vite.config.ts`

在 `build.rollupOptions.input` 中添加新工具的入口：

```typescript
import path from 'node:path'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { viteSingleFile } from 'vite-plugin-singlefile'

export default defineConfig({
  base: './',
  plugins: [vue(), tailwindcss(), viteSingleFile({ removeViteModuleLoader: true })],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  build: {
    rollupOptions: {
      input: {
        index: path.resolve(__dirname, 'index.html'),
        'tool-encoding-tool': path.resolve(__dirname, 'public/tool-encoding-tool.html'),
        'tool-json-formatter': path.resolve(__dirname, 'public/tool-json-formatter.html'),
        'tool-crypto-tool': path.resolve(__dirname, 'public/tool-crypto-tool.html'),
      },
    },
  },
})
```

**修改点：**
在 `input` 对象中添加：
```typescript
'tool-your-tool-name': path.resolve(__dirname, 'public/tool-your-tool-name.html'),
```

---

#### 7.3 更新 `src/App.vue`

在导航页中添加新工具的链接：

```vue
<script setup lang="ts">
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'

const isDev = import.meta.env.DEV

const getToolHref = (toolId: string) => {
  if (isDev) {
    return `/tool-${toolId}.html`
  } else {
    return `./tools/${toolId}/index.html`
  }
}

const tools = [
  {
    id: 'encoding-tool',
    name: '编码工具',
    description: '支持 Base64、URL、HTML、Unicode、十六进制、二进制及哈希加密等多种编码方式。',
    href: getToolHref('encoding-tool'),
  },
  {
    id: 'json-formatter',
    name: 'JSON 工具箱',
    description: '格式化、压缩、验证、JSONPath 查询、结构分析等多功能 JSON 处理工具。',
    href: getToolHref('json-formatter'),
  },
  {
    id: 'crypto-tool',
    name: '加解密工具',
    description: '支持 AES、DES、3DES、RC4、Rabbit 等对称加密，以及 MD5、SHA、HMAC 等哈希算法。',
    href: getToolHref('crypto-tool'),
  },
  // 在这里添加新工具
]
</script>
```

**修改点：**
在 `tools` 数组中添加新对象：
```typescript
{
  id: 'your-tool-name',
  name: '工具名称',
  description: '工具描述信息',
  href: getToolHref('your-tool-name'),
},
```

---

## 🎯 实际案例：加解密工具

以下是添加"加解密工具"的完整实施过程。

### 1. 安装依赖

```bash
npm install crypto-js
npm install --save-dev @types/crypto-js
```

### 2. 创建目录结构

```
src/tools/crypto-tool/
├── CryptoToolApp.vue
├── main.ts
└── assets/
    └── favicon.svg
```

### 3. 创建主组件

文件：`src/tools/crypto-tool/CryptoToolApp.vue`

**核心功能：**
- 支持 15+ 种加密算法（AES、DES、3DES、RC4、Rabbit、MD5、SHA系列、HMAC系列）
- 算法分类展示（对称加密、哈希、HMAC）
- 密钥管理（手动输入、随机生成）
- 加密/解密操作
- 结果复制、输入输出交换

**代码示例（关键部分）：**

```vue
<script setup lang="ts">
import CryptoJS from 'crypto-js'

type CryptoAlgorithm = 'AES' | 'DES' | 'TripleDES' | 'RC4' | 'Rabbit' | 
                       'MD5' | 'SHA1' | 'SHA256' | 'SHA512' | 'SHA3' | 'RIPEMD160' |
                       'HMAC-MD5' | 'HMAC-SHA1' | 'HMAC-SHA256' | 'HMAC-SHA512'

const selectedAlgorithm = ref<CryptoAlgorithm>('AES')
const secretKey = ref('')
const inputText = ref('')
const outputText = ref('')

const encrypt = () => {
  try {
    let result = ''
    switch (selectedAlgorithm.value) {
      case 'AES':
        result = CryptoJS.AES.encrypt(inputText.value, secretKey.value).toString()
        break
      case 'MD5':
        result = CryptoJS.MD5(inputText.value).toString()
        break
      // ... 其他算法
    }
    outputText.value = result
    toast.success('加密成功')
  } catch (error) {
    toast.error(`加密失败: ${error.message}`)
  }
}
</script>
```

### 4. 创建入口文件

文件：`src/tools/crypto-tool/main.ts`

```typescript
import { createApp } from 'vue'
import '../../style.css'
import CryptoToolApp from './CryptoToolApp.vue'

createApp(CryptoToolApp).mount('#app')
```

### 5. 创建 HTML 模板

文件：`public/tool-crypto-tool.html`

```html
<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="./assets/favicon.svg" />
    <title>加解密工具 - Funny Toolbox</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  </head>
  <body class="min-h-screen bg-background text-foreground">
    <div id="app"></div>
    <script type="module" src="/src/tools/crypto-tool/main.ts"></script>
  </body>
</html>
```

### 6. 创建图标

文件：`src/tools/crypto-tool/assets/favicon.svg`

```svg
<svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
  <rect width="32" height="32" rx="6" fill="#8b5cf6"/>
  <g transform="translate(16, 16)">
    <rect x="-5" y="-2" width="10" height="8" rx="1" fill="white" opacity="0.9"/>
    <path d="M -3 -2 L -3 -5 A 3 3 0 0 1 3 -5 L 3 -2" stroke="white" stroke-width="2" fill="none" opacity="0.9"/>
    <circle cx="0" cy="2" r="1.5" fill="#8b5cf6"/>
  </g>
  <circle cx="6" cy="6" r="1.5" fill="white" opacity="0.6"/>
  <circle cx="26" cy="6" r="1.5" fill="white" opacity="0.6"/>
</svg>
```

### 7. 更新配置

**package.json：**
```json
"scripts": {
  "build": "npm run build:encoding-tool && npm run build:json-formatter && npm run build:crypto-tool && npm run build:index && node scripts/reorganize-dist.mjs",
  "build:crypto-tool": "cross-env TOOL_NAME=crypto-tool vite build --config vite.config.tools.ts"
}
```

**vite.config.ts：**
```typescript
build: {
  rollupOptions: {
    input: {
      // ...
      'tool-crypto-tool': path.resolve(__dirname, 'public/tool-crypto-tool.html'),
    },
  },
}
```

**src/App.vue：**
```typescript
const tools = [
  // ...
  {
    id: 'crypto-tool',
    name: '加解密工具',
    description: '支持 AES、DES、3DES、RC4、Rabbit 等对称加密，以及 MD5、SHA、HMAC 等哈希算法。',
    href: getToolHref('crypto-tool'),
  },
]
```

---

## ✅ 验证与测试

### 开发环境测试

```bash
# 启动开发服务器
npm run dev
```

访问：
- 导航页：http://localhost:5173/
- 工具页面：http://localhost:5173/tool-crypto-tool.html

**检查项：**
- [ ] 页面能正常加载
- [ ] 所有 UI 组件显示正常
- [ ] 核心功能运行正确
- [ ] 错误处理正常工作
- [ ] Toast 通知显示正确
- [ ] 响应式布局在不同屏幕下正常

### 构建测试

```bash
# 构建单个工具
npm run build:crypto-tool

# 构建所有工具
npm run build
```

**检查项：**
- [ ] 构建成功无错误
- [ ] 生成的文件在 `dist/tools/crypto-tool/index.html`
- [ ] 文件大小合理（通常 < 500KB）
- [ ] 打开生成的 HTML 文件能正常运行
- [ ] 所有资源（CSS、JS、图标）已内联

### 功能测试

**测试清单：**
- [ ] 所有按钮点击响应正常
- [ ] 输入验证工作正确
- [ ] 复制到剪贴板功能正常
- [ ] 清空/重置功能正常
- [ ] 错误处理和用户反馈友好
- [ ] 在不同浏览器中测试（Chrome、Firefox、Safari、Edge）
- [ ] 移动端响应式测试

---

## ❓ 常见问题

### Q1: 构建后文件过大怎么办？

**答：** 
1. 检查是否引入了不必要的依赖
2. 使用 Tree Shaking 优化导入：
   ```typescript
   // ❌ 不好
   import _ from 'lodash'
   
   // ✅ 好
   import debounce from 'lodash/debounce'
   ```
3. 考虑使用更轻量的替代库
4. 查看构建分析：`npm run build -- --mode analyze`

### Q2: 开发环境正常但构建后不工作？

**答：**
1. 检查是否使用了只在开发环境可用的 API
2. 检查路径引用是否正确（使用 `@/` 别名）
3. 检查控制台是否有错误信息
4. 确认 `vite.config.tools.ts` 配置正确

### Q3: Toast 通知不显示？

**答：**
1. 确认已导入 `Toaster` 组件并添加到模板中：
   ```vue
   <template>
     <div>
       <Toaster position="top-center" richColors />
       <!-- 其他内容 -->
     </div>
   </template>
   ```
2. 确认已正确导入 `toast`：
   ```typescript
   import { toast } from 'vue-sonner'
   ```

### Q4: 样式不生效？

**答：**
1. 确认已导入全局样式：`import '../../style.css'`
2. 检查 Tailwind 类名是否正确
3. 使用浏览器开发者工具检查 CSS 是否加载
4. 确认组件使用了正确的 UI 组件库导入

### Q5: 如何调试构建后的 HTML？

**答：**
1. 构建时保留源码映射（在 vite.config 中设置）
2. 使用 `npm run preview` 预览构建结果
3. 直接用浏览器打开 `dist/tools/your-tool/index.html`
4. 查看浏览器控制台错误信息

### Q6: 可以使用其他 UI 框架吗？

**答：** 可以，但建议使用已集成的 shadcn-vue 组件保持风格一致。如需其他组件：
1. 安装依赖
2. 在你的工具组件中单独导入使用
3. 注意打包体积影响

---

## 📚 参考资源

### 官方文档

- [Vue 3 文档](https://vuejs.org/)
- [Vite 文档](https://vitejs.dev/)
- [Tailwind CSS](https://tailwindcss.com/)
- [shadcn-vue](https://www.shadcn-vue.com/)
- [vue-sonner](https://vue-sonner.vercel.app/)

### 项目文件

- `DEVELOPMENT.md` - 详细开发指南
- `README.md` - 项目说明
- `vite.config.tools.ts` - 工具构建配置
- `components.json` - shadcn-vue 配置

### 加密库参考（加解密工具示例）

- [crypto-js](https://github.com/brix/crypto-js) - JavaScript 加密库
- [MDN Web Crypto API](https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API)

---

## 🎉 总结

添加一个新工具的核心步骤：

1. **准备**：安装依赖（如需要）
2. **创建**：目录、Vue组件、入口文件、HTML模板、图标
3. **配置**：更新 package.json、vite.config.ts、App.vue
4. **测试**：开发环境测试、构建测试、功能测试
5. **优化**：性能优化、错误处理、用户体验

**关键文件清单：**
```
✅ src/tools/your-tool-name/YourToolApp.vue
✅ src/tools/your-tool-name/main.ts
✅ src/tools/your-tool-name/assets/favicon.svg
✅ public/tool-your-tool-name.html
✅ package.json (scripts)
✅ vite.config.ts (input)
✅ src/App.vue (tools array)
```

遵循这个指南，你可以快速、规范地为 Funny Toolbox 添加新工具！🚀

---

**最后更新：** 2025年12月5日  
**版本：** 1.0.0  
**维护者：** Funny Toolbox Team
