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
});

const emits = defineEmits(["update:value"]);

const editorContainer = ref(null);
let editorInstance = null;

async function initializeMonaco() {
  const monaco = await loader.init();
  return monaco;
}

onMounted(async () => {
  const monaco = await initializeMonaco();
  editorInstance = monaco.editor.create(editorContainer.value, {
    value: props.value || "",
    language: props.language,
    theme: props.theme,
    readOnly: false,
    domReadOnly: false,
    quickSuggestions: false,
    minimap: { enabled: false },
    lineNumbersMinChars: 1,
    lineNumbers: "off",
    wordWrap: "on",
    unicodeHighlight: {
      ambiguousCharacters: false,
    },
  });

  editorInstance.onDidChangeModelContent(() => {
    emits("update:value", editorInstance.getValue());
  });
});

onBeforeUnmount(() => {
  if (editorInstance) {
    editorInstance.dispose();
  }
});

watch(
  () => props.language,
  async (newLanguage) => {
    if (editorInstance) {
      const monaco = await initializeMonaco();
      monaco.editor.setModelLanguage(editorInstance.getModel(), newLanguage);
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
</script>

<template>
  <div ref="editorContainer" class="editor-container" />
</template>

<style>
.editor-container {
  width: 100%;
  height: 800px;
}
</style>
