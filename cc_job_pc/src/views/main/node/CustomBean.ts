import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomBeanModel extends BaseButtonNodeModel {
  getNodeStyle() {

    const beanStyle = getNodeTypeStyle("bean");
    return {
      ...super.getNodeStyle(),
      stroke: beanStyle.stroke,
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

class CustomBeanView extends BaseButtonNodeView {
  // 创建专业的Bean图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const beanStyle = getNodeTypeStyle("bean");
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
        // 扁平风格豆子
        h('ellipse', {
          cx: 12,
          cy: 12,
          rx: 7,
          ry: 10,
          fill: beanStyle.iconColor,
        }),
        h('ellipse', {
          cx: 15,
          cy: 10,
          rx: 2,
          ry: 4,
          fill: beanStyle.accentColor,
          opacity: 0.5,
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const beanStyle = getNodeTypeStyle("bean");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: beanStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'Bean')
    ]);
  }
}

export default {
  type: 'custom-bean',
  view: CustomBeanView,
  model: CustomBeanModel,
}
