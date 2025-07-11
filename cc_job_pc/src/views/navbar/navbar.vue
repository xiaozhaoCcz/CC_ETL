<script setup lang="ts">
import {
  Back,
  CaretRight,
  Document,
  Grid,
  Minus,
  Plus,
  Right,
  CaretBottom,
  VideoPause,
  Download,
  Upload,
  Refresh,
  FullScreen,
  Setting,
  Monitor,
  Connection,
  Operation,
  Crop,
} from "@element-plus/icons-vue";
import { useNavbarStoreHook, usePageStoreHook } from "@/store";
import { ElMessage } from "element-plus";

async function updateRunStatus(status: boolean) {
  const currentPageId = usePageStoreHook().getCurrentPage();

  if (!currentPageId) {
    ElMessage.warning("请先选择一个任务组");
    return;
  }

  // 更新当前任务组的运行状态
  usePageStoreHook().updatePageRunStatus(currentPageId, status);

  // 通知main组件进行相应的操作（启动或停止任务）
  const actionObj = {
    actionName: status ? "start-current-job" : "stop-current-job",
    nowDate: new Date(),
    pageId: currentPageId,
  };
  useNavbarStoreHook().setAction(actionObj);
}

function handleAction(actionName: string) {
  const actionObj = { actionName: actionName, nowDate: new Date() };
  useNavbarStoreHook().setAction(actionObj);
}

// 处理LogicFlow相关操作
function handleLogicFlowAction(actionName: string) {
  const actionObj = { actionName: actionName, nowDate: new Date() };
  useNavbarStoreHook().setAction(actionObj);
}
</script>

