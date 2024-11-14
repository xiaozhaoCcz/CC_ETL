
<template>
  <div class="app-container ">
    <div class="task_rank_top">
      <div class="btn_left_list">
        <el-button type="primary" v-if="!showTaskVisible" @click="showTaskVisible=true">展示任务</el-button>
        <el-button type="info" v-else @click="showTaskVisible=false">隐藏任务</el-button>
      </div>
      <div class="btn_right_list">
        <el-button type="primary" :icon="Edit" circle />
        <el-button type="success" :icon="Check" circle />
        <el-button type="info" :icon="Message" circle />
      </div>
    </div>
    <div class="task_rank">
      <div class="task_set_tree">
        <el-input
          v-model="filterText"
          placeholder="Filter keyword"
        />

        <el-tree
          ref="treeRef"
          class="filter-tree"
          :data="data"
          :props="defaultProps"
          default-expand-all
          :filter-node-method="filterNode"
        />
      </div>
      <div class="task_info_tree" v-if="showTaskVisible">
        <el-input
          v-model="filterText"
          placeholder="Filter keyword"
        />

        <el-tree
          ref="treeRef"
          class="filter-tree"
          :data="data"
          :props="defaultProps"
          default-expand-all
          :filter-node-method="filterNode"
        />

      </div>
      <div class="vue_flow_platform">
        <VueFlow :nodes="nodes" :edges="edges" >
          <Panel>
            <button type="button" @click="addNode">Add a node</button>
          </Panel>
        </VueFlow>
      </div>
    </div>
  </div>

</template>
<script setup lang="ts">
/* these are necessary styles for vue flow */
import '@vue-flow/core/dist/style.css';
/* this contains the default theme, these are optional styles */
import '@vue-flow/core/dist/theme-default.css';
import { ref, onMounted } from 'vue'
import { VueFlow, Panel, Position } from "@vue-flow/core";
import { Background } from '@vue-flow/background'
import {
  Check,
  Delete,
  Edit,
  Message,
  Search,
  Star,
} from '@element-plus/icons-vue'

const showTaskVisible = ref(false);

const nodes = ref([
  {
    id: '1',
    position: { x: 50, y: 50 },
    data: { label: 'Node 1', },
    style:{color:'red',border:'1px solid red'},
  },
  {
    id: '2',
    position: { x: 50, y: 250 },
    data: { label: 'Node 2', },
  }
]);

const edges = ref([
  {
    id: 'e1->2',
    source: '1',
    target: '2',
  }
]);

function addNode() {
  const id = Date.now().toString()

  nodes.value.push({
    id,
    position: { x: 150, y: 50 },
    data: { label: `Node ${id}`, },
  })
}

const filterText = ref('')
const treeRef = ref<InstanceType<typeof ElTree>>()

const defaultProps = {
  children: 'children',
  label: 'label',
}

watch(filterText, (val) => {
  treeRef.value!.filter(val)
})

const filterNode = (value: string, data: Tree) => {
  if (!value) return true
  return data.label.includes(value)
}

const data: Tree[] = [
  {
    id: 1,
    label: 'Level one 1',
    children: [
      {
        id: 4,
        label: 'Level two 1-1',
        children: [
          {
            id: 9,
            label: 'Level three 1-1-1',
          },
          {
            id: 10,
            label: 'Level three 1-1-2',
          },
        ],
      },
    ],
  },
  {
    id: 2,
    label: 'Level one 2',
    children: [
      {
        id: 5,
        label: 'Level two 2-1',
      },
      {
        id: 6,
        label: 'Level two 2-2',
      },
    ],
  },
  {
    id: 3,
    label: 'Level one 3',
    children: [
      {
        id: 7,
        label: 'Level two 3-1',
      },
      {
        id: 8,
        label: 'Level two 3-2',
      },
    ],
  },
]
</script>

<style lang="scss" scoped>

.task_rank_top{
  margin-bottom: 2px;
  display: flex;
  justify-content: space-between;
  align-items: center;

  .btn_right_list{
    background: #fff;
    padding: 5px 10px;
  }
}

.task_rank{
   width: 100%;
  display: flex;
  justify-content: left;
  height: 80vh;
  .task_set_tree{
      width: 20%;
      background: #fff;
      padding: 10px;
  }

  .task_info_tree{
    border-left: 1px solid #f0f0f0;
    width: 20%;
    background: #fff;
    padding: 10px;
  }

  .vue_flow_platform{
    background: #fff;
    border: 1px solid #f0f0f0;
    width: 100%;
  }
}

</style>
