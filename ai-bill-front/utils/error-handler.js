/**
 * 统一错误处理工具
 * @description 统一管理错误提示、加载状态等UI交互
 */

const { TOAST_DURATION } = require('./constants');

/**
 * 显示Toast提示
 * @param {string} title 提示文本
 * @param {Object} options 选项
 * @param {string} options.icon 图标类型 'success' | 'error' | 'loading' | 'none'
 * @param {number} options.duration 显示时长（毫秒）
 * @param {Function} options.callback 显示完成后的回调
 * @returns {void}
 */
const showToast = (title, options = {}) => {
  const {
    icon = 'none',
    duration = TOAST_DURATION.NORMAL,
    callback = null
  } = options;

  wx.showToast({
    title: title || '操作完成',
    icon: icon,
    duration: duration
  });

  if (callback) {
    setTimeout(callback, duration);
  }
};

/**
 * 显示成功提示
 * @param {string} message 成功消息
 * @param {Function} callback 回调函数
 * @returns {void}
 */
const showSuccess = (message = '操作成功', callback = null) => {
  showToast(message, {
    icon: 'success',
    duration: TOAST_DURATION.NORMAL,
    callback
  });
};

/**
 * 显示错误提示
 * @param {string|Error|Object} error 错误对象或消息
 * @param {string} defaultMsg 默认错误消息
 * @returns {void}
 */
const showError = (error, defaultMsg = '操作失败') => {
  const errorMsg = parseErrorMessage(error, defaultMsg);

  showToast(errorMsg, {
    icon: 'none',
    duration: TOAST_DURATION.NORMAL
  });
};

/**
 * 显示警告提示
 * @param {string} message 警告消息
 * @returns {void}
 */
const showWarning = (message = '请注意') => {
  showToast(message, {
    icon: 'none',
    duration: TOAST_DURATION.SHORT
  });
};

/**
 * 显示模态对话框
 * @param {Object} options 选项
 * @param {string} options.title 标题
 * @param {string} options.content 内容
 * @param {boolean} options.showCancel 是否显示取消按钮
 * @param {string} options.confirmText 确认按钮文本
 * @param {string} options.cancelText 取消按钮文本
 * @param {Function} options.onConfirm 确认回调
 * @param {Function} options.onCancel 取消回调
 * @returns {void}
 */
const showModal = (options = {}) => {
  const {
    title = '提示',
    content = '',
    showCancel = true,
    confirmText = '确定',
    cancelText = '取消',
    confirmColor = '#576B95',
    onConfirm = null,
    onCancel = null
  } = options;

  wx.showModal({
    title,
    content,
    showCancel,
    confirmText,
    cancelText,
    confirmColor,
    success: (res) => {
      if (res.confirm && onConfirm) {
        onConfirm();
      } else if (res.cancel && onCancel) {
        onCancel();
      }
    }
  });
};

/**
 * 显示错误模态框
 * @param {string|Error|Object} error 错误对象或消息
 * @param {string} title 标题
 * @param {string} defaultMsg 默认错误消息
 * @returns {void}
 */
const showErrorModal = (error, title = '错误', defaultMsg = '操作失败') => {
  const errorMsg = parseErrorMessage(error, defaultMsg);

  showModal({
    title,
    content: errorMsg,
    showCancel: false,
    confirmText: '知道了'
  });
};

/**
 * 显示确认对话框
 * @param {Object} options 选项
 * @param {string} options.title 标题
 * @param {string} options.content 内容
 * @param {Function} options.onConfirm 确认回调
 * @param {Function} options.onCancel 取消回调
 * @returns {void}
 */
const showConfirm = (options = {}) => {
  const {
    title = '确认操作',
    content = '确定要执行此操作吗？',
    onConfirm = null,
    onCancel = null,
    confirmColor = '#576B95'
  } = options;

  showModal({
    title,
    content,
    showCancel: true,
    confirmText: '确定',
    cancelText: '取消',
    confirmColor,
    onConfirm,
    onCancel
  });
};

/**
 * 显示删除确认对话框
 * @param {Object} options 选项
 * @param {string} options.title 标题
 * @param {string} options.content 内容
 * @param {Function} options.onConfirm 确认回调
 * @returns {void}
 */
