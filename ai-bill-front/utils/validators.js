/**
 * 表单验证工具
 * @description 统一管理所有表单验证逻辑
 */

const { REGEX, TRANSACTION_TYPE } = require('./constants');

/**
 * 验证结果对象
 * @typedef {Object} ValidationResult
 * @property {boolean} valid 是否有效
 * @property {string} message 错误消息
 */

/**
 * 验证是否为空
 * @param {any} value 值
 * @returns {boolean} 是否为空
 */
const isEmpty = (value) => {
  if (value === null || value === undefined) return true;
  if (typeof value === 'string') return value.trim() === '';
  if (Array.isArray(value)) return value.length === 0;
  if (typeof value === 'object') return Object.keys(value).length === 0;
  return false;
};

/**
 * 验证金额
 * @param {number|string} amount 金额
 * @param {Object} options 选项
 * @param {number} options.min 最小值，默认0
 * @param {number} options.max 最大值，默认无限制
 * @param {boolean} options.allowZero 是否允许为0，默认false
 * @returns {ValidationResult} 验证结果
 */
const validateAmount = (amount, options = {}) => {
  const {
    min = 0,
    max = Infinity,
    allowZero = false
  } = options;

  if (isEmpty(amount)) {
    return { valid: false, message: '金额不能为空' };
  }

  const num = parseFloat(amount);

  if (isNaN(num)) {
    return { valid: false, message: '金额格式不正确' };
  }

  if (!allowZero && num <= 0) {
    return { valid: false, message: '金额必须大于0' };
  }

  if (num < min) {
    return { valid: false, message: `金额不能小于${min}` };
  }

  if (num > max) {
    return { valid: false, message: `金额不能大于${max}` };
  }

  // 检查小数位数（最多2位）
  const decimalStr = String(amount);
  const decimalIndex = decimalStr.indexOf('.');
  if (decimalIndex !== -1) {
    const decimalPlaces = decimalStr.length - decimalIndex - 1;
    if (decimalPlaces > 2) {
      return { valid: false, message: '金额最多保留2位小数' };
    }
  }

  return { valid: true, message: '' };
};

/**
 * 验证日期
 * @param {string} date 日期字符串，格式 YYYY-MM-DD
 * @param {Object} options 选项
 * @param {boolean} options.allowFuture 是否允许未来日期，默认true
 * @returns {ValidationResult} 验证结果
 */
const validateDate = (date, options = {}) => {
  const { allowFuture = true } = options;

  if (isEmpty(date)) {
    return { valid: false, message: '日期不能为空' };
  }

  // 验证日期格式
  if (!REGEX.DATE.test(date)) {
    return { valid: false, message: '日期格式不正确，应为YYYY-MM-DD' };
  }

  const dateObj = new Date(date);

  // 验证日期有效性
  if (isNaN(dateObj.getTime())) {
    return { valid: false, message: '无效的日期' };
  }

  // 验证是否为未来日期
  if (!allowFuture) {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (dateObj > today) {
      return { valid: false, message: '不能选择未来日期' };
    }
  }

  return { valid: true, message: '' };
};

/**
 * 验证账单名称
 * @param {string} name 名称
 * @param {Object} options 选项
 * @param {number} options.minLength 最小长度，默认1
 * @param {number} options.maxLength 最大长度，默认50
 * @returns {ValidationResult} 验证结果
 */
const validateBillName = (name, options = {}) => {
  const {
    minLength = 1,
    maxLength = 50
  } = options;

  if (isEmpty(name)) {
    return { valid: false, message: '账单名称不能为空' };
  }

  const trimmedName = String(name).trim();

  if (trimmedName.length < minLength) {
    return { valid: false, message: `账单名称至少${minLength}个字符` };
  }

  if (trimmedName.length > maxLength) {
    return { valid: false, message: `账单名称不能超过${maxLength}个字符` };
  }

  return { valid: true, message: '' };
};

/**
 * 验证账单分类
 * @param {string} billType 账单分类
 * @returns {ValidationResult} 验证结果
 */
const validateBillType = (billType) => {
  if (isEmpty(billType)) {
    return { valid: false, message: '请选择账单分类' };
  }

  return { valid: true, message: '' };
};

/**
 * 验证交易类型
 * @param {string} transactionType 交易类型
 * @returns {ValidationResult} 验证结果
 */
