<template>
  <el-dialog
    v-model="_glueVisible"
    title="GLUE IDE"
    width="520px"
    :before-close="handleCloseDialog"
    draggable
    append-to-body
    data-resize-options='{"minWidth":520,"maxWidth":520,"minHeight":600, "maxHeight":600,"width":520}'
  >
    <div class="editor-container">
      <MonacoEditor style="text-align: left; height: 400px" v-model:value="_code" />
    </div>
    <template #footer>
      <div class="footer">
        <div class="footer-item">
          <span class="m_title">备注</span>
          <el-input
            v-model="_input"
            placeholder="备注"
            size="small"
            style="width: 120px"
          />
        </div>
        <div class="footer-item">
          <el-select
            v-model="glueId"
            filterable
            placeholder="选择历史"
            size="small"
            style="width: 120px"
            @change="handleGlueChange"
          >
            <el-option
              v-for="item in glueList"
              :key="item.id"
              :label="item.glueRemark"
              :value="item.id"
            />
          </el-select>
        </div>
        <div class="footer-item">
          <el-button type="primary" size="small" @click="submitForm">保存</el-button>
          <el-button size="small" @click="handleCloseDialog">取消</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import MonacoEditor from "./MonacoEditor.vue";
import JobInfoAPI from "@/api/job-info";
import { ref, watch } from "vue";
import { ElMessage } from "element-plus";

// 定义接口
interface GlueItem {
  id: number;
  glueRemark: string;
  glueSource: string;
}

const emit = defineEmits(["close"]);
const props = defineProps({
  glueVisible: Boolean,
  code: String,
  glueTaskId: Number,
  nowDate: Date,
});

const _glueVisible = ref(false);
const _input = ref("");
const _code = ref("");
const taskId = ref<number | null>(null);
const glueList = ref<GlueItem[]>([]);
const glueId = ref<number | null>(null);

watch(
  () => props.glueVisible,
  (val) => {
    _glueVisible.value = val;
  }
);

watch(
  () => props.code,
  (val) => {
    _code.value = val || "";
  }
);

watch(
  () => props.glueTaskId,
  (val) => {
    if (val && val > 0) {
      taskId.value = val;
      getGlueList(val);
    } else {
      taskId.value = null;
      glueList.value = [];
    }
  }
);

watch(
  () => props.nowDate,
  () => {}
);

function handleCloseDialog() {
  emit("close", "");
}

function handleGlueChange(val: number | null) {
  if (val != null) {
    const selectedItem = glueList.value.find((item) => item.id === val);
    if (selectedItem) {
      _code.value = selectedItem.glueSource;
    }
  }
}

function getGlueList(taskId: number) {
  if (taskId && taskId > 0) {
    JobInfoAPI.getGlueList(taskId)
      .then((response: any) => {
        glueList.value = (response as any).data || [];
      })
      .catch((error) => {
        console.error("获取GLUE列表失败:", error);
        glueList.value = [];
      });
  } else {
    glueList.value = [];
  }
}

function submitForm() {
  if (_input.value == "" || _input.value.trim().length <= 0) {
    ElMessage.warning("请输入备注～");
    return;
  }

  if (!taskId.value) {
    ElMessage.error("任务ID不能为空");
    return;
  }

  const obj = {
    taskId: taskId.value,
    glueRemark: _input.value,
    glueSource: _code.value,
  };

  JobInfoAPI.saveGlueSource(obj)
    .then(() => {
      _input.value = "";
      ElMessage.success("保存成功");
    })
    .catch((error) => {
      console.error("保存GLUE代码失败:", error);
      ElMessage.error("保存失败");
    })
    .finally(() => {
      emit("close", _code.value);
    });
}
</script>

<style lang="scss" scoped>
.editor-container {
  margin-bottom: 10px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e6eb;
  background: #232323;
  min-height: 120px;
  max-height: 400px;
}
.footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 8px 0 0 0;
}
.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.m_title {
  font-size: 14px;
  color: #333;
  margin-right: 2px;
  &::after {
    content: "*";
    color: #f56c6c;
    font-size: 14px;
    margin-left: 2px;
  }
}
</style>
