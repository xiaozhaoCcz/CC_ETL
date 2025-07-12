import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeView, BaseButtonNodeModel } from "./BaseNodeWithButtons";
import { NODE_STYLES, getNodeTypeStyle } from "@/utils/nodeStyles";

class CustomSqlModel extends BaseButtonNodeModel {
  getNodeStyle() {
    const sqlStyle = getNodeTypeStyle("sql");
    return {
      ...super.getNodeStyle(),
      stroke: sqlStyle.stroke,
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

class CustomSqlView extends BaseButtonNodeView {
  // 创建专业的SQL数据库图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const sqlStyle = getNodeTypeStyle("sql");
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
        // 扁平风格数据库圆柱体
        h('ellipse', {
          cx: 12,
          cy: 6,
          rx: 8,
          ry: 3,
          fill: sqlStyle.iconColor,
        }),
        h('rect', {
          x: 4,
          y: 6,
          width: 16,
          height: 10,
          fill: sqlStyle.iconColor,
        }),
        h('ellipse', {
          cx: 12,
          cy: 16,
          rx: 8,
          ry: 3,
          fill: sqlStyle.iconColor,
        }),
        // 装饰线
        h('ellipse', {
          cx: 12,
          cy: 11,
          rx: 8,
          ry: 3,
          fill: sqlStyle.accentColor,
          opacity: 0.2,
        }),
      ]
    );
  }

  // 组合矩形主体和图标
  getResizeShape() {
    const { model } = this.props;
    const sqlStyle = getNodeTypeStyle("sql");

    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width / 2 + 40,
        y: model.y - model.height / 2 + 25,
        fontSize: NODE_STYLES.text.fontSize,
        fontFamily: NODE_STYLES.text.fontFamily,
        fontWeight: NODE_STYLES.text.fontWeight,
        fill: sqlStyle.iconColor,
        style: {
          userSelect: "none",
        },
      }, 'SQL')
    ]);
  }
}

export default {
  type: 'custom-sql',
  view: CustomSqlView,
  model: CustomSqlModel,
}
