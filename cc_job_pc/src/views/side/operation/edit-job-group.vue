<template>
  <div class="job-group-container">
    <el-dialog 
      v-model="jobGroupVisible.visible"
      :title="jobGroupVisible.title"
      :before-close="handleCloseDialog"
      draggable
      class="job-group-dialog"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      append-to-body
      data-resize-options='{"minWidth":720,"maxWidth":"60%","minHeight":800,"maxHeight":"100vh","width":720}'
    >
      <div class="form-container">
        <!-- 基础配置 -->
        <div class="form-section">
          <div class="section-header">
            <div class="section-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <div class="section-title">基础配置</div>
          </div>
          <div class="section-content">
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">执行器</label>
                <el-select
                  v-model="formData.jobGroup"
                  filterable
                  placeholder="请选择执行器"
                  class="form-control"
                >
                  <el-option
                    v-for="item in jobGroupList"
                    :key="item.id"
                    :label="item.title"
                    :value="item.id"
                  />
                </el-select>
              </div>
              <div class="form-item">
                <label class="form-label required">负责人</label>
                <el-input
                  v-model="formData.author"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入负责人"
                  class="form-control"
                />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">任务描述</label>
                <el-input
                  v-model="formData.jobDesc"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入任务描述"
                  class="form-control"
                />
              </div>
              <div class="form-item">
                <label class="form-label">报警邮件</label>
                <el-input
                  v-model="formData.alarmEmail"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入报警邮件地址"
                  class="form-control"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 调度配置 -->
        <div class="form-section">
          <div class="section-header">
            <div class="section-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="section-title">调度配置</div>
          </div>
          <div class="section-content">
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度类型</label>
                <el-select
                  v-model="formData.scheduleType"
                  filterable
                  placeholder="请选择调度类型"
                  class="form-control"
                >
                  <el-option
                    v-for="item in scheduleTypeList"
                    :key="item.type"
                    :label="item.title"
                    :value="item.type"
                  />
                </el-select>
              </div>
              <div class="form-item" v-if="formData.scheduleType === 'CRON'">
                <label class="form-label required">CRON表达式</label>
                <div class="cron-input-wrapper">
                  <el-input
                    v-model="formData.scheduleConf"
                    placeholder="请输入cron表达式..."
                    class="form-control"
                    readonly
                  >
                    <template #append>
                      <el-button
                        @click="handleCronButtonClick"
                        type="primary"
                        size="small"
                      >
                        <el-icon><Edit /></el-icon>
                        设置
                      </el-button>
                    </template>
                  </el-input>
                  <div v-show="cronPopover" class="cron-popover">
                    <div class="cron-popover-header">
                      <span>CRON表达式设置</span>
                      <el-button
                        type="text"
                        @click="cronPopover = false"
                        class="close-btn"
                      >
                        <el-icon><Close /></el-icon>
                      </el-button>
                    </div>
                    <div class="cron-popover-content">
                      <noVue3Cron
                        :cron-value="formData.scheduleConf"
                        i18n="cn"
                        @change="changeCron"
                        @close="cronPopover = false"
                      />
                    </div>
                  </div>
                </div>
              </div>
              <div class="form-item" v-if="formData.scheduleType === 'FIX_RATE'">
                <label class="form-label required">固定速度</label>
                <el-input
                  v-model="formData.scheduleConf"
                  placeholder="请输入间隔时间（秒）"
                  class="form-control"
                />
              </div>
            </div>
          </div>
        </div>

        <!-- 高级配置 -->
        <div class="form-section">
          <div class="section-header">
            <div class="section-icon">
              <el-icon><Tools /></el-icon>
            </div>
            <div class="section-title">高级配置</div>
          </div>
          <div class="section-content">
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">路由策略</label>
                <el-select
                  v-model="formData.executorRouteStrategy"
                  filterable
                  placeholder="请选择路由策略"
                  class="form-control"
                >
                  <el-option
                    v-for="item in routeStrategyList"
                    :key="item.type"
                    :label="item.title"
                    :value="item.type"
                  />
                </el-select>
              </div>
              <div class="form-item">
                <label class="form-label">子任务ID</label>
                <el-input
                  v-model="formData.childJobid"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入子任务ID"
                  class="form-control"
                />
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label required">调度过期策略</label>
                <el-select
                  v-model="formData.misfireStrategy"
                  filterable
                  placeholder="请选择过期策略"
                  class="form-control"
                >
                  <el-option
                    v-for="item in misfireStrategyList"
                    :key="item.type"
                    :label="item.title"
                    :value="item.type"
                  />
                </el-select>
              </div>
              <div class="form-item">
                <label class="form-label required">阻塞处理策略</label>
                <el-select
                  v-model="formData.executorBlockStrategy"
                  filterable
                  placeholder="请选择阻塞策略"
                  class="form-control"
                >
                  <el-option
                    v-for="item in blockStrategyList"
                    :key="item.type"
                    :label="item.title"
                    :value="item.type"
                  />
                </el-select>
              </div>
            </div>
            <div class="form-row">
              <div class="form-item">
                <label class="form-label">任务超时时间(秒)</label>
                <el-input
                  v-model="formData.executorTimeout"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入超时时间（秒）"
                  class="form-control"
                />
              </div>
              <div class="form-item">
                <label class="form-label">失败重试次数</label>
                <el-input
                  v-model="formData.executorFailRetryCount"
                  type="text"
                  autocomplete="off"
                  placeholder="请输入重试次数"
                  class="form-control"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCloseDialog()" class="cancel-btn">
            <el-icon><Close /></el-icon>
            取消
          </el-button>
          <el-button type="primary" @click="submitForm()" class="submit-btn">
            <el-icon><Check /></el-icon>
            保存
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import JobGroupAPI from "@/api/job-group";
import JobInfoAPI from "@/api/job-info";
import NoVue3Cron from "@/components/NoVue3Cron/index.vue";
import { onMounted, ref, watch, nextTick } from "vue";
import { ElMessage } from "element-plus";
import { useJobInfoStoreHook } from "@/store/modules/jobInfo";
import { Setting, Clock, Tools, Edit, Check, Close } from "@element-plus/icons-vue";

