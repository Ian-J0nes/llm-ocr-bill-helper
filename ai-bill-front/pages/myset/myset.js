const app = getApp();
const { loginHelper } = require('../../utils/login-helper');
const { aiConfigApi } = require('../../utils/api-modules');
const defaultAvatarUrl = 'https://mmbiz.qpic.cn/mmbiz/icTdbqWNOwNRna42FI242Lcia07jQodd2FJGIYQfG0LAJGFxM4FbnQP6yfMxBgJ0F3YRqJCJ1aPAK2dQagdusBZg/0';

Page({
  data: {
    userInfo: {
      avatarUrl: defaultAvatarUrl, // 初始为默认头像
      nickName: '微信用户' // 初始为默认昵称
    },
    // 添加AI模型相关数据
    aiModels: [], // 存储可用的AI模型列表
    currentAiModel: '', // 当前选择的AI模型
    currentAiModelDisplayName: '', // 当前选择的AI模型显示名称
    currentAiTemperature: 0.3, // 当前的温度参数（默认值）
    // 添加预算相关数据
    hasBudget: false, // 用户是否设置了预算
  },
  onLoad(options) {
    this.loadUserInfo(); 
    // 加载AI模型配置
    this.loadAiModels();
    
    // 开启分享功能
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline']
    });
  },
  onReady() {

  },
  onShow() {
    // 检查登录状态但不强制登录
    const isLoggedIn = wx.getStorageSync('token') ? true : false;
    
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
        this.getTabBar().setData({
          selected: 2 
        });
    }
    this.loadUserInfo(); // 每次页面显示时都加载/刷新用户信息
    
    // 只有已登录用户才加载AI模型配置和预算状态
    if (isLoggedIn) {
      this.loadUserAiConfig();
      this.checkUserBudget(); // 检查用户预算状态
    }
  },
  
  loadUserInfo(){
    const cachedAvatarUrl = wx.getStorageSync('avatarUrl');
    const cachedNickName = wx.getStorageSync('nickName');
    let newNickName = cachedNickName || '微信用户'; // 如果缓存为空，给个默认值
    let newAvatarUrl = cachedAvatarUrl || defaultAvatarUrl; // 如果缓存为空，给个默认值

    if (this.data.userInfo.avatarUrl !== newAvatarUrl || this.data.userInfo.nickName !== newNickName) {
      this.setData({
        userInfo: {
          avatarUrl: newAvatarUrl,
          nickName: newNickName
        }
      });
      console.log('MySet Page: UserInfo updated in data:', this.data.userInfo);
    } else {
      console.log('MySet Page: UserInfo no change needed.');
    }
  },
  
  // 加载可用的AI模型列表
  loadAiModels() {
    const token = wx.getStorageSync('token');
    if (!token) {
      console.error('未登录，无法加载AI模型');
      return;
    }

    aiConfigApi.getModels()
      .then(res => {
        // 处理响应数据
        let modelData = res;
        if (res && res.code === 200 && res.data) {
          modelData = res.data;
        }

        this.setData({
          aiModels: modelData
        });
        console.log('AI模型列表加载成功:', modelData);
        // 加载完模型列表后，加载用户当前配置
        this.loadUserAiConfig();
      })
      .catch(err => {
        console.error('加载AI模型列表失败:', err);
        wx.showToast({
          title: '加载AI模型失败',
          icon: 'none'
        });
      });
  },

  // 加载用户当前的AI配置
  loadUserAiConfig() {
    const token = wx.getStorageSync('token');
    if (!token) {
      console.error('未登录，无法加载AI配置');
      return;
    }

    aiConfigApi.getUserConfig()
      .then(res => {
        // 处理响应数据
        let userData = res;
        if (res && res.code === 200 && res.data) {
          userData = res.data;
        }

        const aiModel = userData.aiModel || '';

        // 根据当前模型名找到对应的显示名称
        let modelDisplayName = '';
        if (aiModel && this.data.aiModels.length > 0) {
          const modelInfo = this.data.aiModels.find(m => m.modelName === aiModel);
          if (modelInfo) {
            modelDisplayName = modelInfo.modelDisplayName || aiModel;
          } else {
            modelDisplayName = aiModel; // 如果找不到对应模型，使用模型名称作为显示名
          }
        }

        this.setData({
          currentAiModel: aiModel,
          currentAiModelDisplayName: modelDisplayName,
          currentAiTemperature: userData.aiTemperature || 0.3
        });

        console.log('用户AI配置加载成功:', userData);
      })
      .catch(err => {
        console.error('加载用户AI配置失败:', err);
      });
  },

  // 跳转到AI模型选择页面
  navigateToAiModelSelection() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再进行AI模型设置'
    })) {
      return;
    }
    
    // 检查是否已加载模型列表
    if (this.data.aiModels.length === 0) {
      wx.showToast({
        title: '正在加载模型列表...',
        icon: 'loading',
        duration: 2000
      });
      // 重新加载模型列表
      this.loadAiModels();
      return;
    }

    // 显示操作表选择模型
    const modelItems = this.data.aiModels.map(model => model.modelDisplayName || model.modelName);
    
    wx.showActionSheet({
      itemList: modelItems,
      success: (res) => {
        if (res.tapIndex !== undefined) {
          // 获取选择的模型
          const selectedModel = this.data.aiModels[res.tapIndex];
          // 更新AI配置
          this.updateAiConfig(selectedModel.modelName);
        }
      },
      fail: (res) => {
        console.log('用户取消选择', res);
      }
    });
  },

  // 更新AI配置
  updateAiConfig(modelName) {
    if (!modelName) {
      console.error('模型名称不能为空');
      return;
    }

    const token = wx.getStorageSync('token');
    if (!token) {
      console.error('未登录，无法更新AI配置');
      return;
    }

    // 查找当前选择模型的配置
    const selectedModel = this.data.aiModels.find(model => model.modelName === modelName);
    if (!selectedModel) {
      console.error('找不到选择的模型配置');
      return;
    }

    // 使用模型的默认温度值
    const temperature = selectedModel.defaultTemperature || 0.3;

    aiConfigApi.updateUserConfig({
      aiModel: modelName,
      aiTemperature: temperature
    })
      .then(res => {
        // 处理响应数据
        let successMsg = '模型设置成功';
        if (res && res.code === 200) {
          successMsg = res.message || successMsg;
        }

        this.setData({
          currentAiModel: modelName,
          currentAiModelDisplayName: selectedModel.modelDisplayName || modelName,
          currentAiTemperature: temperature
        });
        wx.showToast({
          title: successMsg,
          icon: 'success'
        });
      })
      .catch(err => {
        console.error('更新AI配置失败:', err);
        wx.showToast({
          title: err.message || '更新配置失败',
          icon: 'none'
        });
      });
  },

  navigateToAiPreview() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用AI洞察功能'
    })) {
      return;
    }
    
    // 已登录，导航到AI洞察页面
    wx.navigateTo({
      url: '/pages/ai-insight/ai-insight'
    });
  },

  navigateToExchangeRate() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用货币汇率功能'
    })) {
      return;
    }
    
    // 已登录，正常导航
    wx.navigateTo({
      url: '/pages/currency-exchange/currency-exchange'
    });
  },

  navigateToBillStatistics() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再查看账单统计'
    })) {
      return;
    }
    
    // 已登录，正常导航
    wx.navigateTo({
      url: '/pages/bill-summary/bill-summary'
    });
  },

  navigateToBillImport() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用账单导入功能'
    })) {
      return;
    }
    
    // 已登录，正常导航
    wx.showToast({
      title: '账单导入功能开发中',
      icon: 'none'
    });
  },

  navigateToBillExport() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用账单导出功能'
    })) {
      return;
    }
    
    // 已登录，正常导航
    wx.navigateTo({ url: '/pages/bill-export/bill-export' });
  },

  navigateToUserInfo() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再查看/修改用户信息'
    })) {
      return;
    }
    
    // 已登录，正常导航
    wx.navigateTo({ url: '/pages/user-info/user-info' });
  },

  navigateToHelp() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用帮助与反馈功能'
    })) {
      return;
    }
    
    // 已登录，正常导航 - 原有逻辑保持不变
    wx.showModal({
      title: '帮助与反馈',
      content: '您是否需要反馈问题或获取帮助？',
      cancelText: '帮助中心',
      confirmText: '问题反馈',
      success: (res) => {
        if (res.confirm) {
          // 用户选择问题反馈，直接跳转到feedback页面
          wx.navigateTo({
            url: '/pages/feedback/feedback'
          });
        } else if (res.cancel) {
          // 用户选择帮助中心
          this.showHelpCenter();
        }
      }
    });
  },

  // 显示反馈选项
  showFeedbackOptions() {
    wx.showActionSheet({
      itemList: ['功能建议', '使用问题', '界面问题', '其他问题', '详细反馈'],
      success: (res) => {
        const feedbackTypes = ['功能建议', '使用问题', '界面问题', '其他问题'];
        
        if (res.tapIndex < 4) {
          // 快速反馈
          const selectedType = feedbackTypes[res.tapIndex];
          this.submitFeedback(selectedType);
        } else {
          // 跳转到详细反馈页面
          wx.navigateTo({
            url: '/pages/feedback/feedback'
          });
        }
      }
    });
  },

  // 提交反馈
  submitFeedback(type) {
    // 使用微信官方数据分析API记录反馈
    if (wx.reportEvent) {
      wx.reportEvent('user_feedback', {
        feedback_type: type,
        page: 'myset',
        timestamp: Date.now()
      });
    }

    wx.showToast({
      title: `已记录您的${type}反馈`,
      icon: 'success',
      duration: 2000
    });

    // 可以在这里添加更多的反馈处理逻辑
    // 比如跳转到详细反馈页面或者调用后端API
  },

  // 显示帮助中心
  showHelpCenter() {
    wx.showModal({
      title: '帮助中心',
      content: '常见问题：\n\n1. 如何添加账单？\n点击首页的"+"按钮即可添加账单。\n\n2. 如何查看统计？\n在账单统计页面可以查看详细的收支分析。\n\n3. 如何导出账单？\n在账单导出页面选择时间范围即可导出。\n\n4. 如何设置AI模型？\n在"我的"页面中，点击"AI模型选择"可以进行设置。\n\n5. 如何联系我们？\n在"我的"页面点击"帮助与反馈"，选择"问题反馈"即可。',
      showCancel: false,
      confirmText: '我知道了'
    });
  },

  shareToFriend() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用分享功能'
    })) {
      return;
    }
    
    // 已登录，正常分享
    // 开启分享功能
    wx.showShareMenu({
      withShareTicket: true,
      menus: ['shareAppMessage', 'shareTimeline'],
      success: () => {
        console.log('分享菜单显示成功');
      },
      fail: (err) => {
        console.error('分享菜单显示失败:', err);
        // 如果显示分享菜单失败，直接触发分享
        wx.shareAppMessage({
          title: '记账小程序 - 让记账变得简单',
          path: '/pages/index/index',
          imageUrl: '', // 可以设置分享图片
          success: (res) => {
            wx.showToast({
              title: '分享成功',
              icon: 'success'
            });
          },
          fail: (err) => {
            wx.showToast({
              title: '分享失败',
              icon: 'none'
            });
          }
        });
      }
    });
  },

  onHide() {

  },
  onUnload() {

  },
  onPullDownRefresh() {

  },
  onReachBottom() {

  },
  onShareAppMessage(options) {
    // 处理分享给朋友
    console.log('分享来源:', options.from); // button: 页面内分享按钮；menu: 右上角分享按钮
    
    // 记录分享事件
    if (wx.reportEvent) {
      wx.reportEvent('share_to_friend', {
        from: options.from,
        page: 'myset'
      });
    }

    return {
      title: '记账小程序 - 让记账变得简单',
      path: '/pages/index/index',
      imageUrl: '', // 可以设置自定义分享图片路径
      success: (res) => {
        console.log('分享成功', res);
        wx.showToast({
          title: '分享成功',
          icon: 'success'
        });
      },
      fail: (res) => {
        console.log('分享失败', res);
        wx.showToast({
          title: '分享失败',
          icon: 'none'
        });
      }
    };
  },

  onShareTimeline() {
    // 处理分享到朋友圈
    console.log('分享到朋友圈');
    
    // 记录分享事件
    if (wx.reportEvent) {
      wx.reportEvent('share_to_timeline', {
        page: 'myset'
      });
    }

    return {
      title: '记账小程序 - 让记账变得简单',
      query: {
        from: 'timeline'
      },
      imageUrl: '' // 可以设置自定义分享图片路径
    };
  },

  // 检查用户预算状态
  checkUserBudget() {
    const token = wx.getStorageSync('token');
    if (!token) {
      console.error('未登录，无法检查预算状态');
      return;
    }

    wx.request({
      url: app.globalData.baseURL + '/user-budget/active',
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        if (res.statusCode === 200) {
          // 如果返回的预算列表不为空，则表示用户有有效预算
          this.setData({
            hasBudget: res.data && res.data.data && res.data.data.length > 0
          });
          
          // 首次登录或无有效预算时，提示用户设置预算
          if (!this.data.hasBudget) {
            this.showBudgetReminder();
          }
        } else {
          console.error('检查预算状态失败:', res);
        }
      },
      fail: (err) => {
        console.error('请求预算状态失败:', err);
      }
    });
  },

  // 显示预算提醒
  showBudgetReminder() {
    wx.showModal({
      title: '预算提醒',
      content: '您尚未设置预算，设置预算可以帮助您更好地管理财务，是否现在设置？',
      confirmText: '立即设置',
      cancelText: '稍后再说',
      success: (res) => {
        if (res.confirm) {
          this.navigateToBudget();
        }
      }
    });
  },
  
  // 跳转到预算设置页面
  navigateToBudget() {
    // 使用统一的登录检查
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再进行预算设置'
    })) {
      return;
    }
    
    wx.navigateTo({
      url: '/pages/user-budget/user-budget'
    });
  },
})