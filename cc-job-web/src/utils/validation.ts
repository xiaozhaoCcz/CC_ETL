/**
 * 表单验证工具函数
 * 
 * @author cc-job-team
 */

/**
 * 验证邮箱格式
 */
export function validateEmail(email: string): boolean {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

/**
 * 验证手机号格式
 */
export function validatePhone(phone: string): boolean {
  const phoneRegex = /^1[3-9]\d{9}$/;
  return phoneRegex.test(phone);
}

/**
 * 验证URL格式
 */
export function validateUrl(url: string): boolean {
  try {
    new URL(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * 验证IP地址格式
 */
export function validateIP(ip: string): boolean {
  const ipRegex = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/;
  return ipRegex.test(ip);
}

/**
 * 验证端口号
 */
export function validatePort(port: number): boolean {
  return Number.isInteger(port) && port >= 1 && port <= 65535;
}

/**
 * 验证非空字符串
 */
export function validateRequired(value: any): boolean {
  if (value === null || value === undefined) {
    return false;
  }
  
  if (typeof value === 'string') {
    return value.trim().length > 0;
  }
  
  return true;
}

/**
 * 验证字符串长度
 */
export function validateLength(
  value: string,
  min: number,
  max?: number
): boolean {
  if (!value) {
    return false;
  }
  
  const length = value.trim().length;
  
  if (max !== undefined) {
    return length >= min && length <= max;
  }
  
  return length >= min;
}

/**
 * 验证数字范围
 */
export function validateRange(
  value: number,
  min: number,
  max?: number
): boolean {
  if (typeof value !== 'number') {
    return false;
  }
  
  if (max !== undefined) {
    return value >= min && value <= max;
  }
  
  return value >= min;
}

/**
 * 验证 JSON 格式
 */
export function validateJSON(str: string): boolean {
  try {
    JSON.parse(str);
    return true;
  } catch {
    return false;
  }
}

/**
 * Element Plus 表单规则生成器
 */
export const FormRules = {
  /**
   * 必填规则
   */
  required(message = '此项为必填项') {
    return {
      required: true,
      message,
      trigger: 'blur',
    };
  },
  
  /**
   * 邮箱规则
   */
  email(message = '请输入有效的邮箱地址') {
    return {
      type: 'email' as const,
      message,
      trigger: 'blur',
    };
  },
  
  /**
   * URL规则
   */
  url(message = '请输入有效的URL') {
    return {
      type: 'url' as const,
      message,
      trigger: 'blur',
    };
  },
  
  /**
   * 长度规则
   */
  length(min: number, max: number, message?: string) {
    return {
      min,
      max,
      message: message || `长度应在 ${min} 到 ${max} 个字符之间`,
      trigger: 'blur',
    };
  },
  
  /**
   * 数字范围规则
   */
  range(min: number, max: number, message?: string) {
    return {
      type: 'number' as const,
      min,
      max,
      message: message || `值应在 ${min} 到 ${max} 之间`,
      trigger: 'blur',
    };
  },
  
  /**
   * 自定义验证规则
   */
  custom(validator: (rule: any, value: any, callback: any) => void) {
    return {
      validator,
      trigger: 'blur',
    };
  },
};

