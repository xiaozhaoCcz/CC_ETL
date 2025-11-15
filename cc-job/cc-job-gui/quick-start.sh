#!/bin/bash

# NodeFx 快速启动脚本

echo "=================================="
echo "NodeFx 快速启动工具"
echo "=================================="
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查后端服务
echo "1. 检查后端服务..."
if curl -s http://localhost:8080 > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 后端服务运行正常 (http://localhost:8080)${NC}"
    BACKEND_STATUS="running"
else
    echo -e "${YELLOW}⚠ 后端服务未运行${NC}"
    echo "  提示: 请先启动后端服务"
    echo "  命令: cd ../cc-job-admin && mvn spring-boot:run"
    BACKEND_STATUS="stopped"
fi
echo ""

# 检查 Maven
echo "2. 检查 Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version | head -n 1)
    echo -e "${GREEN}✓ Maven 已安装: $MVN_VERSION${NC}"
else
    echo -e "${RED}✗ 未找到 Maven，请先安装 Maven${NC}"
    exit 1
fi
echo ""

# 检查 Java
echo "3. 检查 Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ Java 已安装: $JAVA_VERSION${NC}"
else
    echo -e "${RED}✗ 未找到 Java，请先安装 Java 17+${NC}"
    exit 1
fi
echo ""

# 提示用户选择
echo "=================================="
echo "请选择操作:"
echo "=================================="
echo "1. 编译项目"
echo "2. 运行应用 (需要先编译)"
echo "3. 编译并运行"
echo "4. 清理并重新编译"
echo "5. 查看帮助"
echo "0. 退出"
echo ""
read -p "请输入选项 [0-5]: " choice

case $choice in
    1)
        echo ""
        echo "开始编译项目..."
        mvn clean compile
        ;;
    2)
        echo ""
        echo "启动应用..."
        if [ "$BACKEND_STATUS" = "stopped" ]; then
            echo -e "${YELLOW}警告: 后端服务未运行，将使用示例数据${NC}"
            read -p "是否继续? [y/N]: " confirm
            if [ "$confirm" != "y" ] && [ "$confirm" != "Y" ]; then
                echo "已取消"
                exit 0
            fi
        fi
        mvn javafx:run
        ;;
    3)
        echo ""
        echo "编译并运行..."
        if [ "$BACKEND_STATUS" = "stopped" ]; then
            echo -e "${YELLOW}警告: 后端服务未运行，将使用示例数据${NC}"
        fi
        mvn clean compile && mvn javafx:run
        ;;
    4)
        echo ""
        echo "清理并重新编译..."
        mvn clean install -U
        ;;
    5)
        echo ""
        echo "=================================="
        echo "NodeFx 帮助文档"
        echo "=================================="
        echo ""
        echo "目录结构:"
        echo "  src/main/java/          - Java 源代码"
        echo "  src/main/resources/     - 资源文件和配置"
        echo "  target/                 - 编译输出目录"
        echo ""
        echo "配置文件:"
        echo "  src/main/resources/application.properties"
        echo "  配置项:"
        echo "    api.base.url          - 后端服务地址"
        echo "    api.connect.timeout   - 连接超时时间(秒)"
        echo "    api.read.timeout      - 读取超时时间(秒)"
        echo ""
        echo "常用命令:"
        echo "  mvn clean compile       - 编译项目"
        echo "  mvn javafx:run          - 运行应用"
        echo "  mvn clean package       - 打包应用"
        echo "  mvn clean install -U    - 更新依赖并编译"
        echo ""
        echo "相关文档:"
        echo "  树形数据加载说明.md     - 功能使用说明"
        echo "  测试指南.md            - 测试步骤"
        echo "  IMPLEMENTATION_SUMMARY.md - 实现总结"
        echo ""
        ;;
    0)
        echo "再见！"
        exit 0
        ;;
    *)
        echo -e "${RED}无效的选项${NC}"
        exit 1
        ;;
esac

echo ""
echo "=================================="
echo "操作完成"
echo "=================================="

