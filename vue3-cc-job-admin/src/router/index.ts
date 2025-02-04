import type { App } from "vue";
import {
  createRouter,
  createWebHashHistory,
  type RouteRecordRaw,
} from "vue-router";

export const Layout = () => import("@/layout/index.vue");

// 静态路由
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: "/redirect",
    component: Layout,
    meta: { hidden: true },
    children: [
      {
        path: "/redirect/:path(.*)",
        component: () => import("@/views/redirect/index.vue"),
      },
    ],
  },

  {
    path: "/login",
    component: () => import("@/views/login/index.vue"),
    meta: { hidden: true },
  },

  {
    path: "/",
    name: "/",
    component: Layout,
    redirect: "/dashboard",
    children: [
      {
        path: "dashboard",
        component: () => import("@/views/dashboard/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "Dashboard",
        meta: {
          title: "dashboard",
          icon: "homepage",
          affix: true,
          keepAlive: true,
        },
      },
      {
        path: "401",
        component: () => import("@/views/error/401.vue"),
        meta: { hidden: true },
      },
      {
        path: "404",
        component: () => import("@/views/error/404.vue"),
        meta: { hidden: true },
      },
    ],
  },
  {
    path: "/job",
    name: "job",
    component: Layout,
    redirect: "/job-info",
    meta: {
      title: "任务管理",
      icon: "system",
    },
    children: [
      {
        path: "job-info",
        component: () => import("@/views/task/job-info/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobInfo",
        meta: {
          title: "任务管理",
          icon: "menu",
        },
      },
      {
        path: "job-group",
        component: () => import("@/views/task/job-group/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobGroup",
        meta: {
          title: "执行器管理",
          icon: "fullscreen",
        },
      },
      {
        path: "job-jdbc-datasource",
        component: () => import("@/views/task/job-jdbc-datasource/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobJdbcDatasource",
        meta: {
          title: "数据源管理",
          icon: "menu",
        },
      },

      {
        path: "job-datax",
        component: () => import("@/views/task/job-datax/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobDatax",
        meta: {
          title: "数据源同步",
          icon: "close_other",
        },
      },
      {
        path: "job-platform",
        component: () => import("@/views/task/job-platform/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobPlatform",
        meta: {
          title: "任务编排",
          icon: "document",
        },
      },
      // {
      //   path: "task-rank",
      //   component: () => import("@/views/task/task-rank/index.vue"),
      //   // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
      //   // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
      //   name: "taskRank",
      //   meta: {
      //     title: "任务编排",
      //     icon: "cascader",
      //   },
      // },
      {
        path: "job-log",
        component: () => import("@/views/task/job-log/index.vue"),
        // 用于 keep-alive 功能，需要与 SFC 中自动推导或显式声明的组件名称一致
        // 参考文档: https://cn.vuejs.org/guide/built-ins/keep-alive.html#include-exclude
        name: "jobLog",
        meta: {
          title: "任务日志",
          icon: "document",
        },
      },

    ],
  },
];

/**
 * 创建路由
 */
const router = createRouter({
  history: createWebHashHistory(),
  routes: constantRoutes,
  // 刷新时，滚动条位置还原
  scrollBehavior: () => ({ left: 0, top: 0 }),
});

// 全局注册 router
export function setupRouter(app: App<Element>) {
  app.use(router);
}

export default router;
