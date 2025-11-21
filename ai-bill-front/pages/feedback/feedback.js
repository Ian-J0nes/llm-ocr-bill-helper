Page({
  data: {
    feedbackType: '',
    feedbackContent: '',
    contactInfo: '',
    selectedType: 0,
    typeOptions: [
      '功能建议',
      '使用问题', 
      '界面问题',
      '性能问题',
      '其他问题'
    ]
  },

  onLoad(options) {
    if (options.type) {
      const typeIndex = this.data.typeOptions.indexOf(options.type);
      this.setData({
        feedbackType: options.type,
        selectedType: typeIndex >= 0 ? typeIndex : 0
      });
    }
  },

  // 选择反馈类型
  onTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      selectedType: index,
      feedbackType: this.data.typeOptions[index]
    });
  },

  // 输入反馈内容
  onContentInput(e) {
    this.setData({
      feedbackContent: e.detail.value
    });
  },

  // 输入联系方式
  onContactInput(e) {
    this.setData({
      contactInfo: e.detail.value
    });
  },

  // 提交反馈
  submitFeedback() {
    if (!this.data.feedbackContent.trim()) {
      wx.showToast({
        title: '请填写反馈内容',
        icon: 'none'
      });
      return;
    }

    // 构建反馈数据
    const feedbackData = {
      type: this.data.feedbackType,
      content: this.data.feedbackContent,
      contact: this.data.contactInfo,
      timestamp: new Date().toISOString(),
      appVersion: wx.getSystemInfoSync().version,
      platform: wx.getSystemInfoSync().platform
    };

    // 使用微信数据分析API记录详细反馈
    if (wx.reportEvent) {
      wx.reportEvent('detailed_feedback', feedbackData);
    }

    // 可以在这里调用后端API保存反馈
    // 这里暂时只是本地存储
    let feedbacks = wx.getStorageSync('user_feedbacks') || [];
    feedbacks.push(feedbackData);
    wx.setStorageSync('user_feedbacks', feedbacks);

    wx.showModal({
      title: '反馈提交成功',
      content: '感谢您的反馈！我们会认真对待每一条意见，持续改进产品体验。',
      showCancel: false,
      success: () => {
        wx.navigateBack();
      }
    });
  },

  // 取消反馈
  cancelFeedback() {
    if (this.data.feedbackContent.trim()) {
      wx.showModal({
        title: '确认取消',
        content: '您已填写的内容将会丢失，确认退出吗？',
        success: (res) => {
          if (res.confirm) {
            wx.navigateBack();
          }
        }
      });
    } else {
      wx.navigateBack();
    }
  }
}); 