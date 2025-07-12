import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { NODE_STYLES, getIconColor } from "../../../utils/nodeStyles";
import { getNodeStatusColor } from "@/utils/colors";

// 定义类型接口
interface IconConfig {
  viewBox: string;
  path: string;
  fill: string;
  hoverFill: string;
}

export class BaseButtonNodeView extends RectResize.view {
  constructor(props: any) {
    super(props);
    // 辅助排查 NODE_STYLES
    // eslint-disable-next-line no-console
    console.log('NODE_STYLES in BaseButtonNodeView:', NODE_STYLES);
  }
  getButtonGroup() {
    const { model } = this.props;
    const { x, y, width, height } = model;

    // 定义提示文本信息
    const tooltips = {
      copy: "复制节点",
      "task-edit": "选择任务",
      edit: "编辑节点",
      delete: "删除节点",
      prop: "节点属性",
      "toggle-status": model.isPause ? "启动任务" : "暂停任务",
    };

    return h(
      "g",
      {
        style: {
          opacity: model.buttonGroupOpacity,
        },
      },
      [
        // 按钮组背景 - 使用样式配置
        h("rect", {
          x: x - width / 2,
          y: y - height / 2 - 45,
          width: width,
          height: 45,
          fill: "transparent",
          "stroke-width": NODE_STYLES.buttonGroup?.borderWidth ?? 1,
          rx: NODE_STYLES.buttonGroup?.radius ?? 8,
          ry: NODE_STYLES.buttonGroup?.radius ?? 8,
          filter: NODE_STYLES.buttonGroup?.shadow ?? '',
        }),
        // 按钮组顶部装饰线
      ],
      [
        // 复制图标
        this.createButtonWithTooltip(
          x - width / 2 + width - 120,
          y - height / 2 - 22,
          "copy",
          tooltips.copy
        ),
        // 选择任务图标
        this.createButtonWithTooltip(
          x - width / 2 + width - 100,
          y - height / 2 - 22,
          "task-edit",
          tooltips["task-edit"]
        ),
        // 编辑节点图标
        this.createButtonWithTooltip(
          x - width / 2 + width - 80,
          y - height / 2 - 22,
          "edit",
          tooltips.edit
        ),
        // 删除图标
        this.createButtonWithTooltip(
          x - width / 2 + width - 60,
          y - height / 2 - 22,
          "delete",
          tooltips.delete
        ),
        // 属性图标
        this.createButtonWithTooltip(
          x - width / 2 + width - 40,
          y - height / 2 - 22,
          "prop",
          tooltips["prop"]
        ),
        // 状态切换按钮
        this.createButtonWithTooltip(
          x - width / 2 + width - 20,
          y - height / 2 - 22,
          "toggle-status",
          tooltips["toggle-status"]
        ),
      ]
    );
  }

