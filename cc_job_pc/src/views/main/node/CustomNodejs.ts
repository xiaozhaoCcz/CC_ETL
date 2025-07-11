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
        // Node.js六边形图标
        h('path', {
          fill: nodejsStyle.iconColor,
          d: 'M12 2L2 7v10l10 5 10-5V7L12 2zm0 2.236L19.764 8 12 11.764 4.236 8 12 4.236zM4 9.236V16l7 3.764V13L4 9.236zM20 16V9.236L13 13v6.764L20 16z',
        }),
        // 中心装饰
        h('circle', {
          fill: nodejsStyle.accentColor,
          cx: "12",
          cy: "12",
          r: "2",
        }),
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
