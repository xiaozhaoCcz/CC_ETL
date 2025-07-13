# 对话框调整大小功能使用说明

## 概述

现在系统支持两种方式来为对话框添加调整大小功能：

1. **全局自动补丁**：所有 `.el-dialog` 元素都会自动获得调整大小功能
2. **自定义指令**：使用 `v-resize-dialog` 指令来精确控制对话框的调整大小行为

## 使用方法

### 方法一：全局自动补丁（推荐）

所有对话框都会自动获得调整大小功能，无需额外配置：

```vue
<template>
  <el-dialog v-model="visible" title="对话框">
    内容
  </el-dialog>
</template>
```

### 方法二：自定义指令

使用 `v-resize-dialog` 指令来精确控制调整大小行为：

```vue
<template>
  <el-dialog 
    v-resize-dialog="{
      minWidth: 600,
      maxWidth: '80%',
      minHeight: 400,
      maxHeight: '70vh',
      width: 720,
      responsive: true,
      disabled: false
    }"
    v-model="visible" 
    title="自定义对话框"
  >
    内容
  </el-dialog>
</template>
```

## 配置选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minWidth` | `number \| string` | `400` | 最小宽度 |
| `maxWidth` | `number \| string` | `90%` | 最大宽度 |
| `minHeight` | `number \| string` | `300` | 最小高度 |
| `maxHeight` | `number \| string` | `90%` | 最大高度 |
| `width` | `number \| string` | - | 初始宽度 |
| `height` | `number \| string` | - | 初始高度 |
| `aspectRatio` | `number` | - | 宽高比 |
| `lockAspectRatio` | `boolean` | `false` | 是否锁定宽高比 |
| `responsive` | `boolean` | `true` | 是否响应窗口大小变化 |
| `disabled` | `boolean` | `false` | 是否禁用调整大小 |

## 支持的尺寸单位

- **像素值**：`500`, `800px`
- **百分比**：`50%`, `80%`
- **视口单位**：`50vw`, `70vh`

## 使用示例

### 基础用法
```vue
<el-dialog v-resize-dialog v-model="visible" title="基础对话框">
  内容
</el-dialog>
```

### 限制尺寸
```vue
<el-dialog 
  v-resize-dialog="{
    minWidth: 500,
    maxWidth: '70%',
    minHeight: 300,
    maxHeight: '60vh'
  }"
  v-model="visible" 
  title="限制尺寸对话框"
>
  内容
</el-dialog>
```

### 固定宽高比
```vue
<el-dialog 
  v-resize-dialog="{
    aspectRatio: 16/9,
    lockAspectRatio: true,
    minWidth: 400,
    maxWidth: '80%'
  }"
  v-model="visible" 
  title="固定宽高比对话框"
>
  内容
</el-dialog>
```

### 禁用调整大小
```vue
<el-dialog 
  v-resize-dialog="{ disabled: true }"
  v-model="visible" 
  title="禁用调整大小"
>
  内容
</el-dialog>
```

## 注意事项

1. **全局补丁优先级**：如果同时使用了全局补丁和自定义指令，自定义指令的配置会覆盖全局补丁的默认配置。

2. **性能考虑**：全局补丁会自动为所有对话框添加调整大小功能，如果不需要，可以通过设置 `disabled: true` 来禁用。

3. **响应式设计**：建议在移动设备上使用百分比和视口单位，而不是固定像素值。

4. **兼容性**：功能兼容所有现代浏览器，包括移动端浏览器。

## 故障排除

### 对话框无法调整大小
- 检查是否设置了 `disabled: true`
- 确认对话框元素有 `.el-dialog` 类名

### 尺寸限制不生效
- 检查配置值是否正确
- 确认单位格式是否正确

### 拖拽手柄不可见
- 检查是否有其他样式覆盖了手柄
- 确认对话框的 `position` 设置正确 