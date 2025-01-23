import { dynamicGroup } from "@logicflow/extension";

class CustomGroup extends dynamicGroup.view {}

class CustomGroupModel extends dynamicGroup.model {
  getDefaultAnchor() {
    const { x, y, id, width, height } = this;
    return [
      {
        x: x + width / 2,
        y: y,
        id: `1_${id}`,
        type: "right",
      },
      {
        x: x - width / 2,
        y: y,
        id: `2_${id}`,
        type: "left",
      },
      {
        x: x,
        y: y + height / 2,
        id: `3_${id}`,
        type: "bottom",
      },
      {
        x: x,
        y: y - height / 2,
        id: `4_${id}_top`,
        type: "top",
      },
    ];
  }
}

export default {
  type: "CustomGroup",
  model: CustomGroupModel,
  view: CustomGroup,
};
