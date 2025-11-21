/**
 * 全局常量配置文件
 * @description 统一管理所有魔法数字和配置项，避免硬编码
 */

// ==================== HTTP 状态码 ====================
const HTTP_STATUS = {
  SUCCESS: 200,
  CREATED: 201,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  INTERNAL_SERVER_ERROR: 500
};

// ==================== 业务状态码 ====================
const RESULT_CODE = {
  SUCCESS: { code: 200, message: '操作成功' },
  UNAUTHORIZED: { code: 401, message: '未授权访问' },
  FORBIDDEN: { code: 403, message: '无权限' },
  NOT_FOUND: { code: 404, message: '资源不存在' },
  SERVER_ERROR: { code: 500, message: '服务器错误' }
};

// ==================== 文件大小限制 ====================
const FILE_SIZE = {
  MAX_IMAGE_SIZE: 20 * 1024 * 1024,  // 20MB - 图片上传最大限制
  MAX_UPLOAD_SIZE: 50 * 1024 * 1024, // 50MB - 通用上传最大限制
  KB: 1024,
  MB: 1024 * 1024,
  GB: 1024 * 1024 * 1024
};

// ==================== 缓存时间配置 ====================
const CACHE_TIME = {
  SHORT: 30 * 1000,           // 30秒 - 短期缓存
  MEDIUM: 2 * 60 * 1000,      // 2分钟 - 中期缓存
  LONG: 5 * 60 * 1000,        // 5分钟 - 长期缓存
  VERY_LONG: 10 * 60 * 1000,  // 10分钟 - 超长缓存
  HOUR: 60 * 60 * 1000,       // 1小时
  DAY: 24 * 60 * 60 * 1000    // 1天
};

// ==================== Toast 提示时间 ====================
const TOAST_DURATION = {
  SHORT: 1500,   // 1.5秒
  NORMAL: 2000,  // 2秒
  LONG: 3000     // 3秒
};

// ==================== 页面路径常量 ====================
const PAGES = {
  INDEX: '/pages/index/index',
  LOGIN: '/pages/login/login',
  CHAT: '/pages/chat/chat',
  BILL_DETAIL: '/pages/bill-detail/bill-detail',
  BILL_SUMMARY: '/pages/bill-summary/bill-summary',
  AI_INSIGHT: '/pages/ai-insight/ai-insight',
  MYSET: '/pages/myset/myset',
  USER_INFO: '/pages/user-info/user-info',
  USER_BUDGET: '/pages/user-budget/user-budget',
  CURRENCY_EXCHANGE: '/pages/currency-exchange/currency-exchange',
  AI_MODEL_SELECTION: '/pages/ai-model-selection/ai-model-selection',
  FEEDBACK: '/pages/feedback/feedback',
  LOGS: '/pages/logs/logs'
};

// ==================== 导航超时配置 ====================
const NAVIGATION = {
  DEFAULT_TIMEOUT: 5000,        // 5秒 - 默认导航超时
  PREVENT_REPEAT_DELAY: 1000,   // 1秒 - 防止重复跳转的延迟
  REDIRECT_DELAY: 1500          // 1.5秒 - 操作成功后跳转延迟
};

// ==================== UI 动画配置 ====================
const UI_CONFIG = {
  STREAMING_SPEED: 30,          // 30ms - 流式文本显示速度
  MONTH_PICKER_RANGE: 12,       // 12个月 - 月份选择器范围
  SCROLL_ANIMATION_DURATION: 300, // 300ms - 滚动动画时长
  DEBOUNCE_DELAY: 300           // 300ms - 防抖延迟
};

// ==================== 默认头像 ====================
const DEFAULT_AVATAR = {
  WECHAT_USER: 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0',
  LOCAL_DEFAULT: '/static/icons/user-default.png',
  XIAOMIE: '/static/icons/xiaomie_avatar.png'
};

// ==================== 账单默认分类 ====================
const DEFAULT_BILL_CATEGORIES = [
  '餐饮', '交通', '购物', '娱乐',
  '居家', '通讯', '医疗', '教育', '其他'
];

// ==================== 交易类型 ====================
const TRANSACTION_TYPE = {
  EXPENSE: 'expense',  // 支出
  INCOME: 'income'     // 收入
};

// ==================== 交易类型选项（用于UI） ====================
const TRANSACTION_TYPE_OPTIONS = [
  { value: TRANSACTION_TYPE.EXPENSE, name: '支出' },
  { value: TRANSACTION_TYPE.INCOME, name: '收入' }
];

// ==================== 日期格式 ====================
const DATE_FORMAT = {
  STANDARD: 'YYYY-MM-DD',           // 标准日期格式
  DATETIME: 'YYYY-MM-DD HH:mm:ss',  // 日期时间格式
  CHINESE: 'YYYY年MM月DD日',         // 中文日期格式
  MONTH_ONLY: 'YYYY-MM'             // 仅年月
};

// ==================== API 请求配置 ====================
const API_CONFIG = {
  DEFAULT_TIMEOUT: 60000,      // 60秒 - 默认请求超时
  UPLOAD_TIMEOUT: 120000,      // 120秒 - 上传请求超时
  DEFAULT_RETRY_COUNT: 3       // 默认重试次数
};

// ==================== 存储键名 ====================
const STORAGE_KEYS = {
  TOKEN: 'token',
  USER_ID: 'userId',
  AVATAR_URL: 'avatarUrl',
  NICK_NAME: 'nickName',
  USER_INFO: 'userInfo'
};

// ==================== 正则表达式 ====================
const REGEX = {
  // 金额格式：最多两位小数
  AMOUNT: /^[0-9]*\.?[0-9]{0,2}$/,
  // 日期格式：YYYY-MM-DD
  DATE: /^\d{4}-\d{2}-\d{2}$/,
  // 手机号
  PHONE: /^1[3-9]\d{9}$/,
  // 邮箱
  EMAIL: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
};

// ==================== 导出 ====================
module.exports = {
  HTTP_STATUS,
  RESULT_CODE,
  FILE_SIZE,
  CACHE_TIME,
  TOAST_DURATION,
  PAGES,
  NAVIGATION,
  UI_CONFIG,
  DEFAULT_AVATAR,
  DEFAULT_BILL_CATEGORIES,
  TRANSACTION_TYPE,
  TRANSACTION_TYPE_OPTIONS,
  DATE_FORMAT,
  API_CONFIG,
  STORAGE_KEYS,
  REGEX
};
