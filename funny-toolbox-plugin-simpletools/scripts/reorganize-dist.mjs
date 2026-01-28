import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const distDir = path.resolve(__dirname, '../dist')

// 清理并重组工具目录结构
function reorganizeToolsDir() {
  const toolsDir = path.join(distDir, 'tools')
  
  if (!fs.existsSync(toolsDir)) {
    console.log('No tools directory found')
    return
  }

  const toolDirs = fs.readdirSync(toolsDir)

  for (const toolName of toolDirs) {
    const toolPath = path.join(toolsDir, toolName)
    
    if (!fs.statSync(toolPath).isDirectory()) continue
    
    const publicPath = path.join(toolPath, 'public')
    
    // 处理 public 子目录
    if (fs.existsSync(publicPath)) {
      const htmlFiles = fs.readdirSync(publicPath).filter(f => f.endsWith('.html'))
      
      if (htmlFiles.length > 0) {
        const htmlFile = htmlFiles[0]
        const htmlPath = path.join(publicPath, htmlFile)
        const targetPath = path.join(toolPath, 'index.html')
        
        fs.renameSync(htmlPath, targetPath)
        console.log(`✓ Moved ${toolName}/${htmlFile} → ${toolName}/index.html`)
      }
      
      if (fs.readdirSync(publicPath).length === 0) {
        fs.rmdirSync(publicPath)
        console.log(`✓ Removed empty ${toolName}/public directory`)
      }
    }
    
    // 删除工具目录下的冗余 HTML 文件（除了 index.html）
    const filesInTool = fs.readdirSync(toolPath)
    for (const file of filesInTool) {
      if (file.endsWith('.html') && file !== 'index.html') {
        fs.unlinkSync(path.join(toolPath, file))
        console.log(`✓ Removed redundant ${toolName}/${file}`)
      }
      // 删除冗余的 vite.svg
      if (file === 'vite.svg') {
        fs.unlinkSync(path.join(toolPath, file))
        console.log(`✓ Removed redundant ${toolName}/${file}`)
      }
    }
  }
  
  // 删除 tools/index 目录（这是导航页误放的）
  const indexToolDir = path.join(toolsDir, 'index')
  if (fs.existsSync(indexToolDir)) {
    fs.rmSync(indexToolDir, { recursive: true, force: true })
    console.log('✓ Removed redundant tools/index directory')
  }
}

// 清理 dist 根目录的冗余 HTML
function cleanDistRoot() {
  const files = fs.readdirSync(distDir)
  
  for (const file of files) {
    const filePath = path.join(distDir, file)
    if (fs.statSync(filePath).isFile() && file.startsWith('tool-') && file.endsWith('.html')) {
      fs.unlinkSync(filePath)
      console.log(`✓ Removed redundant dist/${file}`)
    }
  }
}

console.log('Reorganizing dist directory...')
reorganizeToolsDir()
cleanDistRoot()
console.log('Done!')
