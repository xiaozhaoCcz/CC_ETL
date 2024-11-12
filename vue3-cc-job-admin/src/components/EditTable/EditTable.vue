<template>
  <div>
    <div style="margin:5px">
      <el-button type="primary" @click="handleAdd">
        新增
      </el-button>
    </div>
    <el-table
      :data="tableData"
      style="width: 100%"
      border
      @cell-click="showUnitInput"
    >
      <el-table-column prop="columnKey" label="参数头">
        <template #default="{ row, column }">
          <el-input
            v-if="
                tableRowEditId === row.id &&tableColumnEditIndex === column.id"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
            v-model="row.columnKey"
          />
          <span v-else>{{ row.columnKey }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="columnValue" label="参数值">
        <template #default="{ row, column }">
          <el-input
            v-if="
                tableRowEditId === row.id &&tableColumnEditIndex === column.id"
            @blur="blurValueInput(row, column)"
            @keyup.enter="blurValueInput(row, column)"
            v-model="row.columnValue"
          />
          <span v-else>{{ row.columnValue }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="danger" link @click="handleDelete(row)">Delete</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref } from 'vue'

let tableRowEditId = ref(null) // 控制可编辑的每一行
let tableColumnEditIndex = ref(null) //控制可编辑的每一列

const showUnitInput = (row, column) => {
  //赋值给定义的变量
  tableRowEditId.value = row.id //确定点击的单元格在哪行 如果数据中有ID可以用ID判断，没有可以使用其他值判断，只要能确定是哪一行即可
  tableColumnEditIndex.value = column.id //确定点击的单元格在哪列
}
const blurValueInput = (row, column) => {
  // tableRowEditId.value = null
  // tableColumnEditIndex.value = null
  //在此处调接口传数据
}
const tableData = ref([
  {
    id:1,
    columnKey: "11",
    columnValue: "22",
  },
])

const handleDelete = (row) => {
  const index = tableData.value.indexOf(row)
  if (index !== -1) {
    tableData.value.splice(index, 1)
  }
}

const handleAdd = () => {
  tableData.value.unshift({
    id:tableData.value.length+1,
    date: '2016-05-05',
    name: 'Tom',
    address: 'No. 189, Grove St, Los Angeles',
    value: tableData.value.length+1
  });

}
</script>

<style>
</style>
