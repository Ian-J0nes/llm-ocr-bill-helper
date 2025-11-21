// pages/login/login.js
const app = getApp(); // 获取 App 实例
const { userApi } = require('../../utils/api-modules');
const { tokenManager } = require('../../utils/token-manager');
const defaultAvatarUrl = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';

Page({
  data: {
    avatarUrl: defaultAvatarUrl,
    nickName: '',
    loginDisabled: true,
    isLoading: false, // 保留 isLoading 以便将来可能的异步操作
  },

  onLoad: function (options) {
    const cachedAvatarUrl = wx.getStorageSync('avatarUrl');
    const cachedNickName = wx.getStorageSync('nickName');

    let updates = {};
    if (cachedAvatarUrl) {
      updates.avatarUrl = cachedAvatarUrl;
    }
    if (cachedNickName) {
      updates.nickName = cachedNickName;
    }
    if (Object.keys(updates).length > 0) {
      this.setData(updates);
    }
    this.checkLoginButtonState();
  },

  onChooseAvatar(e) {
    const { avatarUrl } = e.detail;
    if (avatarUrl) {
      this.setData({
        avatarUrl,
      });
      this.checkLoginButtonState();
    } else {
      // 用户可能取消选择或者选择的图片不合规
      wx.showToast({
        title: '头像选择失败或图片不合规',
        icon: 'none'
      });
    }
  },

  onNicknameInput(e) {
    const nickName = e.detail.value;
    this.setData({
      nickName,
    });
    // 实时检查，但不作为最终提交依据，因为微信可能清空
    // this.checkLoginButtonState(); 
    // 考虑到微信安全策略，按钮状态主要由 avatar 和 onLogin 时的 nickname 决定
    // 或者，如果希望更实时地反馈按钮状态，可以保留调用，但 onLogin 仍需最终校验
    if (nickName.trim() && this.data.avatarUrl !== defaultAvatarUrl) {
        if (this.data.loginDisabled) this.setData({ loginDisabled: false });
    } else {
        if (!this.data.loginDisabled) this.setData({ loginDisabled: true });
    }
  },

  checkLoginButtonState() {
    const { avatarUrl, nickName } = this.data;
    const isDisabled = avatarUrl === defaultAvatarUrl || !nickName.trim();
    if (this.data.loginDisabled !== isDisabled) {
      this.setData({
        loginDisabled: isDisabled,
      });
    }
  },

  onLogin(e) {
    if (this.data.isLoading) return;

    const nickNameFromEvent = e.detail.value.nickname;

    if (this.data.avatarUrl === defaultAvatarUrl) {
      wx.showToast({
        title: '请选择头像',
        icon: 'none'
      });
      return;
    }

    if (!nickNameFromEvent || !nickNameFromEvent.trim()) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none'
      });
      this.setData({
        nickName: '',
        loginDisabled: true
      });
      return;
    }
    
    const finalNickName = nickNameFromEvent.trim();
    const finalAvatarUrl = this.data.avatarUrl;

    this.setData({
      nickName: finalNickName,
      isLoading: true,
      loginDisabled: true,
    });

    // 1. 调用 wx.login 获取 code
    wx.login({
      success: resLogin => {
        if (resLogin.code) {
          // 2. 请求后端 wxlogin 接口，使用新的 API 模块
          userApi.wxLogin(resLogin.code)
            .then((response) => {
              if (response && response.code === 200 && response.data && response.data.token) {
                const loginResult = response.data;
                // 登录成功，存储 token 和后端返回的用户信息
                app.loginSuccess(loginResult); // loginSuccess 应该会处理 token 和用户ID的存储

                // 将用户选择的头像和昵称存储到本地缓存，并更新到 globalData
                // 后端也应该有接口来更新用户的头像和昵称，这里前端先存起来
                wx.setStorageSync('avatarUrl', finalAvatarUrl);
                wx.setStorageSync('nickName', finalNickName);

                // 更新 globalData 中的 userInfo
                // 如果 app.loginSuccess 已经处理了 userInfo 的部分字段，这里需要合并
                const currentUserInfo = app.globalData.userInfo || {};
                app.globalData.userInfo = {
                  ...currentUserInfo, // 保留从后端获取的信息，如id, openid
                  avatarUrl: finalAvatarUrl,
                  nickName: finalNickName,
                  // username: loginResult.username // 如果后端返回username，也可以用它
                };
                app.globalData.isLoggedIn = true;

                // TODO: 此处应调用后端接口，使用 loginResult.id 或 loginResult.token
                // 将 finalAvatarUrl 和 finalNickName 更新到后端数据库
                // userApi.updateUser(loginResult.id, {
                //   avatarUrl: finalAvatarUrl,
                //   nickname: finalNickName
                // }).then((response) => {
                //   if (response && response.code === 200) {
                //     console.log("用户信息更新到后端成功");
                //   } else {
                //     console.warn("用户信息更新到后端失败", response);
                //   }
                // }).catch((updateErr) => {
                //   console.error("更新用户信息请求后端失败", updateErr);
                // }).finally(() => {
                //    // 不论更新是否成功，都先跳转
                //    this.navigateToNextPage();
                // });
                // 由于上述后端调用被注释，我们直接跳转
                wx.showToast({
                  title: '登录成功',
                  icon: 'success',
                  duration: 1500,
                  complete: () => {
                    // 使用 app.js 的统一跳转逻辑
                    app.loginSuccess(loginResult);
                  }
                });

              } else {
                wx.showToast({
                  title: response.message || '登录失败',
                  icon: 'none',
                  duration: 2000
                });
                this.setData({ isLoading: false, loginDisabled: false });
              }
            })
            .catch((err) => {
              wx.showToast({
                title: '登录请求异常',
                icon: 'none',
                duration: 2000
              });
              this.setData({ isLoading: false, loginDisabled: false });
            });
        } else {
          wx.showToast({
            title: '获取登录凭证失败',
            icon: 'none',
            duration: 2000
          });
          this.setData({ isLoading: false, loginDisabled: false });
        }
      },
      fail: err => {
        wx.showToast({
          title: '微信登录调用失败',
          icon: 'none',
          duration: 2000
        });
        this.setData({ isLoading: false, loginDisabled: false });
      }
    });
  },

  // 旧的 performLogin 和 handleLogin (基于wx.getUserProfile) 不再需要，可以删除或注释
});
