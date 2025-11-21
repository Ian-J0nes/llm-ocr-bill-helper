// pages/user-info/user-info.js
const app = getApp();
const { userApi } = require('../../utils/api-modules');
const defaultAvatarUrl = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';

Page({

  /**
   * 页面的初始数据
   */
  data: {
    userInfo: {
      id: '',
      username: '',
      email: '',
      phoneNumber: '',
      avatarUrl: defaultAvatarUrl,
      nickname: '微信用户',
      status: '',
      createTime: '',
      createTimeFormatted: '',
    },
    originalBackendUserInfo: {}, // For comparing editable fields from backend (username, email, phone)
    token: ''
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    const token = wx.getStorageSync('token');
    this.setData({
      token
    });
    // Initial load of cached info can also be here if onShow is too frequent for this part
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // Load cached avatar and nickname
    const cachedAvatarUrl = wx.getStorageSync('avatarUrl');
    const cachedNickName = wx.getStorageSync('nickName');

    this.setData({
      'userInfo.avatarUrl': cachedAvatarUrl || defaultAvatarUrl,
      'userInfo.nickname': cachedNickName || '微信用户'
    });

    if (this.data.token) {
      this.fetchBackendUserInfo();
    } else {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
        duration: 2000,
      });
    }
  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  },

  fetchBackendUserInfo() {
    wx.showLoading({
      title: '加载中...',
    });
    // console.log('Current token for /user/me request:', this.data.token); // Debugging token
    userApi.getCurrentUser()
      .then((response) => {
        if (response && response.code === 200) {
          const backendData = response.data;
          let createTimeFormatted = this.data.userInfo.createTimeFormatted;
          if (backendData.createTime) {
            createTimeFormatted = this.formatBackendDateTime(backendData.createTime);
          }

          const updatedUserInfo = {
            ...this.data.userInfo, // Keep cached avatarUrl and nickname
            id: backendData.id,
            username: backendData.username,
            email: backendData.email,
            phoneNumber: backendData.phoneNumber,
            status: backendData.status,
            createTime: backendData.createTime,
            createTimeFormatted: createTimeFormatted
          };

          this.setData({
            userInfo: updatedUserInfo,
            originalBackendUserInfo: { // Store only the editable parts from backend for comparison
              username: backendData.username,
              email: backendData.email,
              phoneNumber: backendData.phoneNumber
            }
          });
        } else {
          wx.showToast({
            title: response.message || '获取用户信息失败',
            icon: 'none',
            duration: 2000
          });
        }
      })
      .catch((err) => {
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none',
          duration: 2000
        });
        console.error('fetchBackendUserInfo fail:', err);
      })
      .finally(() => {
        wx.hideLoading();
      });
  },

  onInputValueChange(e) {
    const field = e.currentTarget.dataset.field;
    const value = e.detail.value;
    if (field === 'username' || field === 'email' || field === 'phoneNumber') {
      this.setData({
        [`userInfo.${field}`]: value
      });
    }
  },

  // handleChooseAvatar 方法已移除

  saveUserInfo() {
    const currentUserInfo = this.data.userInfo;
    const originalComparableInfo = this.data.originalBackendUserInfo;

    const currentEditableInfo = {
      username: currentUserInfo.username,
      email: currentUserInfo.email,
      phoneNumber: currentUserInfo.phoneNumber
    };

    // Compare against the state fetched from backend for these specific fields
    if (JSON.stringify(currentEditableInfo) === JSON.stringify(originalComparableInfo)) {
      wx.showToast({
        title: '信息未修改',
        icon: 'none'
      });
      return;
    }

    const userDTO = {
      id: currentUserInfo.id, // id is crucial for update
      username: currentUserInfo.username,
      email: currentUserInfo.email,
      phoneNumber: currentUserInfo.phoneNumber
    };

    if (!userDTO.id) {
        wx.showToast({ title: '无法更新：用户ID未知', icon: 'none' });
        return;
    }
    if (!userDTO.username) { 
      wx.showToast({ title: '用户名不能为空', icon: 'none' });
      return;
    }

    wx.showLoading({
      title: '保存中...',
    });

    api.request({
      endpoint: '/user/update',
      method: 'PUT',
      data: userDTO
    }).then((response) => {
      if (response && response.code === 200) {
        this.setData({
          originalBackendUserInfo: { // Update the reference for comparison
            username: userDTO.username,
            email: userDTO.email,
            phoneNumber: userDTO.phoneNumber
          }
        });
        wx.showToast({
          title: '保存成功',
          icon: 'success',
          duration: 2000
        });
      } else {
        wx.showToast({
          title: response.message || '保存失败',
          icon: 'none',
          duration: 2000
        });
      }
    }).catch((err) => {
      wx.showToast({
        title: '网络错误，保存失败',
        icon: 'none',
        duration: 2000
      });
      console.error('saveUserInfo fail:', err);
    }).finally(() => {
      wx.hideLoading();
    });
  },

  formatBackendDateTime(dateTime) {
    try {
      if (typeof dateTime === 'string') {
        return dateTime;
      }
      
      if (Array.isArray(dateTime) && dateTime.length >= 6) {
        const [year, month, day, hour, minute, second] = dateTime;
        const date = new Date(year, month - 1, day, hour, minute, second);
        return this.formatDate(date);
      }
      
      const date = new Date(dateTime);
      if (!isNaN(date.getTime())) {
        return this.formatDate(date);
      }
      
      return String(dateTime);
    } catch (error) {
      console.error('时间格式化失败:', error, dateTime);
      return '时间格式错误';
    }
  },

  formatDate(date) {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  }
})