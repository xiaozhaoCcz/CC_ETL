<template>
  <div class="app-container">
    <el-dialog
      v-model="taskInfoVisible.visible"
      :title="taskInfoVisible.title"
      width="1080px"
      :before-close="handleCloseDialog"
    >
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
              <div class="c_cont">
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
                <el-input
                  v-model="formData.scheduleConf"
                  placeholder="默认秒"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div style="color: #8e8e8e; font-size: 14px">任务配置</div>
          <el-divider style="margin: 8px" />
          <div class="child_main" style="margin-left: 6px">
            <div class="m_left">
              <div class="c_cont">
                <span class="m_title">运行模式</span>
                <el-select
                  v-model="formData.glueType"
                  filterable
                  placeholder="选择运行模式"
                  style="width: 210px"
                  @change="handleChangeGlueType"
                >
                  <el-option
                    v-for="item in glueTypeList"
                    :key="item.type"
                    :label="item.title"
                    :value="item.type"
                  />
                </el-select>
              </div>
            </div>
            <div class="m_right">
              <div class="c_cont" v-if="formData.glueType == 'BEAN'">
                <span class="m_title">JobHandler</span>
                <el-input
                  v-model="formData.executorHandler"
                  type="text"
                  autocomplete="off"
                />
              </div>
              <div class="c_cont" v-if="formData.glueType == 'API'">
                <span class="m_title">请求类型</span>
                <el-select
                  v-model="formData.reqType"
                  filterable
                  placeholder="Select"
                  style="width: 210px"
                >
                  <el-option key="GET" label="GET" value="GET" />
                  <el-option key="POST" label="POST" value="POST" />
                </el-select>
              </div>
              <div class="c_cont" v-if="formData.glueType == 'SQL'">
                <span class="m_title">数据库</span>
                <el-select
                  v-model="formData.jdbcDatasourceId"
                  filterable
                  placeholder="Select"
                  style="width: 210px"
                >
                  <el-option
                    v-for="item in jdbcDatasourceList"
                    :key="item.id"
                    :label="item.databaseName"
                    :value="item.id"
                  />
                </el-select>
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont_param" v-if="formData.glueType === 'API'">
                <span class="m_title">请求地址</span>
                <el-input
                  v-model="formData.reqUrl"
                  type="textarea"
                  autocomplete="off"
                />
              </div>
            </div>
          </div>
        </div>
        <div
          class="child_form"
          v-if="formData.glueType === 'API'"
          style="margin-bottom: 10px"
        >
          <div class="child_main">
            <div class="m_left">
              <div class="c_cont_param">
                <span>请求头</span>
                <!-- 可编辑表格 -->
                <EditTable
                  :list="
                    formData.reqHeader == null
                      ? []
                      : JSON.parse(formData.reqHeader)
                  "
                  @handleTableData="handleTableData"
                  style="width: 720px"
                />
              </div>
            </div>
          </div>
        </div>
        <div class="child_form">
          <div class="child_main">
            <div class="m_left">
              <div
                class="c_cont_param"
                v-if="
                  formData.glueType === 'API' && formData.reqType === 'POST'
                "
              >
                <span>body</span>
                <el-input
                  v-model="formData.reqBody"
                  type="textarea"
                  autocomplete="off"
                />
              </div>

              <div
                class="c_cont_param"
                v-if="!['API', 'DATAX'].includes(formData.glueType)"
              >
                <span v-if="formData.glueType == 'SQL'">SQL</span>
                <span v-else>任务参数</span>
                <el-input
                  v-model="formData.executorParam"
                  type="textarea"
                  autocomplete="off"
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

        <div class="child_form" v-if="formData.glueType == 'DATAX'">
          <div style="color: #8e8e8e; font-size: 14px">JSON</div>
          <el-divider style="margin: 8px" />
          <div class="child_main" style="margin-left: 6px">
            <div class="m_left">
              <div class="c_cont" style="justify-content: left">
                <span>增量备份</span>
                <el-radio-group v-model="formData.incrType">
                  <el-radio :value="0" style="margin-top: 5px">全量</el-radio>
                  <el-radio :value="1" style="margin-top: 5px">增量</el-radio>
                </el-radio-group>
              </div>
              <div class="c_cont" v-if="formData.incrType == 1">
                <span>默认自增数据</span>
                <IncrEditTable
                  :list="
                  formData.incrContent == null
                    ? []
                    : JSON.parse(formData.incrContent)
                "
                  @handleTableData="handleTableData2"
                ></IncrEditTable>
              </div>
            </div>
          </div>
        </div>

        <div class="child_form" v-if="formData.glueType == 'DATAX'">
          <div style="color: #8e8e8e; font-size: 14px">JSON</div>
          <el-divider style="margin: 8px" />
          <div class="child_main" style="margin-left: 6px">
            <div class="m_left">
              <div class="c_cont">
                <span class="m_title">JSON</span>
                <json-editor-vue
                  @update:modelValue="updateModel"
                  @validationError="editError"
                  class="editor"
                  v-model="jsonData"
                  style="height: 600px; width: 600px"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button type="primary" @click="submitForm()">保存</el-button>
        <el-button @click="handleCloseDialog()">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup lang="ts">
