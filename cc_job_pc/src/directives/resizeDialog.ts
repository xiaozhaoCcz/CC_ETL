import type { DirectiveBinding } from "vue";
import type { ResizeDialogOptions } from "@/types/resizeDialog";
import { setDialogResizeConfig, forceReapplyConfig } from "@/utils/dialogResizePatch";

// 将值转换为像素
function parseValue(value: number | string | undefined, containerSize: number, defaultValue: number): number {
    if (value === undefined) return defaultValue;
    if (typeof value === 'number') return value;
    if (typeof value === 'string') {
        if (value.endsWith('%')) {
            return (parseFloat(value) / 100) * containerSize;
        }
        if (value.endsWith('px')) {
            return parseFloat(value);
        }
        if (value.endsWith('vw')) {
            return (parseFloat(value) / 100) * window.innerWidth;
        }
        if (value.endsWith('vh')) {
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
        height: window.innerHeight
    };
}

// 设置对话框的最小最大尺寸
function setDialogMinMax(dialog: HTMLElement, options: ResizeDialogOptions) {
    const containerSize = getContainerSize();

    const minWidth = parseValue(options.minWidth, containerSize.width, 400);
    const maxWidth = parseValue(options.maxWidth, containerSize.width, containerSize.width * 0.9);
    const minHeight = parseValue(options.minHeight, containerSize.height, 300);
    const maxHeight = parseValue(options.maxHeight, containerSize.height, containerSize.height * 0.9);

    dialog.style.minWidth = minWidth + "px";
    dialog.style.maxWidth = maxWidth + "px";
    dialog.style.minHeight = minHeight + "px";
    dialog.style.maxHeight = maxHeight + "px";
}

// 强制限制对话框尺寸
function enforceDialogSize(dialog: HTMLElement, options: ResizeDialogOptions) {
    const rect = dialog.getBoundingClientRect();
    const containerSize = getContainerSize();

    const minWidth = parseValue(options.minWidth, containerSize.width, 400);
    const maxWidth = parseValue(options.maxWidth, containerSize.width, containerSize.width * 0.9);
    const minHeight = parseValue(options.minHeight, containerSize.height, 300);
    const maxHeight = parseValue(options.maxHeight, containerSize.height, containerSize.height * 0.9);

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

    dialog.style.width = newWidth + "px";
    dialog.style.height = newHeight + "px";
}

const resizeDialog = {
    mounted(el: HTMLElement, binding: DirectiveBinding<ResizeDialogOptions>) {
        const dialog = el;
        if (!dialog) return;

        console.log("自定义指令mounted:", dialog, binding.value);

        // 如果禁用调整大小，直接返回
        if (binding.value?.disabled) {
            console.log("指令禁用调整大小");
            return;
        }

        const containerSize = getContainerSize();
        const options: ResizeDialogOptions = {
            minWidth: binding.value?.minWidth ?? 400,
            minHeight: binding.value?.minHeight ?? 300,
            maxWidth: binding.value?.maxWidth ?? containerSize.width * 0.9,
            maxHeight: binding.value?.maxHeight ?? containerSize.height * 0.9,
            width: binding.value?.width,
            height: binding.value?.height,
            aspectRatio: binding.value?.aspectRatio,
            lockAspectRatio: binding.value?.lockAspectRatio ?? false,
            responsive: binding.value?.responsive ?? true,
            disabled: binding.value?.disabled ?? false,
        };

        console.log("指令配置选项:", options);

        // 设置初始尺寸
        if (options.width) {
            const width = parseValue(options.width, containerSize.width, 400);
            console.log("指令设置初始宽度:", width);
            dialog.style.width = width + "px";
        }
        if (options.height) {
            const height = parseValue(options.height, containerSize.height, 300);
            console.log("指令设置初始高度:", height);
            dialog.style.height = height + "px";
        }

        setDialogMinMax(dialog, options);
        enforceDialogSize(dialog, options);

        // 将配置传递给全局补丁
        console.log("指令调用setDialogResizeConfig");
        setDialogResizeConfig(dialog, options);

        // 延迟强制重新应用配置，确保DOM完全渲染
        setTimeout(() => {
            console.log("延迟强制重新应用配置");
            forceReapplyConfig(dialog);
        }, 100);

        // 保存状态用于更新
        (el as any).__resizeDialogState = {
            options,
            originalWidth: dialog.style.width,
            originalHeight: dialog.style.height,
        };
    },

    updated(el: HTMLElement, binding: DirectiveBinding<ResizeDialogOptions>) {
        const state: any = (el as any).__resizeDialogState;
        if (!state) return;

        console.log("自定义指令updated:", el, binding.value);

        // 如果禁用调整大小，移除所有手柄
        if (binding.value?.disabled) {
            const handles = el.querySelectorAll('.resize-handle');
            handles.forEach((handle) => handle.remove());
            return;
        }

        // 更新选项
        if (binding.value) {
            const containerSize = getContainerSize();
            const options: ResizeDialogOptions = {
                minWidth: binding.value.minWidth ?? 400,
                minHeight: binding.value.minHeight ?? 300,
                maxWidth: binding.value.maxWidth ?? containerSize.width * 0.9,
                maxHeight: binding.value.maxHeight ?? containerSize.height * 0.9,
                width: binding.value.width,
                height: binding.value.height,
                aspectRatio: binding.value.aspectRatio,
                lockAspectRatio: binding.value.lockAspectRatio ?? false,
                responsive: binding.value.responsive ?? true,
                disabled: binding.value.disabled ?? false,
            };

            console.log("指令更新配置:", options);

            state.options = options;
            setDialogMinMax(el, options);
            enforceDialogSize(el, options);

            // 更新全局配置
            setDialogResizeConfig(el, options);
        }
    },

    unmounted(el: HTMLElement) {
        console.log("自定义指令unmounted:", el);
        // 清理状态
        delete (el as any).__resizeDialogState;
    },
};

export default resizeDialog;
