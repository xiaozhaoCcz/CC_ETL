#!/bin/bash

# JavaFX流程节点编辑器启动脚本

echo "正在启动 CC_ETL 流程节点编辑器..."
echo "================================"

# 检查Java版本
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')

if [ "$JAVA_VERSION" -lt 11 ]; then




    echo "当前 Java 版本: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

# 编译项目
echo "正在编译项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo ""
echo "编译成功！正在启动应用..."
echo "如果程序无法启动，请确保您的系统支持图形界面"
echo "================================"
echo ""

# 运行应用
mvn javafx:run

# 如果失败，尝试使用java直接运行
if [ $? -ne 0 ]; then
    echo ""
    echo "Maven运行失败，尝试直接运行..."
    java --module-path $HOME/.m2/repository/org/openjfx/javafx-controls/17.0.2/javafx-controls-17.0.2-mac.jar:$HOME/.m2/repository/org/openjfx/javafx-graphics/17.0.2/javafx-graphics-17.0.2-mac.jar:$HOME/.m2/repository/org/openjfx/javafx-base/17.0.2/javafx-base-17.0.2-mac.jar \
         --add-modules javafx.controls,javafx.fxml \
         -cp target/classes \
         com.example.nodefx.NodeFxApplication
fi

