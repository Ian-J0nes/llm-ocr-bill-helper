/**
 * API请求工具
 * @description 统一管理所有API请求，处理认证、缓存、错误等
 */

const app = getApp();
const { cacheManager } = require('./cache');
const { tokenManager } = require('./token-manager');
const { HTTP_STATUS, RESULT_CODE, CACHE_TIME } = require('./constants');

/**
 * 根据状态码获取结果代码对象
 * @param {number} code 状态码
 * @returns {Object|null} 结果代码对象
 */
function getByCode(code) {
  for (const key in RESULT_CODE) {
    if (RESULT_CODE[key].code === code) {
      return RESULT_CODE[key];
    }
  }
  return null;
}

/**
 * 判断HTTP状态码是否表示成功
 * @param {number} code HTTP状态码
 * @returns {boolean} 是否成功
 */
function isSuccess(code) {
  return code >= HTTP_STATUS.SUCCESS && code < 300;
}

/**
 * 获取API基础URL
 * @returns {string} API基础URL
 */
function getBaseURL() {
  const baseURL = app.globalData.baseURL;
  if (!baseURL) {
    console.error('API baseURL未配置');
    throw new Error('API baseURL未配置');
  }
  return baseURL;
}

/**
 * 获取完整的API URL
 * @param {string} endpoint API端点
 * @returns {string} 完整的API URL
 */
function getApiURL(endpoint) {
  if (!endpoint) {
    throw new Error('API endpoint不能为空');
  }
  // 确保endpoint以/开头
  if (!endpoint.startsWith('/')) {
    endpoint = '/' + endpoint;
  }
  return `${getBaseURL()}${endpoint}`;
}

/**
 * 处理401未授权响应
 * @param {Object} res 响应对象
 */
function handleUnauthorized(res) {
  console.error('Token过期或无效:', res);

  // 使用token管理器清除无效的token
  tokenManager.clearToken();

  // 显示提示
  wx.showToast({
    title: RESULT_CODE.UNAUTHORIZED.message + '，请重新登录',
    icon: 'none',
    duration: 2000,
    complete: () => {
      // 重定向到登录页面
      app.navigateToLogin();
    }
  });
}

/**
 * 处理API响应结果
 * @param {Object} res 响应对象
 * @returns {Object|String} 处理后的响应
 */
function handleApiResponse(res) {
  // 处理HTTP状态码
  if (res.statusCode === HTTP_STATUS.UNAUTHORIZED) {
    handleUnauthorized(res);
    throw new Error(RESULT_CODE.UNAUTHORIZED.message);
  }
  
  // 检查响应数据类型
  if (res.data === null || res.data === undefined) {
    return { code: res.statusCode, message: '响应为空' };
  }
  
  // 处理字符串类型响应
  if (typeof res.data === 'string') {
    try {
      // 尝试解析为JSON
      return JSON.parse(res.data);
    } catch (e) {
      console.log('响应不是JSON格式，返回原始文本');
      // 如果不是有效的JSON，则直接返回原始文本
      return res.data;
    }
  }
  
  // 处理对象类型响应
  const responseData = res.data;
  
  // 处理业务状态码
  if (responseData && responseData.code) {
    const resultCodeObj = getByCode(responseData.code);
    if (resultCodeObj) {
      // 如果是错误状态码，且响应中没有自定义消息，则使用预定义的消息
      if (!isSuccess(resultCodeObj.code) && !responseData.message && !responseData.msg) {
        responseData.message = resultCodeObj.message;
      }
    }
  }
  
  return responseData;
}

/**
 * 统一的请求方法
 * @param {Object} options 请求选项
 * @param {string} options.endpoint API端点
 * @param {string} options.method 请求方法
 * @param {Object} options.data 请求数据
 * @param {Object} options.header 额外请求头
 * @param {boolean} options.needAuth 是否需要认证
 * @param {boolean} options.rawResponse 是否返回原始响应，不进行处理
 * @param {boolean} options.useCache 是否使用缓存，默认GET请求使用缓存
 * @param {number} options.cacheTime 缓存时间(毫秒)，默认使用中期缓存
 * @returns {Promise} 请求Promise，返回处理后的数据，而不是完整的wx.request响应
 */
function request({ endpoint, method = 'GET', data, header = {}, needAuth = true, rawResponse = false, useCache = method === 'GET', cacheTime = CACHE_TIME.MEDIUM }) {
  return new Promise((resolve, reject) => {
    const requestHeader = { ...header };

    // 添加认证头
    if (needAuth) {
      const token = tokenManager.getValidToken();
      if (!token) {
        reject(new Error('用户未登录'));
        return;
      }
      requestHeader['Authorization'] = `Bearer ${token}`;
    }

    // 生成缓存键
    const cacheKey = useCache ? cacheManager.generateKey(`api_${endpoint}_${method}`, data) : null;

    // 如果使用缓存且是GET请求，先尝试从缓存获取
    if (useCache && method === 'GET' && cacheKey) {
      const cachedData = cacheManager.get(cacheKey);
      if (cachedData) {
        console.log('使用缓存数据:', endpoint);
        resolve(cachedData);
        return;
      }
    }

    wx.request({
      url: getApiURL(endpoint),
      method,
      data,
      header: requestHeader,
      dataType: 'text', // 使用text而非json，以便自行处理非标准JSON响应
      responseType: 'text',
      success: (res) => {
        // 如果要求原始响应，直接返回
        if (rawResponse) {
          resolve(res);
          return;
        }

        // 处理HTTP错误
        if (res.statusCode >= HTTP_STATUS.SUCCESS && res.statusCode < 300) {
          try {
            const processedResponse = handleApiResponse(res);

            // 缓存成功的GET请求响应
            if (useCache && method === 'GET' && cacheKey && processedResponse) {
              cacheManager.set(cacheKey, processedResponse, cacheTime);
            }

            resolve(processedResponse);
          } catch (error) {
            // 返回原始数据，而不是拒绝Promise
            console.error('处理API响应时出错:', error);
            // 即使解析失败也尝试返回原始数据
            if (res.data !== undefined && res.data !== null) {
              resolve(res.data);
            } else {
              reject(error);
            }
          }
        } else if (res.statusCode === HTTP_STATUS.UNAUTHORIZED) {
          // 处理401未授权响应（token过期或无效）
          handleUnauthorized(res);
          reject(new Error(RESULT_CODE.UNAUTHORIZED.message));
        } else {
          // 处理其他HTTP错误
          const statusCodeObj = getByCode(res.statusCode);
          const errorMessage = statusCodeObj ? statusCodeObj.message : `请求失败: ${res.statusCode}`;
          reject(new Error(errorMessage));
        }
      },
      fail: (err) => {
        reject(err);
      }
    });
  });
}

module.exports = {
  getBaseURL,
  getApiURL,
  request,
  handleUnauthorized,
  handleApiResponse
}; 