<template>
  <div class="merges-container" style="background: #fff">
    <div class="info_form">
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">基础配置</div>
        <el-divider style="margin: 8px" />
        <div class="child_main">
          <div class="m_left">
            <div class="c_cont">
              <span class="m_title">执行器</span>
              <el-select
                v-model="userStore.dataxGroups.formData.jobGroup"
                filterable
                placeholder="选择执行器"
              >
                <el-option
                  v-for="item in taskGroupList"
                  :key="item.id"
                  :label="item.title"
                  :value="item.id"
                />
              </el-select>
            </div>
            <div class="c_cont">
              <span class="m_title">负责人</span>
              <el-input
                v-model="userStore.dataxGroups.formData.author"
                type="text"
                autocomplete="off"
                placeholder="输入负责人"
              />
            </div>
          </div>
          <div class="m_right">
            <div class="c_cont">
              &nbsp;
              <!--              <span class="m_title">任务描述</span>-->
              <!--              <el-input-->
              <!--                v-model="userStore.dataxGroups.formData.jobDesc"-->
              <!--                type="text"-->
              <!--                autocomplete="off"-->
              <!--                placeholder="任务描述"-->
              <!--              />-->
            </div>
            <div class="c_cont">
              <span>报警邮件</span>
              <el-input
                v-model="userStore.dataxGroups.formData.alarmEmail"
                type="text"
                autocomplete="off"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">调度配置</div>
        <el-divider style="margin: 8px" />
        <div class="child_main" style="margin-left: 6px">
          <div class="m_left">
            <div class="c_cont">
              <span class="m_title">调度类型</span>
              <el-select
                v-model="userStore.dataxGroups.formData.scheduleType"
                filterable
                placeholder="Select"
                style="width: 210px"
              >
                <el-option
                  v-for="item in scheduleTypeList"
                  :key="item.type"
                  :label="item.title"
                  :value="item.type"
                />
              </el-select>
            </div>
          </div>
          <div
            class="m_right"
            v-if="userStore.dataxGroups.formData.scheduleType == 'CRON'"
          >
            <div class="c_cont" style="position: relative">
              <span class="m_title">CRON</span>
              <el-input
                v-model="userStore.dataxGroups.formData.scheduleConf"
                placeholder="cron表达式..."
              >
                <template #append>
                  <el-button @click="cronPopover = !cronPopover">
                    设置
                  </el-button>
                </template>
              </el-input>
              <div v-show="cronPopover" class="cronPopover">
                <noVue3Cron
                  :cron-value="userStore.dataxGroups.formData.scheduleConf"
                  i18n="cn"
                  @change="changeCron"
                  @close="cronPopover = false"
                />
              </div>
            </div>
          </div>
          <div
            class="m_right"
            v-if="userStore.dataxGroups.formData.scheduleType == 'FIX_RATE'"
          >
            <div class="c_cont">
              <span class="m_title">固定速度</span>
              <el-input
                v-model="userStore.dataxGroups.formData.scheduleConf"
                placeholder="默认秒"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">高级配置</div>
        <el-divider style="margin: 8px" />
        <div class="child_main">
          <div class="m_left">
            <div class="c_cont">
              <span class="m_title">路由策略</span>
              <el-select
                v-model="userStore.dataxGroups.formData.executorRouteStrategy"
                filterable
              >
                <el-option
                  v-for="item in routeStrategyList"
                  :key="item.type"
                  :label="item.title"
                  :value="item.type"
                />
              </el-select>
            </div>
            <div class="c_cont">
              <span class="m_title">调度过期策略</span>
              <el-select
                v-model="userStore.dataxGroups.formData.misfireStrategy"
                filterable
                placeholder="Select"
              >
                <el-option
                  v-for="item in misfireStrategyList"
                  :key="item.type"
                  :label="item.title"
                  :value="item.type"
                />
              </el-select>
            </div>
            <div class="c_cont">
              <span>任务超时时间</span>
              <el-input
                v-model="userStore.dataxGroups.formData.executorTimeout"
                type="text"
                autocomplete="off"
              />
            </div>
          </div>
          <div class="m_right">
            <div class="c_cont">
              <span>子任务id</span>
              <el-input
                v-model="userStore.dataxGroups.formData.childJobid"
                type="text"
                autocomplete="off"
                placeholder="多个子任务使用逗号分隔"
              />
            </div>
            <div class="c_cont">
              <span class="m_title">阻塞处理策略</span>

              <el-select
                v-model="userStore.dataxGroups.formData.executorBlockStrategy"
                filterable
                placeholder="Select"
              >
                <el-option
                  v-for="item in blockStrategyList"
                  :key="item.type"
                  :label="item.title"
                  :value="item.type"
                />
              </el-select>
            </div>
            <div class="c_cont">
              <span>失败重试次数</span>
              <el-input
                v-model="userStore.dataxGroups.formData.executorFailRetryCount"
                type="text"
                autocomplete="off"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">数据表</div>
        <el-divider style="margin: 8px" />
        <div class="child_main" style="margin-left: 6px">
          <el-table
            :data="userStore.dataxGroups.tableList"
            style="height: 830px; overflow-y: hidden"
            @cell-click="showUnitInput"
          >
            <el-table-column
              label="reader名称"
              width="240"
              align="center"
              prop="reader"
            />
            <el-table-column
              label="writer名称"
              width="240"
              align="center"
              prop="writer"
            />
            <el-table-column
              label="任务名称"
              width="240"
              align="center"
              prop="jobDesc"
            >
              <template #default="{ row, column }">
                <el-input
                  v-if="tableRowEditId === row.reader"
                  v-model="row.jobDesc"
                  @blur="blurValueInput(row, column)"
                  @keyup.enter="blurValueInput(row, column)"
                />
                <span v-else>{{ row.jobDesc }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                  size="small"
                  type="danger"
                  @click="handleDelete(scope.$index, scope.row)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import JobGroupAPI from "@/api/task/job-group";
//当前使用的页面引入
import NoVue3Cron from "@/components/NoVue3Cron/index.vue";
import { useDataxStore } from "@/store";
import { ref } from "vue";
const userStore = useDataxStore();

const taskGroupList = ref([]);
let tableRowEditId = ref(null); // 控制可编辑的每一行

const scheduleTypeList = [
  {
    type: "CRON",
    title: "CRON",
  },
  { type: "NONE", title: "无" },
  { type: "FIX_RATE", title: "固定速度" },
];

const routeStrategyList = [
  {
    type: "FIRST",
    title: "第一个",
  },
  {
    type: "LAST",
    title: "最后一个",
  },
  {
    type: "ROUND",
    title: "轮询",
  },
  {
    type: "RANDOM",
    title: "随机",
  },
  {
    type: "CONSISTENT_HASH",
    title: "一致性哈希",
  },
  {
    type: "LEASTY_FREQUENTY_USED",
    title: "最不经常使用",
  },
  {
    type: "LEASTY_RECENTLY_USED",
    title: "最近最久未使用",
  },
  {
    type: "FAILOVER",
    title: "故障转移",
  },
  {
    type: "BUSYOVER",
    title: "忙碌转移",
  },
  {
    type: "SHARDING_BORADCAST",
    title: "分片广播",
  },
];

const misfireStrategyList = [
  {
    type: "DO_NOTHING",
    title: "忽略",
  },
  {
    type: "FIRE_ONCE_NOW",
    title: "立即执行一次",
  },
];

const blockStrategyList = [
  {
    type: "SERIAL_EXECUTION",
    title: "单机串行",
  },
  {
    type: "DISCARD_LATER",
    title: "丢弃后续调度",
  },
  {
    type: "COVER_EARLY",
    title: "覆盖之前调度",
  },
];

const cronPopover = ref(false);

async function fetchTaskGroupList() {
  const data = await JobGroupAPI.getAllJobGroupList();
  taskGroupList.value = data as any;
}

function changeCron(cron: string) {
  userStore.dataxGroups.formData.scheduleConf = cron;
}

const showUnitInput = (row, column) => {
  //赋值给定义的变量
  tableRowEditId.value = row.reader; //确定点击的单元格在哪行 如果数据中有ID可以用ID判断，没有可以使用其他值判断，只要能确定是哪一行即可
};

const blurValueInput = (row, column) => {
  // tableRowEditId.value = null
  // tableColumnEditIndex.value = null
};

const handleDelete = (index: number, row: any) => {
  userStore.dataxGroups.tableList.splice(index, 1);
};

function setTableList() {
  const readers = userStore.dataxGroups.readers.tableList;
  const writers = userStore.dataxGroups.writers.tableList;
  const size = readers.length;
  for (let i = 0; i < size; i++) {
    const obj = {};
    obj.reader = readers[i];
    obj.writer = writers[i];
    userStore.dataxGroups.tableList.push(obj);
  }
}

onMounted(() => {
  fetchTaskGroupList();
  setTableList();
});
</script>
<style lang="scss" scoped>
.merges-container {
  background: #fff;
  margin: 0 auto;
  padding: 30px;
  border-radius: 8px;
}

.m_title::after {
  content: "*";
  color: red;
  font-size: 16px;
}

.info_form {
  width: 900px;
  margin: 0 auto;

  .child_form {
    width: 100%;

    .child_main {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin: 0 50px;

      .c_cont {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        span {
          display: inline-block;
          width: 140px;
          font-size: 14px;
          text-align: right;
          margin-right: 10px;
        }
      }

      .c_cont_param {
        display: flex;
        align-items: center;
        width: 710px;
        margin-left: 10px;

        span {
          display: inline-block;
          width: 100px;
          font-size: 14px;
          text-align: right;
          margin-right: 10px;
        }
      }
    }

    .cronPopover {
      position: absolute;
      top: 100%;
      left: 20%;
      width: 700px;

      z-index: 1000;
      background-color: #fff;
      border-radius: 8px;
      padding: 5px;
      border: 1px solid #f4f4f4;
    }
  }
}
</style>
