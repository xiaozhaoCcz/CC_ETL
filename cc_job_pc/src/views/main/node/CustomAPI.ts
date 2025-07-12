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
        // 扁平风格三点连线
        h('circle', {
          cx: 6,
          cy: 12,
          r: 2,
          fill: apiStyle.iconColor,
        }),
        h('circle', {
          cx: 18,
          cy: 12,
          r: 2,
          fill: apiStyle.iconColor,
        }),
        h('circle', {
          cx: 12,
          cy: 6,
          r: 2,
          fill: apiStyle.iconColor,
        }),
        h('line', {
          x1: 6,
          y1: 12,
          x2: 12,
          y2: 6,
          stroke: apiStyle.accentColor,
          'stroke-width': 1.5,
        }),
        h('line', {
          x1: 12,
          y1: 6,
          x2: 18,
          y2: 12,
          stroke: apiStyle.accentColor,
          'stroke-width': 1.5,
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
