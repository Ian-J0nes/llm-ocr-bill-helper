/**
 * 统一登录跳转工具
 * @description 管理登录状态检查、跳转逻辑和用户信息
 */

const { routerOptimizer } = require('./router');
const { tokenManager } = require('./token-manager');
const { TOAST_DURATION, NAVIGATION, STORAGE_KEYS } = require('./constants');

class LoginHelper {
  constructor() {
    this.isNavigatingToLogin = false; // 防止重复跳转
  }

  /**
   * 检查登录状态
   * @returns {boolean} 是否已登录
   */
  isLoggedIn() {
    const token = tokenManager.getValidToken();
    return !!token;
  }

  /**
   * 统一的登录检查和跳转 - 使用Modal提示框
   * @param {Object} options 选项
   * @param {string} options.title 提示标题
   * @param {string} options.content 提示内容
   * @param {Function} options.onConfirm 确认登录后的回调
   * @param {Function} options.onCancel 取消登录的回调
   * @returns {boolean} 是否已登录
   */
  checkLoginWithPrompt(options = {}) {
    const {
      title = '提示',
      content = '请先登录后再使用此功能',
      onConfirm = null,
      onCancel = null
    } = options;

    if (this.isLoggedIn()) {
      return true;
    }

    // 防止重复弹窗
    if (this.isNavigatingToLogin) {
      console.warn('正在跳转到登录页面，忽略重复请求');
      return false;
    }

    wx.showModal({
      title: title,
      content: content,
      confirmText: '去登录',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.navigateToLoginWithModal().then(() => {
            if (onConfirm) onConfirm();
          }).catch(err => {
            console.error('跳转到登录页面失败:', err);
          });
        } else {
          if (onCancel) onCancel();
        }
      }
    });

    return false;
  }

  /**
   * 使用wx.navigateTo跳转到登录页面（用于Modal确认后）
   * @returns {Promise} 跳转Promise
   */
  navigateToLoginWithModal() {
    if (this.isNavigatingToLogin) {
      console.warn('正在跳转到登录页面，忽略重复请求');
      return Promise.resolve();
    }

    this.isNavigatingToLogin = true;

    return new Promise((resolve, reject) => {
      wx.navigateTo({
        url: '/pages/login/login',
        success: resolve,
        fail: reject
      });
    }).finally(() => {
      // 延迟重置状态，避免快速重复点击
      setTimeout(() => {
        this.isNavigatingToLogin = false;
      }, NAVIGATION.PREVENT_REPEAT_DELAY);
    });
  }

  /**
   * 直接跳转到登录页面（不弹窗）
   * @returns {Promise} 跳转Promise
   */
  navigateToLogin() {
    if (this.isNavigatingToLogin) {
      console.warn('正在跳转到登录页面，忽略重复请求');
      return Promise.resolve();
    }

    this.isNavigatingToLogin = true;

    return routerOptimizer.navigate({
      url: '/pages/login/login',
      type: 'reLaunch'
    }).finally(() => {
      // 延迟重置状态，避免快速重复点击
      setTimeout(() => {
        this.isNavigatingToLogin = false;
      }, NAVIGATION.PREVENT_REPEAT_DELAY);
    });
  }

  /**
   * 显示登录提示Toast
   * @param {string} message 提示消息
   */
  showLoginToast(message = '请先登录') {
    wx.showToast({
      title: message,
      icon: 'none',
      duration: TOAST_DURATION.NORMAL
    });
  }

  /**
   * 检查登录状态，未登录时显示Toast并跳转
   * @param {string} message Toast消息
   * @returns {boolean} 是否已登录
   */
  checkLoginWithToast(message = '请先登录') {
    if (this.isLoggedIn()) {
      return true;
    }

    this.showLoginToast(message);
    
    // 延迟跳转，让用户看到Toast
    setTimeout(() => {
      this.navigateToLogin();
    }, 1500);

    return false;
  }

  /**
   * 获取当前用户信息
   * @returns {Object|null} 用户信息或null
   */
  getCurrentUser() {
    if (!this.isLoggedIn()) {
      return null;
    }

    const app = getApp();
    return app.globalData.userInfo || null;
  }

  /**
   * 清除登录状态
   */
  logout() {
    tokenManager.clearToken();

    // 清除用户相关的本地存储
    wx.removeStorageSync(STORAGE_KEYS.AVATAR_URL);
    wx.removeStorageSync(STORAGE_KEYS.NICK_NAME);
    wx.removeStorageSync(STORAGE_KEYS.USER_ID);

    // 清除全局用户信息
    const app = getApp();
    app.globalData.userInfo = null;
    app.globalData.isLoggedIn = false;

    wx.showToast({
      title: '已退出登录',
      icon: 'success',
      duration: TOAST_DURATION.NORMAL
    });
  }

  /**
   * 页面混入 - 提供统一的登录检查方法
   */
  getPageMixin() {
    return {
      // 检查登录状态（带提示弹窗）
      checkLogin(options = {}) {
        return loginHelper.checkLoginWithPrompt(options);
      },

      // 检查登录状态（带Toast提示）
      checkLoginToast(message) {
        return loginHelper.checkLoginWithToast(message);
      },

      // 直接跳转到登录页面
      goToLogin() {
        return loginHelper.navigateToLogin();
      },

      // 获取当前用户信息
      getCurrentUser() {
        return loginHelper.getCurrentUser();
      },

      // 退出登录
      logout() {
        return loginHelper.logout();
      }
    };
  }
}

// 创建全局登录助手实例
const loginHelper = new LoginHelper();

module.exports = {
  LoginHelper,
  loginHelper
};
