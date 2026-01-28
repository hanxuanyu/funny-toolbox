// 构建后处理脚本：
// vite-plugin-singlefile 已经自动将 JS/CSS 内联到 HTML 中
// 此脚本只负责：为每个工具创建独立的发布目录，并拷贝静态资源

const fs = require('fs')
const path = require('path')

const distDir = path.resolve(__dirname, '..', 'dist')

if (!fs.existsSync(distDir)) {
  console.error('[organize-tools] dist 目录不存在，请先运行 npm run build')
  process.exit(1)
}

/** @param {string} dir */
function walk(dir) {
  const entries = fs.readdirSync(dir, { withFileTypes: true })
  for (const entry of entries) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) {
      walk(full)
    } else if (entry.isFile() && entry.name.endsWith('.html')) {
      processHtml(full)
    }
  }
}

/** @param {string} htmlPath */
function processHtml(htmlPath) {
  const html = fs.readFileSync(htmlPath, 'utf8')
  // vite-plugin-singlefile 已经处理了内联，直接使用原 HTML
  createPerToolBundle(htmlPath, htmlPath, html)
}

walk(distDir)

// 清理临时文件，只保留 tools 目录
cleanupDistFiles()

/**
 * 根据 html 路径和 inline html，生成每个工具独立的目录：
 *  - dist/tools/<name>/index.html  （inline 版本）
 *  - dist/tools/<name>/assets/*    （当前页面仍引用到的静态资源，如图片/icon 等）
 *
 * @param {string} htmlPath 原始 html 路径
 * @param {string} inlinePath 生成的 inline html 路径
 * @param {string} inlineHtml inline html 内容
 */
function createPerToolBundle(htmlPath, inlinePath, inlineHtml) {
  const rel = path.relative(distDir, htmlPath).replace(/\\/g, '/')

  // 只针对工具页面和首页生成独立目录：
  // - index.html → tools/index
  // - public/tool-xxx.html → tools/xxx
  let name = ''
  if (rel === 'index.html') {
    name = 'index'
  } else if (rel.startsWith('public/')) {
/**
 * 根据 html 路径，生成每个工具独立的目录：
 *  - dist/tools/<name>/index.html  （已由 vite-plugin-singlefile 内联的版本）
 *  - dist/tools/<name>/*           （静态资源，如图片/icon 等）
 *
 * @param {string} htmlPath 原始 html 路径
 * @param {string} _inlinePath 未使用（保留参数兼容性）
  const outDir = path.join(distDir, 'tools', name)
  fs.mkdirSync(outDir, { recursive: true })

  // 写入 HTML 为 index.html
  const targetHtmlPath = path.join(outDir, 'index.html')
  fs.writeFileSync(targetHtmlPath, html, 'utf8')

  // 拷贝静态资源（图片/icon 等）
  const assetHrefs = new Set()

  // 匹配 src/href 中的静态资源
  const assetRegex = /(src|href)=["']([^"']+\.(png|jpe?g|gif|svg|webp|ico))["']/gi
  let m
  while ((m = assetRegex.exec(html)) !== null) {
    const url = m[2]
    if (/^https?:\/\//.test(url)) continue
    assetHrefs.add(url)
  }

  // 扫描 public 目录中的常见静态资源
  const publicDir = path.join(path.dirname(distDir), 'public')
  if (fs.existsSync(publicDir)) {
    const publicFiles = fs.readdirSync(publicDir)
    for (const file of publicFiles) {
      if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(file)) {
        assetHrefs.add(file)
      }
    }
  }

  for (const href of assetHrefs) {
    const srcPath = path.resolve(distDir, href.replace(/^\.\//, ''))
    if (!fs.existsSync(srcPath)) continue
    const relToDist = path.relative(distDir, srcPath)
    const destPath = path.join(outDir, relToDist)
    fs.mkdirSync(path.dirname(destPath), { recursive: true })
    fs.copyFileSync(srcPath, destPath)
  }

  console.log('[organize-tools] bundled tool ->', path.relative(distDir, outDir))
}

/**
 * 清理 dist 目录中的临时文件，只保留最终的 tools 目录结构
/**
 * 清理 dist 目录中的临时文件，只保留最终的 tools 目录结构
 */
function cleanupDistFiles() {
  console.log('[organize-tools] cleaning up temporary files...')

  const rootFiles = fs.readdirSync(distDir)
  for (const file of rootFiles) {
    const fullPath = path.join(distDir, file)
    const stat = fs.statSync(fullPath)
    
    if (stat.isFile()) {
      // 删除非 index.html 的其他 HTML 文件
      if (file.endsWith('.html') && file !== 'index.html') {
        fs.unlinkSync(fullPath)
        console.log('[organize-tools] removed', file)
      }
    } else if (stat.isDirectory()) {
      // 删除 public 目录（已复制到各工具目录）
      // 注意：vite-plugin-singlefile 不会生成 assets 目录，所以不需要删除
      if (file === 'public') {
        fs.rmSync(fullPath, { recursive: true, force: true })
        console.log('[organize-tools] removed', file + '/')
      }
    }
  }

  console.log('[organize-tools] cleanup complete')
}