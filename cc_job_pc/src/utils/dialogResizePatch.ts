// 定义类型
interface ResizeOptions {
    minWidth?: number | string;
    minHeight?: number | string;
    maxWidth?: number | string;
    maxHeight?: number | string;
    width?: number | string;
    height?: number | string;
    aspectRatio?: number;
    lockAspectRatio?: boolean;
    responsive?: boolean;
    disabled?: boolean;
}

interface ResizeState {
    resizing: boolean;
    dir: string;
    startX: number;
    startY: number;
    startWidth: number;
    startHeight: number;
    startTop: number;
    startLeft: number;
    options: ResizeOptions;
}

// 全局配置存储
const dialogConfigs = new Map<HTMLElement, ResizeOptions>();

// 全局函数：设置对话框配置
export function setDialogResizeConfig(dialog: HTMLElement, options: ResizeOptions) {
    console.log("设置对话框配置:", dialog, options);
    dialogConfigs.set(dialog, options);

    // 如果对话框已经存在，立即应用配置
    if (dialog.classList.contains("el-dialog")) {
        // 移除现有的手柄
        const existingHandles = dialog.querySelectorAll(".resize-handle");
        existingHandles.forEach((handle) => handle.remove());

        // 重置patch状态
        delete (dialog as any).__resizePatched;

        // 重新应用配置
        patchDialog(dialog);
    }
}

// 强制重新应用配置
export function forceReapplyConfig(dialog: HTMLElement) {
    if (dialogConfigs.has(dialog)) {
        const options = dialogConfigs.get(dialog)!;
        console.log("强制重新应用配置:", options);

        // 移除现有的手柄
        const existingHandles = dialog.querySelectorAll(".resize-handle");
        existingHandles.forEach((handle) => handle.remove());

        // 重置patch状态
        delete (dialog as any).__resizePatched;

        // 重新应用配置
        patchDialog(dialog);
    }
}

// 将值转换为像素
function parseValue(
    value: number | string | undefined,
    containerSize: number,
    defaultValue: number
): number {
    if (value === undefined) return defaultValue;
    if (typeof value === "number") return value;
    if (typeof value === "string") {
        if (value.endsWith("%")) {
            return (parseFloat(value) / 100) * containerSize;
        }
        if (value.endsWith("px")) {
            return parseFloat(value);
        }
        if (value.endsWith("vw")) {
            return (parseFloat(value) / 100) * window.innerWidth;
        }
        if (value.endsWith("vh")) {
            return (parseFloat(value) / 100) * window.innerHeight;
        }
        return parseFloat(value) || defaultValue;
    }
    return defaultValue;
}

// 获取容器尺寸
function getContainerSize(): { width: number; height: number } {
    return {
        width: window.innerWidth,
        height: window.innerHeight,
    };
}

// 从对话框元素获取配置选项
function getDialogOptions(dialog: HTMLElement): ResizeOptions {
    const containerSize = getContainerSize();

    // 首先检查全局配置
    if (dialogConfigs.has(dialog)) {
        const config = dialogConfigs.get(dialog)!;
        console.log("使用全局配置:", config);
        return config;
    }

    // 尝试从 data 属性获取配置
    const dataOptions = dialog.getAttribute("data-resize-options");
    if (dataOptions) {
        try {
            const config = JSON.parse(dataOptions);
            console.log("使用data属性配置:", config);
            return config;
        } catch (e) {
            console.warn("解析对话框配置失败:", e);
        }
    }

    // 默认配置
    const defaultConfig = {
        minWidth: 400,
        minHeight: 300,
        maxWidth: containerSize.width * 0.9,
        maxHeight: containerSize.height * 0.9,
        responsive: true,
        disabled: false,
    };
    console.log("使用默认配置:", defaultConfig);
    return defaultConfig;
}

// 设置对话框的最小最大尺寸
function setDialogMinMax(dialog: HTMLElement, options: ResizeOptions) {
    const containerSize = getContainerSize();

    const minWidth = parseValue(options.minWidth, containerSize.width, 400);
    const maxWidth = parseValue(
        options.maxWidth,
        containerSize.width,
        containerSize.width * 0.9
    );
    const minHeight = parseValue(options.minHeight, containerSize.height, 300);
    const maxHeight = parseValue(
        options.maxHeight,
        containerSize.height,
        containerSize.height * 0.9
    );

    console.log("设置对话框尺寸限制:", { minWidth, maxWidth, minHeight, maxHeight });

    dialog.style.minWidth = minWidth + "px";
    dialog.style.maxWidth = maxWidth + "px";
    dialog.style.minHeight = minHeight + "px";
    dialog.style.maxHeight = maxHeight + "px";
}

