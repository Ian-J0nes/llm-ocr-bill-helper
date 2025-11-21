/**
 * API 接口模块
 * 根据后端 API 文档 (api.md) 统一管理所有接口
 * 基础 URL: http://localhost:8080
 */

const { request } = require('./api');

/**
 * 构建 query 参数字符串（兼容微信小程序环境）
 * @param {Object} params - 参数对象
 * @returns {string} query 字符串
 */
function buildQueryString(params) {
  if (!params || typeof params !== 'object') {
    return '';
  }
  return Object.keys(params)
    .filter(key => params[key] !== null && params[key] !== undefined)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
}

/**
 * 1. 用户相关接口 (/user)
 */
const userApi = {
  /**
   * 微信 code 登录
   * @param {string} code - 微信登录 code
   * @returns {Promise} 登录信息 (含 token)
   */
  wxLogin(code) {
    return request({
      endpoint: '/user/wxlogin',
      method: 'POST',
      data: { code },
      needAuth: false,
      useCache: false
    });
  },

  /**
   * 根据 ID 获取用户信息
   * @param {string} id - 用户ID
   * @returns {Promise} 用户信息
   */
  getUserById(id) {
    return request({
      endpoint: `/user/${id}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 根据用户名获取用户信息
   * @param {string} username - 用户名
   * @returns {Promise} 用户信息
   */
  getUserByUsername(username) {
    return request({
      endpoint: `/user/username/${username}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取当前登录用户信息
   * @returns {Promise} 当前用户信息
   */
  getCurrentUser() {
    return request({
      endpoint: '/user/me',
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取用户列表（管理员）
   * @param {Object} params - 查询参数 {current, size}
   * @returns {Promise} 用户列表或分页对象
   */
  getUserList(params = {}) {
    return request({
      endpoint: '/user',
      method: 'GET',
      data: params,
      needAuth: true
    });
  },

  /**
   * 更新用户信息
   * @param {string} id - 用户ID
   * @param {Object} userData - 用户数据
   * @returns {Promise} 更新结果
   */
  updateUser(id, userData) {
    return request({
      endpoint: `/user/${id}`,
      method: 'PUT',
      data: userData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 删除用户（逻辑删除）
   * @param {string} id - 用户ID
   * @returns {Promise} 删除结果
   */
  deleteUser(id) {
    return request({
      endpoint: `/user/${id}`,
      method: 'DELETE',
      needAuth: true,
      useCache: false
    });
  }
};

/**
 * 2. 用户预算接口 (/user-budget)
 */
const userBudgetApi = {
  /**
   * 获取预算列表
   * @param {Object} params - 查询参数 {budgetType, current, size}
   * @returns {Promise} 预算列表或分页对象
   */
  getBudgetList(params = {}) {
    return request({
      endpoint: '/user-budget',
      method: 'GET',
      data: params,
      needAuth: true
    });
  },

  /**
   * 获取预算详情
   * @param {string} id - 预算ID
   * @returns {Promise} 预算详情
   */
  getBudgetById(id) {
    return request({
      endpoint: `/user-budget/${id}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 新增预算
   * @param {Object} budgetData - 预算数据 {budgetAmount, budgetType, startDate, endDate, alertThreshold}
   * @returns {Promise} 创建结果
   */
  createBudget(budgetData) {
    return request({
      endpoint: '/user-budget',
      method: 'POST',
      data: budgetData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 更新预算
   * @param {string} id - 预算ID
   * @param {Object} budgetData - 预算数据
   * @returns {Promise} 更新结果
   */
  updateBudget(id, budgetData) {
    return request({
      endpoint: `/user-budget/${id}`,
      method: 'PUT',
      data: budgetData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 删除预算
   * @param {string} id - 预算ID
   * @returns {Promise} 删除结果
   */
  deleteBudget(id) {
    return request({
      endpoint: `/user-budget/${id}`,
      method: 'DELETE',
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 获取当前生效预算
   * @returns {Promise} 生效预算列表
   */
  getActiveBudgets() {
    return request({
      endpoint: '/user-budget/active',
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取预算统计
   * @returns {Promise} 预算统计数据
   */
  getBudgetStatistics() {
    return request({
      endpoint: '/user-budget/statistics',
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取预算预警
   * @returns {Promise} 预警预算列表
   */
  getBudgetAlerts() {
    return request({
      endpoint: '/user-budget/alerts',
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取即将到期预算
   * @returns {Promise} 即将到期预算列表
   */
  getExpiringBudgets() {
    return request({
      endpoint: '/user-budget/expiring',
      method: 'GET',
      needAuth: true
    });
  }
};

/**
 * 3. 账单接口 (/bill)
 */
const billApi = {
  /**
   * 获取账单详情
   * @param {string} id - 账单ID
   * @returns {Promise} 账单详情
   */
  getBillById(id) {
    return request({
      endpoint: `/bill/${id}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 获取账单列表（支持分页和筛选）
   * @param {Object} params - 查询参数 {current, size, transactionType, billType, categoryId, startDate, endDate}
   * @returns {Promise} 账单列表或分页对象
   */
  getBillList(params = {}) {
    return request({
      endpoint: '/bill',
      method: 'GET',
      data: params,
      needAuth: true,
      useCache: false // 账单数据不缓存，确保实时性
    });
  },

  /**
   * 新增账单
   * @param {Object} billData - 账单数据 {totalAmount, transactionType, ...}
   * @returns {Promise} 创建结果 {id, fileId, transactionType}
   */
  createBill(billData) {
    return request({
      endpoint: '/bill',
      method: 'POST',
      data: billData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 更新账单
   * @param {string} id - 账单ID
   * @param {Object} billData - 账单数据
   * @returns {Promise} 更新结果
   */
  updateBill(id, billData) {
    return request({
      endpoint: `/bill/${id}`,
      method: 'PUT',
      data: billData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 删除账单
   * @param {string} id - 账单ID
   * @returns {Promise} 删除结果
   */
  deleteBill(id) {
    return request({
      endpoint: `/bill/${id}`,
      method: 'DELETE',
      needAuth: true,
      useCache: false
    });
  }
};

/**
 * 4. 账单分类接口 (/bill-category)
 */
const billCategoryApi = {
  /**
   * 获取分类列表
   * @param {Object} params - 查询参数 {current, size}
   * @returns {Promise} 分类列表或分页对象
   */
  getCategoryList(params = {}) {
    return request({
      endpoint: '/bill-category',
      method: 'GET',
      data: params,
      needAuth: true
    });
  },

  /**
   * 获取分类详情
   * @param {string} id - 分类ID
   * @returns {Promise} 分类详情
   */
  getCategoryById(id) {
    return request({
      endpoint: `/bill-category/${id}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 新增分类
   * @param {Object} categoryData - 分类数据 {categoryName, ...}
   * @returns {Promise} 创建结果
   */
  createCategory(categoryData) {
    return request({
      endpoint: '/bill-category',
      method: 'POST',
      data: categoryData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 更新分类
   * @param {string} id - 分类ID
   * @param {Object} categoryData - 分类数据
   * @returns {Promise} 更新结果
   */
  updateCategory(id, categoryData) {
    return request({
      endpoint: `/bill-category/${id}`,
      method: 'PUT',
      data: categoryData,
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 删除分类
   * @param {string} id - 分类ID
   * @returns {Promise} 删除结果
   */
  deleteCategory(id) {
    return request({
      endpoint: `/bill-category/${id}`,
      method: 'DELETE',
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 更新分类状态
   * @param {string} id - 分类ID
   * @param {number} status - 状态 (0/1)
   * @returns {Promise} 更新结果
   */
  updateCategoryStatus(id, status) {
    return request({
      endpoint: `/bill-category/${id}/status/${status}`,
      method: 'PUT',
      needAuth: true,
      useCache: false
    });
  },

  /**
   * 获取系统预设分类
   * @returns {Promise} 系统分类列表
   */
  getSystemCategories() {
    return request({
      endpoint: '/bill-category/system',
      method: 'GET',
      needAuth: false
    });
  },

  /**
   * 获取当前用户可用分类名称
   * @returns {Promise} 分类名称列表
   */
  getCategoryNames() {
    return request({
      endpoint: '/bill-category/names',
      method: 'GET',
      needAuth: true
    });
  }
};

/**
 * 5. 发票文件接口 (/files)
 * 注意：文件上传需要使用 wx.uploadFile，不能使用普通的 request
 */
const invoiceFileApi = {
  /**
   * 上传发票文件
   * 注意：此方法需要特殊处理，使用 wx.uploadFile
   * @param {string} filePath - 本地文件路径
   * @param {function} onProgress - 上传进度回调
   * @returns {Promise} 上传结果 {fileId}
   */
  uploadFile(filePath, onProgress) {
    return new Promise((resolve, reject) => {
      const app = getApp();
      const token = wx.getStorageSync('token');

      if (!token) {
        reject(new Error('用户未登录'));
        return;
      }

      const uploadTask = wx.uploadFile({
        url: `${app.globalData.baseURL}/files`,
        filePath: filePath,
        name: 'file',
        header: {
          'Authorization': `Bearer ${token}`
        },
        success: (res) => {
          try {
            const data = typeof res.data === 'string' ? JSON.parse(res.data) : res.data;
            if (data.code === 200) {
              resolve(data);
            } else {
              reject(new Error(data.message || '上传失败'));
            }
          } catch (e) {
            reject(new Error('响应数据解析失败'));
          }
        },
        fail: (err) => {
          reject(err);
        }
      });

      // 监听上传进度
      if (onProgress && typeof onProgress === 'function') {
        uploadTask.onProgressUpdate((res) => {
          onProgress(res.progress);
        });
      }
    });
  },

  /**
   * 获取文件信息
   * @param {string} fileId - 文件ID
   * @returns {Promise} 文件信息
   */
  getFileInfo(fileId) {
    return request({
      endpoint: `/files/${fileId}`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 检查文件是否存在
   * @param {string} fileId - 文件ID
   * @returns {Promise} 是否存在 (true/false)
   */
  checkFileExists(fileId) {
    return request({
      endpoint: `/files/${fileId}/exists`,
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 删除文件
   * @param {string} fileId - 文件ID
   * @returns {Promise} 删除结果
   */
  deleteFile(fileId) {
    return request({
      endpoint: `/files/${fileId}`,
      method: 'DELETE',
      needAuth: true,
      useCache: false
    });
  }
};

/**
 * 6. 汇率接口 (/api/exchange)
 */
const exchangeRateApi = {
  /**
   * 获取支持的货币列表
   * @returns {Promise} 货币列表
   */
  getCurrencies() {
    return request({
      endpoint: '/api/exchange/currencies',
      method: 'GET',
      needAuth: false
    });
  },

  /**
   * 使用存储汇率进行换算
   * @param {Object} params - 换算参数 {amount, from, to}
   * @returns {Promise} 换算结果
   */
  convertCurrency(params) {
    // 构建 query 参数字符串（兼容微信小程序）
    const queryString = buildQueryString(params);
    return request({
      endpoint: `/api/exchange/conversions?${queryString}`,
      method: 'POST',
      needAuth: false
    });
  },

  /**
   * 手动触发汇率更新（管理员）
   * @returns {Promise} 更新结果
   */
  triggerUpdate() {
    return request({
      endpoint: '/api/exchange/admin/updates',
      method: 'POST',
      needAuth: false,
      useCache: false
    });
  }
};

/**
 * 7. AI 聊天接口 (/aio)
 * 注意：此接口返回流式响应，需要特殊处理
 */
const aioChatApi = {
  /**
   * AI 聊天（支持文本和附件）
   * 注意：此方法需要特殊处理，返回流式响应
   * 使用 wx.request 的 enableChunked: true 来接收流式数据
   * @param {Object} params - 聊天参数 {message, files}
   * @param {function} onChunk - 接收到数据块的回调
   * @returns {Promise} 请求任务对象
   */
  sendMessage(params, onChunk) {
    return new Promise((resolve, reject) => {
      const app = getApp();
      const token = wx.getStorageSync('token');

      if (!token) {
        reject(new Error('用户未登录'));
        return;
      }

      // 如果有文件，使用 uploadFile
      if (params.files && params.files.length > 0) {
        const formData = {};
        if (params.message) {
          formData.message = params.message;
        }

        wx.uploadFile({
          url: `${app.globalData.baseURL}/aio/messages`,
          filePath: params.files[0], // 目前只支持单个文件
          name: 'files',
          formData: formData,
          header: {
            'Authorization': `Bearer ${token}`
          },
          success: (res) => {
            // 流式响应处理
            if (onChunk && typeof onChunk === 'function') {
              onChunk(res.data);
            }
            resolve(res.data);
          },
          fail: (err) => {
            reject(err);
          }
        });
      } else {
        // 纯文本消息，使用普通 request
        wx.request({
          url: `${app.globalData.baseURL}/aio/messages`,
          method: 'POST',
          data: params,
          header: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          enableChunked: true, // 启用分块传输
          success: (res) => {
            if (onChunk && typeof onChunk === 'function') {
              onChunk(res.data);
            }
            resolve(res.data);
          },
          fail: (err) => {
            reject(err);
          }
        });
      }
    });
  }
};

/**
 * 8. AI 洞察接口 (/ai-insight)
 * 注意：所有接口都返回流式响应
 */
const aiInsightApi = {
  /**
   * 获取月度洞察（流式）
   * @param {string} date - 日期 (yyyy-MM-dd)，可选
   * @param {function} onChunk - 接收到数据块的回调
   * @returns {Promise} 请求任务对象
   */
  getMonthlyInsight(date, onChunk) {
    return this._getInsight('monthly', date, onChunk);
  },

  /**
   * 获取季度洞察（流式）
   * @param {string} date - 日期 (yyyy-MM-dd)，可选
   * @param {function} onChunk - 接收到数据块的回调
   * @returns {Promise} 请求任务对象
   */
  getQuarterlyInsight(date, onChunk) {
    return this._getInsight('quarterly', date, onChunk);
  },

  /**
   * 获取年度洞察（流式）
   * @param {string} date - 日期 (yyyy-MM-dd)，可选
   * @param {function} onChunk - 接收到数据块的回调
   * @returns {Promise} 请求任务对象
   */
  getYearlyInsight(date, onChunk) {
    return this._getInsight('yearly', date, onChunk);
  },

  /**
   * 获取原始财务摘要（调试用）
   * @param {string} period - 周期 (monthly/quarterly/yearly)
   * @param {string} date - 日期 (yyyy-MM-dd)，可选
   * @returns {Promise} 财务摘要文本
   */
  getSummary(period, date) {
    const params = date ? { date } : {};
    return request({
      endpoint: `/ai-insight/summary/${period}`,
      method: 'GET',
      data: params,
      needAuth: true,
      rawResponse: true // 返回原始文本
    });
  },

  /**
   * 内部方法：获取洞察（流式）
   * @private
   */
  _getInsight(type, date, onChunk) {
    return new Promise((resolve, reject) => {
      const app = getApp();
      const token = wx.getStorageSync('token');

      if (!token) {
        reject(new Error('用户未登录'));
        return;
      }

      const params = date ? { date } : {};
      const url = `${app.globalData.baseURL}/ai-insight/${type}`;

      wx.request({
        url: url,
        method: 'GET',
        data: params,
        header: {
          'Authorization': `Bearer ${token}`
        },
        enableChunked: true, // 启用分块传输
        success: (res) => {
          if (onChunk && typeof onChunk === 'function') {
            onChunk(res.data);
          }
          resolve(res.data);
        },
        fail: (err) => {
          reject(err);
        }
      });
    });
  }
};

/**
 * 9. AI 配置接口 (/ai-config)
 */
const aiConfigApi = {
  /**
   * 获取可用 AI 模型列表
   * @returns {Promise} AI 模型列表
   */
  getModels() {
    return request({
      endpoint: '/ai-config/models',
      method: 'GET',
      needAuth: false
    });
  },

  /**
   * 获取当前用户 AI 配置
   * @returns {Promise} 用户 AI 配置 (含 aiModel, aiTemperature)
   */
  getUserConfig() {
    return request({
      endpoint: '/ai-config/user',
      method: 'GET',
      needAuth: true
    });
  },

  /**
   * 更新用户 AI 配置
   * @param {Object} config - AI 配置 {aiModel, aiTemperature}
   * @returns {Promise} 更新结果
   */
  updateUserConfig(config) {
    // 构建 query 参数字符串（兼容微信小程序）
    const queryString = buildQueryString(config);
    return request({
      endpoint: `/ai-config/user?${queryString}`,
      method: 'PUT',
      needAuth: true,
      useCache: false
    });
  }
};

// 导出所有 API 模块
module.exports = {
  userApi,
  userBudgetApi,
  billApi,
  billCategoryApi,
  invoiceFileApi,
  exchangeRateApi,
  aioChatApi,
  aiInsightApi,
  aiConfigApi
};
