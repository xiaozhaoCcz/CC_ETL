import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";
import { BaseButtonNodeModel } from "./BaseNodeWithButtons";
import { BaseButtonNodeView } from "./BaseNodeWithButtons";
class CustomShellModel extends BaseButtonNodeModel {

  getNodeStyle() {
    const style = super.getNodeStyle();
    style.stroke = 'black';// 设置节点边框
    return style;
  }
  getTextStyle() {
    const style = super.getTextStyle();
    style.stroke ='black';
    
    return style;
  }
}

class CustomShellView extends BaseButtonNodeView {


    // 创建SVG图标
    private getLabelShape() {
      const {model} = this.props;
      const {x, y, width, height} = model;
      const style= model.getNodeStyle();

      return h(
        "svg",
        {
          x: x - width / 2+5,
          y: y - height / 2+5,
          width: 25,
          height:25,
          viewBox: '0 0 1274 1024',     
     },
     h('path', {
      fill: "black",
      d: 'M989.06624 131.285333c24.234667 0 39.381333 19.754667 33.792 44.16l-156.117333 681.856c-5.546667 24.405333-29.738667 44.16-53.973334 44.16H34.95424c-24.234667 0-39.381333-19.754667-33.792-44.16L157.279573 175.445333c5.546667-24.405333 29.738667-44.16 53.973334-44.16h777.813333z m-357.333333 398.72c10.709333-16.810667 9.685333-38.613333-3.84-53.034666L389.215573 223.061333c-16.213333-17.28-44.245333-17.365333-62.549333-0.170666-18.304 17.194667-19.968 45.098667-3.754667 62.336l198.912 211.626666v4.693334l-316.586666 229.248c-19.2 13.952-22.741333 41.685333-7.978667 61.994666 14.762667 20.309333 42.282667 25.472 61.44 11.52l351.104-252.117333c11.946667-8.405333 18.688-15.616 21.930667-22.186667z m-119.296 187.690667a39.594667 39.594667 0 0 0-39.850667 39.381333c0 21.76 17.834667 39.381333 39.850667 39.381334h189.141333a39.594667 39.594667 0 0 0 39.850667-39.381334 39.594667 39.594667 0 0 0-39.850667-39.381333H512.436907z',
    }),

  
  );
    }



   // 组合矩形主体和头像图标
  getResizeShape() {
    const { model } = this.props;
    return h('g', {}, [
      super.getResizeShape(),
      this.getLabelShape(),
      h('text', {
        x: model.x - model.width/2 + 30,
        y: model.y - model.height/2 + 25.5,
        fontSize: 12,
        fill: 'black'
      }, 'PowerShell')
    ]);
  }
}

export default {

    type: 'custom-powershell',
    view: CustomShellView,
    model: CustomShellModel,
}