const showDeleteConfirm = (options = {}) => {
  const {
    title = '确认删除',
    content = '确定要删除吗？删除后无法恢复。',
    onConfirm = null
  } = options;

  showModal({
    title,
    content,
    showCancel: true,
    confirmText: '删除',
    cancelText: '取消',
    confirmColor: '#ff4444',
    onConfirm
  });
};

/**
 * 显示加载提示
 * @param {string} title 提示文本
 * @param {boolean} mask 是否显示透明蒙层，防止触摸穿透
 * @returns {void}
 */
const showLoading = (title = '加载中...', mask = true) => {
  wx.showLoading({
    title,
    mask
  });
};

/**
 * 隐藏加载提示
 * @returns {void}
 */
const hideLoading = () => {
  wx.hideLoading();
};

/**
 * 解析错误消息
 * @param {string|Error|Object} error 错误对象
 * @param {string} defaultMsg 默认消息
 * @returns {string} 错误消息
 */
const parseErrorMessage = (error, defaultMsg = '未知错误') => {
  if (!error) {
    return defaultMsg;
  }

  // 如果是字符串，直接返回
  if (typeof error === 'string') {
    return error;
  }

  // 如果是Error对象
  if (error instanceof Error) {
    return error.message || defaultMsg;
  }

  // 如果是对象，尝试提取消息
  if (typeof error === 'object') {
    // 优先使用message字段
    if (error.message) {
      return error.message;
    }
    // 其次使用msg字段
    if (error.msg) {
      return error.msg;
    }
    // 尝试使用errMsg字段（微信API错误）
    if (error.errMsg) {
      return parseMiniProgramError(error.errMsg);
    }
    // 尝试使用data.message字段
    if (error.data && error.data.message) {
      return error.data.message;
    }
  }

  return defaultMsg;
};

/**
 * 解析小程序API错误消息，转换为用户友好的提示
 * @param {string} errMsg 小程序错误消息
 * @returns {string} 友好的错误消息
 */
const parseMiniProgramError = (errMsg) => {
  if (!errMsg || typeof errMsg !== 'string') {
    return '操作失败';
  }

  // 网络错误
  if (errMsg.includes('request:fail')) {
    if (errMsg.includes('timeout')) {
      return '请求超时，请检查网络连接';
    }
    if (errMsg.includes('net::ERR_')) {
      return '网络连接失败，请检查网络';
    }
    return '网络请求失败';
  }

  // 上传错误
  if (errMsg.includes('uploadFile:fail')) {
    if (errMsg.includes('timeout')) {
      return '上传超时，请重试';
    }
    if (errMsg.includes('size')) {
      return '文件大小超出限制';
    }
    return '上传失败';
  }

  // 下载错误
  if (errMsg.includes('downloadFile:fail')) {
    return '下载失败';
  }

  // 授权错误
  if (errMsg.includes('auth deny') || errMsg.includes('permission')) {
    return '缺少必要的权限';
  }

  // 用户取消
  if (errMsg.includes('cancel')) {
    return '操作已取消';
  }

  // JSON解析错误
  if (errMsg.includes('SyntaxError') && errMsg.includes('JSON')) {
    return '数据格式错误';
  }

  // 其他错误，返回简化后的消息
  const colonIndex = errMsg.indexOf(':');
  if (colonIndex > 0) {
    return errMsg.substring(colonIndex + 1).trim();
  }

  return errMsg;
};

/**
 * 处理API响应错误
 * @param {Object} response API响应对象
 * @param {string} defaultMsg 默认错误消息
 * @returns {string} 错误消息
 */
const handleApiError = (response, defaultMsg = '请求失败') => {
  if (!response) {
    return defaultMsg;
  }

  // 检查HTTP状态码
  if (response.statusCode) {
    if (response.statusCode === 401) {
      return '登录已过期，请重新登录';
    }
    if (response.statusCode === 403) {
      return '无权限访问';
    }
    if (response.statusCode === 404) {
      return '请求的资源不存在';
    }
    if (response.statusCode >= 500) {
      return '服务器错误，请稍后重试';
    }
  }

  // 检查业务状态码
  if (response.code && response.code !== 200) {
    return parseErrorMessage(response, defaultMsg);
  }

  return defaultMsg;
};

module.exports = {
  showToast,
  showSuccess,
  showError,
  showWarning,
  showModal,
  showErrorModal,
  showConfirm,
  showDeleteConfirm,
  showLoading,
  hideLoading,
  parseErrorMessage,
  parseMiniProgramError,
  handleApiError
};
