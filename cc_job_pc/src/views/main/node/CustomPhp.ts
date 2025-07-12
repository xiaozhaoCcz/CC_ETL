import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomPhpModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const phpStyle = getNodeTypeStyle("php");
    return {
      ...super.getNodeStyle(),
      stroke: phpStyle.stroke,
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

class CustomPhpView extends BaseButtonNodeView {
  // 创建专业的PHP图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const phpStyle = getNodeTypeStyle("php");
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
        // 扁平风格椭圆
        h('ellipse', {
          cx: 12,
          cy: 12,
          rx: 9,
          ry: 7,
          fill: phpStyle.iconColor,
        }),
        // PHP字母
        h('text', {
          x: 12,
          y: 16,
          fontSize: 7,
          fill: phpStyle.accentColor,
          fontFamily: 'monospace',
          'text-anchor': 'middle',
        }, 'PHP'),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const phpStyle = getNodeTypeStyle("php");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: phpStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'PHP')
    ]);
  }
}

export default {
  type: 'custom-php',
  view: CustomPhpView,
  model: CustomPhpModel,
}
