<template>
  <div class="app-container" style="background: #fff">
    <div class="info_form">
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">基础配置</div>
        <el-divider style="margin: 8px" />
        <div class="child_main">
          <div class="m_left">
            <div class="c_cont">
              <span class="m_title">执行器</span>
              <el-select
                v-model="formData.jobGroup"
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
                v-model="formData.author"
                type="text"
                autocomplete="off"
                placeholder="输入负责人"
              />
            </div>
          </div>
          <div class="m_right">
            <div class="c_cont">
              <span class="m_title">任务描述</span>
              <el-input
                v-model="formData.jobDesc"
                type="text"
                autocomplete="off"
                placeholder="任务描述"
              />
            </div>
            <div class="c_cont">
              <span>报警邮件</span>
              <el-input
                v-model="formData.alarmEmail"
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
                v-model="formData.scheduleType"
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
          <div class="m_right" v-if="formData.scheduleType == 'CRON'">
            <div class="c_cont" style="position: relative">
              <span class="m_title">CRON</span>
              <el-input
                v-model="formData.scheduleConf"
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
                  :cron-value="formData.scheduleConf"
                  i18n="cn"
                  @change="changeCron"
                  @close="cronPopover = false"
                />
              </div>
            </div>
          </div>
          <div class="m_right" v-if="formData.scheduleType == 'FIX_RATE'">
            <div class="c_cont">
              <span class="m_title">固定速度</span>
              <el-input v-model="formData.scheduleConf" placeholder="默认秒" />
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
              <el-select v-model="formData.executorRouteStrategy" filterable>
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
                v-model="formData.misfireStrategy"
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
                v-model="formData.executorTimeout"
                type="text"
                autocomplete="off"
              />
            </div>
          </div>
          <div class="m_right">
            <div class="c_cont">
              <span>子任务id</span>
              <el-input
                v-model="formData.childJobid"
                type="text"
                autocomplete="off"
                placeholder="多个子任务使用逗号分隔"
              />
            </div>
            <div class="c_cont">
              <span class="m_title">阻塞处理策略</span>

              <el-select
                v-model="formData.executorBlockStrategy"
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
                v-model="formData.executorFailRetryCount"
                type="text"
                autocomplete="off"
              />
            </div>
          </div>
        </div>
      </div>
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">JSON</div>
        <el-divider style="margin: 8px" />
        <div class="child_main" style="margin-left: 6px">
          <div class="m_left">
            <div class="c_cont" style="justify-content: left">
              <span>增量备份</span>
              <el-radio-group v-model="formData.incrType" disabled>
                <el-radio :value="0" style="margin-top: 5px">全量</el-radio>
                <el-radio :value="1" style="margin-top: 5px">增量</el-radio>
              </el-radio-group>
            </div>
            <div class="c_cont" v-if="formData.incrType == 1">
              <span>默认自增数据</span>
              <el-input
                v-model="formData.incrId"
                style="width: 210px"
                placeholder="Please input"
                v-if="formData.incrColumnType == 0"
                disabled
              />
              <el-date-picker
                v-else
                v-model="formData.incrTime"
                type="datetime"
                placeholder="Select date and time"
                style="width: 210px"
                value-format="x"
                disabled
              />
            </div>

            <div
              class="c_cont"
              v-if="formData.incrType == 1 && formData.incrColumnType == 1"
            >
              <span>时间格式</span>
              <el-select
                v-model="formData.timeFormat"
                filterable
                style="width: 210px"
                disabled
              >
                <el-option
                  label="YYYY/MM/DD hh:mm:ss"
                  value="yyyy/MM/dd hh:mm:ss"
                />
                <el-option
                  label="YYYY-MM-DD hh:mm:ss"
                  value="yyyy-MM-dd hh:mm:ss"
                />
                <el-option label="timestamp" value="timestamp" />
              </el-select>
            </div>
          </div>
          <div class="m_right">
            <div class="c_cont" v-if="formData.incrType == 1">
              <span>自增序列</span>
              <el-select
                v-model="formData.incrColumnType"
                filterable
                style="width: 210px"
                disabled
              >
                <el-option label="主键自增" :value="0" />
                <el-option label="时间自增" :value="1" />
              </el-select>
            </div>
            <div class="c_cont" v-if="formData.incrType == 1">
              <span>默认参数</span>
              <el-input
                v-model="formData.incrParam"
                style="width: 210px"
                placeholder="Please input"
                disabled
              />
            </div>
            <div class="c_cont" v-if="formData.incrColumnType == 1">
              <span>&nbsp;</span>
            </div>
          </div>
        </div>
      </div>
      <div class="child_form">
        <div style="color: #8e8e8e; font-size: 14px">Json</div>
        <el-divider style="margin: 8px" />
        <div class="child_main" style="margin-left: 6px">
          <div class="m_left">
            <div class="c_cont">
              <span class="m_title">Json</span>
              <json-editor-vue
                @update:modelValue="updateModel"
                @validationError="editError"
                class="editor"
                v-model="jsonData"
                style="height: 800px; width: 800px"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
    <el-button @click="pre">上一步</el-button>
    <el-button type="primary" @click="submitForm()">保存</el-button>
  </div>
