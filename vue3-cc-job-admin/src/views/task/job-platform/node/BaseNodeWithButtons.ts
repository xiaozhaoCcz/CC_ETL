import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";

export class BaseButtonNodeView extends RectResize.view {
getButtonGroup() {
  const { model } = this.props;
  const { x, y, width, height } = model;

  // 定义提示文本信息
  const tooltips = {
    copy: "复制",
    "task-edit": "选择任务",
    edit: "编辑",
    delete: "删除",
    "toggle-status": model.isPause ? "运行" : "暂停"
  };
  
  return h(
    "g",
    {
      style: {
        opacity: model.buttonGroupOpacity,
        transition: "opacity 0.3s ease",
      },
    },
    [
      h("rect", {
        x: x - width / 2,
        y: y - height / 2 - 40,
        width: width,
        height: 40,
        fill: "transparent",
      }),
    ],
    [
      // 复制图标
      this.createButtonWithTooltip(x - width / 2 + width - 110, y - height / 2 - 20, "copy", tooltips.copy),
      // 选择任务图标
      this.createButtonWithTooltip(x - width / 2 + width - 90, y - height / 2 - 20, "task-edit", tooltips["task-edit"]),
      // 编辑节点图标
      this.createButtonWithTooltip(x - width / 2 + width - 70, y - height / 2 - 20, "edit", tooltips.edit),
      // 删除图标
      this.createButtonWithTooltip(x - width / 2 + width - 50, y - height / 2 - 20, "delete", tooltips.delete),
      // 状态切换按钮
      this.createButtonWithTooltip(x - width / 2 + width - 30, y - height / 2 - 20, "toggle-status", tooltips["toggle-status"]),
    ]
  );
}

// 创建带有提示的按钮方法
// 创建带有提示的按钮方法
createButtonWithTooltip(posX, posY, action, tooltipText) {
  // SVG路径和样式配置
  const iconConfig = {
    "copy": {
      viewBox: "0 0 448 512",
      path: "M208 0L332.1 0c12.7 0 24.9 5.1 33.9 14.1l67.9 67.9c9 9 14.1 21.2 14.1 33.9L448 336c0 26.5-21.5 48-48 48l-192 0c-26.5 0-48-21.5-48-48l0-288c0-26.5 21.5-48 48-48zM48 128l80 0 0 64-64 0 0 256 192 0 0-32 64 0 0 48c0 26.5-21.5 48-48 48L48 512c-26.5 0-48-21.5-48-48L0 176c0-26.5 21.5-48 48-48z",
      fill: "#4c4c4c"
    },
    "task-edit": {
      viewBox: "0 0 512 512",
      path: "M152.1 38.2c9.9 8.9 10.7 24 1.8 33.9l-72 80c-4.4 4.9-10.6 7.8-17.2 7.9s-12.9-2.4-17.6-7L7 113C-2.3 103.6-2.3 88.4 7 79s24.6-9.4 33.9 0l22.1 22.1 55.1-61.2c8.9-9.9 24-10.7 33.9-1.8zm0 160c9.9 8.9 10.7 24 1.8 33.9l-72 80c-4.4 4.9-10.6 7.8-17.2 7.9s-12.9-2.4-17.6-7L7 273c-9.4-9.4-9.4-24.6 0-33.9s24.6-9.4 33.9 0l22.1 22.1 55.1-61.2c8.9-9.9 24-10.7 33.9-1.8zM224 96c0-17.7 14.3-32 32-32l224 0c17.7 0 32 14.3 32 32s-14.3 32-32 32l-224 0c-17.7 0-32-14.3-32-32zm0 160c0-17.7 14.3-32 32-32l224 0c17.7 0 32 14.3 32 32s-14.3 32-32 32l-224 0c-17.7 0-32-14.3-32-32zM160 416c0-17.7 14.3-32 32-32l288 0c17.7 0 32 14.3 32 32s-14.3 32-32 32l-288 0c-17.7 0-32-14.3-32-32zM48 368a48 48 0 1 1 0 96 48 48 0 1 1 0-96z",
      fill: "#4c4c4c"
    },
    "edit": {
      viewBox: "0 0 512 512",
      path: "M471.6 21.7c-21.9-21.9-57.3-21.9-79.2 0L362.3 51.7l97.9 97.9 30.1-30.1c21.9-21.9 21.9-57.3 0-79.2L471.6 21.7zm-299.2 220c-6.1 6.1-10.8 13.6-13.5 21.9l-29.6 88.8c-2.9 8.6-.6 18.1 5.8 24.6s15.9 8.7 24.6 5.8l88.8-29.6c8.2-2.7 15.7-7.4 21.9-13.5L437.7 172.3 339.7 74.3 172.4 241.7zM96 64C43 64 0 107 0 160L0 416c0 53 43 96 96 96l256 0c53 0 96-43 96-96l0-96c0-17.7-14.3-32-32-32s-32 14.3-32 32l0 96c0 17.7-14.3 32-32 32L96 448c-17.7 0-32-14.3-32-32l0-256c0-17.7 14.3-32 32-32l96 0c17.7 0 32-14.3 32-32s-14.3-32-32-32L96 64z",
      fill: "#4c4c4c"
    },
    "delete": {
      viewBox: "0 0 448 512",
      path: "M135.2 17.7L128 32 32 32C14.3 32 0 46.3 0 64S14.3 96 32 96l384 0c17.7 0 32-14.3 32-32s-14.3-32-32-32l-96 0-7.2-14.3C307.4 6.8 296.3 0 284.2 0L163.8 0c-12.1 0-23.2 6.8-28.6 17.7zM416 128L32 128 53.2 467c1.6 25.3 22.6 45 47.9 45l245.8 0c25.3 0 46.3-19.7 47.9-45L416 128z",
      fill: "#4c4c4c"
    },
    "toggle-status": {
      viewBox: "0 0 384 512",
      path: this.props.model.isPause
        ? "M73 39c-14.8-9.1-33.4-9.4-48.5-.9S0 62.6 0 80L0 432c0 17.4 9.4 33.4 24.5 41.9s33.7 8.1 48.5-.9L361 297c14.3-8.7 23-24.2 23-41s-8.7-32.2-23-41L73 39z"
        : "M48 64C21.5 64 0 85.5 0 112L0 400c0 26.5 21.5 48 48 48l32 0c26.5 0 48-21.5 48-48l0-288c0-26.5-21.5-48-48-48L48 64zm192 0c-26.5 0-48 21.5-48 48l0 288c0 26.5 21.5 48 48 48l32 0c26.5 0 48-21.5 48-48l0-288c0-26.5-21.5-48-48-48l-32 0z",
      fill: this.props.model.isPause ? "red" : "#4CAF50"
    }
  };

  // 计算提示框的宽度和位置
  const tooltipWidth = tooltipText.length * 8 + 10;
  const tooltipX = 7.5 - tooltipWidth / 2; // 居中对齐图标
  
  // 创建按钮和提示组件
  return h(
    "g",
    {
      transform: `translate(${posX}, ${posY})`,
      cursor: "pointer",
      onClick: (e) => this.handleButtonClick(e, action),
    },
    [
      // 图标容器区域（用于鼠标事件）
      h("rect", {
        x: 0,
        y: 0,
        width: 15,
        height: 15,
        fill: "transparent", // 透明填充
        onMouseenter: () => {
          // 显示当前图标的提示
          const tooltipBg = document.querySelector(`#tooltip-bg-${action}`);
          const tooltipText = document.querySelector(`#tooltip-text-${action}`);
          if (tooltipBg) tooltipBg.style.opacity = "1";
          if (tooltipText) tooltipText.style.opacity = "1";
        },
        onMouseleave: () => {
          // 隐藏当前图标的提示
          const tooltipBg = document.querySelector(`#tooltip-bg-${action}`);
          const tooltipText = document.querySelector(`#tooltip-text-${action}`);
          if (tooltipBg) tooltipBg.style.opacity = "0";
          if (tooltipText) tooltipText.style.opacity = "0";
        }
      }),
      
      // 图标SVG
      h(
        "svg",
        {
          width: "15",
          height: "15", 
          viewBox: iconConfig[action].viewBox,
          pointerEvents: "none", // 确保鼠标事件穿透到下面的rect
        },
        [
          h("path", {
            d: iconConfig[action].path,
            fill: iconConfig[action].fill,
          }),
        ]
      ),
      
      // 提示背景
      h("rect", {
        id: `tooltip-bg-${action}`,
        x: tooltipX,
        y: -25, // 显示在图标上方
        rx: 3,
        ry: 3,
        width: tooltipWidth,
        height: 20,
        fill: "rgba(0, 0, 0, 0.7)",
        style: {
          opacity: 0,
          transition: "opacity 0.3s ease",
          pointerEvents: "none", // 防止提示框捕获鼠标事件
        },
      }),
      
      // 提示文本
      h("text", {
        id: `tooltip-text-${action}`,
        x: 7.5, // 水平居中
        y: -14, // 垂直居中
        "text-anchor": "middle",
        "dominant-baseline": "middle",
        fill: "white",
        "font-size": "11px",
        style: {
          opacity: 0,
          transition: "opacity 0.3s ease",
          pointerEvents: "none",
          userSelect: "none",
        },
      }, tooltipText)
    ]
  );
}

  getResizeShape() {
    return h("g", {}, [super.getResizeShape(), this.getButtonGroup()]);
  }

  handleButtonClick(e: MouseEvent, action: string) {
    e.stopPropagation();
    const { model, graphModel } = this.props;

    graphModel.eventCenter.emit(`custom:node-${action}`, {
      nodeId: model.id,
      position: { x: e.clientX, y: e.clientY },
    });
  }
}

export class BaseButtonNodeModel extends RectResize.model {
  initNodeData(data) {
    console.log("初始化",data);
    super.initNodeData(data);
    this.isPause = data.isPause || false; 
    console.log("init",this);
    this.width = 160;
    this.height = 90;
    this.minHeight = 90;
    this.minWidth = 160;
    this.radius = 18;
  }
  buttonGroupOpacity = 0;
}