<template>
  <div class="nav-container">
    <div class="nav-top">
      <a href="#" class="nav-top-item">
        <el-icon><Document /></el-icon>
        <span>文件</span>
      </a>
      <a href="#" class="nav-top-item">
        <el-icon><Operation /></el-icon>
        <span>开始</span>
      </a>
      <a href="#" class="nav-top-item">
        <el-icon><Connection /></el-icon>
        <span>任务组</span>
      </a>
    </div>
    <div class="nav-main">
      <div class="nva-m-2">
        <div class="m2-left">
          <div class="m2-file">
            <div class="m2-icon-cont" @click="handleAction('add')">
              <el-icon size="20" color="#409EFF">
                <Document />
              </el-icon>
              <p>新建</p>
            </div>
            <div class="m2-icon-cont" @click="handleAction('open')">
              <el-icon size="20" color="#67C23A">
                <Upload />
              </el-icon>
              <p>打开</p>
            </div>
            <div class="m2-icon-cont" @click="handleAction('save')">
              <el-icon size="20" color="#E6A23C">
                <Download />
              </el-icon>
              <p>保存</p>
            </div>
          </div>
          <div class="m2-step">
            <div class="m2-icon-cont" @click="handleLogicFlowAction('undo')">
              <el-icon size="20" color="#909399">
                <Back />
              </el-icon>
              <p>撤销</p>
            </div>
            <div class="m2-icon-cont" @click="handleLogicFlowAction('redo')">
              <el-icon size="20" color="#909399">
                <Right />
              </el-icon>
              <p>重做</p>
            </div>
            <div class="m2-icon-cont" @click="handleLogicFlowAction('select')">
              <el-icon size="20" color="#409EFF">
                <Crop />
              </el-icon>
              <p>框选</p>
            </div>
          </div>
          <div class="m2-layout layout-op">
            <div class="m2-icon-cont">
              <el-icon size="20" color="#409EFF">
                <Grid />
              </el-icon>
              <p>布局</p>
              <el-icon size="12" color="#909399">
                <CaretBottom />
              </el-icon>
            </div>

            <div class="layout-menu">
              <ul>
                <li @click="handleLogicFlowAction('layout-horizontal')">
                  <a href="#"
                    ><el-icon><Grid /></el-icon>横向布局</a
                  >
                </li>
                <li @click="handleLogicFlowAction('layout-vertical')">
                  <a href="#"
                    ><el-icon><Grid /></el-icon>纵向布局</a
                  >
                </li>
              </ul>
            </div>
          </div>
          <div class="m2-opera">
            <div class="m2-icon-cont" @click="handleLogicFlowAction('fit')">
              <el-icon size="20" color="#67C23A">
                <FullScreen />
              </el-icon>
              <p>适应</p>
            </div>
            <div class="m2-icon-cont" @click="handleLogicFlowAction('zoom-in')">
              <el-icon size="20" color="#E6A23C">
                <Plus />
              </el-icon>
              <p>放大</p>
            </div>
            <div class="m2-icon-cont" @click="handleLogicFlowAction('zoom-out')">
              <el-icon size="20" color="#E6A23C">
                <Minus />
              </el-icon>
              <p>缩小</p>
            </div>
          </div>
        </div>
        <div class="m2-right">
          <div class="m2-run">
            <div
              class="m2-icon-cont run-btn"
              v-if="!usePageStoreHook().getCurrentPageRunStatus()"
              @click="updateRunStatus(true)"
            >
              <el-icon size="20" color="#67C23A">
                <CaretRight />
              </el-icon>
              <p>运行</p>
            </div>
            <div class="m2-icon-cont stop-btn" v-else @click="updateRunStatus(false)">
              <el-icon size="20" color="#F56C6C">
                <VideoPause />
              </el-icon>
              <p>停止</p>
            </div>
            <div class="m2-icon-cont" @click="handleAction('clear')">
              <el-icon size="20" color="#F56C6C">
                <Refresh />
              </el-icon>
              <p>清除</p>
            </div>
            <div class="m2-icon-cont" @click="handleAction('logs')">
              <el-icon size="20" color="#909399">
                <Monitor />
              </el-icon>
              <p>日志</p>
            </div>
            <div class="m2-icon-cont" @click="handleAction('settings')">
              <el-icon size="20" color="#909399">
                <Setting />
              </el-icon>
              <p>设置</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.nav-container {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border-bottom: 1px solid #e1e5e9;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  .nav-top {
    display: flex;
    padding: 8px 16px;
    background: rgba(255, 255, 255, 0.9);
    border-bottom: 1px solid #e1e5e9;

    .nav-top-item {
      display: flex;
      align-items: center;
      margin-right: 24px;
      padding: 6px 12px;
      border-radius: 6px;
      text-decoration: none;
      color: #606266;
      transition: all 0.3s ease;
      font-weight: 500;

      &:hover {
        background: rgba(64, 158, 255, 0.1);
        color: #409eff;
        transform: translateY(-1px);
      }

      .el-icon {
        margin-right: 6px;
        font-size: 16px;
      }

      span {
        font-size: 14px;
      }
    }
  }

  @mixin m2-content() {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .nav-main {
    display: flex;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.95);
    height: 80px;

    .nva-m-2 {
      width: 100%;
      @include m2-content();

      .m2-left {
        display: flex;
        gap: 8px;

        .m2-file {
          display: flex;
          gap: 4px;
          padding-right: 16px;
          border-right: 2px solid #f0f2f5;
        }

        .m2-step {
          display: flex;
          gap: 4px;
          padding: 0 16px;
          border-right: 2px solid #f0f2f5;
        }

        .m2-layout {
          display: flex;
          gap: 4px;
          padding: 0 16px;
          border-right: 2px solid #f0f2f5;
        }

        .layout-op {
          position: relative;

          .layout-menu {
            display: none;
            width: 140px;
            position: absolute;
            background: #ffffff;
            top: 100%;
            left: -16px;
            z-index: 999;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
            border: 1px solid #e1e5e9;
            margin-top: 8px;
            opacity: 0;
            visibility: hidden;
            transition: opacity 0.2s ease, visibility 0.2s ease;

            ul {
              list-style: none;
              padding: 0;
              margin: 0;
            }

            li {
              padding: 0;

              &:first-child {
                border-radius: 8px 8px 0 0;
              }

              &:last-child {
                border-radius: 0 0 8px 8px;
              }

              &:hover {
                background: #f5f7fa;
              }
            }

            a {
              display: flex;
              align-items: center;
              padding: 12px 16px;
              text-decoration: none;
              color: #606266;
              font-size: 14px;
              transition: all 0.2s ease;

              &:hover {
                color: #409eff;
              }

              .el-icon {
                margin-right: 8px;
                font-size: 16px;
              }
            }
          }

          &:hover .layout-menu {
            display: block;
            opacity: 1;
            visibility: visible;
          }

          // 添加一个透明的连接区域，防止鼠标移动时菜单消失
          &::after {
            content: "";
            position: absolute;
            top: 100%;
            left: 0;
            right: 0;
            height: 8px;
            background: transparent;
          }
        }

        .m2-opera {
          display: flex;
          gap: 4px;
          padding-left: 16px;
        }
      }

      .m2-right {
        display: flex;

        .m2-run {
          display: flex;
          gap: 4px;
          padding-left: 16px;
          border-left: 2px solid #f0f2f5;
        }
      }
    }

    .m2-icon-cont {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 8px 12px;
      border-radius: 8px;
      transition: all 0.3s ease;
      cursor: pointer;
      min-width: 60px;
      position: relative;
      overflow: hidden;

      &::before {
        content: "";
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(
          135deg,
          rgba(64, 158, 255, 0.1) 0%,
          rgba(103, 194, 58, 0.1) 100%
        );
        opacity: 0;
        transition: opacity 0.3s ease;
      }

      p {
        margin: 4px 0 0 0;
        font-size: 12px;
        font-weight: 500;
        color: #606266;
        transition: color 0.3s ease;
      }

      .el-icon {
        transition: transform 0.3s ease;
      }

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

        &::before {
          opacity: 1;
        }

        p {
          color: #303133;
        }

        .el-icon {
          transform: scale(1.1);
        }
      }

      &:active {
        transform: translateY(0);
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
      }

      &.run-btn {
        background: linear-gradient(
          135deg,
          rgba(103, 194, 58, 0.1) 0%,
          rgba(103, 194, 58, 0.2) 100%
        );
        border: 1px solid rgba(103, 194, 58, 0.3);

        &:hover {
          background: linear-gradient(
            135deg,
            rgba(103, 194, 58, 0.2) 0%,
            rgba(103, 194, 58, 0.3) 100%
          );
          border-color: rgba(103, 194, 58, 0.5);
        }
      }

      &.stop-btn {
        background: linear-gradient(
          135deg,
          rgba(245, 108, 108, 0.1) 0%,
          rgba(245, 108, 108, 0.2) 100%
        );
        border: 1px solid rgba(245, 108, 108, 0.3);

        &:hover {
          background: linear-gradient(
            135deg,
            rgba(245, 108, 108, 0.2) 0%,
            rgba(245, 108, 108, 0.3) 100%
          );
          border-color: rgba(245, 108, 108, 0.5);
        }
      }
    }
  }
}
</style>
