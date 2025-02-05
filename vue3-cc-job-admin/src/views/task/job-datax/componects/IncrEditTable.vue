<template>
  <div>
    <div style="margin: 5px">
      <el-button type="primary" @click="handleAdd(0)">新增</el-button>
      <el-button type="warning" @click="handleAdd(1)">新增时间增量</el-button>
    </div>
    <el-table
      :data="tableData"
      style="width: 100%"
      border
      @cell-click="showUnitInput"
    >
      <el-table-column
        prop="columnKey"
        label="表头列"
        width="120"
        align="center"
      >
        <template #default="{ row, column }">
          <el-input
            v-if="
              tableRowEditId === row.id && tableColumnEditIndex === column.id
            "
            v-model="row.columnKey"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
          />
          <span v-else>{{ row.columnKey }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="columnValue"
        label="数据值"
        width="248"
        align="center"
      >
        <template #default="{ row, column }">
          <div v-if="row.columnType === 1">
            <el-date-picker
              v-if="
                tableRowEditId === row.id && tableColumnEditIndex === column.id
              "
              v-model="row.columnValue"
              type="datetime"
              placeholder="Select date and time"
              format="YYYY-MM-DD hh:mm:ss"
              value-format="x"
            />
            <span v-else>
              {{
                row.columnValue === ""
                  ? row.columnValue
                  : timestampToDateTime(row.columnValue)
              }}
            </span>
          </div>
          <div v-else>
            <el-input
              v-if="
                tableRowEditId === row.id && tableColumnEditIndex === column.id
              "
              v-model="row.columnValue"
              @blur="blurValueInput(row, column)"
              @keyup.enter="blurValueInput(row, column)"
            />
            <span v-else>
              {{ row.columnValue }}
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column
        prop="columnParam"
        label="参数值"
        width="180"
        align="center"
      >
        <template #default="{ row, column }">
          <el-input
            v-if="
              tableRowEditId === row.id && tableColumnEditIndex === column.id
            "
            v-model="row.columnParam"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
          />
          <span v-else>{{ row.columnParam }}</span>
        </template>
      </el-table-column>
      <el-table-column
        prop="columnTimeFormat"
        label="时间格式"
        width="240"
        align="center"
      >
        <template #default="{ row, column }">
          <el-select
            v-if="
              tableRowEditId === row.id &&
              tableColumnEditIndex === column.id &&
              row.columnType == 1
            "
            v-model="row.columnTimeFormat"
            filterable
            style="width: 210px"
          >
            <el-option label="YYYY/MM/DD hh:mm:ss" value="YYYY/MM/DD hh:mm:ss" />
            <el-option label="YYYY-MM-DD hh:mm:ss" value="YYYY-MM-DD hh:mm:ss" />
            <el-option label="YYYY/MM/DD" value="YYYY/MM/DD" />
            <el-option label="YYYY-MM-DD" value="YYYY-MM-DD" />
            <el-option label="timestamp" value="x" />
          </el-select>
          <span v-else>{{ row.columnTimeFormat }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { timestampToDateTime } from "@/utils/index.ts";
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
  console.log(row, column);
};

const handleDelete = (row) => {
  const index = tableData.value.indexOf(row);
  if (index !== -1) {
    tableData.value.splice(index, 1);
  }
};

const handleAdd = (rowType) => {
  tableData.value.unshift({
    id: tableData.value.length + 1,
    columnKey: "",
    columnValue: "",
    columnParam: "",
    columnTimeFormat: "",
    columnType: rowType,
  });
};
</script>

<style></style>