// 强制限制对话框尺寸
function enforceDialogSize(dialog: HTMLElement, options: ResizeOptions) {
    const rect = dialog.getBoundingClientRect();
    const containerSize = getContainerSize();

    const minWidth = parseValue(options.minWidth, containerSize.width, 400);
    const maxWidth = parseValue(
        options.maxWidth,
        containerSize.width,
        containerSize.width * 0.9
    );
    const minHeight = parseValue(options.minHeight, containerSize.height, 300);
    const maxHeight = parseValue(
        options.maxHeight,
        containerSize.height,
        containerSize.height * 0.9
    );

    let newWidth = rect.width;
    let newHeight = rect.height;

    // 应用尺寸限制
    if (maxWidth && newWidth > maxWidth) newWidth = maxWidth;
    if (minWidth && newWidth < minWidth) newWidth = minWidth;
    if (maxHeight && newHeight > maxHeight) newHeight = maxHeight;
    if (minHeight && newHeight < minHeight) newHeight = minHeight;

    // 应用宽高比限制
    if (options.aspectRatio && options.lockAspectRatio) {
        const ratio = options.aspectRatio;
        if (newWidth / newHeight > ratio) {
            newHeight = newWidth / ratio;
        } else {
            newWidth = newHeight * ratio;
        }
    }

    console.log("强制限制对话框尺寸:", { newWidth, newHeight });
    dialog.style.width = newWidth + "px";
    dialog.style.height = newHeight + "px";
}

const dirs = [
    "top",
    "right",
    "bottom",
    "left",
    "top-left",
    "top-right",
    "bottom-left",
    "bottom-right",
];

function createHandle(dir: string, dialog: HTMLElement) {
    const handle = document.createElement("div");
    handle.className = `resize-handle resize-handle-${dir}`;
    handle.style.cssText = `
      position: absolute;
      background: transparent;
      z-index: 1000;
    `;

    // 根据方向设置手柄样式
    switch (dir) {
        case "top":
            handle.style.top = "0";
            handle.style.left = "50%";
            handle.style.transform = "translateX(-50%)";
            handle.style.width = "30px";
            handle.style.height = "6px";
            handle.style.cursor = "ns-resize";
            break;
        case "right":
            handle.style.top = "50%";
            handle.style.right = "0";
            handle.style.transform = "translateY(-50%)";
            handle.style.width = "6px";
            handle.style.height = "30px";
            handle.style.cursor = "ew-resize";
            break;
        case "bottom":
            handle.style.bottom = "0";
            handle.style.left = "50%";
            handle.style.transform = "translateX(-50%)";
            handle.style.width = "30px";
            handle.style.height = "6px";
            handle.style.cursor = "ns-resize";
            break;
        case "left":
            handle.style.top = "50%";
            handle.style.left = "0";
            handle.style.transform = "translateY(-50%)";
            handle.style.width = "6px";
            handle.style.height = "30px";
            handle.style.cursor = "ew-resize";
            break;
        case "top-left":
            handle.style.top = "0";
            handle.style.left = "0";
            handle.style.width = "10px";
            handle.style.height = "10px";
            handle.style.cursor = "nw-resize";
            break;
        case "top-right":
            handle.style.top = "0";
            handle.style.right = "0";
            handle.style.width = "10px";
            handle.style.height = "10px";
            handle.style.cursor = "ne-resize";
            break;
        case "bottom-left":
            handle.style.bottom = "0";
            handle.style.left = "0";
            handle.style.width = "10px";
            handle.style.height = "10px";
            handle.style.cursor = "sw-resize";
            break;
        case "bottom-right":
            handle.style.bottom = "0";
            handle.style.right = "0";
            handle.style.width = "10px";
            handle.style.height = "10px";
            handle.style.cursor = "se-resize";
            break;
    }

    handle.addEventListener("mousedown", (e) => handleMousedown(e, dir, dialog));
    return handle;
}

