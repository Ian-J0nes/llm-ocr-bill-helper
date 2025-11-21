// pages/ai-insight/ai-insight.js
const app = getApp();
const { loginHelper } = require('../../utils/login-helper');

const formatTime = (date, format = 'default') => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  // 支持不同的日期格式
  if (format === 'YYYY-MM-DD') {
    return `${year}-${formatNumber(month)}-${formatNumber(day)}`
  } else if (format === 'YYYY-MM') {
    return `${year}-${formatNumber(month)}`
  } else if (format === 'MM-DD') {
    return `${formatNumber(month)}-${formatNumber(day)}`
  } else {
    // 默认格式
    return `${[year, month, day].map(formatNumber).join('/')} ${[hour, minute, second].map(formatNumber).join(':')}`
  }
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : `0${n}`
}

Page({

  /**
   * 页面的初始数据
   */
  data: {
    currentPeriod: 'monthly', // 默认是月度洞察
    datePickerFields: 'month', // 默认日期选择器精度为月
    selectedDate: '', // 当前选择的日期（格式：YYYY-MM-DD）
    dateDisplay: '', // 显示用的日期文本
    insightContent: '', // 洞察内容
    isLoading: false, // 是否正在加载数据
    errorMessage: '', // 错误信息
    streamContent: '', // 用于存储流式接收的内容
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 初始化日期为当前日期
    const today = new Date();
    const formattedDate = formatTime(today, 'YYYY-MM-DD');
    
    this.setData({
      selectedDate: formattedDate,
      dateDisplay: this.formatDateDisplay(today, this.data.currentPeriod)
    });
    
    // 页面加载后检查缓存
    this.checkAndLoadCache();
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
    // 检查登录状态
    this.checkLoginStatus();
    
    // 页面显示时检查缓存
    this.checkAndLoadCache();
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
    // 如果当前有日期和周期，下拉刷新时先检查缓存
    if (this.data.selectedDate && this.data.currentPeriod) {
      // 生成缓存键
      const period = this.data.currentPeriod;
      const date = this.data.selectedDate;
      const cacheKey = `ai_insight_${period}_${date}`;
      
      // 检查缓存是否存在
      const cachedData = wx.getStorageSync(cacheKey);
      
      if (cachedData) {
        // 有缓存，显示提示让用户选择是否要刷新
        wx.showModal({
          title: '刷新提示',
          content: '已有缓存数据，是否清除缓存并重新获取最新分析？',
          confirmText: '重新分析',
          cancelText: '使用缓存',
          success: (res) => {
            if (res.confirm) {
              // 用户选择重新获取数据
              this.refreshInsight();
            } else {
              // 用户选择使用缓存，直接加载缓存数据
              const formattedContent = this.formatMarkdownContent(cachedData);
              this.setData({
                insightContent: formattedContent,
                isLoading: false,
                errorMessage: ''
              });
              wx.showToast({
                title: '已加载缓存',
                icon: 'success',
                duration: 1500
              });
            }
            wx.stopPullDownRefresh();
          }
        });
      } else {
        // 没有缓存，直接获取新数据
        this.getInsight();
        wx.stopPullDownRefresh();
      }
    } else {
      // 无有效日期或周期，停止下拉刷新
      wx.stopPullDownRefresh();
    }
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

  /**
   * 检查登录状态
   */
  checkLoginStatus() {
    if (!loginHelper.checkLoginWithPrompt({
      content: '请先登录后再使用AI洞察功能',
      onCancel: () => {
        wx.navigateBack();
      }
    })) {
      return false;
    }
    return true;
  },

  /**
   * 切换洞察周期（月度/季度/年度）
   */
  switchPeriod(e) {
    const period = e.currentTarget.dataset.period;
    
    if (this.data.currentPeriod === period) {
      return; // 如果点击的是当前已选中的周期，不做任何操作
    }
    
    // 更新选择的周期
    let datePickerFields = 'month'; // 默认为月
    
    if (period === 'yearly') {
      datePickerFields = 'year';
    } else if (period === 'quarterly' || period === 'monthly') {
      datePickerFields = 'month';
    }
    
    const currentDate = new Date(this.data.selectedDate);
    
    this.setData({
      currentPeriod: period,
      datePickerFields: datePickerFields,
      dateDisplay: this.formatDateDisplay(currentDate, period),
      insightContent: '', // 清空之前的洞察内容
      errorMessage: '' // 清空错误信息
    });
    
    // 切换周期后检查新周期的缓存
    this.checkCacheForPeriodAndDate(period, this.data.selectedDate);
  },

  /**
   * 日期选择器变化处理
   */
  onDateChange(e) {
    const date = new Date(e.detail.value);
    const formattedDate = formatTime(date, 'YYYY-MM-DD');
    
    this.setData({
      selectedDate: formattedDate,
      dateDisplay: this.formatDateDisplay(date, this.data.currentPeriod),
      insightContent: '', // 清空之前的洞察内容
      errorMessage: '' // 清空错误信息
    });
    
    // 更改日期后检查新日期的缓存
    this.checkCacheForPeriodAndDate(this.data.currentPeriod, formattedDate);
  },

  /**
   * 检查并加载缓存数据
   */
  checkAndLoadCache() {
    const token = wx.getStorageSync('token');
    if (!token) return; // 未登录不处理缓存
    
    if (!this.data.insightContent && !this.data.isLoading) {
      // 当前没有显示内容且不在加载状态时，检查缓存
      const period = this.data.currentPeriod;
      const date = this.data.selectedDate;
      
      if (period && date) {
        const cacheKey = `ai_insight_${period}_${date}`;
        console.log('检查缓存键:', cacheKey);
        
        try {
          const cachedData = wx.getStorageSync(cacheKey);
          
          // 调试信息：列出当前所有缓存键
          const storageInfo = wx.getStorageInfoSync();
          console.log('当前所有缓存键:', storageInfo.keys);
          
          if (cachedData) {
            console.log('找到缓存数据，长度:', cachedData.length);
            // 有缓存，使用缓存数据
            const formattedContent = this.formatMarkdownContent(cachedData);
            this.setData({
              insightContent: formattedContent,
              errorMessage: ''
            });
            console.log('成功加载缓存数据');
          } else {
            console.log('未找到缓存数据');
          }
        } catch (error) {
          console.error('读取缓存时出错:', error);
        }
      }
    }
  },
  
  /**
   * 检查指定周期和日期的缓存
   */
  checkCacheForPeriodAndDate(period, date) {
    if (!period || !date) {
      console.log('周期或日期无效，不检查缓存');
      return;
    }
    
    const token = wx.getStorageSync('token');
    if (!token) {
      console.log('未登录，不检查缓存');
      return;
    }
    
    // 确保日期格式正确
    let formattedDate = date;
    if (typeof date === 'object' && date instanceof Date) {
      formattedDate = formatTime(date, 'YYYY-MM-DD');
    }
    
    const cacheKey = `ai_insight_${period}_${formattedDate}`;
    console.log('检查特定缓存键:', cacheKey);
    
    try {
      const cachedData = wx.getStorageSync(cacheKey);
      
      if (cachedData) {
        console.log('找到指定日期的缓存数据，长度:', cachedData.length);
        // 有缓存，使用缓存数据
        const formattedContent = this.formatMarkdownContent(cachedData);
        this.setData({
          insightContent: formattedContent,
          errorMessage: ''
        });
        console.log('成功加载指定日期的缓存数据');
      } else {
        console.log('未找到指定日期的缓存数据');
      }
    } catch (error) {
      console.error('读取指定日期缓存时出错:', error);
    }
  },

  /**
   * 格式化显示用的日期文本
   */
  formatDateDisplay(date, period) {
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    
    if (period === 'yearly') {
      return `${year}年`;
    } else if (period === 'quarterly') {
      // 确定季度
      const quarter = Math.ceil(month / 3);
      return `${year}年 第${quarter}季度`;
    } else {
      // 月度
      return `${year}年${month}月`;
    }
  },

  /**
   * 获取AI洞察数据
   */
  getInsight() {
    // 检查登录状态
    const token = wx.getStorageSync('token');
    if (!token) {
      this.checkLoginStatus();
      return;
    }
    
    // 设置加载状态
    this.setData({
      isLoading: true,
      insightContent: '',
      errorMessage: ''
    });
    
    const period = this.data.currentPeriod;
    const date = this.data.selectedDate;
    
    // 生成缓存键
    const cacheKey = `ai_insight_${period}_${date}`;
    
    // 先尝试从缓存中读取数据
    const cachedData = wx.getStorageSync(cacheKey);
    if (cachedData) {
      console.log('从缓存加载AI洞察数据:', cacheKey);
      // 有缓存，直接使用
      const formattedContent = this.formatMarkdownContent(cachedData);
      this.setData({
        insightContent: formattedContent,
        isLoading: false
      });
      return; // 直接返回，不再请求
    }
    
    // 没有缓存或缓存过期，发起请求
    console.log('缓存不存在，请求新的AI洞察数据:', cacheKey);
    
    // 准备请求URL和参数
    const baseURL = app.globalData.baseURL;
    const url = `${baseURL}/ai-insight/${period}?date=${date}`;
    
    // 创建请求
    wx.request({
      url: url,
      method: 'GET',
      header: {
        'Authorization': 'Bearer ' + token
      },
      success: (res) => {
        // 如果状态码是200，表示请求成功
        if (res.statusCode === 200) {
          // 检查返回数据
          if (res.data) {
            let content = '';
            
            // 处理返回的数据（字符串或对象）
            if (typeof res.data === 'string') {
              content = res.data;
            } else if (typeof res.data === 'object') {
              // 如果是JSON对象，可能需要提取特定字段或转换为字符串
              try {
                content = JSON.stringify(res.data);
              } catch (e) {
                content = '数据解析错误';
              }
            }
            
            // 保存到缓存
            wx.setStorageSync(cacheKey, content);
            console.log('AI洞察数据已缓存:', cacheKey);
            
            // 格式化并显示内容
            const formattedContent = this.formatMarkdownContent(content);
            this.setData({
              insightContent: formattedContent,
              isLoading: false
            });
            
            console.log('AI洞察响应成功解析并显示');
          } else {
            // 数据为空
            this.setData({
              errorMessage: '未获取到分析结果',
              isLoading: false
            });
            console.error('AI洞察响应数据为空');
          }
        } else {
          // 处理错误状态码
          let errorMsg = '服务器错误，请稍后再试';
          if (res.data && typeof res.data === 'string' && res.data.includes('error')) {
            try {
              const errorObj = JSON.parse(res.data);
              if (errorObj.error) {
                errorMsg = errorObj.error;
              }
            } catch (e) {
              // 解析失败，使用默认错误信息
            }
          }
          
          this.setData({
            errorMessage: errorMsg,
            isLoading: false
          });
          console.error('AI洞察请求状态码错误:', res.statusCode);
        }
      },
      fail: (err) => {
        console.error('AI洞察请求失败:', err);
        this.setData({
          isLoading: false,
          errorMessage: '网络请求失败，请检查网络连接后重试'
        });
      }
    });
  },

  /**
   * 清除当前洞察数据的缓存并重新加载
   */
  refreshInsight() {
    const period = this.data.currentPeriod;
    const date = this.data.selectedDate;
    
    // 生成缓存键
    const cacheKey = `ai_insight_${period}_${date}`;
    
    // 移除缓存
    wx.removeStorageSync(cacheKey);
    console.log('已清除缓存:', cacheKey);
    
    // 重新加载数据
    this.getInsight();
    
    wx.showToast({
      title: '已更新数据',
      icon: 'success',
      duration: 1500
    });
  },

  /**
   * 格式化Markdown内容为小程序可显示的富文本
   */
  formatMarkdownContent(markdown) {
    if (!markdown) return '';
    
    let formattedText = markdown;
    
    // 替换Markdown的标题
    formattedText = formattedText.replace(/#{1,6}\s+(.*?)(?:\n|$)/g, function(match, title) {
      const headingLevel = match.trim().split(' ')[0].length;
      const fontSize = 40 - (headingLevel - 1) * 4; // 更大的标题字体
      return `<div style="font-weight: bold; font-size: ${fontSize}rpx; margin: 20rpx 0; color: #333;">${title}</div>`;
    });
    
    // 替换Markdown的粗体
    formattedText = formattedText.replace(/\*\*(.*?)\*\*/g, '<strong style="color: #333;">$1</strong>');
    
    // 替换Markdown的斜体
    formattedText = formattedText.replace(/\*(.*?)\*/g, '<em style="color: #666;">$1</em>');
    
    // 替换Markdown的列表（无序列表）
    formattedText = formattedText.replace(/^-\s+(.*?)(?:\n|$)/gm, 
      '<div style="margin: 10rpx 0; display: flex;"><div style="margin-right: 10rpx;">•</div><div>$1</div></div>');
    
    // 替换Markdown的列表（有序列表）
    formattedText = formattedText.replace(/^(\d+)\.\s+(.*?)(?:\n|$)/gm, 
      '<div style="margin: 10rpx 0; display: flex;"><div style="margin-right: 10rpx;">$1.</div><div>$2</div></div>');
    
    // 保留换行符（确保段落有适当的间距）
    formattedText = formattedText.replace(/\n{2,}/g, '<div style="height: 20rpx;"></div>');
    formattedText = formattedText.replace(/\n/g, '<br>');
    
    // 保留emoji（已经支持）
    
    // 使用div包裹整个内容，添加基本样式
    formattedText = `<div style="line-height: 1.6; color: #333; font-size: 28rpx;">${formattedText}</div>`;
    
    return formattedText;
  },
})