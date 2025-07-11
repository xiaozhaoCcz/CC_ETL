/**
 * 雪花算法构成
 * 符号位（1bit）- 时间戳相对值（41bit）- 数据中心标志（5bit）- 机器标志（5bit）- 递增序号（12bit）
 * 0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 */
class Snowflake {
  // 机器ID bit数
  static get WorkIdBits() {
    return 5;
  }
  // 最大机器ID
  static get MaxWorkId() {
    return -1 ^ (-1 << Snowflake.WorkIdBits);
  }
  // 数据中心ID bit数
  static get DataCenterIdBits() {
    return 5;
  }
  // 做大数据中心ID
  static get MaxDataCenterId() {
    return -1 ^ (-1 << Snowflake.DataCenterIdBits);
  }
  // 序列号bit数
  static get SequenceBits() {
    return 12;
  }
  // 最大序列号
  static get MaxSequence() {
    return ~(-1 << Snowflake.SequenceBits);
  }
  // 上次ID时间戳
  #lastTimestamp = -1;
  // 序列号
  #sequence = 0;

  /**
   * 构造函数
   * @param {Number} dataCenterId 数据中心ID
   * @param {Number} workId 机器ID
   * @param {Boolean} randomStart  是否使用随机数（0-100之间）作为开始序号（避免数据倾斜）
   * @param {Number} twepoch 开始时间
   */
  constructor(dataCenterId, workId, randomStart, twepoch) {
    this.dataCenterId = dataCenterId || 0;
    this.workId = workId || 0;
    this.randomStart = randomStart || false;
    this.twepoch = twepoch || 1288834974657;
    if (
      this.dataCenterId < 0 ||
      this.dataCenterId > Snowflake.MaxDataCenterId
    ) {
      throw Error("数据中心ID不符合要求");
    }
    if (this.workId < 0 || this.workId > Snowflake.MaxWorkId) {
      throw Error("工作ID不符合要求");
    }
  }

  /**
   * 获取下一个ID
   * @param {*} num 需要生成的ID数量 默认一个
   * @param {*} singleReturnStr 一个的时候是否之间返回字符串类型（多个返回数组类型）
   * @returns id
   */
  nextId(num = 1, singleReturnStr = true) {
    num = num < 1 ? 1 : num;
    const currentTime = new Date().getTime();
    if (singleReturnStr && num === 1) {
      return this.#getNextId(currentTime);
    }
    const ids = [];
    for (let i = 0; i < num; i++) {
      ids.push(this.#getNextId(currentTime));
    }
    return ids;
  }

  /**
   * 解析雪花ID生成时间
   * @param {*} snowflakeId 雪花ID
   * @returns 生成时间时间戳
   */
  parseIdGenerateTime(snowflakeId) {
    // 雪花id转为二进制
    const idBinStr = this.#snowflakeIdParse(snowflakeId);
    // 获取时间结束位置的索引
    const endIndex =
      idBinStr.length -
      Snowflake.WorkIdBits -
      Snowflake.DataCenterIdBits -
      Snowflake.SequenceBits;
    // 把时间二进制字段转为数字 并加上twepoch时间 获取生成时间时间戳
    const generateTimeBin = idBinStr.substring(0, endIndex);
    return parseInt(generateTimeBin, 2) + this.twepoch;
  }

  parseDataCenterId(snowflakeId) {
    // 雪花id转为二进制
    const idBinStr = this.#snowflakeIdParse(snowflakeId);
    // 数据中心开始索引
    const startIndex =
      idBinStr.length -
      Snowflake.DataCenterIdBits -
      Snowflake.WorkIdBits -
      Snowflake.SequenceBits;
    // 数据中心结束索引
    const endIndex =
      idBinStr.length - Snowflake.WorkIdBits - Snowflake.SequenceBits;
    // 获取数据中心二进制数据
    const dataCenterIdBin = idBinStr.substring(startIndex, endIndex);
    return parseInt(dataCenterIdBin, 2);
  }

  parseWorkerId(snowflakeId) {
    // 雪花id转为二进制
    const idBinStr = this.#snowflakeIdParse(snowflakeId);
    // 机器ID开始索引
    const startIndex =
      idBinStr.length - Snowflake.WorkIdBits - Snowflake.SequenceBits;
    // 机器ID结束索引
    const endIndex = idBinStr.length - Snowflake.SequenceBits;
    // 获取数据中心二进制数据
    const workerIdBin = idBinStr.substring(startIndex, endIndex);
    return parseInt(workerIdBin, 2);
  }

  /**
   * 解析雪花ID
   * @param {*} snowflakeId 雪花ID
   * @returns 二进制类型雪花ID
   */
  #snowflakeIdParse(snowflakeId) {
    if (!snowflakeId) {
      throw Error("id不合法");
    }
    return BigInt(snowflakeId).toString(2);
  }

