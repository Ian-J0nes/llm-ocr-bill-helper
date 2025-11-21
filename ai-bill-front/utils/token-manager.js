// utils/token-manager.js - Token管理工具
class TokenManager {
  constructor() {
    this.tokenKey = 'token';
    this.listeners = new Set(); // token变化监听器
  }

  /**
   * 获取当前有效的token
   * @returns {string|null} token字符串或null
   */
  getToken() {
    // 优先从本地存储获取
    const localToken = wx.getStorageSync(this.tokenKey);
    if (localToken && this.isValidTokenFormat(localToken)) {
      // 同步到全局数据
      const app = getApp();
      if (app && app.globalData && app.globalData.token !== localToken) {
        app.globalData.token = localToken;
        app.globalData.isLoggedIn = true;
      }
      return localToken;
    }

    // 如果本地没有或格式无效，尝试从全局数据获取
    const app = getApp();
    if (app && app.globalData) {
      const globalToken = app.globalData.token;
      if (globalToken && this.isValidTokenFormat(globalToken)) {
        // 同步到本地存储
        wx.setStorageSync(this.tokenKey, globalToken);
        return globalToken;
      }
    }

    // 都没有有效token
    this.clearToken();
    return null;
  }

  /**
   * 设置token
   * @param {string} token token字符串
   */
  setToken(token) {
    if (!token || !this.isValidTokenFormat(token)) {
      console.error('尝试设置无效的token格式:', token);
      return false;
    }

    // 同时更新本地存储和全局数据
    wx.setStorageSync(this.tokenKey, token);
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.token = token;
      app.globalData.isLoggedIn = true;
    }

    // 通知监听器
    this.notifyListeners('set', token);
    
    console.log('Token已更新');
    return true;
  }

  /**
   * 清除token
   */
  clearToken() {
    wx.removeStorageSync(this.tokenKey);
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.token = null;
      app.globalData.isLoggedIn = false;
    }

    // 通知监听器
    this.notifyListeners('clear', null);
    
    console.log('Token已清除');
  }

  /**
   * 检查token格式是否有效
   * @param {string} token token字符串
   * @returns {boolean} 是否有效
   */
  isValidTokenFormat(token) {
    if (!token || typeof token !== 'string') {
      return false;
    }

    // JWT token应该有3个部分，用.分隔
    const parts = token.split('.');
    if (parts.length !== 3) {
      console.warn('Token格式无效: 应该包含3个部分，实际包含', parts.length, '个部分');
      return false;
    }

    // 检查每个部分是否为空
    if (parts.some(part => !part || part.trim() === '')) {
      console.warn('Token格式无效: 包含空的部分');
      return false;
    }

    return true;
  }

  /**
   * 检查token是否过期
   * @param {string} token token字符串
   * @returns {boolean} 是否过期
   */
  isTokenExpired(token) {
    if (!token || !this.isValidTokenFormat(token)) {
      return true;
    }

    try {
      // 解析JWT payload
      const payload = JSON.parse(atob(token.split('.')[1]));
      const now = Math.floor(Date.now() / 1000);
      
      // 检查exp字段
      if (payload.exp && payload.exp < now) {
        console.warn('Token已过期');
        return true;
      }

      return false;
    } catch (error) {
      console.warn('解析token失败:', error);
      return true;
    }
  }

  /**
   * 获取有效的token（检查过期）
   * @returns {string|null} 有效的token或null
   */
  getValidToken() {
    const token = this.getToken();
    if (!token) {
      return null;
    }

    if (this.isTokenExpired(token)) {
      console.warn('Token已过期，清除token');
      this.clearToken();
      return null;
    }

    return token;
  }

  /**
   * 添加token变化监听器
   * @param {Function} listener 监听器函数
   */
  addListener(listener) {
    this.listeners.add(listener);
  }

  /**
   * 移除token变化监听器
   * @param {Function} listener 监听器函数
   */
  removeListener(listener) {
    this.listeners.delete(listener);
  }

  /**
   * 通知所有监听器
   * @param {string} action 操作类型: 'set' | 'clear'
   * @param {string|null} token token值
   */
  notifyListeners(action, token) {
    this.listeners.forEach(listener => {
      try {
        listener(action, token);
      } catch (error) {
        console.error('Token监听器执行失败:', error);
      }
    });
  }

  /**
   * 刷新token（重新从存储获取）
   * @returns {string|null} 刷新后的token
   */
  refreshToken() {
    // 清除内存中的缓存，重新从存储获取
    const app = getApp();
    if (app && app.globalData) {
      app.globalData.token = null;
    }
    
    return this.getToken();
  }

  /**
   * 获取token的基本信息（不验证签名）
   * @param {string} token token字符串
   * @returns {Object|null} token信息
   */
  getTokenInfo(token) {
    if (!token || !this.isValidTokenFormat(token)) {
      return null;
    }

    try {
      const header = JSON.parse(atob(token.split('.')[0]));
      const payload = JSON.parse(atob(token.split('.')[1]));
      
      return {
        header,
        payload,
        isExpired: this.isTokenExpired(token),
        expiresAt: payload.exp ? new Date(payload.exp * 1000) : null,
        issuedAt: payload.iat ? new Date(payload.iat * 1000) : null
      };
    } catch (error) {
      console.error('解析token信息失败:', error);
      return null;
    }
  }
}

// 创建全局token管理器实例
const tokenManager = new TokenManager();

module.exports = {
  TokenManager,
  tokenManager
};