// 定义接口类型
interface JobGroupItem {
  id: number;
  title: string;
}

const emit = defineEmits(["close"]);

const props = defineProps({
  jobGroupVisible: {
    type: Object,
    default: null,
  },
  formData: {
    type: Object,
    default: null,
  },
});

const jobGroupList = ref<JobGroupItem[]>([]);

const scheduleTypeList = [
  { type: "CRON", title: "CRON" },
  { type: "NONE", title: "无" },
  { type: "FIX_RATE", title: "固定速度" },
];

const routeStrategyList = [
  { type: "FIRST", title: "第一个" },
  { type: "LAST", title: "最后一个" },
  { type: "ROUND", title: "轮询" },
  { type: "RANDOM", title: "随机" },
  { type: "CONSISTENT_HASH", title: "一致性哈希" },
  { type: "LEASTY_FREQUENTY_USED", title: "最不经常使用" },
  { type: "LEASTY_RECENTLY_USED", title: "最近最久未使用" },
  { type: "FAILOVER", title: "故障转移" },
  { type: "BUSYOVER", title: "忙碌转移" },
  { type: "SHARDING_BORADCAST", title: "分片广播" },
];

const misfireStrategyList = [
  { type: "DO_NOTHING", title: "忽略" },
  { type: "FIRE_ONCE_NOW", title: "立即执行一次" },
];

const blockStrategyList = [
  { type: "SERIAL_EXECUTION", title: "单机串行" },
  { type: "DISCARD_LATER", title: "丢弃后续调度" },
  { type: "COVER_EARLY", title: "覆盖之前调度" },
];

const cronPopover = ref(false);

// 添加点击外部关闭CRON弹出层的功能
const handleClickOutside = (event: Event) => {
  const target = event.target as HTMLElement;
  if (cronPopover.value && !target.closest(".cron-input-wrapper")) {
    cronPopover.value = false;
  }
};

// 动态计算CRON弹出层位置
const calculateCronPosition = () => {
  if (!cronPopover.value) return;

  nextTick(() => {
    const inputElement = document.querySelector(
      ".cron-input-wrapper input"
    ) as HTMLElement;
    const popoverElement = document.querySelector(".cron-popover") as HTMLElement;

    if (!inputElement || !popoverElement) return;

    const inputRect = inputElement.getBoundingClientRect();
    const popoverHeight = 500; // 预估高度
    const windowHeight = window.innerHeight;
    const spaceBelow = windowHeight - inputRect.bottom;
    const spaceAbove = inputRect.top;

    // 计算最佳位置
    if (spaceBelow >= popoverHeight || spaceBelow > spaceAbove) {
      // 显示在下方
      popoverElement.style.top = `${inputRect.bottom + 8}px`;
      popoverElement.style.left = `${inputRect.left}px`;
      popoverElement.style.right = `${window.innerWidth - inputRect.right}px`;
      popoverElement.classList.remove("position-top");
      popoverElement.classList.add("position-bottom");
    } else {
      // 显示在上方
      popoverElement.style.bottom = `${windowHeight - inputRect.top + 8}px`;
      popoverElement.style.left = `${inputRect.left}px`;
      popoverElement.style.right = `${window.innerWidth - inputRect.right}px`;
      popoverElement.classList.remove("position-bottom");
      popoverElement.classList.add("position-top");
    }
  });
};

