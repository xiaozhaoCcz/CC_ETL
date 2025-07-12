import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomPowerShellModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const powershellStyle = getNodeTypeStyle("powershell");
    return {
      ...super.getNodeStyle(),
      stroke: powershellStyle.stroke,
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

class CustomPowerShellView extends BaseButtonNodeView {
  // 创建专业的PowerShell图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const powershellStyle = getNodeTypeStyle("powershell");
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
        // 扁平风格命令行窗口
        h('rect', {
          x: 2,
          y: 4,
          width: 20,
          height: 14,
          rx: 2,
          fill: powershellStyle.iconColor,
        }),
        h('rect', {
          x: 2,
          y: 4,
          width: 20,
          height: 3,
          fill: powershellStyle.accentColor,
        }),
        // PS字母
        h('text', {
          x: 12,
          y: 14,
          fontSize: 8,
          fill: powershellStyle.accentColor,
          fontFamily: 'monospace',
          'text-anchor': 'middle',
        }, 'PS'),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const powershellStyle = getNodeTypeStyle("powershell");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: powershellStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'PowerShell')
    ]);
  }
}

export default {
  type: 'custom-powershell',
  view: CustomPowerShellView,
  model: CustomPowerShellModel,
}
