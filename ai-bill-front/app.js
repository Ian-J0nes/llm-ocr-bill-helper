/**
 * 小程序应用入口
 * @description 管理全局状态、登录认证和应用生命周期
 */

const { cacheManager } = require('./utils/cache');
const { tokenManager } = require('./utils/token-manager');
const { loginHelper } = require('./utils/login-helper');
const { PAGES } = require('./utils/constants');

App({
    onLaunch: function (options) {
      // 清理过期缓存
      cacheManager.clearExpired();

      // 使用token管理器获取有效token
      const token = tokenManager.getValidToken();
      if (token) {
        this.globalData.token = token;
        this.globalData.isLoggedIn = true;
      } else {
        this.globalData.isLoggedIn = false;
        // 不再强制跳转到登录页面
        // this.navigateToLogin();
      }
    },
  
    globalData: {
      userInfo: null,
      token: null,
      isLoggedIn: false,
      intendedUrl: '',
      // baseURL: 'https://java.3arre.me'
      baseURL:'http://127.0.0.1:8080'
    },  
  
    /**
     * 检查登录状态，如果未登录则跳转到登录页面
     * @returns {boolean} 是否已登录
     */
    checkLoginStatus: function() {
      if (!this.globalData.isLoggedIn || !this.globalData.token) {
        this.navigateToLogin();
        return false;
      }
      return true;
    },
  
    /**
     * 跳转到登录页面
     * 使用统一的登录助手，避免重复跳转和死循环
     */
    navigateToLogin: function() {
        const pages = getCurrentPages();
        const currentPage = pages.length > 0 ? pages[pages.length - 1] : null;
        let currentRoute = currentPage ? currentPage.route : '';

        // 如果已经在登录页面，直接返回
        if (currentRoute === 'pages/login/login') {
          return;
        }

        // 保存当前页面信息，登录成功后跳转回来
        let intendedUrl = '';
        if (currentPage && currentPage.route !== PAGES.LOGIN.substring(1)) {
          intendedUrl = `/${currentPage.route}`;
          if (currentPage.options && Object.keys(currentPage.options).length > 0) {
            intendedUrl += '?' + Object.entries(currentPage.options).map(([key, value]) => `${key}=${encodeURIComponent(value)}`).join('&');
          }
          this.globalData.intendedUrl = intendedUrl;
        } else if (!this.globalData.intendedUrl) {
            this.globalData.intendedUrl = PAGES.INDEX; // 默认首页
        }

        // 使用统一的登录助手跳转
        loginHelper.navigateToLogin().catch(err => {
          console.error('App.js: Failed to navigate to login page:', err);
        });
      },
  
    /**
     * 登录成功后的处理
     * @param {object} loginResponseData - 后端登录接口返回的 data 对象，包含 token 等信息
     */
    loginSuccess: function(loginResponseData) {
      if (!loginResponseData || !loginResponseData.token) {
        console.error('Login success called with invalid data:', loginResponseData);
        wx.showToast({ title: '登录数据错误', icon: 'none' });
        return;
      }

      // 使用token管理器设置token
      const success = tokenManager.setToken(loginResponseData.token);
      if (!success) {
        console.error('设置token失败，token格式无效');
        wx.showToast({ title: '登录数据格式错误', icon: 'none' });
        return;
      }

      // 如果后端返回了用户信息，也可以在这里存储
      // this.globalData.userInfo = loginResponseData.userInfo;
  
      const intendedUrl = this.globalData.intendedUrl;
      this.globalData.intendedUrl = ''; // 清空记录

      if (intendedUrl && intendedUrl !== PAGES.LOGIN) {
        const tabBarList = __wxConfig.tabBar ? __wxConfig.tabBar.list.map(item => `/${item.pagePath}`) : [];
        const targetPath = intendedUrl.split('?')[0];

        if (tabBarList.includes(targetPath)) {
          wx.switchTab({
            url: targetPath,
            fail: (err) => {
              console.error('Failed to switchTab to intended URL:', err, 'Falling back to index.');
              wx.switchTab({ url: PAGES.INDEX });
            }
          });
        } else {
          wx.redirectTo({
            url: intendedUrl,
            fail: (err) => {
              console.error('Failed to redirectTo intended URL:', err, 'Falling back to index.');
              wx.switchTab({ url: PAGES.INDEX });
            }
          });
        }
      } else {
        wx.switchTab({
          url: PAGES.INDEX,
          fail: (err) => console.error('Failed to switchTab to default index:', err)
        });
      }
    }
  });
  