// 监听点击事件
watch(cronPopover, (newVal) => {
  if (newVal) {
    nextTick(() => {
      document.addEventListener("click", handleClickOutside);
      window.addEventListener("resize", calculateCronPosition);
      calculateCronPosition();
    });
  } else {
    document.removeEventListener("click", handleClickOutside);
    window.removeEventListener("resize", calculateCronPosition);
  }
});

watch(
  () => props.jobGroupVisible,
  () => {}
);

async function fetchTaskGroupList() {
  try {
    const response = await JobGroupAPI.getAllJobGroupList();
    // 处理不同类型的响应格式
    if (response && typeof response === "object") {
      if ("data" in response) {
        // AxiosResponse格式
        jobGroupList.value = (response as any).data || [];
      } else if (Array.isArray(response)) {
        // 直接数组格式
        jobGroupList.value = response;
      } else {
        // 其他格式，尝试提取数据
        jobGroupList.value = (response as any).data || (response as any).result || [];
      }
    } else {
      jobGroupList.value = [];
    }
  } catch (error) {
    console.error("获取执行器列表失败:", error);
    ElMessage.error("获取执行器列表失败");
    jobGroupList.value = [];
  }
}

function changeCron(cron: string) {
  props.formData.scheduleConf = cron;
}

// 处理CRON设置按钮点击
function handleCronButtonClick() {
  console.log("CRON设置按钮被点击");
  cronPopover.value = !cronPopover.value;
}

function submitForm() {
  const id = props.formData.id;
  props.formData.glueType = "BEAN";
  props.formData.executorHandler = "runJobGroupXxlJob";

  if (id) {
    JobInfoAPI.updateJobCompose(id, props.formData)
      .then(() => {
        ElMessage.success("修改成功");
        useJobInfoStoreHook().triggerTreeRefresh();
        handleCloseDialog();
      })
      .catch((error) => {
        console.error("修改失败:", error);
        ElMessage.error("修改失败");
      });
  } else {
    JobInfoAPI.saveJobCompose(props.formData)
      .then(() => {
        ElMessage.success("新增成功");
        useJobInfoStoreHook().triggerTreeRefresh();
        handleCloseDialog();
      })
      .catch((error) => {
        console.error("新增失败:", error);
        ElMessage.error("新增失败");
      });
  }
}

function handleCloseDialog() {
  cronPopover.value = false;
  document.removeEventListener("click", handleClickOutside);
  window.removeEventListener("resize", calculateCronPosition);
  emit("close");
}

onMounted(() => {
  fetchTaskGroupList();
});
</script>

<style lang="scss" scoped>
// 全局样式变量
.job-group-container {
  --primary-color: #409eff;
  --success-color: #67c23a;
  --warning-color: #e6a23c;
  --danger-color: #f56c6c;
  --info-color: #909399;
  --border-color: #dcdfe6;
  --background-color: #f5f7fa;
  --text-color: #303133;
  --text-color-secondary: #606266;
  --border-radius: 8px;
  --box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  :deep(.el-dialog__header) {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    border-radius: var(--border-radius) var(--border-radius) 0 0;
    padding: 20px 24px;

    .el-dialog__title {
      font-size: 18px;
      font-weight: 600;
    }

    .el-dialog__headerbtn {
      .el-dialog__close {
        color: white;
        font-size: 18px;

        &:hover {
          color: #f0f0f0;
        }
      }
    }
  }

  :deep(.el-dialog__body) {
    max-height: 65vh;
    overflow-y: auto;
    padding: 0;
  }

  :deep(.el-dialog__footer) {
    padding: 0;
    border-top: 1px solid var(--border-color);
  }
}

.form-container {
  padding: 16px 16px 0 16px;
  background: #fff;
  /* 去掉max-height和overflow-y，内容自适应 */
}

.form-section {
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 20px rgba(102, 126, 234, 0.12);
  }

  &:last-child {
    margin-bottom: 0;
  }
}

.section-header {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-bottom: 1px solid #e4e7ed;
  border-radius: 6px 6px 0 0;

  .section-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 50%;
    margin-right: 8px;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.15);
    border: 2px solid #fff;

    .el-icon {
      color: white;
      font-size: 16px;
    }
  }

  .section-title {
    font-size: 15px;
    font-weight: 600;
    color: #333;
    letter-spacing: 1px;
  }
}

