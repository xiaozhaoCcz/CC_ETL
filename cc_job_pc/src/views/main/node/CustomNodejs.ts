import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomNodeModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const nodejsStyle = getNodeTypeStyle("nodejs");
    return {
      ...super.getNodeStyle(),
      stroke: nodejsStyle.stroke,
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

class CustomNodeView extends BaseButtonNodeView {
  // 创建专业的Node.js图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const nodejsStyle = getNodeTypeStyle("nodejs");
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
        // 扁平风格六边形
        h('polygon', {
          points: '12,2 22,7 22,17 12,22 2,17 2,7',
          fill: nodejsStyle.iconColor,
        }),
        // JS字母
        h('text', {
          x: 12,
          y: 16,
          fontSize: 8,
          fill: nodejsStyle.accentColor,
          fontFamily: 'monospace',
          'text-anchor': 'middle',
        }, 'JS'),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const nodejsStyle = getNodeTypeStyle("nodejs");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: nodejsStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'Node.js')
    ]);
  }
}

export default {
  type: 'custom-nodejs',
  view: CustomNodeView,
  model: CustomNodeModel,
}
