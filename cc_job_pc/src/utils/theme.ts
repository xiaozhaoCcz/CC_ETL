// 辅助函数：将十六进制颜色转换为 RGB
function hexToRgb(hex: string): [number, number, number] {
  const bigint = parseInt(hex.slice(1), 16);
  return [(bigint >> 16) & 255, (bigint >> 8) & 255, bigint & 255];
}

// 辅助函数：将 RGB 转换为十六进制颜色
function rgbToHex(r: number, g: number, b: number): string {
  return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
}

// 辅助函数：调整颜色亮度
function adjustBrightness(hex: string, factor: number): string {
  const rgb = hexToRgb(hex);
  const newRgb = rgb.map((val) =>
    Math.max(0, Math.min(255, Math.round(val + (255 - val) * factor)))
  ) as [number, number, number];
  return rgbToHex(...newRgb);
}

export function generateThemeColors(primary: string) {
  const colors: Record<string, string> = {
    primary
  };

  // 生成浅色变体
  for (let i = 1; i <= 9; i++) {
    const factor = i * 0.1;
    colors[`primary-light-${i}`] = adjustBrightness(primary, factor);
  }

  // 生成深色变体
  colors["primary-dark-2"] = adjustBrightness(primary, -0.2);

  return colors;
}

export function applyTheme(colors: Record<string, string>) {
  const el = document.documentElement;

  Object.entries(colors).forEach(([key, value]) => {
    el.style.setProperty(`--el-color-${key}`, value);
  });
}

export function toggleDarkMode(isDark: boolean) {
  if (isDark) {
    document.documentElement.classList.add("dark");
  } else {
    document.documentElement.classList.remove("dark");
  }
}


export function getThemeCode(type: string) {
  if (type === "GLUE_GROOVY") {
    return `package com.xxl.job.service.handler;

            import com.xxl.job.core.context.XxlJobHelper;
            import com.xxl.job.core.handler.IJobHandler;
            
            public class DemoGlueJobHandler extends IJobHandler {
            
            \t@Override
            \tpublic void execute() throws Exception {
            \t\tXxlJobHelper.log("XXL-JOB, Hello World.");
            \t}
            }
            `;
  } else if (type === "GLUE_SHELL") {

    return `#!/bin/bash
echo "xxl-job: hello shell"

echo "脚本位置：$0"
echo "任务参数：$1"
echo "分片序号 = $2"
echo "分片总数 = $3"

echo "Good bye!"
exit 0
`;

  } else if (type === "GLUE_PYTHON") {
    return `#!/usr/bin/python
# -*- coding: UTF-8 -*-
import time
import sys

print "xxl-job: hello python"

print "脚本位置：", sys.argv[0]
print "任务参数：", sys.argv[1]
print "分片序号：", sys.argv[2]
print "分片总数：", sys.argv[3]

print "Good bye!"
exit(0)
`;

  } else if (type === "GLUE_NODEJS") {
    return `#!/usr/bin/env node
console.log("xxl-job: hello nodejs")

var arguments = process.argv

console.log("脚本位置: " + arguments[1])
console.log("任务参数: " + arguments[2])
console.log("分片序号: " + arguments[3])
console.log("分片总数: " + arguments[4])

console.log("Good bye!")
process.exit(0)
`;

  } else if (type === "GLUE_PHP") {
return `<?php

    echo "xxl-job: hello php  \\n";

    echo "脚本位置：$argv[0]  \\n";
    echo "任务参数：$argv[1]  \\n";
    echo "分片序号 = $argv[2]  \\n";
    echo "分片总数 = $argv[3]  \\n";

    echo "Good bye!  \\n";
    exit(0);

?>
`
  } else if (type === "GLUE_POWERSHELL") {
      return  `Write-Host "xxl-job: hello powershell"

Write-Host "脚本位置: " $MyInvocation.MyCommand.Definition
Write-Host "任务参数: "
\tif ($args.Count -gt 2) { $args[0..($args.Count-3)] }
Write-Host "分片序号: " $args[$args.Count-2]
Write-Host "分片总数: " $args[$args.Count-1]

Write-Host "Good bye!"
exit 0
`;
  }

  return ''
}
