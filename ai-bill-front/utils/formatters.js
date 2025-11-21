/**
 * 公共格式化工具
 * @description 统一管理所有数据格式化逻辑，避免重复代码
 */

const { DATE_FORMAT, FILE_SIZE } = require('./constants');

/**
 * 格式化日期
 * @param {Date|string|Array} date 日期对象、日期字符串或数组格式
 * @param {string} format 目标格式，默认 'YYYY-MM-DD'
 * @returns {string} 格式化后的日期字符串
 */
const formatDate = (date, format = DATE_FORMAT.STANDARD) => {
  if (!date) return '';

  let dateObj;

  // 处理数组格式 [year, month, day]（后端LocalDate序列化格式）
  if (Array.isArray(date) && date.length >= 3) {
    const year = date[0];
    const month = String(date[1]).padStart(2, '0');
    const day = String(date[2]).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // 处理字符串格式
  if (typeof date === 'string') {
    // 移除时间部分（如果包含T或空格分隔符）
    if (date.includes('T')) {
      date = date.split('T')[0];
    } else if (date.includes(' ')) {
      date = date.split(' ')[0];
    }

    // 如果已经是标准格式，直接返回
    if (format === DATE_FORMAT.STANDARD && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      return date;
    }

    dateObj = new Date(date);
  } else if (date instanceof Date) {
    dateObj = date;
  } else {
    console.warn('不支持的日期格式:', date);
    return String(date);
  }

  // 验证日期有效性
  if (!dateObj || isNaN(dateObj.getTime())) {
    console.warn('无效的日期:', date);
    return String(date);
  }

  const year = dateObj.getFullYear();
  const month = dateObj.getMonth() + 1;
  const day = dateObj.getDate();

  // 根据格式返回
  switch (format) {
    case DATE_FORMAT.STANDARD:
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    case DATE_FORMAT.CHINESE:
      return `${year}年${month}月${day}日`;
    case DATE_FORMAT.MONTH_ONLY:
      return `${year}-${String(month).padStart(2, '0')}`;
    default:
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
  }
};

/**
 * 获取日期标签（今天、昨天、具体日期）
 * @param {string} dateString 日期字符串，格式 YYYY-MM-DD
 * @returns {string} 日期标签
 */
const getDayLabel = (dateString) => {
  if (!dateString) return '';

  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);

  const targetDate = new Date(dateString);

  // 验证日期有效性
  if (isNaN(targetDate.getTime())) {
    console.warn('无效的日期字符串:', dateString);
    return dateString;
  }

  const todayStr = formatDate(today);
  const yesterdayStr = formatDate(yesterday);
  const targetStr = formatDate(targetDate);

  if (targetStr === todayStr) {
    return '今天';
  } else if (targetStr === yesterdayStr) {
    return '昨天';
  } else {
    return `${targetDate.getDate()}日`;
  }
};

/**
 * 格式化货币金额
 * @param {number|string} amount 金额
 * @param {number} decimals 小数位数，默认2位
 * @returns {string} 格式化后的金额字符串
 */
const formatCurrency = (amount, decimals = 2) => {
  if (amount === null || amount === undefined || amount === '') {
    return (0).toFixed(decimals);
  }

  const num = parseFloat(amount);
  if (isNaN(num)) {
    console.warn('无效的金额:', amount);
    return (0).toFixed(decimals);
  }

  return Math.abs(num).toFixed(decimals);
};

/**
 * 格式化文件大小
 * @param {number} bytes 文件大小（字节）
 * @returns {string} 格式化后的文件大小字符串
 */
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B';

  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(FILE_SIZE.KB));

  return parseFloat((bytes / Math.pow(FILE_SIZE.KB, i)).toFixed(2)) + ' ' + sizes[i];
};

/**
 * 格式化手机号（中间4位显示为*）
 * @param {string} phone 手机号
 * @returns {string} 格式化后的手机号
 */
const formatPhone = (phone) => {
  if (!phone || phone.length !== 11) {
    return phone || '';
  }
  return phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
};

/**
 * 格式化百分比
 * @param {number} value 数值
 * @param {number} total 总数
 * @param {number} decimals 小数位数，默认1位
 * @returns {string} 格式化后的百分比字符串
 */
const formatPercent = (value, total, decimals = 1) => {
  if (!total || total === 0) return '0%';
  const percent = (value / total) * 100;
  return percent.toFixed(decimals) + '%';
};

/**
 * 格式化数字（千分位分隔）
 * @param {number|string} num 数字
 * @returns {string} 格式化后的数字字符串
 */
const formatNumber = (num) => {
  if (num === null || num === undefined || num === '') {
    return '0';
  }

  const number = parseFloat(num);
  if (isNaN(number)) {
    return '0';
  }

  return number.toLocaleString('zh-CN');
};

/**
 * 格式化时长（秒转为 XX小时XX分钟）
 * @param {number} seconds 秒数
 * @returns {string} 格式化后的时长字符串
 */
const formatDuration = (seconds) => {
  if (!seconds || seconds <= 0) return '0分钟';

  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);

  if (hours > 0) {
    return `${hours}小时${minutes}分钟`;
  }
  return `${minutes}分钟`;
};

/**
 * 截断长文本
 * @param {string} text 文本
 * @param {number} maxLength 最大长度，默认50
 * @param {string} suffix 后缀，默认'...'
 * @returns {string} 截断后的文本
 */
const truncateText = (text, maxLength = 50, suffix = '...') => {
  if (!text || text.length <= maxLength) {
    return text || '';
  }
  return text.substring(0, maxLength) + suffix;
};

module.exports = {
  formatDate,
  getDayLabel,
  formatCurrency,
  formatFileSize,
  formatPhone,
  formatPercent,
  formatNumber,
  formatDuration,
  truncateText
};
