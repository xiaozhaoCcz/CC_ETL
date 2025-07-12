// 全局 Element Plus 弹窗缩放 patch
console.log("全局弹窗缩放patch已加载");
// 监听 body 下所有 .el-dialog，为每个弹窗插入缩放点

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

function createHandle(dir, dialog) {
    const handle = document.createElement("div");
    handle.className = `resize-handle resize-handle-${dir}`;
    handle.addEventListener("mousedown", (e) => handleMousedown(e, dir, dialog));
    return handle;
}

let resizing = false;
let resizeDir = "";
let startX = 0,
    startY = 0,
    startWidth = 0,
    startHeight = 0,
    startTop = 0,
    startLeft = 0;

function handleMousedown(e, dir, dialog) {
    e.stopPropagation();
    resizing = true;
    resizeDir = dir;
    const rect = dialog.getBoundingClientRect();
    startX = e.clientX;
    startY = e.clientY;
    startWidth = rect.width;
    startHeight = rect.height;
    startTop = rect.top;
    startLeft = rect.left;

    function handleMousemove(e) {
        if (!resizing) return;
        let dx = e.clientX - startX;
        let dy = e.clientY - startY;
        let newWidth = startWidth,
            newHeight = startHeight;
        let newTop = startTop,
            newLeft = startLeft;
        const maxWidth = window.innerWidth * 0.9;
        const maxHeight = window.innerHeight * 0.9;
        const minWidth = 400;
        const minHeight = 200;
        if (resizeDir.includes("right"))
            newWidth = Math.max(minWidth, Math.min(maxWidth, startWidth + dx));
        if (resizeDir.includes("left")) {
            newWidth = Math.max(minWidth, Math.min(maxWidth, startWidth - dx));
            newLeft = startLeft + dx;
        }
        if (resizeDir.includes("bottom"))
            newHeight = Math.max(minHeight, Math.min(maxHeight, startHeight + dy));
        if (resizeDir.includes("top")) {
            newHeight = Math.max(minHeight, Math.min(maxHeight, startHeight - dy));
            newTop = startTop + dy;
        }
        dialog.style.width = newWidth + "px";
        dialog.style.height = newHeight + "px";
        dialog.style.top = newTop + "px";
        dialog.style.left = newLeft + "px";
        dialog.style.margin = "0";
    }
    function handleMouseup() {
        resizing = false;
        document.removeEventListener("mousemove", handleMousemove);
        document.removeEventListener("mouseup", handleMouseup);
    }
    document.addEventListener("mousemove", handleMousemove);
    document.addEventListener("mouseup", handleMouseup);
}

function patchDialog(dialog) {
    console.log("尝试patch dialog:", dialog);
    // 防止重复插入
    if (dialog.__resizePatched) return;
    dirs.forEach((dir) => {
        const handle = createHandle(dir, dialog);
        dialog.appendChild(handle);
    });
    dialog.__resizePatched = true;
}

function observeDialogs() {
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
