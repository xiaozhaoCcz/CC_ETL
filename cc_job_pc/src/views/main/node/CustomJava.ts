import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomJavaModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const javaStyle = getNodeTypeStyle("java");
    return {
      ...super.getNodeStyle(),
      stroke: javaStyle.stroke,
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

class CustomJavaView extends BaseButtonNodeView {
  // 创建专业的Java图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const javaStyle = getNodeTypeStyle("java");
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
        // Java咖啡杯主体
        h('path', {
          fill: javaStyle.iconColor,
          d: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.94-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z',
        }),
        // 咖啡蒸汽
        h('path', {
          fill: javaStyle.accentColor,
          d: 'M7 9h2v2H7zm4 0h2v2h-2zm4 0h2v2h-2z',
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const javaStyle = getNodeTypeStyle("java");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: javaStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'Java')
    ]);
  }
}

export default {
  type: 'custom-java',
  view: CustomJavaView,
  model: CustomJavaModel,
}
