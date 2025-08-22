<script setup>
import { ref, onMounted, onBeforeUnmount, watch, defineProps, defineEmits } from "vue";
import loader from "@monaco-editor/loader";

const props = defineProps({
  value: String,
  language: {
    type: String,
    default: "java",
  },
  theme: {
    type: String,
    default: "vs-dark",
  },
  height: {
    type: String,
    default: "800px",
  },
  readOnly: {
    type: Boolean,
    default: false,
  },
});

const emits = defineEmits(["update:value"]);

const editorContainer = ref(null);
let editorInstance = null;
let monacoInstance = null;

// 懒加载Monaco
async function initializeMonaco() {
  if (monacoInstance) {
    return monacoInstance;
  }
  
  try {
    monacoInstance = await loader.init();
    return monacoInstance;
  } catch (error) {
    console.error('Monaco编辑器初始化失败:', error);
    throw error;
  }
}

// 优化编辑器配置
function getEditorOptions(monaco) {
  return {
    value: props.value || "",
    language: props.language,
    theme: props.theme,
    readOnly: props.readOnly,
    domReadOnly: props.readOnly,
    quickSuggestions: false,
    minimap: { enabled: false },
    lineNumbersMinChars: 1,
    lineNumbers: "off",
    wordWrap: "on",
    unicodeHighlight: {
      ambiguousCharacters: false,
    },
    // 性能优化配置
    renderWhitespace: "none",
    folding: false,
    foldingStrategy: "indentation",
    showFoldingControls: "never",
    // 禁用不必要的功能
    suggestOnTriggerCharacters: false,
    acceptSuggestionOnCommitCharacter: false,
    acceptSuggestionOnEnter: "off",
    tabCompletion: "off",
    wordBasedSuggestions: false,
    parameterHints: {
      enabled: false,
    },
    // 优化渲染
    renderLineHighlight: "none",
    overviewRulerBorder: false,
    hideCursorInOverviewRuler: true,
    overviewRulerLanes: 0,
    scrollBeyondLastLine: false,
    // 减少内存使用
    maxTokenizationLineLength: 20000,
    maxTokenizationLineLengthLimit: 20000,
  };
}

onMounted(async () => {
  try {
    const monaco = await initializeMonaco();
    editorInstance = monaco.editor.create(editorContainer.value, getEditorOptions(monaco));

    // 优化事件监听
    const debouncedChangeHandler = debounce(() => {
      emits("update:value", editorInstance.getValue());
    }, 300);

    editorInstance.onDidChangeModelContent(debouncedChangeHandler);
    
    // 添加性能监控
    if (window.performance) {
      performance.mark('monaco-editor-ready');
    }
  } catch (error) {
    console.error('编辑器创建失败:', error);
  }
});

onBeforeUnmount(() => {
  if (editorInstance) {
    // 清理事件监听器
    editorInstance.dispose();
    editorInstance = null;
  }
  
  // 清理Monaco实例
  if (monacoInstance) {
    monacoInstance = null;
  }
});

// 防抖函数
function debounce(func, wait) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

watch(
  () => props.language,
  async (newLanguage) => {
    if (editorInstance && monacoInstance) {
      try {
        monacoInstance.editor.setModelLanguage(editorInstance.getModel(), newLanguage);
      } catch (error) {
        console.error('语言切换失败:', error);
      }
    }
  }
);

watch(
  () => props.value,
  (newValue) => {
    if (editorInstance && editorInstance.getValue() !== newValue) {
      editorInstance.setValue(newValue);
    }
  }
);

watch(
  () => props.readOnly,
  (newReadOnly) => {
    if (editorInstance) {
      editorInstance.updateOptions({ readOnly: newReadOnly });
    }
  }
);
</script>

<template>
  <div ref="editorContainer" class="editor-container" :style="{ height: height }" />
</template>

<style scoped>
.editor-container {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

/* 性能优化样式 */
.editor-container :deep(.monaco-editor) {
  /* 启用硬件加速 */
  transform: translateZ(0);
  will-change: transform;
}

.editor-container :deep(.monaco-editor .overflow-guard) {
  /* 优化滚动性能 */
  -webkit-overflow-scrolling: touch;
}
</style>
