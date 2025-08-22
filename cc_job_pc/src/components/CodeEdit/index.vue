<template>
  <el-dialog
    v-model="_glueVisible"
    title="GLUE IDE"
    width="auto"
    :before-close="handleCloseDialog"
    draggable
    append-to-body
    data-resize-options='{"minWidth":"60%","maxWidth":"90%","minHeight":"auto","maxHeight":"auto","width":"auto"}'
  >
    <div class="editor-container">
      <MonacoEditor
        style="text-align: left; height: 100%; width: 100%"
        v-model:value="_code"
      />
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
          <el-button type="primary" size="small" @click="submitForm"
            >保存</el-button
          >
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
    console.log("👁️ GLUE IDE可见性变化:", val, "glueTaskId:", props.glueTaskId);
    
    // 当GLUE IDE打开时，如果有taskId则获取历史记录
    if (val && props.glueTaskId && props.glueTaskId > 0) {
      console.log("📥 设置taskId并获取历史记录:", props.glueTaskId);
      taskId.value = props.glueTaskId;
      getGlueList(props.glueTaskId);
    } else if (val) {
      console.log("ℹ️ GLUE IDE打开但无有效taskId，清空历史记录");
      taskId.value = null;
      glueList.value = [];
    }
  }
);

watch(
  () => props.code,
  (val) => {
    _code.value = val || "";
  }
);

watch(
  () => props.nowDate,
  () => {
    if (props.glueTaskId && props.glueTaskId > 0) {
      taskId.value = props.glueTaskId;
      // 只有在GLUE IDE打开时才获取历史记录
      if (_glueVisible.value) {
        getGlueList(props.glueTaskId);
      }
    } else {
      taskId.value = null;
      glueList.value = [];
    }
  }
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
        // 检查响应结构
        if (response && response.data) {
          glueList.value = response.data;
        } else if (Array.isArray(response)) {
          // 如果响应直接是数组
          glueList.value = response;
        } else {
          glueList.value = [];
        }
      })
      .catch((error) => {
        glueList.value = [];
      });
  } else {
    console.log("ℹ️ taskId无效，清空GLUE历史记录列表");
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
* {
  box-sizing: border-box;
}

:deep(.el-dialog) {
  min-width: 320px;
  width: auto !important;
  height: auto !important;
  max-width: 90vw;
  max-height: 90vh;
  margin: 5vh auto;
}

:deep(.el-dialog__body) {
  height: auto;
  overflow: visible;
  padding: 20px;
  box-sizing: border-box;
}

.editor-container {
  margin-bottom: 10px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e5e6eb;
  background: #232323;
  min-height: 300px;
  height: 50vh;
  box-sizing: border-box;
}
.footer {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  padding: 8px 0 0 0;
  box-sizing: border-box;
}
.footer-item {
  display: flex;
  align-items: center;
  gap: 4px;
  box-sizing: border-box;
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