.section-content {
  padding: 8px 12px;
  background: #fff;
}

.form-row {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;

  &:last-child {
    margin-bottom: 0;
  }

  @media (max-width: 768px) {
    flex-direction: column;
    gap: 16px;
  }
}

.form-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.form-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-color);
  line-height: 1.4;

  &.required::after {
    content: " *";
    color: #f56c6c; // 更鲜明的红色
    font-weight: bold;
    font-size: 16px; // 比正文略大
    vertical-align: middle;
    margin-left: 2px;
  }
}

.form-control {
  :deep(.el-input__wrapper) {
    border-radius: var(--border-radius);
    box-shadow: 0 0 0 1px var(--border-color);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 0 0 1px var(--primary-color);
    }

    &.is-focus {
      box-shadow: 0 0 0 1px var(--primary-color);
    }
  }

  :deep(.el-input__inner) {
    font-size: 12px;
    color: var(--text-color);

    &::placeholder {
      color: var(--text-color-secondary);
    }
  }
}

.cron-input-wrapper {
  position: relative;

  .cron-popover {
    position: fixed;
    z-index: 9999;
    background: white;
    border-radius: var(--border-radius);
    box-shadow: var(--box-shadow);
    border: 1px solid var(--border-color);
    min-width: 400px;
    max-width: 600px;

    &.position-top {
      bottom: calc(100% + 8px);
      left: 0;
      right: 0;

      &::before {
        top: auto;
        bottom: -6px;
        border-top: 6px solid white;
        border-bottom: none;
      }

      &::after {
        top: auto;
        bottom: -7px;
        border-top: 6px solid var(--border-color);
        border-bottom: none;
      }
    }

    &.position-bottom {
      top: calc(100% + 8px);
      left: 0;
      right: 0;

      &::before {
        top: -6px;
        border-bottom: 6px solid white;
      }

      &::after {
        top: -7px;
        border-bottom: 6px solid var(--border-color);
      }
    }

    .cron-popover-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      background: #f8f9fa;
      border-bottom: 1px solid var(--border-color);
      border-radius: var(--border-radius) var(--border-radius) 0 0;

      span {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-color);
      }

      .close-btn {
        padding: 4px;
        color: var(--text-color-secondary);

        &:hover {
          color: var(--danger-color);
        }
      }
    }

    .cron-popover-content {
      padding: 16px;
      max-height: 500px;
      overflow-y: auto;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: #f1f1f1;
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: #c1c1c1;
        border-radius: 3px;

        &:hover {
          background: #a8a8a8;
        }
      }
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 10px 16px;
  background: #fafafa;

  .cancel-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 7px 16px;
    border-radius: var(--border-radius);
    font-weight: 500;
    transition: all 0.3s ease;

    &:hover {
      background: #f5f5f5;
      border-color: #d9d9d9;
    }
  }

  .submit-btn {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 7px 16px;
    border-radius: var(--border-radius);
    font-weight: 500;
    background: linear-gradient(135deg, var(--primary-color) 0%, #66b1ff 100%);
    border: none;
    color: rgba(64, 158, 255);
    transition: all 0.3s ease;

    :deep(.el-button) {
      color: rgba(64, 158, 255);
    }

    :deep(.el-button__text) {
      color: rgba(64, 158, 255);
    }

    &:hover {
      background: linear-gradient(135deg, #66b1ff 0%, var(--primary-color) 100%);
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(64, 158, 255);
      color: rgba(64, 158, 255);

      :deep(.el-button) {
        color: rgba(64, 158, 255);
      }

      :deep(.el-button__text) {
        color: rgba(64, 158, 255);
      }
    }

    &:active {
      transform: translateY(0);
    }
  }
}

// 响应式设计
@media (max-width: 768px) {
  .job-group-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
      margin: 5vh auto;
    }
  }

  .form-container {
    padding: 16px;
  }

  .section-content {
    padding: 16px;
  }

  .dialog-footer {
    padding: 12px 16px;
    flex-direction: column;

    .cancel-btn,
    .submit-btn {
      width: 100%;
      justify-content: center;
    }
  }
}

// 动画效果
.form-section {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

// 加载状态
.form-control {
  :deep(.el-input__wrapper.is-loading) {
    background: #fafafa;
  }
}

// 错误状态
.form-control {
  :deep(.el-input__wrapper.is-error) {
    box-shadow: 0 0 0 1px var(--danger-color);
  }
}
</style>
