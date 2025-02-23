<template>
  <div>
    <div style="margin: 5px">
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </div>
    <el-table :data="tableData" style="width: 100%" border @cell-click="showUnitInput">
      <el-table-column prop="columnKey" label="参数头">
        <template #default="{ row, column }">
          <el-input
            v-if="tableRowEditId === row.id && tableColumnEditIndex === column.id"
            v-model="row.columnKey"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
          />
          <span v-else>{{ row.columnKey }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="columnValue" label="参数值">
        <template #default="{ row, column }">
          <el-input
            v-if="tableRowEditId === row.id && tableColumnEditIndex === column.id"
            v-model="row.columnValue"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
          />
          <span v-else>{{ row.columnValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDelete(row)"> 删除 </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from "vue";
const props = defineProps({
  list: {
    type: Array,
    default: [],
  },
});
const emit = defineEmits(["handleTableData"]);

const tableData = ref([]);

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

<style></style>
