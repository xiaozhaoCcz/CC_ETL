import type { DirectiveBinding } from "vue";

interface ResizeOptions {
  minWidth?: number;
  minHeight?: number;
  maxWidth?: number;
  maxHeight?: number;
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
  handles: HTMLElement[];
  options: ResizeOptions;
}

function setDialogMinMax(dialog: HTMLElement, options: ResizeOptions) {
  if (options.minWidth) dialog.style.minWidth = options.minWidth + "px";
  if (options.maxWidth) dialog.style.maxWidth = options.maxWidth + "px";
  if (options.minHeight) dialog.style.minHeight = options.minHeight + "px";
  if (options.maxHeight) dialog.style.maxHeight = options.maxHeight + "px";
}

function enforceDialogSize(dialog: HTMLElement, options: ResizeOptions) {
  const rect = dialog.getBoundingClientRect();
  if (options.maxWidth && rect.width > options.maxWidth)
    dialog.style.width = options.maxWidth + "px";
  if (options.minWidth && rect.width < options.minWidth)
    dialog.style.width = options.minWidth + "px";
  if (options.maxHeight && rect.height > options.maxHeight)
    dialog.style.height = options.maxHeight + "px";
  if (options.minHeight && rect.height < options.minHeight)
    dialog.style.height = options.minHeight + "px";
}

function createHandle(dir: string, onMousedown: (e: MouseEvent, dir: string) => void) {
  const handle = document.createElement("div");
  handle.className = `resize-handle resize-handle-${dir}`;
  handle.addEventListener("mousedown", (e) => onMousedown(e, dir));
  return handle;
}

const resizeDialog = {
  mounted(el: HTMLElement, binding: DirectiveBinding<ResizeOptions>) {
    const dialog = el;
    if (!dialog) return;
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
    const options: ResizeOptions = {
      minWidth: binding.value?.minWidth ?? 400,
      minHeight: binding.value?.minHeight ?? 800,
      maxWidth: binding.value?.maxWidth ?? window.innerWidth * 0.9,
      maxHeight: binding.value?.maxHeight ?? window.innerHeight * 0.9,
    };
    setDialogMinMax(dialog, options);
    enforceDialogSize(dialog, options);
    const state: ResizeState = {
      resizing: false,
      dir: "",
      startX: 0,
      startY: 0,
      startWidth: 0,
      startHeight: 0,
      startTop: 0,
      startLeft: 0,
      handles: [],
      options,
    };

    function handleMousedown(e: MouseEvent, dir: string) {
      e.stopPropagation();
      state.resizing = true;
      state.dir = dir;
      const rect = dialog.getBoundingClientRect();
      state.startX = e.clientX;
      state.startY = e.clientY;
      state.startWidth = rect.width;
      state.startHeight = rect.height;
      state.startTop = rect.top;
      state.startLeft = rect.left;

      document.addEventListener("mousemove", handleMousemove);
      document.addEventListener("mouseup", handleMouseup);
    }

    function handleMousemove(e: MouseEvent) {
      if (!state.resizing) return;
      let dx = e.clientX - state.startX;
      let dy = e.clientY - state.startY;
      let newWidth = state.startWidth,
        newHeight = state.startHeight;
      let newTop = state.startTop,
        newLeft = state.startLeft;
      const { minWidth, minHeight, maxWidth, maxHeight } = state.options;
      setDialogMinMax(dialog, state.options);
      if (state.dir.includes("right"))
        newWidth = Math.max(minWidth!, Math.min(maxWidth!, state.startWidth + dx));
      if (state.dir.includes("left")) {
        newWidth = Math.max(minWidth!, Math.min(maxWidth!, state.startWidth - dx));
        newLeft = state.startLeft + dx;
      }
      if (state.dir.includes("bottom"))
        newHeight = Math.max(minHeight!, Math.min(maxHeight!, state.startHeight + dy));
      if (state.dir.includes("top")) {
        newHeight = Math.max(minHeight!, Math.min(maxHeight!, state.startHeight - dy));
        newTop = state.startTop + dy;
      }
      // 强制限制
      if (maxWidth && newWidth > maxWidth) newWidth = maxWidth;
      if (minWidth && newWidth < minWidth) newWidth = minWidth;
      if (maxHeight && newHeight > maxHeight) newHeight = maxHeight;
      if (minHeight && newHeight < minHeight) newHeight = minHeight;
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

    // 创建并插入8个拖拽点
    dirs.forEach((dir) => {
      const handle = createHandle(dir, handleMousedown);
      dialog.appendChild(handle);
      state.handles.push(handle);
    });

    (el as any).__resizeDialogState = state;
  },
  updated(el: HTMLElement, binding: DirectiveBinding<ResizeOptions>) {
    // 响应参数变化
    const state: ResizeState | undefined = (el as any).__resizeDialogState;
    if (state && binding.value) {
      state.options = {
        minWidth: binding.value.minWidth ?? 400,
        minHeight: binding.value.minHeight ?? 200,
        maxWidth: binding.value.maxWidth ?? window.innerWidth * 0.9,
        maxHeight: binding.value.maxHeight ?? window.innerHeight * 0.9,
      };
      setDialogMinMax(el, state.options);
      enforceDialogSize(el, state.options);
    }
  },
  unmounted(el: HTMLElement) {
    const state: ResizeState | undefined = (el as any).__resizeDialogState;
    if (state) {
      state.handles.forEach((h) => h.remove());
    }
  },
};

export default resizeDialog;
