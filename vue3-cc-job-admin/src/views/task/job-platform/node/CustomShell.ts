import { RectResize } from "@logicflow/extension";
import { h } from "@logicflow/core";

class CustomShellModel extends RectResize.model {
  getNodeStyle() {
    const style = super.getNodeStyle();
    style.stroke = "black"; // 设置节点边框
    return style;
  }
  getTextStyle() {
    const style = super.getTextStyle();
    style.stroke = "black";

    return style;
  }
}

class CustomShellView extends RectResize.view {
  // 创建SVG图标
  private getLabelShape() {
    const { model } = this.props;
    const { x, y, width, height } = model;
    const style = model.getNodeStyle();

    return h(
      "svg",
      {
        x: x - width / 2 + 5,
        y: y - height / 2 + 5,
        width: 25,
        height: 25,
        viewBox: "0 0 1274 1024",
      },
      h("path", {
        fill: "black",
        d: "M144 112h736c17.673 0 32 14.327 32 32v736c0 17.673-14.327 32-32 32H144c-17.673 0-32-14.327-32-32V144c0-17.673 14.327-32 32-32z m112 211.24v72.43a8.81 8.81 0 0 0 3.35 7L386.09 509 259.35 615.37a9.32 9.32 0 0 0-3.35 7v72.43a9.2 9.2 0 0 0 15.15 7L492.7 516.04a9.29 9.29 0 0 0 0-14.2l-221.55-185.6a9.2 9.2 0 0 0-15.15 7zM521.57 624a9.82 9.82 0 0 0-9.57 10v60a9.82 9.82 0 0 0 9.57 10h236.86a9.82 9.82 0 0 0 9.57-10v-60a9.82 9.82 0 0 0-9.57-10H521.57z",
      })
    );
  }

  // 组合矩形主体和头像图标
  getResizeShape() {
    const { model } = this.props;
    const { x, y, width, height, radius } = model;
    const style = model.getNodeStyle();
    return h("g", {}, [
      h("rect", {
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
      h(
        "text",
        {
          x: x - width / 2 + 30,
          y: y - height / 2 + 25.5,
          fontSize: 12,
          fill: "black",
          className: "node-label",
        },
        "Shell"
      ),
    ]);
  }
}

export default {
  type: "custom-shell",
  view: CustomShellView,
  model: CustomShellModel,
};
