// pages/currency-exchange/currency-exchange.js
// 获取全局数据
const app = getApp();
const api = require('../../utils/api');
const { exchangeRateApi } = require('../../utils/api-modules');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    sourceCurrencies: [], // 将由API填充 { code: 'USD', name: '美元' }
    targetCurrencies: [], // 同上
    sourceCurrencyIndex: 0,
    targetCurrencyIndex: 1, // 默认尝试选中第二个
    amount: '',
    convertedAmount: null,
    exchangeRate: null,
    rateTimestamp: null, // 汇率更新时间
    fromCurrencyCode: '', // 当前选择的源货币代码
    toCurrencyCode: '',   // 当前选择的目标货币代码
    isLoadingCurrencies: true, // 货币列表加载状态
    errorLoadingCurrencies: '', // 加载货币列表的错误信息
    conversionError: '', // 转换操作的错误信息
    token: '', // 新增：用于存储认证Token
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 新增：页面加载时获取Token
    const token = wx.getStorageSync('token') || '';
    if (!token) {
      // 如果没有token，可以根据实际需求选择跳转到登录页或给出提示
      wx.showToast({
        title: '请先登录',
        icon: 'none',
        duration: 2000,
        complete: () => {
          // 示例：跳转到登录页，请根据您的项目实际路径修改
          wx.reLaunch({ url: '/pages/login/login' });
        }
      });
      // 即使没有token，也尝试加载一次货币列表，可能部分API不需要严格验证
      // 或者在这里直接setData，提示用户需要登录才能使用全部功能
      this.setData({ isLoadingCurrencies: false, errorLoadingCurrencies: '请先登录后使用完整功能。'});
      // return; // 如果没有token则不继续加载
    }
    this.setData({ token: token });
    this.loadCurrencies();
  },

  loadCurrencies: function() {
    this.setData({ isLoadingCurrencies: true, errorLoadingCurrencies: '' });

    api.request({
      endpoint: '/api/exchange/currencies',
      method: 'GET'
    }).then((response) => {
      console.log('获取货币列表响应:', response);
      
      let currencyData = response;
      
      // 处理多种可能的响应格式
      if (response && response.code === 200 && response.data) {
        // 标准包装响应，data是数组
        currencyData = response.data;
      }
      
      // 判断返回数据是否为数组
      if (Array.isArray(currencyData) && currencyData.length > 0) {
        const currencies = currencyData.map(currency => ({
          code: currency.code,
          name: currency.name 
        }));

        if (currencies.length > 0) {
          let initialTargetIndex = currencies.length > 1 ? 1 : 0;
          let initialToCode = currencies.length > 1 ? currencies[initialTargetIndex].code : currencies[0].code;
          
          if (currencies[0].code === initialToCode && currencies.length > 1) {
              initialTargetIndex = 1; 
              initialToCode = currencies[initialTargetIndex].code;
          } else if (currencies.length > 1 && initialTargetIndex < currencies.length -1 && currencies[0].code === currencies[initialTargetIndex].code) {
              for(let i = initialTargetIndex + 1; i < currencies.length; i++) {
                  if(currencies[i].code !== currencies[0].code) {
                      initialTargetIndex = i;
                      initialToCode = currencies[i].code;
                      break;
                  }
              }
          }

          this.setData({
            sourceCurrencies: currencies,
            targetCurrencies: currencies,
            sourceCurrencyIndex: 0,
            fromCurrencyCode: currencies[0].code,
            targetCurrencyIndex: initialTargetIndex,
            toCurrencyCode: initialToCode,
            isLoadingCurrencies: false,
          });

          if (currencies.length === 1) {
            this.setData({ conversionError: '只有一个可用货币，无法进行兑换' });
          }
        } else {
          this.setData({ errorLoadingCurrencies: '未获取到可用货币列表', isLoadingCurrencies: false });
        }
      } else if (response && response.code) {
        // 如果有code字段，可能是错误响应
        let errorMsg = response?.message || `加载货币列表失败 (状态码: ${response?.code || '未知'})`;
        this.setData({ errorLoadingCurrencies: errorMsg, isLoadingCurrencies: false });
        
        if (response?.code === 401) {
          wx.showToast({ title: '请先登录', icon: 'none', duration: 2000 });
        }
      } else {
        // 其他情况
        console.error('货币数据格式错误:', response);
        this.setData({ errorLoadingCurrencies: '货币数据格式错误', isLoadingCurrencies: false });
      }
    }).catch((err) => {
      console.error("加载货币列表失败:", err);
      this.setData({ errorLoadingCurrencies: '网络请求失败，无法加载货币列表', isLoadingCurrencies: false });
    });
  },

  bindAmountInput: function (e) {
    this.setData({
      amount: e.detail.value,
      convertedAmount: null,
      exchangeRate: null,
      rateTimestamp: null,
      conversionError: ''
    });
  },

  bindSourceCurrencyChange: function (e) {
    const selectedIndex = parseInt(e.detail.value, 10);
    if (this.data.sourceCurrencies[selectedIndex]) {
      this.setData({
        sourceCurrencyIndex: selectedIndex,
        fromCurrencyCode: this.data.sourceCurrencies[selectedIndex].code,
        convertedAmount: null, 
        exchangeRate: null,
        rateTimestamp: null,
        conversionError: ''
      });
    }
  },

  bindTargetCurrencyChange: function (e) {
    const selectedIndex = parseInt(e.detail.value, 10);
    if (this.data.targetCurrencies[selectedIndex]) {
      this.setData({
        targetCurrencyIndex: selectedIndex,
        toCurrencyCode: this.data.targetCurrencies[selectedIndex].code,
        convertedAmount: null, 
        exchangeRate: null,
        rateTimestamp: null,
        conversionError: ''
      });
    }
  },

  convertCurrency: function () {
    const amountStr = this.data.amount;
    const amount = parseFloat(amountStr);
    const fromCurrency = this.data.fromCurrencyCode;
    const toCurrency = this.data.toCurrencyCode;

    if (amountStr.trim() === '' || isNaN(amount) || amount <= 0) {
      wx.showToast({ title: '请输入有效的金额', icon: 'none' });
      this.setData({ conversionError: '金额必须为大于零的数字' });
      return;
    }
    if (!fromCurrency || !toCurrency) {
      wx.showToast({ title: '请选择货币', icon: 'none' });
      this.setData({ conversionError: '请选择源货币和目标货币' });
      return;
    }

    this.setData({ conversionError: '', convertedAmount: null, exchangeRate: null, rateTimestamp: null });

    if (fromCurrency === toCurrency) {
      this.setData({
        convertedAmount: amount.toFixed(2),
        exchangeRate: '1.00000000',
        rateTimestamp: 'N/A',
        conversionError: this.data.sourceCurrencies.length === 1 ? '只有一个可用货币' : ''
      });
      return;
    }
    
    wx.showLoading({ title: '转换中...' });

    const currentToken = this.data.token || wx.getStorageSync('token');
    if (!currentToken) {
       wx.hideLoading();
       wx.showToast({ title: '登录信息缺失，请重新登录', icon: 'none' });
       this.setData({ conversionError: '请先登录后再进行转换。'});
       return;
    }

    exchangeRateApi.convertCurrency({
      amount: amount,
      from: fromCurrency,
      to: toCurrency
    })
      .then((response) => {
        wx.hideLoading();
        console.log('货币转换响应:', response);

        // 增加响应格式检测和处理能力
        if (response && response.code === 200) {
          // 标准结果包装的成功响应
          if (response.data) {
            this.handleSuccessConversion(response.data);
          } else {
            // 响应结构不完整
            this.setData({ conversionError: '响应格式异常，但状态码正常' });
            console.error('响应状态码为200但缺少data:', response);
          }
        } else if (response && response.code) {
          // 包含code但非200的响应
          let errorMsg = response?.message || `转换失败 (状态码: ${response.code})`;
          this.setData({ conversionError: errorMsg });

          if (response.code === 401) {
            wx.showToast({ title: '请先登录', icon: 'none', duration: 2000 });
          } else {
            wx.showToast({ title: errorMsg, icon: 'none', duration: 2500 });
          }
        } else if (response && response.error !== undefined) {
          // 直接的错误响应
          this.setData({ conversionError: response.error || '转换失败' });
          wx.showToast({ title: response.error || '转换失败', icon: 'none' });
        } else if (response && response.convertedAmount !== undefined) {
          // 直接返回结果的响应
          this.handleSuccessConversion(response);
        } else if (typeof response === 'object') {
          // 其他可识别的对象
          const responseStr = JSON.stringify(response).substring(0, 100);
          console.error('未预期的响应格式:', responseStr);
          this.setData({ conversionError: '收到意外的响应格式' });
        } else {
          // 完全未知的响应
          console.error('不可识别的响应:', response);
          this.setData({ conversionError: '系统错误，请稍后重试' });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        let errorMsg = '网络请求失败，请检查网络连接';

        if (err && err.message) {
          console.error("货币转换错误:", err.message);
          errorMsg = `请求错误: ${err.message}`;
        } else {
          console.error("货币转换失败:", err);
        }

        this.setData({ conversionError: errorMsg });
        wx.showToast({ title: errorMsg, icon: 'none' });
      });
  },
  
  // 处理成功转换结果
  handleSuccessConversion: function(data) {
    try {
      if (!data) {
        throw new Error('空数据');
      }
      
      let convertedAmount, rateUsed, rateTimestamp;
      
      // 检查字段是否存在
      if (data.convertedAmount !== undefined) {
        convertedAmount = parseFloat(data.convertedAmount);
      } else {
        throw new Error('缺少转换金额');
      }
      
      if (data.rateUsed !== undefined) {
        rateUsed = parseFloat(data.rateUsed);
      } else {
        rateUsed = 0; // 默认值
        console.warn('响应中缺少汇率信息');
      }
      
      this.setData({
        convertedAmount: isNaN(convertedAmount) ? '0.00' : convertedAmount.toFixed(2),
        exchangeRate: isNaN(rateUsed) ? '0.00000000' : rateUsed.toFixed(8),
        rateTimestamp: data.rateTimestamp ? new Date(data.rateTimestamp).toLocaleString() : 'N/A',
        conversionError: ''
      });
    } catch(e) {
      console.error('处理转换结果时出错:', e, data);
      this.setData({ 
        conversionError: `处理转换结果时出错: ${e.message || '未知错误'}`,
        convertedAmount: null,
        exchangeRate: null,
        rateTimestamp: null
      });
    }
  },

  swapCurrencies: function () {
    const { sourceCurrencyIndex, targetCurrencyIndex, sourceCurrencies, targetCurrencies } = this.data;

    if (sourceCurrencies.length === 0 || targetCurrencies.length === 0) {
        wx.showToast({ title: '货币列表未加载', icon: 'none' });
        return;
    }
    
    const oldFromCode = this.data.fromCurrencyCode;
    const oldToCode = this.data.toCurrencyCode;

    this.setData({
      sourceCurrencyIndex: targetCurrencyIndex,
      targetCurrencyIndex: sourceCurrencyIndex,
      fromCurrencyCode: oldToCode,
      toCurrencyCode: oldFromCode,
      convertedAmount: null,
      exchangeRate: null,
      rateTimestamp: null,
      conversionError: sourceCurrencies.length > 0 && targetCurrencies.length > 0 ? '货币已交换, 请点击转换' : ''
    });
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
    // 检查token是否有效
    const token = wx.getStorageSync('token');
    if (token !== this.data.token) {
      this.setData({ token });
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
    // 重新加载货币列表
    this.loadCurrencies();
    setTimeout(() => {
      wx.stopPullDownRefresh();
    }, 1000);
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
    return {
      title: '货币汇率查询',
      path: '/pages/currency-exchange/currency-exchange'
    };
  }
})