const validateTransactionType = (transactionType) => {
  if (isEmpty(transactionType)) {
    return { valid: false, message: '请选择交易类型' };
  }

  const validTypes = [TRANSACTION_TYPE.EXPENSE, TRANSACTION_TYPE.INCOME];
  if (!validTypes.includes(transactionType)) {
    return { valid: false, message: '无效的交易类型' };
  }

  return { valid: true, message: '' };
};

/**
 * 验证手机号
 * @param {string} phone 手机号
 * @returns {ValidationResult} 验证结果
 */
const validatePhone = (phone) => {
  if (isEmpty(phone)) {
    return { valid: false, message: '手机号不能为空' };
  }

  if (!REGEX.PHONE.test(phone)) {
    return { valid: false, message: '手机号格式不正确' };
  }

  return { valid: true, message: '' };
};

/**
 * 验证邮箱
 * @param {string} email 邮箱
 * @returns {ValidationResult} 验证结果
 */
const validateEmail = (email) => {
  if (isEmpty(email)) {
    return { valid: false, message: '邮箱不能为空' };
  }

  if (!REGEX.EMAIL.test(email)) {
    return { valid: false, message: '邮箱格式不正确' };
  }

  return { valid: true, message: '' };
};

/**
 * 验证密码强度
 * @param {string} password 密码
 * @param {Object} options 选项
 * @param {number} options.minLength 最小长度，默认6
 * @param {number} options.maxLength 最大长度，默认20
 * @param {boolean} options.requireNumber 是否要求包含数字，默认false
 * @param {boolean} options.requireLetter 是否要求包含字母，默认false
 * @returns {ValidationResult} 验证结果
 */
const validatePassword = (password, options = {}) => {
  const {
    minLength = 6,
    maxLength = 20,
    requireNumber = false,
    requireLetter = false
  } = options;

  if (isEmpty(password)) {
    return { valid: false, message: '密码不能为空' };
  }

  if (password.length < minLength) {
    return { valid: false, message: `密码至少${minLength}个字符` };
  }

  if (password.length > maxLength) {
    return { valid: false, message: `密码不能超过${maxLength}个字符` };
  }

  if (requireNumber && !/\d/.test(password)) {
    return { valid: false, message: '密码必须包含数字' };
  }

  if (requireLetter && !/[a-zA-Z]/.test(password)) {
    return { valid: false, message: '密码必须包含字母' };
  }

  return { valid: true, message: '' };
};

/**
 * 验证文本长度
 * @param {string} text 文本
 * @param {Object} options 选项
 * @param {number} options.minLength 最小长度
 * @param {number} options.maxLength 最大长度
 * @param {string} options.fieldName 字段名称
 * @returns {ValidationResult} 验证结果
 */
const validateTextLength = (text, options = {}) => {
  const {
    minLength = 0,
    maxLength = 500,
    fieldName = '文本'
  } = options;

  if (minLength > 0 && isEmpty(text)) {
    return { valid: false, message: `${fieldName}不能为空` };
  }

  const length = String(text || '').trim().length;

  if (length < minLength) {
    return { valid: false, message: `${fieldName}至少${minLength}个字符` };
  }

  if (length > maxLength) {
    return { valid: false, message: `${fieldName}不能超过${maxLength}个字符` };
  }

  return { valid: true, message: '' };
};

/**
 * 验证账单数据（完整验证）
 * @param {Object} billData 账单数据
 * @returns {ValidationResult} 验证结果
 */
const validateBillData = (billData) => {
  if (!billData || typeof billData !== 'object') {
    return { valid: false, message: '账单数据无效' };
  }

  // 验证账单名称
  const nameResult = validateBillName(billData.name);
  if (!nameResult.valid) {
    return nameResult;
  }

  // 验证总金额
  const amountResult = validateAmount(billData.totalAmount);
  if (!amountResult.valid) {
    return amountResult;
  }

  // 验证日期
  const dateResult = validateDate(billData.issueDate);
  if (!dateResult.valid) {
    return dateResult;
  }

  // 验证交易类型
  const typeResult = validateTransactionType(billData.transactionType);
  if (!typeResult.valid) {
    return typeResult;
  }

  // 验证账单分类
  const billTypeResult = validateBillType(billData.billType);
  if (!billTypeResult.valid) {
    return billTypeResult;
  }

  return { valid: true, message: '' };
};

module.exports = {
  isEmpty,
  validateAmount,
  validateDate,
  validateBillName,
  validateBillType,
  validateTransactionType,
  validatePhone,
  validateEmail,
  validatePassword,
  validateTextLength,
  validateBillData
};
