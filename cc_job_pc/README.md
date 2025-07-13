# Vue 3 + TypeScript + Vite

This template should help get you started developing with Vue 3 and TypeScript in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Type Support For `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin) to make the TypeScript language service aware of `.vue` types.

If the standalone TypeScript plugin doesn't feel fast enough to you, Volar has also implemented a [Take Over Mode](https://github.com/johnsoncodehk/volar/discussions/471#discussioncomment-1361669) that is more performant. You can enable it by the following steps:

1. Disable the built-in TypeScript Extension
   1. Run `Extensions: Show Built-in Extensions` from VSCode's command palette
   2. Find `TypeScript and JavaScript Language Features`, right click and select `Disable (Workspace)`
2. Reload the VSCode window by running `Developer: Reload Window` from the command palette.

## 自定义指令使用说明

### v-resize-dialog 指令

这是一个用于对话框大小调整的自定义指令，支持多种配置选项。

#### 基本用法

```vue
<el-dialog v-resize-dialog v-model="visible" title="对话框">
  <!-- 对话框内容 -->
</el-dialog>
```

#### 高级用法

```vue
<el-dialog 
  v-resize-dialog="{
    minWidth: 600,
    maxWidth: '80%',
    minHeight: 400,
    maxHeight: '70vh',
    width: 720,
    aspectRatio: 16/9,
    lockAspectRatio: false,
    responsive: true,
    disabled: false
  }"
  v-model="visible" 
  title="对话框"
>
  <!-- 对话框内容 -->
</el-dialog>
```

#### 配置选项

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `minWidth` | `number \| string` | `400` | 最小宽度，支持像素值、百分比、vw单位 |
| `maxWidth` | `number \| string` | `90%` | 最大宽度，支持像素值、百分比、vw单位 |
| `minHeight` | `number \| string` | `300` | 最小高度，支持像素值、百分比、vh单位 |
| `maxHeight` | `number \| string` | `90%` | 最大高度，支持像素值、百分比、vh单位 |
| `width` | `number \| string` | - | 初始宽度 |
| `height` | `number \| string` | - | 初始高度 |
| `aspectRatio` | `number` | - | 宽高比（如 16/9） |
| `lockAspectRatio` | `boolean` | `false` | 是否锁定宽高比 |
| `responsive` | `boolean` | `true` | 是否响应窗口大小变化 |
| `disabled` | `boolean` | `false` | 是否禁用调整大小功能 |

#### 支持的尺寸单位

- **像素值**: `600`, `800px`
- **百分比**: `50%`, `80%`
- **视口宽度**: `50vw`, `80vw`
- **视口高度**: `50vh`, `70vh`

#### 使用示例

```vue
<!-- 基础用法 -->
<el-dialog v-resize-dialog v-model="visible" title="基础对话框">
  内容
</el-dialog>

<!-- 限制最小尺寸 -->
<el-dialog 
  v-resize-dialog="{ minWidth: 600, minHeight: 400 }"
  v-model="visible" 
  title="限制最小尺寸"
>
  内容
</el-dialog>

<!-- 限制最大尺寸为百分比 -->
<el-dialog 
  v-resize-dialog="{ maxWidth: '80%', maxHeight: '70vh' }"
  v-model="visible" 
  title="限制最大尺寸"
>
  内容
</el-dialog>

<!-- 固定宽高比 -->
<el-dialog 
  v-resize-dialog="{ aspectRatio: 16/9, lockAspectRatio: true }"
  v-model="visible" 
  title="固定宽高比"
>
  内容
</el-dialog>

<!-- 禁用调整大小 -->
<el-dialog 
  v-resize-dialog="{ disabled: true }"
  v-model="visible" 
  title="禁用调整大小"
>
  内容
</el-dialog>

<!-- 响应式设计 -->
<el-dialog 
  v-resize-dialog="{ 
    minWidth: '50%', 
    maxWidth: '90%', 
    responsive: true 
  }"
  v-model="visible" 
  title="响应式对话框"
>
  内容
</el-dialog>
```

#### 特性

1. **多方向调整**: 支持8个方向的拖拽调整（上、下、左、右、四个角）
2. **智能限制**: 自动应用最小/最大尺寸限制
3. **宽高比锁定**: 可选的宽高比锁定功能
4. **响应式**: 支持响应窗口大小变化
5. **多种单位**: 支持像素、百分比、视口单位
6. **可禁用**: 可以完全禁用调整大小功能
7. **平滑动画**: 拖拽时平滑的尺寸变化

#### 注意事项

1. 指令会自动为对话框添加调整大小的手柄
2. 拖拽时会自动限制在设定的最小/最大尺寸范围内
3. 如果设置了 `lockAspectRatio: true`，调整时会保持设定的宽高比
4. 响应式模式下，窗口大小变化时会自动重新计算限制值
5. 可以通过 `disabled: true` 完全禁用调整大小功能