  // 创建带有提示的按钮方法
  createButtonWithTooltip(posX: number, posY: number, action: string, tooltipText: string) {
    // SVG路径配置 - 使用更专业的图标
    const iconPaths: { [key: string]: { viewBox: string; path: string } } = {
      copy: {
        viewBox: "0 0 24 24",
        path: "M16 1H4C2.9 1 2 1.9 2 3v14h2V3h12V1zm3 4H8C6.9 5 6 5.9 6 7v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z",
      },
      "task-edit": {
        viewBox: "0 0 24 24",
        path: "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z",
      },
      edit: {
        viewBox: "0 0 24 24",
        path: "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z",
      },
      delete: {
        viewBox: "0 0 24 24",
        path: "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z",
      },
      prop: {
        viewBox: "0 0 24 24",
        path: "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z",
      },
      "toggle-status": {
        viewBox: "0 0 24 24",
        path: this.props.model.isPause
          ? "M8 5v14l11-7z" // 播放图标
          : "M6 19h4V5H6v14zm8-14v14h4V5h-4z", // 暂停图标
      },
    };

    // 获取图标颜色
    const iconColor = getIconColor(action, this.props.model.isPause);

    // 计算提示框的宽度和位置
    const tooltipWidth = tooltipText.length * 7 + 16;
    const tooltipX = 8 - tooltipWidth / 2;

    // 创建按钮和提示组件
    return h(
      "g",
      {
        transform: `translate(${posX}, ${posY})`,
        cursor: "pointer",
        onClick: (e: MouseEvent) => this.handleButtonClick(e, action),
      },
      [
        // 按钮背景 - 使用样式配置
        h("rect", {
          x: 0,
          y: 0,
          width: NODE_STYLES.button?.size ?? 16,
          height: NODE_STYLES.button?.size ?? 16,
          fill: NODE_STYLES.button?.background ?? '#fff',
          stroke: NODE_STYLES.button?.border ?? '#e2e8f0',
          "stroke-width": NODE_STYLES.button?.borderWidth ?? 1,
          rx: NODE_STYLES.button?.radius ?? 3,
          ry: NODE_STYLES.button?.radius ?? 3,
          onMouseenter: (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const parent = target.parentElement;
            if (parent) {
              const tooltipBg = parent.querySelector(".tooltip-bg") as HTMLElement;
              const tooltipText = parent.querySelector(".tooltip-text") as HTMLElement;
              const iconPath = parent.querySelector(".icon-path") as HTMLElement;
              if (tooltipBg) tooltipBg.style.opacity = "1";
              if (tooltipText) tooltipText.style.opacity = "1";
              if (iconPath) iconPath.style.fill = iconColor.hover;
              target.style.fill = NODE_STYLES.button?.hoverBackground ?? '#fff';
              target.style.stroke = NODE_STYLES.button?.hoverBorder ?? '#3b82f6';
            }
          },
          onMouseleave: (e: MouseEvent) => {
            const target = e.target as HTMLElement;
            const parent = target.parentElement;
            if (parent) {
              const tooltipBg = parent.querySelector(".tooltip-bg") as HTMLElement;
              const tooltipText = parent.querySelector(".tooltip-text") as HTMLElement;
              const iconPath = parent.querySelector(".icon-path") as HTMLElement;
              if (tooltipBg) tooltipBg.style.opacity = "0";
              if (tooltipText) tooltipText.style.opacity = "0";
              if (iconPath) iconPath.style.fill = iconColor.normal;
              target.style.fill = NODE_STYLES.button?.background ?? '#fff';
              target.style.stroke = NODE_STYLES.button?.border ?? '#e2e8f0';
            }
          },
        }),

        // 图标SVG - 使用样式配置
        h(
          "svg",
          {
            width: "14",
            height: "14",
            viewBox: iconPaths[action].viewBox,
            pointerEvents: "none",
            style: {
              transform: "translate(1px, 1px)",
            },
          },
          [
            h("path", {
              class: "icon-path",
              d: iconPaths[action].path,
              fill: iconColor.normal,
            }),
          ]
        ),

        // 提示背景 - 使用样式配置
        h("rect", {
          class: "tooltip-bg",
          x: tooltipX,
          y: -28,
          rx: NODE_STYLES.tooltip.radius,
          ry: NODE_STYLES.tooltip.radius,
          width: tooltipWidth,
          height: 24,
          fill: NODE_STYLES.tooltip.background,
          style: {
            opacity: 0,
            pointerEvents: "none",
            filter: NODE_STYLES.tooltip.shadow,
          },
        }),

        // 提示文本 - 使用样式配置
        h(
          "text",
          {
            class: "tooltip-text",
            x: 8,
            y: -16,
            "text-anchor": "middle",
            "dominant-baseline": "middle",
            fill: NODE_STYLES.tooltip.textColor,
            "font-size": NODE_STYLES.tooltip.fontSize,
            "font-weight": NODE_STYLES.tooltip.fontWeight,
            style: {
              opacity: 0,
              pointerEvents: "none",
              userSelect: "none",
              fontFamily: NODE_STYLES.tooltip.fontFamily,
            },
          },
          tooltipText
        ),
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
  initNodeData(data: any) {
    super.initNodeData(data);
    this.isPause = data.isPause || false;
    this.width = 160;
    this.height = 90;
    this.minHeight = 90;
    this.minWidth = 160;
    this.radius = NODE_STYLES.base.radius;
  }
  buttonGroupOpacity = 0;

  // 添加状态属性
  nodeStatus: number = 0; // 0: 默认, 1: 失败, 2: 成功, 3: 运行中, 4: 等待, 5: 暂停

  // 重写getNodeStyle方法，支持状态颜色
  getNodeStyle() {
    const baseStyle = super.getNodeStyle();

    // 如果有状态颜色，优先使用状态颜色
    if (this.nodeStatus > 0) {
      const statusColor = getNodeStatusColor(this.nodeStatus);
      return {
        ...baseStyle,
        fill: statusColor,
        stroke: statusColor,
        strokeWidth: NODE_STYLES.base.strokeWidth,
        filter: NODE_STYLES.base.shadow,
      };
    }

    // 否则使用默认样式
    return baseStyle;
  }

  // 设置节点状态
  setNodeStatus(status: number) {
    this.nodeStatus = status;
    // 触发重新渲染
    this.setAttributes();
  }
}
