/**
 * 统一缓存管理工具
 * @description 管理本地存储和内存缓存，支持过期时间控制
 */

const { CACHE_TIME } = require('./constants');

class CacheManager {
  constructor() {
    this.defaultExpireTime = CACHE_TIME.LONG; // 默认使用长期缓存（5分钟）
    this.requestCache = new Map(); // 内存中的请求缓存
  }

  /**
   * 设置缓存
   * @param {string} key 缓存键
   * @param {any} data 缓存数据
   * @param {number} expireTime 过期时间(毫秒)，默认5分钟
   */
  set(key, data, expireTime = this.defaultExpireTime) {
    const cacheData = {
      data: data,
      timestamp: Date.now(),
      expireTime: expireTime
    };
    
    try {
      wx.setStorageSync(key, cacheData);
    } catch (error) {
      console.warn('缓存设置失败:', error);
    }
  }

  /**
   * 获取缓存
   * @param {string} key 缓存键
   * @returns {any|null} 缓存数据，过期或不存在返回null
   */
  get(key) {
    try {
      const cacheData = wx.getStorageSync(key);
      if (!cacheData) return null;

      const now = Date.now();
      if (now - cacheData.timestamp > cacheData.expireTime) {
        // 缓存过期，删除
        this.remove(key);
        return null;
      }

      return cacheData.data;
    } catch (error) {
      console.warn('缓存读取失败:', error);
      return null;
    }
  }

  /**
   * 删除缓存
   * @param {string} key 缓存键
   */
  remove(key) {
    try {
      wx.removeStorageSync(key);
    } catch (error) {
      console.warn('缓存删除失败:', error);
    }
  }

  /**
   * 清理过期缓存
   */
  clearExpired() {
    try {
      const storageInfo = wx.getStorageInfoSync();
      const now = Date.now();
      
      storageInfo.keys.forEach(key => {
        try {
          const cacheData = wx.getStorageSync(key);
          if (cacheData && cacheData.timestamp && cacheData.expireTime) {
            if (now - cacheData.timestamp > cacheData.expireTime) {
              wx.removeStorageSync(key);
            }
          }
        } catch (error) {
          // 忽略单个缓存项的错误
        }
      });
    } catch (error) {
      console.warn('清理过期缓存失败:', error);
    }
  }

  /**
   * 请求缓存 - 内存级别的短期缓存
   * @param {string} key 请求键
   * @param {Function} requestFn 请求函数
   * @param {number} cacheTime 缓存时间(毫秒)，默认使用短期缓存
   */
  async requestWithCache(key, requestFn, cacheTime = CACHE_TIME.SHORT) {
    const cached = this.requestCache.get(key);
    const now = Date.now();
    
    if (cached && (now - cached.timestamp < cacheTime)) {
      return cached.data;
    }

    try {
      const data = await requestFn();
      this.requestCache.set(key, {
        data: data,
        timestamp: now
      });
      return data;
    } catch (error) {
      // 如果请求失败且有过期缓存，返回过期缓存
      if (cached) {
        console.warn('请求失败，使用过期缓存:', error);
        return cached.data;
      }
      throw error;
    }
  }

  /**
   * 清理内存缓存
   */
  clearMemoryCache() {
    this.requestCache.clear();
  }

  /**
   * 生成缓存键
   * @param {string} prefix 前缀
   * @param {object} params 参数对象
   */
  generateKey(prefix, params = {}) {
    const paramStr = Object.keys(params)
      .sort()
      .map(key => `${key}=${params[key]}`)
      .join('&');
    return paramStr ? `${prefix}_${paramStr}` : prefix;
  }
}

// 创建全局缓存管理器实例
const cacheManager = new CacheManager();

module.exports = {
  CacheManager,
  cacheManager
};
