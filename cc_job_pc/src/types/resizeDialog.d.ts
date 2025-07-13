declare module 'vue' {
    interface ComponentCustomProperties {
        vResizeDialog: {
            minWidth?: number | string
            maxWidth?: number | string
            minHeight?: number | string
            maxHeight?: number | string
            width?: number | string
            height?: number | string
            aspectRatio?: number
            lockAspectRatio?: boolean
            responsive?: boolean
            disabled?: boolean
        }
    }
}

export interface ResizeDialogOptions {
    minWidth?: number | string
    maxWidth?: number | string
    minHeight?: number | string
    maxHeight?: number | string
    width?: number | string
    height?: number | string
    aspectRatio?: number
    lockAspectRatio?: boolean
    responsive?: boolean
    disabled?: boolean
}

export interface ResizeDialogState {
    resizing: boolean
    dir: string
    startX: number
    startY: number
    startWidth: number
    startHeight: number
    startTop: number
    startLeft: number
    handles: HTMLElement[]
    options: ResizeDialogOptions
    originalWidth?: string
    originalHeight?: string
} 