# 配置参数 - 请修改为你的 Spring Boot 项目路径
$SPRINGBOOT_PROJECT_PATH = "C:\path\to\your\springboot\project"
$STATIC_DIR = "$SPRINGBOOT_PROJECT_PATH\src\main\resources\static"

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Funny Toolbox UI - 嵌入式部署脚本" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Spring Boot 项目路径是否存在
if (-not (Test-Path $SPRINGBOOT_PROJECT_PATH)) {
    Write-Host "错误: Spring Boot 项目路径不存在: $SPRINGBOOT_PROJECT_PATH" -ForegroundColor Red
    Write-Host "请在脚本中修改 SPRINGBOOT_PROJECT_PATH 变量" -ForegroundColor Yellow
    exit 1
}

Write-Host "[1/4] 开始构建前端项目（嵌入式模式）..." -ForegroundColor Green
npm run build:embedded

if ($LASTEXITCODE -ne 0) {
    Write-Host "错误: 构建失败！" -ForegroundColor Red
    exit 1
}

Write-Host "[2/4] 清理旧的静态文件..." -ForegroundColor Yellow
if (Test-Path $STATIC_DIR) {
    Remove-Item -Path "$STATIC_DIR\*" -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  已清理: $STATIC_DIR" -ForegroundColor Gray
} else {
    New-Item -ItemType Directory -Path $STATIC_DIR -Force | Out-Null
    Write-Host "  已创建目录: $STATIC_DIR" -ForegroundColor Gray
}

Write-Host "[3/4] 复制新的静态文件到 Spring Boot 项目..." -ForegroundColor Yellow
Copy-Item -Path "dist-embedded\*" -Destination $STATIC_DIR -Recurse -Force
Write-Host "  已复制到: $STATIC_DIR" -ForegroundColor Gray

Write-Host "[4/4] 统计文件信息..." -ForegroundColor Yellow
$fileCount = (Get-ChildItem -Path $STATIC_DIR -Recurse -File | Measure-Object).Count
$totalSize = (Get-ChildItem -Path $STATIC_DIR -Recurse -File | Measure-Object -Property Length -Sum).Sum / 1MB
Write-Host "  文件数量: $fileCount" -ForegroundColor Gray
Write-Host "  总大小: $([Math]::Round($totalSize, 2)) MB" -ForegroundColor Gray

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  部署完成！" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "后续步骤:" -ForegroundColor Cyan
Write-Host "  1. 进入 Spring Boot 项目目录:" -ForegroundColor White
Write-Host "     cd $SPRINGBOOT_PROJECT_PATH" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. 打包 Spring Boot 应用:" -ForegroundColor White
Write-Host "     mvn clean package" -ForegroundColor Gray
Write-Host ""
Write-Host "  3. 运行应用:" -ForegroundColor White
Write-Host "     java -jar target\your-app.jar" -ForegroundColor Gray
Write-Host ""
Write-Host "  4. 访问应用:" -ForegroundColor White
Write-Host "     http://localhost:8080" -ForegroundColor Gray
Write-Host ""
