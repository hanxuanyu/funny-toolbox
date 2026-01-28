#!/bin/bash

# 配置参数 - 请修改为你的 Spring Boot 项目路径
SPRINGBOOT_PROJECT_PATH="/path/to/your/springboot/project"
STATIC_DIR="$SPRINGBOOT_PROJECT_PATH/src/main/resources/static"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
CYAN='\033[0;36m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

echo -e "${CYAN}================================================${NC}"
echo -e "${CYAN}  Funny Toolbox UI - 嵌入式部署脚本${NC}"
echo -e "${CYAN}================================================${NC}"
echo ""

# 检查 Spring Boot 项目路径是否存在
if [ ! -d "$SPRINGBOOT_PROJECT_PATH" ]; then
    echo -e "${RED}错误: Spring Boot 项目路径不存在: $SPRINGBOOT_PROJECT_PATH${NC}"
    echo -e "${YELLOW}请在脚本中修改 SPRINGBOOT_PROJECT_PATH 变量${NC}"
    exit 1
fi

echo -e "${GREEN}[1/4] 开始构建前端项目（嵌入式模式）...${NC}"
npm run build:embedded

if [ $? -ne 0 ]; then
    echo -e "${RED}错误: 构建失败！${NC}"
    exit 1
fi

echo -e "${YELLOW}[2/4] 清理旧的静态文件...${NC}"
if [ -d "$STATIC_DIR" ]; then
    rm -rf "$STATIC_DIR"/*
    echo -e "${GRAY}  已清理: $STATIC_DIR${NC}"
else
    mkdir -p "$STATIC_DIR"
    echo -e "${GRAY}  已创建目录: $STATIC_DIR${NC}"
fi

echo -e "${YELLOW}[3/4] 复制新的静态文件到 Spring Boot 项目...${NC}"
cp -r dist-embedded/* "$STATIC_DIR/"
echo -e "${GRAY}  已复制到: $STATIC_DIR${NC}"

echo -e "${YELLOW}[4/4] 统计文件信息...${NC}"
FILE_COUNT=$(find "$STATIC_DIR" -type f | wc -l)
TOTAL_SIZE=$(du -sh "$STATIC_DIR" | cut -f1)
echo -e "${GRAY}  文件数量: $FILE_COUNT${NC}"
echo -e "${GRAY}  总大小: $TOTAL_SIZE${NC}"

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  部署完成！${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "${CYAN}后续步骤:${NC}"
echo -e "${NC}  1. 进入 Spring Boot 项目目录:${NC}"
echo -e "${GRAY}     cd $SPRINGBOOT_PROJECT_PATH${NC}"
echo ""
echo -e "${NC}  2. 打包 Spring Boot 应用:${NC}"
echo -e "${GRAY}     mvn clean package${NC}"
echo ""
echo -e "${NC}  3. 运行应用:${NC}"
echo -e "${GRAY}     java -jar target/your-app.jar${NC}"
echo ""
echo -e "${NC}  4. 访问应用:${NC}"
echo -e "${GRAY}     http://localhost:8080${NC}"
echo ""