</template>
<script setup lang="ts">
import TaskGroupAPI from "@/api/task/task-group";
import TaskInfoAPI from "@/api/task/task-info";
//当前使用的页面引入
import NoVue3Cron from "@/components/NoVue3Cron/index.vue";

const emit = defineEmits(["pre"]);

const props = defineProps({
  finalJson: String,
  preData: Object,
  preFormData: Object,
});

const taskGroupList = ref([]);
const formData = ref({});
const jsonData = ref(null);

watch(
  () => props.finalJson,
  (val) => {
    if (val != null && val != "") {
      formData.value.executorParam = val;
      jsonData.value = JSON.parse(val);
    }
  },
  { immediate: true }
);

watch(
  () => props.preFormData,
  (val) => {
    formData.value = val;
    console.log(val);
  },
  { immediate: true, deep: true }
);

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

function pre() {
  emit("pre", props.preData, formData.value);
}

function updateModel(val: any) {
  jsonData.value = val;
}

const errors = ref(0);
// 错误行数
const line = ref();

function editError(a: any, e: any) {
  errors.value = e.length;
  if (e[0]) {
    line.value = e[0].line;
  }
}

async function fetchTaskGroupList() {
  const data = await TaskGroupAPI.getAllTaskGroupList();
  taskGroupList.value = data as any;
}

function changeCron(cron: string) {
  formData.value.scheduleConf = cron;
}

function submitForm() {
  formData.value.glueType = "DATAX";
  formData.value.executorHandler = "runDataxHandler";
  formData.value.executorParam = JSON.stringify(jsonData.value);
  const id = formData.value.id;
  if (id) {
    // if (taskInfoVisible.isCopy) {
    //   formData.id = undefined;
    //   TaskInfoAPI.add(formData)
    //     .then(() => {
    //       ElMessage.success("新增成功");
    //       handleCloseDialog();
    //       emit("handleResetQuery");
    //     })
    //     .finally(() => {});
    // } else {
    TaskInfoAPI.update(id, formData.value)
      .then(() => {
        ElMessage.success("修改成功");
        handleCloseDialog();
        emit("handleReset");
      })
      .finally(() => {});
    //}
  } else {
    TaskInfoAPI.add(formData.value)
      .then(() => {
        ElMessage.success("新增成功");
        handleCloseDialog();
        emit("handleReset");
      })
      .finally(() => {});
  }
}

/** 关闭task_group弹窗 */
function handleCloseDialog() {
  formData.value = {};
  cronPopover.value = false;
}

onMounted(() => {
  fetchTaskGroupList();
});
</script>
<style lang="scss" scoped>
.m_title::after {
  content: "*";
  color: red;
  font-size: 16px;
}

.info_form {
  width: 50%;
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
