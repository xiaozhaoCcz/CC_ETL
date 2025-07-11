import { dynamicGroup } from "@logicflow/extension";

class CustomGroup extends dynamicGroup.view {}

class CustomGroupModel extends dynamicGroup.model {
  getAnchorStyle() {
    const style = super.getAnchorStyle() as any;
    style.stroke = "rgb(24, 125, 255)";
    style.r = 3;
    style.hover.r = 5;
    style.hover.fill = "rgb(24, 125, 255)";
    style.hover.stroke = "rgb(24, 125, 255)";
    return style;
  }
}

export default {
  type: "CustomGroup",
  model: CustomGroupModel,
  view: CustomGroup,
};
