# 部署指南

## 嵌入式部署到 Spring Boot

### 自动化部署脚本

#### Windows (PowerShell)

创建 `deploy-to-springboot.ps1`:

```powershell
# 配置参数
$SPRINGBOOT_PROJECT_PATH = "C:\path\to\your\springboot\project"
$STATIC_DIR = "$SPRINGBOOT_PROJECT_PATH\src\main\resources\static"

Write-Host "开始构建前端项目（嵌入式模式）..." -ForegroundColor Green
npm run build:embedded

if ($LASTEXITCODE -ne 0) {
    Write-Host "构建失败！" -ForegroundColor Red
    exit 1
}

Write-Host "清理旧的静态文件..." -ForegroundColor Yellow
if (Test-Path $STATIC_DIR) {
    Remove-Item -Path "$STATIC_DIR\*" -Recurse -Force
} else {
    New-Item -ItemType Directory -Path $STATIC_DIR -Force
}

Write-Host "复制新的静态文件到 Spring Boot 项目..." -ForegroundColor Yellow
Copy-Item -Path "dist-embedded\*" -Destination $STATIC_DIR -Recurse -Force

Write-Host "部署完成！" -ForegroundColor Green
Write-Host "静态文件已复制到: $STATIC_DIR" -ForegroundColor Cyan
Write-Host "请在 Spring Boot 项目中执行: mvn clean package" -ForegroundColor Cyan
```

运行：
```powershell
.\deploy-to-springboot.ps1
```

#### Linux/Mac (Bash)

创建 `deploy-to-springboot.sh`:

```bash
#!/bin/bash

# 配置参数
SPRINGBOOT_PROJECT_PATH="/path/to/your/springboot/project"
STATIC_DIR="$SPRINGBOOT_PROJECT_PATH/src/main/resources/static"

echo "开始构建前端项目（嵌入式模式）..."
npm run build:embedded

if [ $? -ne 0 ]; then
    echo "构建失败！"
    exit 1
fi

echo "清理旧的静态文件..."
rm -rf "$STATIC_DIR"/*

echo "复制新的静态文件到 Spring Boot 项目..."
mkdir -p "$STATIC_DIR"
cp -r dist-embedded/* "$STATIC_DIR/"

echo "部署完成！"
echo "静态文件已复制到: $STATIC_DIR"
echo "请在 Spring Boot 项目中执行: mvn clean package"
```

运行：
```bash
chmod +x deploy-to-springboot.sh
./deploy-to-springboot.sh
```

### Spring Boot 配置

确保 Spring Boot 项目中有以下配置：

#### application.yml

```yaml
spring:
  web:
    resources:
      static-locations: classpath:/static/
      
  # 确保静态资源可以被访问
  mvc:
    static-path-pattern: /**
```

#### 或 application.properties

```properties
spring.web.resources.static-locations=classpath:/static/
spring.mvc.static-path-pattern=/**
```

### Spring Boot Controller 配置（可选）

如果需要支持 Vue Router 的 HTML5 History 模式，添加以下配置：

```java
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    
    @GetMapping(value = {"", "/", "/login", "/admin"})
    public String forward() {
        return "forward:/index.html";
    }
}
```

### 完整部署流程

1. **修改部署脚本中的路径**：
   - 编辑 `deploy-to-springboot.ps1` (Windows) 或 `deploy-to-springboot.sh` (Linux/Mac)
   - 设置 `SPRINGBOOT_PROJECT_PATH` 为你的 Spring Boot 项目路径

2. **运行部署脚本**：
   ```bash
   # Windows
   .\deploy-to-springboot.ps1
   
   # Linux/Mac
   ./deploy-to-springboot.sh
   ```

3. **打包 Spring Boot 应用**：
   ```bash
   cd path/to/springboot/project
   mvn clean package
   ```

4. **运行应用**：
   ```bash
   java -jar target/funny-toolbox-backend-1.0.0.jar
   ```

5. **访问应用**：
   打开浏览器访问 `http://localhost:8080`

## 独立部署到 Nginx

### 构建

```bash
npm run build
```

### Nginx 完整配置示例

```nginx
server {
    listen 80;
    server_name toolbox.example.com;
    
    # 前端静态文件目录
    root /var/www/funny-toolbox-ui/dist;
    index index.html;
    
    # 开启 gzip 压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript;
    
    # 前端路由支持（Vue Router History 模式）
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 代理后端 API
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持（如果需要）
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
}
```

### 部署步骤

1. 构建项目：
   ```bash
   npm run build
   ```

2. 上传到服务器：
   ```bash
   scp -r dist/* user@server:/var/www/funny-toolbox-ui/dist/
   ```

3. 配置 Nginx 并重启：
   ```bash
   sudo nginx -t
   sudo systemctl reload nginx
   ```

## Docker 部署（独立模式）

### Dockerfile

```dockerfile
# 构建阶段
FROM node:20-alpine as build

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# 生产阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

### nginx.conf

```nginx
server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 构建和运行

```bash
# 构建镜像
docker build -t funny-toolbox-ui .

# 运行容器
docker run -d -p 80:80 --name toolbox-ui funny-toolbox-ui
```

### Docker Compose

```yaml
version: '3.8'

services:
  frontend:
    build: .
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - toolbox-network

  backend:
    image: your-backend-image:latest
    ports:
      - "8080:8080"
    networks:
      - toolbox-network

networks:
  toolbox-network:
    driver: bridge
```

运行：
```bash
docker-compose up -d
```

## 验证部署

部署完成后，访问以下 URL 验证：

- **首页**: `http://your-domain/`
- **登录页**: `http://your-domain/login`
- **管理后台**: `http://your-domain/admin`
- **API 健康检查**: `http://your-domain/api/platform/plugins`
