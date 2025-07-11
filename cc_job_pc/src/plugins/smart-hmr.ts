/**
 * 智能热更新插件
 * 只在真正的代码变更时才触发热更新，忽略注释和空白字符的变更
 */
import { Plugin } from "vite";
import { readFileSync } from "fs";
import { createHash } from "crypto";

interface FileCache {
  [filePath: string]: {
    codeHash: string;
    lastModified: number;
  };
}

export function smartHmrPlugin(): Plugin {
  const fileCache: FileCache = {};

  // 移除注释和多余空白的函数
  function normalizeCode(code: string, ext: string): string {
    if (ext === ".vue") {
      // 对于 Vue 文件，移除模板和样式中的注释，保留脚本注释用于逻辑判断
      return code
        .replace(/<!--[\s\S]*?-->/g, "") // 移除 HTML 注释
        .replace(/\/\*[\s\S]*?\*\//g, "") // 移除 CSS 多行注释
        .replace(/\/\/.*$/gm, "") // 移除单行注释（谨慎使用）
        .replace(/\s+/g, " ") // 标准化空白字符
        .trim();
    }

    if (ext === ".ts" || ext === ".js") {
      // 对于 JS/TS 文件，保留必要的注释，只移除明显的文档注释
      return code
        .replace(/\/\*\*[\s\S]*?\*\//g, "") // 移除文档注释
        .replace(/^\s*\/\/.*$/gm, "") // 移除单行注释
        .replace(/\s+/g, " ")
        .trim();
    }

    return code;
  }

  // 生成代码哈希
  function generateCodeHash(normalizedCode: string): string {
    return createHash("md5").update(normalizedCode).digest("hex");
  }

  return {
    name: "smart-hmr",
    configureServer(server) {
      // 拦截文件变更事件
      server.ws.on("file-changed", (file) => {
        try {
          const ext = file.slice(file.lastIndexOf("."));

          // 只处理我们关心的文件类型
          if (![".vue", ".ts", ".js", ".tsx", ".jsx"].includes(ext)) {
            return;
          }

          const content = readFileSync(file, "utf-8");
          const normalizedCode = normalizeCode(content, ext);
          const currentHash = generateCodeHash(normalizedCode);

          const cached = fileCache[file];

          if (cached && cached.codeHash === currentHash) {
            // 代码实质内容没有变化，阻止热更新
            console.log(`[Smart HMR] 忽略非代码变更: ${file}`);
            return false;
          }

          // 更新缓存
          fileCache[file] = {
            codeHash: currentHash,
            lastModified: Date.now(),
          };

          console.log(`[Smart HMR] 检测到代码变更: ${file}`);
        } catch (error) {
          console.warn(`[Smart HMR] 处理文件变更时出错: ${file}`, error);
        }
      });
    },
  };
}

export default smartHmrPlugin;
