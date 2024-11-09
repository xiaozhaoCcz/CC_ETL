<template>
  <div class="app-container">
    <el-dialog
      v-model="props.triggerLogVisable"
      title="调度备注"
      width="500"
      :before-close="handleClose"
    >
      <div v-html="triggerMsg"></div>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
const emit = defineEmits(["close"]);
const props = defineProps({
  triggerMsg: {
    type: String,
    default: "",
  },
  triggerLogVisable: {
    type: Boolean,
    default: false,
  },
});

watch(
  () => props.triggerLogVisable,
  () => {}
);

watch(
  () => props.triggerMsg,
  () => {}
);

function handleClose() {
  emit("close");
}
</script>
