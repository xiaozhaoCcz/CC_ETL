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
        // 扁平风格咖啡杯
        h('rect', {
          x: 6,
          y: 10,
          width: 8,
          height: 6,
          rx: 2,
          fill: javaStyle.iconColor,
        }),
        h('ellipse', {
          cx: 10,
          cy: 10,
          rx: 4,
          ry: 2,
          fill: javaStyle.iconColor,
        }),
        // 杯把
        h('path', {
          d: 'M14 12c2 0 2 4 0 4',
          stroke: javaStyle.accentColor,
          'stroke-width': 1.5,
          fill: 'none',
        }),
        // 蒸汽
        h('path', {
          d: 'M9 8c0-1 2-1 2 0',
          stroke: javaStyle.accentColor,
          'stroke-width': 1,
          fill: 'none',
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
