#!/bin/bash

# JavaFX流程节点编辑器打包脚本

echo "正在打包 CC_ETL 流程节点编辑器..."
echo "================================"

# 清理并编译
echo "1. 清理旧文件..."
mvn clean

echo ""
echo "2. 编译项目..."
mvn compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo ""
echo "3. 打包 JAR..."
mvn package

if [ $? -ne 0 ]; then
    echo "打包失败，请检查错误信息"
    exit 1
fi

echo ""
echo "================================"
echo "打包成功！"
echo "JAR文件位置: target/CC_ETL-1.0-SNAPSHOT.jar"
echo ""
echo "运行方式："
echo "  mvn javafx:run"
echo "或"
echo "  ./run.sh"
echo "================================"

