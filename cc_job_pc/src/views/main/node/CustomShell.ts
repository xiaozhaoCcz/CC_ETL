import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomShellModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const shellStyle = getNodeTypeStyle("shell");
    return {
      ...super.getNodeStyle(),
      stroke: shellStyle.stroke,
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

class CustomShellView extends BaseButtonNodeView {
  // 创建专业的Shell图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const shellStyle = getNodeTypeStyle("shell");
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
        // Shell终端图标
        h('path', {
          fill: shellStyle.iconColor,
          d: 'M20 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm-5 14H4v-4h11v4zm0-5H4V9h11v4zm5 5h-4V9h4v9z',
        }),
        // 终端光标
        h('rect', {
          fill: shellStyle.accentColor,
          x: "6",
          y: "10",
          width: "2",
          height: "2",
          style: {
            animation: "blink 1s infinite",
          },
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const shellStyle = getNodeTypeStyle("shell");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: shellStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'Shell')
    ]);
  }
}

export default {
  type: 'custom-shell',
  view: CustomShellView,
  model: CustomShellModel,
}
