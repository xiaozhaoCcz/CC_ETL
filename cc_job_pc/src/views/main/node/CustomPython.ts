import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomPythonModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const pythonStyle = getNodeTypeStyle("python");
    return {
      ...super.getNodeStyle(),
      stroke: pythonStyle.stroke,
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

class CustomPythonView extends BaseButtonNodeView {
  // 创建专业的Python图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const pythonStyle = getNodeTypeStyle("python");
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
        // 扁平风格蛇形
        h('path', {
          d: 'M4 16c4-8 12-8 16 0',
          stroke: pythonStyle.iconColor,
          'stroke-width': 2.5,
          fill: 'none',
        }),
        h('circle', {
          cx: 8,
          cy: 16,
          r: 1.5,
          fill: pythonStyle.accentColor,
        }),
        h('circle', {
          cx: 16,
          cy: 16,
          r: 1.5,
          fill: pythonStyle.accentColor,
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const pythonStyle = getNodeTypeStyle("python");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: pythonStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'Python')
    ]);
  }
}

export default {
  type: 'custom-python',
  view: CustomPythonView,
  model: CustomPythonModel,
}
