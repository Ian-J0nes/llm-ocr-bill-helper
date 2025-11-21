const app = getApp();

Page({
  data: {
    aiModels: [], // 可用的AI模型列表
    currentModel: '', // 当前选择的模型
  },

  onLoad: function (options) {
    // 获取页面传递的数据
    const eventChannel = this.getOpenerEventChannel();
    eventChannel.on('initData', (data) => {
      console.log('接收到的AI模型数据:', data);
      if (data && data.aiModels) {
        this.setData({
          aiModels: data.aiModels.sort((a, b) => a.sortOrder - b.sortOrder),
          currentModel: data.currentModel || ''
        });
      }
    });
  },

  // 选择AI模型
  selectModel: function (e) {
    const modelName = e.currentTarget.dataset.model;
    const modelDisplayName = e.currentTarget.dataset.displayName;
    
    // 设置当前选择的模型
    this.setData({
      currentModel: modelName
    });

    // 向打开此页面的来源页面发送选择结果
    const eventChannel = this.getOpenerEventChannel();
    eventChannel.emit('selectAiModel', { 
      modelName: modelName,
      modelDisplayName: modelDisplayName
    });

    // 返回上一页
    wx.navigateBack({
      delta: 1
    });
  },

  // 取消选择，返回上一页
  cancel: function () {
    wx.navigateBack({
      delta: 1
    });
  }
}); 