import JobGroupAPI from "@/api/task/job-group";
import JobInfoAPI from "@/api/task/job-info";
//当前使用的页面引入
import NoVue3Cron from "@/components/NoVue3Cron/index.vue";
import EditTable from "@/components/EditTable/EditTable.vue";
import JobJdbcDatasourceAPI from "@/api/task/job-jdbc-datasource";
import IncrEditTable from "@/views/task/job-datax/componects/IncrEditTable.vue";

const emit = defineEmits(["close", "handleResetQuery"]);

const props = defineProps({
  taskInfoVisible: {
    type: Object,
    default: null,
  },
  formData: {
    type: Object,
    default: null,
  },
});

const taskGroupList = ref([]);
const jdbcDatasourceList = ref([]);
const jsonData = ref(null);

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

const scheduleTypeList = [
  {
    type: "CRON",
    title: "CRON",
  },
  { type: "NONE", title: "无" },
  { type: "FIX_RATE", title: "固定速度" },
];
const glueTypeList = [
  {
    type: "BEAN",
    title: "BEAN",
  },
  {
    type: "API",
    title: "API",
  },
  {
    type: "SQL",
    title: "SQL",
  },
  {
    type: "DATAX",
    title: "DATAX",
  },
  {
    type: "GLUE_GROOVY",
    title: "GLUE(Java)",
  },
  {
    type: "GLUE_SHELL",
    title: "GLUE(Shell)",
  },
  {
    type: "GLUE_PYTHON",
    title: "GLUE(Python)",
  },
  {
    type: "GLUE_PHP",
    title: "GLUE(PHP)",
  },
  {
    type: "GLUE_NODEJS",
    title: "GLUE(Nodejs)",
  },
  {
    type: "GLUE_POWERSHELL",
    title: "GLUE(PowerShell)",
  },
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

function handleTableData(val) {
  props.formData.reqHeader = JSON.stringify(val);
}

function handleTableData2(val) {
  props.formData.incrContent = JSON.stringify(val);
}

watch(
  () => props.formData,
  (data) => {
    if (data.id && data.glueType == "DATAX") {
      jsonData.value = JSON.parse(data.executorParam);
    }
  },
  {
    immediate: true,
    deep: true,
  }
);

watch(
  () => props.taskInfoVisible,
  () => {}
);

function handleChangeGlueType() {
  props.formData.executorHandler = "";
  props.formData.reqType = "";
  props.formData.reqUrl = "";
  props.formData.reqHeader = null;
  props.formData.reqBody = "";
  if (props.formData.glueType == "DATAX") {
    props.formData.executorParam = JSON.stringify(jsonData.value);
  } else {
    props.formData.executorParam = "";
  }
}

async function fetchTaskGroupList() {
  const data = await JobGroupAPI.getAllJobGroupList();
  taskGroupList.value = data as any;
}

async function fetchJdbcDatasource() {
  const data = await JobJdbcDatasourceAPI.getJdbcDatasourceList();
  jdbcDatasourceList.value = data as any;
}

function changeCron(cron: string) {
  props.formData.scheduleConf = cron;
}

function submitForm() {
  if (props.formData.glueType == "SQL") {
    props.formData.executorHandler = "runJobJdbcXxlJob";
  }
  if (props.formData.glueType == "DATAX") {
    props.formData.executorHandler = "runDataxHandler";
    props.formData.executorParam = JSON.stringify(jsonData.value);
  }
  if (props.formData.glueType == "API") {
    props.formData.executorHandler = "runApiHandler";
  }
  const id = props.formData.id;
  if (id) {
    if (props.taskInfoVisible.isCopy) {
      props.formData.id = undefined;
      JobInfoAPI.add(props.formData)
        .then(() => {
          ElMessage.success("新增成功");
          handleCloseDialog();
          emit("handleResetQuery");
        })
        .finally(() => {});
    } else {
      JobInfoAPI.update(id, props.formData)
        .then(() => {
          ElMessage.success("修改成功");
          handleCloseDialog();
          emit("handleResetQuery");
        })
        .finally(() => {});
    }
  } else {
    JobInfoAPI.add(props.formData)
      .then(() => {
        ElMessage.success("新增成功");
        handleCloseDialog();
        emit("handleResetQuery");
      })
      .finally(() => {});
  }
}

/** 关闭task_group弹窗 */
function handleCloseDialog() {
  cronPopover.value = false;
  emit("close");
}

onMounted(() => {
  fetchTaskGroupList();
  fetchJdbcDatasource();
});
</script>
<style lang="scss" scoped>
.m_title::after {
  content: "*";
  color: red;
  font-size: 16px;
}

.info_form {
  width: 100%;

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
      top: -100px;
      left: 300px;
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
