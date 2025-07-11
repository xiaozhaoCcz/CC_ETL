import { BaseButtonNodeModel, BaseButtonNodeView } from "./BaseNodeWithButtons";
import { h } from "@logicflow/core";

export class CustomRectModel extends BaseButtonNodeModel {
  // 可扩展自定义样式
}

export class CustomRectView extends BaseButtonNodeView {
  getResizeShape() {
    const { model } = this.props;
    return h('g', {}, [
      super.getResizeShape(),
      h('text', {
        x: model.x, // 水平居中
        y: model.y + 8, // 垂直居中微调
        fontSize: 14, // 增大字号
        fontWeight: 'bold', // 加粗
        fill: 'black',
        textAnchor: 'middle' // 文本锚点居中
      }, '普通任务')
    ]);
  }
}

export default {
  type: 'custom-rect',
  view: CustomRectView,
  model: CustomRectModel
}