  /**
   * 获取下一个ID
   * @param {Number} currentTime  当前时间
   * @returns 下一个ID
   */
  #getNextId(currentTime) {
    if (currentTime < this.#lastTimestamp) {
      // 时钟回拨 将当前时间设置为最后一毫秒时间
      currentTime = this.#lastTimestamp;
    }
    if (currentTime === this.#lastTimestamp) {
      // 说明是统一毫秒内生成
      const nextSequence = (this.#sequence + 1) & Snowflake.MaxSequence;
      if (nextSequence === 0) {
        // 说明超了 使用下一毫秒 currentTime 也要设置为当前时间 因为后面会使用 currentTime 去修改 lastTimestamp 的值
        currentTime = this.#lastTimestamp = this.#lastTimestamp + 1;
        // 从0开始 忽略randomStart参数 id会用完那就不存在数据倾斜问题
        this.#sequence = 0;
      } else {
        // 正常生成
        this.#sequence = nextSequence;
      }
    } else {
      // 新的毫秒内
      if (this.randomStart) {
        this.#sequence = ~~(Math.random() * 101);
      } else {
        this.#sequence = 0;
      }
    }
    this.#lastTimestamp = currentTime;
    // 因为直接使用位运算会超过Number精度会出问题 所以先把所需参数转换为字符串 在使用BigInt解析回来
    // 需要解析的二进制字符串 把各部分进行组合 雪花算法第一位固定为0
    let binIdStr = "0b0";
    // 时间部分转二进制
    const timeBinStr = (this.#lastTimestamp - this.twepoch).toString(2);
    binIdStr = this.#buildPart(binIdStr, timeBinStr, 41);
    // 数据中心ID转二进制
    const dataCenterBinStr = this.dataCenterId.toString(2);
    binIdStr = this.#buildPart(binIdStr, dataCenterBinStr, 5);
    // 机器ID转二进制
    const workBinStr = this.workId.toString(2);
    binIdStr = this.#buildPart(binIdStr, workBinStr, 5);
    // 序列号转二进制
    const sequencebinStr = this.#sequence.toString(2);
    binIdStr = this.#buildPart(binIdStr, sequencebinStr, 12);
    // 使用BigInt解析二进制字符串 并转为字符串返回
    return BigInt(binIdStr).toString();
  }

  /**
   * 构造所需的部分参数
   * @param {String} originStr 原始字符串 在这个字符串基础上添加后续字符串
   * @param {String} binStr 需要添加的二进制字符串
   * @param {Number} length 当前部分长度
   * @returns 构建后的字符串
   */
  #buildPart(originStr, binStr, length) {
    if (binStr.length < length) {
      const _n = length - binStr.length;
      // 不足部分补0
      for (let i = 0; i < _n; i++) {
        originStr += "0";
      }
    }
    originStr += binStr;
    return originStr;
  }
}

// vue中使用打开注释
export default Snowflake;
