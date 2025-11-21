/**
 * 路由优化工具
 * @description 统一管理页面导航，防止重复跳转
 */

const { PAGES, NAVIGATION } = require('./constants');

let isNavigating = false; // 是否正在导航

/**
 * 获取导航方法
 * @param {string} type 导航类型
 */
function getNavigationMethod(type) {
  const methods = {
    navigateTo: wx.navigateTo,
    redirectTo: wx.redirectTo,
    switchTab: wx.switchTab,
    reLaunch: wx.reLaunch
  };
  return methods[type] || wx.navigateTo;
}

/**
 * 优化的导航方法 - 防止快速连续点击
 * @param {Object} options 导航选项
 * @param {string} options.url 目标页面路径
 * @param {string} options.type 导航类型
 * @param {Object} options.params 导航参数
 * @param {number} options.timeout 超时时间
 */
function navigate(options) {
  let { url, type = 'navigateTo', params = {}, timeout = NAVIGATION.DEFAULT_TIMEOUT } = options;

  if (params && Object.keys(params).length > 0) {
    const query = Object.keys(params).map(k => `${encodeURIComponent(k)}=${encodeURIComponent(params[k])}`).join('&');
    if (url.includes('?')) {
      url += `&${query}`;
    } else {
      url += `?${query}`;
    }
  }

  if (isNavigating) {
    console.warn('正在导航中，忽略重复请求');
    return Promise.reject(new Error('正在导航中'));
  }

  const pages = getCurrentPages();
  const currentPage = pages[pages.length - 1];
  if (currentPage && currentPage.route === url.replace(/^\//, '')) {
    console.warn('已在目标页面，忽略导航请求');
    return Promise.resolve();
  }

  if (url.includes(PAGES.LOGIN)) {
    if (currentPage && currentPage.route === PAGES.LOGIN.substring(1)) {
      console.warn('已在登录页面，忽略重复跳转');
      return Promise.resolve();
    }
    type = 'reLaunch';
  }

  isNavigating = true;

  return new Promise((resolve, reject) => {
    const navigationMethod = getNavigationMethod(type);
    let timeoutId;
    let completed = false;

    timeoutId = setTimeout(() => {
      if (!completed) {
        completed = true;
        isNavigating = false;
        console.error('导航超时:', url);
        reject(new Error(`导航超时: ${url}`));
      }
    }, timeout);

    navigationMethod({
      url: url,
      success: (res) => {
        if (!completed) {
          completed = true;
          clearTimeout(timeoutId);
          isNavigating = false;
          resolve(res);
        }
      },
      fail: (err) => {
        if (!completed) {
          completed = true;
          clearTimeout(timeoutId);
          isNavigating = false;
          console.error('导航失败:', err);
          reject(err);
        }
      }
    });
  });
}

/**
 * 智能返回 - 根据页面栈情况选择最优返回方式
 * @param {Object} options 选项
 * @param {string} options.fallbackUrl 回退URL，默认首页
 * @param {number} options.delta 返回的页面层数
 */
function smartGoBack(options = {}) {
  const pages = getCurrentPages();
  const { fallbackUrl = PAGES.INDEX, delta = 1 } = options;
  
  if (pages.length > delta) {
    return new Promise((resolve, reject) => {
      wx.navigateBack({
        delta: delta,
        success: resolve,
        fail: reject
      });
    });
  } else {
    return navigate({
      url: fallbackUrl,
      type: 'reLaunch'
    });
  }
}

// 为了保持向后兼容，导出的对象名不变
const routerOptimizer = {
    navigate,
    smartGoBack
};

module.exports = {
  routerOptimizer
};
