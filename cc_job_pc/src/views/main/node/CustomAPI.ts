import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomAPIModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const apiStyle = getNodeTypeStyle("api");
    return {
      ...super.getNodeStyle(),
      stroke: apiStyle.stroke,
      strokeWidth: NODE_STYLES.base.strokeWidth,
      filter: NODE_STYLES.base.shadow,
    };
  }

  getTextStyle() {
    const style = super.getTextStyle();
    style.stroke = NODE_STYLES.text.color;
    style.fontSize = NODE_STYLES.text.fontSize;
    style.fontFamily = NODE_STYLES.text.fontFamily;
    style.fontWeight = NODE_STYLES.text.fontWeight;
    return style;
  }
}

class CustomAPIView extends BaseButtonNodeView {
  // 创建专业的API图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const apiStyle = getNodeTypeStyle("api");
    const iconSize = NODE_STYLES.icon.size;
    const padding = NODE_STYLES.icon.padding;

    return h(
      "svg",
      {
        x: x - width / 2 + padding,
        y: y - height / 2 + padding,
        width: iconSize,
        height: iconSize,
        viewBox: "0 0 24 24",
        style: {
          filter: "drop-shadow(0 1px 2px rgba(0,0,0,0.1))",
        },
      },
      [
        // API连接图标
        h('path', {
          fill: apiStyle.iconColor,
          d: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.94-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z',
        }),
        // 连接点
        h('circle', {
          fill: apiStyle.accentColor,
          cx: "8",
          cy: "8",
          r: "1.5",
        }),
        h('circle', {
          fill: apiStyle.accentColor,
          cx: "16",
          cy: "16",
          r: "1.5",
        }),
        h('circle', {
          fill: apiStyle.accentColor,
          cx: "12",
          cy: "12",
          r: "1.5",
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const apiStyle = getNodeTypeStyle("api");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: apiStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'API')
    ]);
  }
}

export default {
  type: 'custom-api',
  view: CustomAPIView,
  model: CustomAPIModel,
}
