/**
 * Check if an element has a class
 * @param {HTMLElement} ele
 * @param {string} cls
 * @returns {boolean}
 */
export function hasClass(ele: HTMLElement, cls: string) {
  return !!ele.className.match(new RegExp("(\\s|^)" + cls + "(\\s|$)"));
}

/**
 * Add class to element
 * @param {HTMLElement} ele
 * @param {string} cls
 */
export function addClass(ele: HTMLElement, cls: string) {
  if (!hasClass(ele, cls)) ele.className += " " + cls;
}

/**
 * Remove class from element
 * @param {HTMLElement} ele
 * @param {string} cls
 */
export function removeClass(ele: HTMLElement, cls: string) {
  if (hasClass(ele, cls)) {
    const reg = new RegExp("(\\s|^)" + cls + "(\\s|$)");
    ele.className = ele.className.replace(reg, " ");
  }
}

/**
 * 判断是否是外部链接
 *
 * @param {string} path
 * @returns {Boolean}
 */
export function isExternal(path: string) {
  const isExternal = /^(https?:|http?:|mailto:|tel:)/.test(path);
  return isExternal;
}

export function timestampToDateTime(timestamp) {
  // 创建一个新的Date对象，使用时间戳作为参数
  const date = new Date(timestamp);

  // 获取年、月、日、时、分、秒
  const year = date.getFullYear();
  let month: string | number = date.getMonth() + 1; // getMonth()返回0-11，表示1-12月
  let day: string | number = date.getDate();
  let hour: string | number = date.getHours();
  let minute: string | number = date.getMinutes();
  let second: string | number = date.getSeconds();

  // 将月、日、时、分、秒转换为两位数的字符串
  month = month < 10 ? "0" + month : month;
  day = day < 10 ? "0" + day : day;
  hour = hour < 10 ? "0" + hour : hour;
  minute = minute < 10 ? "0" + minute : minute;
  second = second < 10 ? "0" + second : second;

  // 构造年月日时分秒的字符串
  var dateTimeStr =
    year + "-" + month + "-" + day + " " + hour + ":" + minute + ":" + second;

  return dateTimeStr;
}
