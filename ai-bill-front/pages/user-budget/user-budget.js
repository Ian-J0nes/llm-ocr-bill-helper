// pages/user-buget/user-buget.js
const app = getApp();
const { userBudgetApi } = require('../../utils/api-modules');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    // 表单数据
    budgetTypes: [
      { name: '月度预算', value: 'MONTHLY' },
      { name: '季度预算', value: 'QUARTERLY' },
      { name: '年度预算', value: 'YEARLY' }
    ],
    typeIndex: 0,
    amount: '',
    alertThreshold: 80, // 默认预警阈值为80%
    startDate: '',
    endDate: '',
    minDate: '',
    maxDate: '',
    
    // 预算列表
    budgets: [],
    
    // 当前编辑的预算ID，为null时表示新增
    currentBudgetId: null,
    
    // 表单显示控制
    showForm: false
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 初始化日期
    this.initDates();
    
    // 如果url中带有showForm参数，则直接显示表单
    if (options && options.showForm) {
      this.setData({
        showForm: true
      });
    }
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
    // 加载预算列表
    this.loadBudgets();
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

  // 初始化日期
  initDates() {
    const today = new Date();
    
    // 格式化日期为 YYYY-MM-DD
    const formatDate = (date) => {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    };
    
    const startDate = formatDate(today);
    
    // 默认结束日期为一个月后
    const endDate = new Date(today);
    endDate.setMonth(today.getMonth() + 1);
    
    // 最大日期设置为当前日期的三年后
    const maxDate = new Date(today);
    maxDate.setFullYear(today.getFullYear() + 3);
    
    this.setData({
      startDate: startDate,
      endDate: formatDate(endDate),
      minDate: startDate,
      maxDate: formatDate(maxDate)
    });
  },
  
  /**
   * 加载用户预算列表
   */
  loadBudgets() {
    const token = wx.getStorageSync('token');
    if (!token) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再查看预算',
        confirmText: '去登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            });
          } else {
            wx.navigateBack();
          }
        }
      });
      return;
    }

    wx.showLoading({
      title: '加载中...',
    });

    // 使用新的 API 模块
    userBudgetApi.getBudgetList()
      .then((response) => {
        wx.hideLoading();
        if (response && response.code === 200) {
          const budgets = response.data || [];

          // 格式化预算数据
          const formattedBudgets = budgets.map(budget => {
            return {
              id: budget.id,
              budgetType: this.getBudgetTypeDisplayName(budget.budgetType),
              budgetAmount: budget.budgetAmount,
              startDate: budget.startDate,
              endDate: budget.endDate,
              alertThreshold: budget.alertThreshold
            };
          });

          this.setData({
            budgets: formattedBudgets
          });
        } else {
          console.error('获取预算列表失败:', response);
          wx.showToast({
            title: response.message || '获取预算失败',
            icon: 'none'
          });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error('请求预算列表失败:', err);
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      });
  },
  
  // 获取预算类型的显示名称
  getBudgetTypeDisplayName(typeValue) {
    const type = this.data.budgetTypes.find(type => type.value === typeValue);
    return type ? type.name : typeValue;
  },
  
  // 处理预算类型变化
  handleTypeChange(e) {
    const index = e.detail.value;
    this.setData({
      typeIndex: index
    });
    
    // 根据预算类型调整结束日期
    const type = this.data.budgetTypes[index].value;
    const startDate = new Date(this.data.startDate);
    let endDate = new Date(startDate);
    
    if (type === 'MONTHLY') {
      endDate.setMonth(startDate.getMonth() + 1);
      endDate.setDate(endDate.getDate() - 1);
    } else if (type === 'YEARLY') {
      endDate.setFullYear(startDate.getFullYear() + 1);
      endDate.setDate(endDate.getDate() - 1);
    } else if (type === 'QUARTERLY') {
      // 季度预算，结束日期为开始日期后3个月
      endDate.setMonth(startDate.getMonth() + 3);
      endDate.setDate(endDate.getDate() - 1);
    }
    
    const formatDate = (date) => {
      const year = date.getFullYear();
      const month = (date.getMonth() + 1).toString().padStart(2, '0');
      const day = date.getDate().toString().padStart(2, '0');
      return `${year}-${month}-${day}`;
    };
    
    this.setData({
      endDate: formatDate(endDate)
    });
  },
  
  // 处理金额变化
  handleAmountChange(e) {
    this.setData({
      amount: e.detail.value
    });
  },
  
  // 处理开始日期变化
  handleStartDateChange(e) {
    const startDate = e.detail.value;
    this.setData({
      startDate: startDate
    });
    
    // 如果结束日期早于开始日期，则自动调整结束日期
    if (this.data.endDate < startDate) {
      this.setData({
        endDate: startDate
      });
    }
    
    // 根据预算类型调整结束日期
    const type = this.data.budgetTypes[this.data.typeIndex].value;
    if (type !== 'custom') {
      this.handleTypeChange({ detail: { value: this.data.typeIndex } });
    }
  },
  
  // 处理结束日期变化
  handleEndDateChange(e) {
    this.setData({
      endDate: e.detail.value
    });
  },
  
  // 处理预警阈值变化
  handleAlertThresholdChange(e) {
    this.setData({
      alertThreshold: e.detail.value
    });
  },
  
  /**
   * 保存预算
   */
  saveBudget() {
    // 表单验证
    if (!this.data.amount) {
      wx.showToast({
        title: '请输入预算金额',
        icon: 'none'
      });
      return;
    }

    if (isNaN(parseFloat(this.data.amount)) || parseFloat(this.data.amount) <= 0) {
      wx.showToast({
        title: '请输入有效的预算金额',
        icon: 'none'
      });
      return;
    }

    const token = wx.getStorageSync('token');
    if (!token) {
      wx.showModal({
        title: '提示',
        content: '请先登录后再设置预算',
        confirmText: '去登录',
        cancelText: '取消',
        success: (res) => {
          if (res.confirm) {
            wx.navigateTo({
              url: '/pages/login/login'
            });
          }
        }
      });
      return;
    }

    wx.showLoading({
      title: '保存中...',
    });

    // 构造请求数据
    const budgetData = {
      budgetType: this.data.budgetTypes[this.data.typeIndex].value,
      budgetAmount: parseFloat(this.data.amount),
      startDate: this.data.startDate,
      endDate: this.data.endDate,
      alertThreshold: this.data.alertThreshold
    };

    // 使用新的 API 模块
    const apiCall = this.data.currentBudgetId
      ? userBudgetApi.updateBudget(this.data.currentBudgetId, budgetData)
      : userBudgetApi.createBudget(budgetData);

    apiCall
      .then((response) => {
        console.log('保存预算响应:', response);
        wx.hideLoading();

        if (response && response.code === 200) {
          console.log('进入成功分支');
          wx.showToast({
            title: this.data.currentBudgetId ? '更新成功' : '添加成功',
            icon: 'success'
          });

          // 重置表单
          this.resetForm();

          // 隐藏表单
          this.hideBudgetForm();

          // 重新加载预算列表
          this.loadBudgets();
        } else {
          console.log('进入失败分支');
          console.error('保存预算失败:', response);
          wx.showToast({
            title: response.message || '保存失败',
            icon: 'none'
          });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error('请求保存预算失败:', err);
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      });
  },
  
  // 重置表单
  resetForm() {
    this.setData({
      typeIndex: 0,
      amount: '',
      alertThreshold: 80, // 重置为默认值80%
      currentBudgetId: null
    });
    
    // 重置日期
    this.initDates();
  },
  
  // 编辑预算
  editBudget(e) {
    const budgetId = e.currentTarget.dataset.id;
    const budget = this.data.budgets.find(item => item.id === budgetId);
    
    if (!budget) return;
    
    // 找到对应的预算类型索引
    const typeIndex = this.data.budgetTypes.findIndex(type => 
      type.name === budget.budgetType || type.value === budget.budgetType
    );
    
    this.setData({
      currentBudgetId: budgetId,
      typeIndex: typeIndex >= 0 ? typeIndex : 0,
      amount: budget.budgetAmount.toString(),
      startDate: budget.startDate,
      endDate: budget.endDate,
      alertThreshold: budget.alertThreshold || 80, // 如果没有预警阈值，默认设为80%
      showForm: true // 显示表单以进行编辑
    });
    
    // 滚动到表单区域
    wx.pageScrollTo({
      scrollTop: 0,
      duration: 300
    });
  },
  
  // 删除预算
  deleteBudget(e) {
    const budgetId = e.currentTarget.dataset.id;
    
    wx.showModal({
      title: '确认删除',
      content: '确定要删除该预算吗？',
      success: (res) => {
        if (res.confirm) {
          this.performDelete(budgetId);
        }
      }
    });
  },
  
  /**
   * 执行删除操作
   */
  performDelete(budgetId) {
    const token = wx.getStorageSync('token');
    if (!token) return;

    wx.showLoading({
      title: '删除中...',
    });

    // 使用新的 API 模块
    userBudgetApi.deleteBudget(budgetId)
      .then((response) => {
        wx.hideLoading();
        if (response && response.code === 200) {
          wx.showToast({
            title: '删除成功',
            icon: 'success'
          });

          // 重新加载预算列表
          this.loadBudgets();

          // 如果当前正在编辑该预算，则重置表单
          if (this.data.currentBudgetId === budgetId) {
            this.resetForm();
          }
        } else {
          console.error('删除预算失败:', response);
          wx.showToast({
            title: response.message || '删除失败',
            icon: 'none'
          });
        }
      })
      .catch((err) => {
        wx.hideLoading();
        console.error('请求删除预算失败:', err);
        wx.showToast({
          title: '网络错误',
          icon: 'none'
        });
      });
  },
  
  // 显示预算表单
  showBudgetForm() {
    // 重置表单，确保显示的是空白表单
    this.resetForm();
    this.setData({
      showForm: true
    });
  },
  
  // 隐藏预算表单
  hideBudgetForm() {
    this.setData({
      showForm: false,
      currentBudgetId: null
    });
  }
})