import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";

class CustomSqlModel extends RectResize.model {

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

class CustomSqlView extends RectResize.view {


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
      fill: "#00CCFF",
      d: 'M33.792 631.808c0 84.992 214.016 153.6 478.208 153.6 263.168 0 477.184-68.608 478.208-153.6V509.952C891.904 580.608 701.44 614.4 512 614.4c-189.44 0-379.904-33.792-478.208-105.472v122.88z',
    }),
    h('path', {
      fill: "#00CCFF",
      d: 'M990.208 747.52C891.904 819.2 701.44 852.992 512 852.992 322.56 852.992 132.096 819.2 33.792 747.52v139.264C60.416 964.608 266.24 1024 512 1024s451.584-59.392 478.208-136.192V747.52zM33.792 392.192c0 84.992 214.016 153.6 478.208 153.6 263.168 0 477.184-68.608 478.208-153.6V270.336c-98.304 71.68-288.768 105.472-478.208 105.472-189.44 0-379.904-33.792-478.208-105.472v121.856z',
    }),
    h('path', {
      fill: "#00CCFF",
      d: 'M33.792 153.6a478.208 153.6 0 1 0 956.416 0 478.208 153.6 0 1 0-956.416 0Z',
    }),
  
  );
    }



   // 组合矩形主体和头像图标
  getResizeShape() {
    const { model } = this.props;
    const { x, y, width, height, radius } = model;
    const style = model.getNodeStyle();
    return h('g', {}, [
      h('rect', {
        ...style,
        x: x - width / 2, // 矩形默认x，y代表左上角顶点坐标，切换为中心点
        y: y - height / 2,
        rx: radius,
        ry: radius,
        width,
        height,
      }),
      this.getLabelShape(),
      // 图标右侧文本
      h('text', {
        x: x - width/2 + 30,
        y: y - height/2 + 25.5, 
        fontSize: 12,
        fill: 'black',
        className: 'node-label'
      }, 'Sql')
    ]);
  }
}

export default {

    type: 'custom-sql',
    view: CustomSqlView,
    model: CustomSqlModel,
}
