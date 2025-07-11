<template>
  <div class="edit-table-container">
    <!-- 头部操作区域 -->
    <div class="table-header">
      <div class="header-left">
        <h3 class="table-title">参数配置</h3>
        <span class="table-subtitle">配置任务执行所需的参数信息</span>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleAdd" class="add-button">
          <el-icon><Plus /></el-icon>
          新增参数
        </el-button>
      </div>
    </div>

    <!-- 表格区域 -->
    <div class="table-wrapper">
      <el-table
        :data="tableData"
        class="custom-table"
        border
        @cell-click="showUnitInput"
        :header-cell-style="headerCellStyle"
        :cell-style="cellStyle"
      >
        <el-table-column prop="columnKey" label="参数名称" min-width="200">
          <template #default="{ row, column }">
            <div class="cell-content">
              <el-input
                v-if="tableRowEditId === row.id && tableColumnEditIndex === column.id"
                v-model="row.columnKey"
                @blur="blurValueInput(row, column)"
                @keyup.enter="blurValueInput(row, column)"
                class="edit-input"
                placeholder="请输入参数名称"
              />
              <span v-else class="cell-text">{{ row.columnKey || "未设置" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="columnValue" label="参数值" min-width="300">
          <template #default="{ row, column }">
            <div class="cell-content">
              <el-input
                v-if="tableRowEditId === row.id && tableColumnEditIndex === column.id"
                v-model="row.columnValue"
                @blur="blurValueInput(row, column)"
                @keyup.enter="blurValueInput(row, column)"
                class="edit-input"
                placeholder="请输入参数值"
              />
              <span v-else class="cell-text">{{ row.columnValue || "未设置" }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="danger"
                link
                @click="handleDelete(row)"
                class="delete-button"
              >
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 空状态 -->
    <div v-if="tableData.length === 0" class="empty-state">
      <el-empty description="暂无参数配置" />
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, watch } from "vue";
import { Plus, Delete } from "@element-plus/icons-vue";

const props = defineProps({
  list: {
    type: Array,
    default: [],
  },
});
const emit = defineEmits(["handleTableData"]);

const tableData = ref([]);

// 表格样式配置
const headerCellStyle = {
  backgroundColor: "#f8f9fa",
  color: "#606266",
  fontWeight: "600",
  fontSize: "14px",
  borderBottom: "1px solid #ebeef5",
};

const cellStyle = {
  padding: "12px 0",
  fontSize: "14px",
};

watch(
  () => props.list,
  async (val) => {
    if (!val || val.length === 0) return;
    // 使用 nextTick 确保 DOM 更新后进行操作，防止 offsetHeight 报错
    await nextTick();
    tableData.value = val;
  },
  {
    deep: true,
    immediate: true,
  }
);

watch(
  () => tableData.value,
  async (val) => {
    emit("handleTableData", val);
  },
  {
    deep: true,
    immediate: true,
  }
);

let tableRowEditId = ref(null); // 控制可编辑的每一行
let tableColumnEditIndex = ref(null); //控制可编辑的每一列

const showUnitInput = (row, column) => {
  //赋值给定义的变量
  tableRowEditId.value = row.id; //确定点击的单元格在哪行 如果数据中有ID可以用ID判断，没有可以使用其他值判断，只要能确定是哪一行即可
  tableColumnEditIndex.value = column.id; //确定点击的单元格在哪列
};

const blurValueInput = (row, column) => {
  // tableRowEditId.value = null
  // tableColumnEditIndex.value = null
  //在此处调接口传数据
};

const handleDelete = (row) => {
  const index = tableData.value.indexOf(row);
  if (index !== -1) {
    tableData.value.splice(index, 1);
  }
};

const handleAdd = () => {
  tableData.value.unshift({
    id: tableData.value.length + 1,
    columnKey: "",
    columnValue: "",
  });
};
</script>

<style scoped>
.edit-table-container {
  background: #fff;
  border-radius: 5px;
  box-shadow: 0 1px 6px 0 rgba(0, 0, 0, 0.06);
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px 8px 14px;
  border-bottom: 1px solid #f0f0f0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.table-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: white;
}

.table-subtitle {
  font-size: 11px;
  opacity: 0.85;
  color: rgba(255, 255, 255, 0.8);
}

.header-right {
  display: flex;
  gap: 8px;
}

.add-button {
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: white;
  font-weight: 500;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 3px;
  height: 28px;
  transition: all 0.2s;
}
.add-button:hover {
  background: rgba(255, 255, 255, 0.28);
  border-color: rgba(255, 255, 255, 0.4);
  transform: translateY(-1px);
}

.table-wrapper {
  padding: 0 0 0 0;
}
.custom-table {
  border: none;
  font-size: 12px;
  table-layout: fixed;
}
.custom-table :deep(.el-table__header) {
  background: #f8f9fa;
}
.custom-table :deep(.el-table__header th) {
  background: #f8f9fa !important;
  border-bottom: 1px solid #ebeef5;
  font-weight: 600;
  color: #606266;
  font-size: 12px;
  padding: 6px 0;
}
.custom-table :deep(.el-table__body tr:hover) {
  background-color: #f5f7fa !important;
}
.custom-table :deep(.el-table__body td) {
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.2s;
  font-size: 12px;
  padding: 6px 0;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-content {
  display: flex;
  align-items: center;
  min-height: 24px;
  width: 100%;
}
.cell-text {
  color: #606266;
  font-size: 12px;
  line-height: 1.5;
  cursor: pointer;
  transition: color 0.2s;
}
.cell-text:hover {
  color: #409eff;
}
.edit-input {
  width: 100%;
  font-size: 12px;
  max-width: 160px;
}
.edit-input :deep(.el-input__wrapper) {
  border-radius: 3px;
  box-shadow: 0 0 0 1px #409eff inset;
  min-height: 22px;
  font-size: 12px;
}
.action-buttons {
  display: flex;
  justify-content: center;
  gap: 4px;
}
.delete-button {
  color: #f56c6c;
  font-size: 12px;
  transition: all 0.2s;
  padding: 0 4px;
}
.delete-button:hover {
  color: #f78989;
  transform: scale(1.05);
}
.empty-state {
  padding: 8px 0 4px 0;
  text-align: center;
  min-height: unset;
}
.empty-state :deep(.el-empty) {
  padding: 0;
}
.empty-state :deep(.el-empty__image) {
  width: 60px !important;
  height: 60px !important;
  margin: 0 auto 4px auto;
}
.empty-state :deep(.el-empty__description) {
  font-size: 12px;
  color: #bcbcbc;
  margin-top: 0;
}
/* 响应式设计 */
@media (max-width: 768px) {
  .table-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
    padding: 8px 6px 6px 6px;
  }
  .header-right {
    width: 100%;
  }
  .add-button {
    width: 100%;
  }
  .table-title {
    font-size: 13.5px;
  }
  .table-subtitle {
    font-size: 10px;
  }
}
/* 动画效果 */
.edit-table-container {
  animation: fadeInUp 0.4s cubic-bezier(0.39, 0.575, 0.565, 1) both;
}
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
/* 表格行悬停效果增强 */
.custom-table :deep(.el-table__body tr) {
  transition: all 0.2s;
}
.custom-table :deep(.el-table__body tr:hover) {
  background-color: #f5f7fa !important;
  transform: translateY(-1px);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
</style>
