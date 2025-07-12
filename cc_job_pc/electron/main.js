import { app, BrowserWindow } from 'electron';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import http from 'http';
import fs from 'fs';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// 检查端口是否可用
function checkPort(port) {
  return new Promise((resolve) => {
    const req = http.get(`http://localhost:${port}`, res => {
      resolve(res.statusCode === 200 || res.statusCode === 404);
    });
    req.on('error', () => resolve(false));
    req.setTimeout(1000, () => {
      req.abort();
      resolve(false);
    });
  });
}

// 查找可用的Vite端口
async function findVitePort() {
  const ports = [3000, 3001, 3002, 3003, 3004, 3005];
  for (const port of ports) {
    if (await checkPort(port)) {
      console.log(`Found Vite server on port ${port}`);
      return port;
    }
  }
  console.log('No Vite server found, defaulting to port 3000');
  return 3000;
}

// 检查是否为开发环境
function isDevelopment() {
  return process.env.NODE_ENV === 'development' || !app.isPackaged;
}

async function createWindow() {
  try {
    const win = new BrowserWindow({
      width: 1400,
      height: 900,
      webPreferences: {
        nodeIntegration: false,
        contextIsolation: true,
        enableRemoteModule: false,
        webSecurity: false, // 允许加载本地文件
        allowRunningInsecureContent: true
      },
      show: false,
      titleBarStyle: 'default'
    });

    // 等待窗口准备就绪后再显示
    win.once('ready-to-show', () => {
      win.show();
      console.log('Electron window ready');
    });

    // 监听页面加载错误
    win.webContents.on('did-fail-load', (event, errorCode, errorDescription, validatedURL) => {
      console.error('Page failed to load:', errorCode, errorDescription, validatedURL);
    });

    // 监听页面加载完成
    win.webContents.on('did-finish-load', () => {
      console.log('Page finished loading');
      
      // 检查DOM内容
      win.webContents.executeJavaScript(`
        console.log('=== DOM Check ===');
        console.log('Document title:', document.title);
        console.log('Document readyState:', document.readyState);
        console.log('App element:', document.getElementById('app'));
        console.log('App element innerHTML length:', document.getElementById('app')?.innerHTML?.length || 0);
        console.log('All script tags:', document.querySelectorAll('script').length);
        console.log('All link tags:', document.querySelectorAll('link').length);
        console.log('Body children:', document.body.children.length);
        
        // 检查Vue应用
        if (window.__VUE_APP__) {
          console.log('Vue app found:', window.__VUE_APP__);
        } else {
          console.log('Vue app not found in window');
        }
        
        // 检查是否有JavaScript错误
        window.addEventListener('error', (e) => {
          console.error('JavaScript error:', e.error);
        });
        
        // 检查Vue挂载
        setTimeout(() => {
          const appElement = document.getElementById('app');
          if (appElement && appElement.children.length === 0) {
            console.log('App element is empty, Vue might not be mounted');
          } else {
            console.log('App element has content:', appElement?.children?.length || 0);
          }
        }, 1000);
      `);
    });

    // 监听控制台消息
    win.webContents.on('console-message', (event, level, message, line, sourceId) => {
      console.log(`[${level}] ${message} (${sourceId}:${line})`);
    });

    if (isDevelopment()) {
      // 开发环境：加载Vite开发服务器
      const port = await findVitePort();
      const url = `http://localhost:${port}`;
      console.log(`Loading development URL: ${url}`);
      await win.loadURL(url);
      
      // 开发环境下打开开发者工具
      win.webContents.openDevTools();
    } else {
      // 生产环境：加载本地构建文件
      const indexPath = join(__dirname, '../dist/index.html');
      console.log(`Loading production file: ${indexPath}`);
      
      if (fs.existsSync(indexPath)) {
        console.log('Index file exists, loading...');
        await win.loadFile(indexPath);
        // 生产环境不再自动打开开发者工具
      } else {
        console.error('Production build not found. Please run "npm run build" first.');
        console.error('Expected path:', indexPath);
        console.error('Current directory:', __dirname);
        
        // 列出当前目录内容
        try {
          const files = fs.readdirSync(__dirname);
          console.log('Files in electron directory:', files);
          
          const parentFiles = fs.readdirSync(join(__dirname, '..'));
          console.log('Files in parent directory:', parentFiles);
          
          if (fs.existsSync(join(__dirname, '..', 'dist'))) {
            const distFiles = fs.readdirSync(join(__dirname, '..', 'dist'));
            console.log('Files in dist directory:', distFiles);
          }
        } catch (err) {
          console.error('Error listing directories:', err);
        }
        
        app.quit();
        return;
      }
    }

  } catch (error) {
    console.error('Failed to create window:', error);
    app.quit();
  }
}

// 应用准备就绪时创建窗口
app.whenReady().then(createWindow);

// 当所有窗口关闭时退出应用
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// macOS下激活应用时重新创建窗口
app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

// 处理未捕获的异常
process.on('uncaughtException', (error) => {
  console.error('Uncaught Exception:', error);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('Unhandled Rejection at:', promise, 'reason:', reason);
}); 