function handleMousedown(e: MouseEvent, dir: string, dialog: HTMLElement) {
    e.stopPropagation();
    e.preventDefault();

    const options = getDialogOptions(dialog);
    if (options.disabled) return;

    const state: ResizeState = {
        resizing: true,
        dir: dir,
        startX: e.clientX,
        startY: e.clientY,
        startWidth: 0,
        startHeight: 0,
        startTop: 0,
        startLeft: 0,
        options: options,
    };

    const rect = dialog.getBoundingClientRect();
    state.startWidth = rect.width;
    state.startHeight = rect.height;
    state.startTop = rect.top;
    state.startLeft = rect.left;

    function handleMousemove(e: MouseEvent) {
        if (!state.resizing) return;
        e.preventDefault();

        let dx = e.clientX - state.startX;
        let dy = e.clientY - state.startY;
        let newWidth = state.startWidth;
        let newHeight = state.startHeight;
        let newTop = state.startTop;
        let newLeft = state.startLeft;

        const containerSize = getContainerSize();
        const minWidth = parseValue(state.options.minWidth, containerSize.width, 400);
        const maxWidth = parseValue(
            state.options.maxWidth,
            containerSize.width,
            containerSize.width * 0.9
        );
        const minHeight = parseValue(state.options.minHeight, containerSize.height, 300);
        const maxHeight = parseValue(
            state.options.maxHeight,
            containerSize.height,
            containerSize.height * 0.9
        );

        // 根据拖拽方向调整尺寸
        if (state.dir.includes("right")) {
            newWidth = Math.max(minWidth, Math.min(maxWidth, state.startWidth + dx));
        }
        if (state.dir.includes("left")) {
            newWidth = Math.max(minWidth, Math.min(maxWidth, state.startWidth - dx));
            newLeft = state.startLeft + dx;
        }
        if (state.dir.includes("bottom")) {
            newHeight = Math.max(minHeight, Math.min(maxHeight, state.startHeight + dy));
        }
        if (state.dir.includes("top")) {
            newHeight = Math.max(minHeight, Math.min(maxHeight, state.startHeight - dy));
            newTop = state.startTop + dy;
        }

        // 应用宽高比限制
        if (state.options.aspectRatio && state.options.lockAspectRatio) {
            const ratio = state.options.aspectRatio;
            if (state.dir.includes("right") || state.dir.includes("left")) {
                newHeight = newWidth / ratio;
            } else if (state.dir.includes("top") || state.dir.includes("bottom")) {
                newWidth = newHeight * ratio;
            }
        }

        // 强制限制尺寸
        newWidth = Math.max(minWidth, Math.min(maxWidth, newWidth));
        newHeight = Math.max(minHeight, Math.min(maxHeight, newHeight));

        // 应用新尺寸
        dialog.style.width = newWidth + "px";
        dialog.style.height = newHeight + "px";
        dialog.style.top = newTop + "px";
        dialog.style.left = newLeft + "px";
        dialog.style.margin = "0";
    }

    function handleMouseup() {
        state.resizing = false;
        document.removeEventListener("mousemove", handleMousemove);
        document.removeEventListener("mouseup", handleMouseup);
    }

    document.addEventListener("mousemove", handleMousemove);
    document.addEventListener("mouseup", handleMouseup);
}

function patchDialog(dialog: Element) {
    if (!(dialog instanceof HTMLElement)) return;

    console.log("尝试patch dialog:", dialog);
    // 防止重复插入
    if ((dialog as any).__resizePatched) {
        console.log("对话框已经patch过，跳过");
        return;
    }

    const options = getDialogOptions(dialog);
    if (options.disabled) {
        console.log("对话框禁用调整大小，跳过");
        return;
    }

    // 设置初始尺寸
    const containerSize = getContainerSize();
    if (options.width) {
        const width = parseValue(options.width, containerSize.width, 400);
        console.log("设置初始宽度:", width);
        dialog.style.width = width + "px";
    }
    if (options.height) {
        const height = parseValue(options.height, containerSize.height, 300);
        console.log("设置初始高度:", height);
        dialog.style.height = height + "px";
    }

    setDialogMinMax(dialog, options);
    enforceDialogSize(dialog, options);

    dirs.forEach((dir) => {
        const handle = createHandle(dir, dialog);
        dialog.appendChild(handle);
    });

    (dialog as any).__resizePatched = true;
    console.log("对话框patch完成");
}

function observeDialogs() {
    console.log("开始观察对话框");
    // 初始patch
    document.querySelectorAll(".el-dialog").forEach(patchDialog);
    // 监听后续弹窗
    const observer = new MutationObserver(() => {
        document.querySelectorAll(".el-dialog").forEach(patchDialog);
    });
    observer.observe(document.body, { childList: true, subtree: true });
}

if (typeof window !== "undefined") {
    window.addEventListener("DOMContentLoaded", observeDialogs);
    // 兼容SPA路由切换
    setTimeout(observeDialogs, 1000);
}
