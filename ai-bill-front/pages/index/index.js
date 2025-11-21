/**
 * 首页 - 账单列表
 * @description 展示用户的账单列表，支持按月筛选
 */

const app = getApp();
const api = require('../../utils/api');
const { cacheManager } = require('../../utils/cache');
const { routerOptimizer } = require('../../utils/router');
const { formatDate, getDayLabel, formatCurrency } = require('../../utils/formatters');
const { showError, showLoading, hideLoading } = require('../../utils/error-handler');
const { DEFAULT_AVATAR, UI_CONFIG, STORAGE_KEYS } = require('../../utils/constants');

Page({
  data: {
    motto: 'Hello World',
    userInfo: {
      avatarUrl: DEFAULT_AVATAR.WECHAT_USER,
      nickName: '',
    },
    hasUserInfo: false,
    canIUseGetUserProfile: wx.canIUse('getUserProfile'),
    canIUseNicknameComp: wx.canIUse('input.type.nickname'),
    isPageReady: false,

    currentYear: new Date().getFullYear(),
    currentMonth: new Date().getMonth() + 1,

    monthPickerRange: [], // 用于 picker 的选择范围

    showExpense: true,
    showDiagonalView: true,

    monthSummary: {
      totalExpense: "0.00", // 初始化为字符串
      totalIncome: "0.00"  // 初始化为字符串
    },
    billGroups: [],
    isLoading: false,
  },

  onLoad: function(options) {
    this.initMonthPickerRange(); // 初始化月份选择器范围

    // 检查是否已登录，但不强制跳转
    const isLoggedIn = wx.getStorageSync(STORAGE_KEYS.TOKEN) ? true : false;
    this.setData({ isPageReady: true });

    if (isLoggedIn) {
      if (app.globalData.userInfo) {
        this.setData({
          userInfo: app.globalData.userInfo,
          hasUserInfo: true
        });
      }
      this.loadBillData();
    }
  },

  onShow: function() {
    // 检查是否已登录，但不强制跳转
    const isLoggedIn = wx.getStorageSync(STORAGE_KEYS.TOKEN) ? true : false;

    // 合并多个setData调用以提高性能
    const updateData = { isPageReady: true };

    if (isLoggedIn) {
      if (app.globalData.userInfo) {
        updateData.userInfo = app.globalData.userInfo;
        updateData.hasUserInfo = true;
      }

      // 设置数据后再加载账单数据
      this.setData(updateData);

      // 避免在登录跳转回来时，如果 currentYear/Month 未初始化或不正确时重复加载
      // loadBillData 会根据当前的 currentYear 和 currentMonth 加载
      if (this.data.currentYear && this.data.currentMonth) {
          this.loadBillData();
      }
    } else {
      // 未登录时清空账单数据
      updateData.billGroups = [];
      updateData.monthSummary = { totalExpense: "0.00", totalIncome: "0.00" };
      this.setData(updateData);
    }

    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
        this.getTabBar().setData({
          selected: 0
        });
    }
  },

  /**
   * 初始化月份选择器范围
   */
  initMonthPickerRange: function() {
    const months = [];
    const currentDate = new Date();

    // 生成最近N个月的选项
    for(let i = 0; i < UI_CONFIG.MONTH_PICKER_RANGE; i++) {
      const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1);
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      months.push({
        year: year,
        month: month,
        text: `${year}年${month}月`
      });
    }

    this.setData({
      monthPickerRange: months
    });

    // 检查当前年月是否在picker范围内，如果不在，可以默认选中第一个
    const currentSelectionExists = months.some(m => m.year === this.data.currentYear && m.month === this.data.currentMonth);
    if (!currentSelectionExists && months.length > 0) {
        this.setData({
            currentYear: months[0].year,
            currentMonth: months[0].month
        });
    }
  },

  onMonthPickerChange: function(e) {
    console.log('picker发送选择改变，携带值为', e.detail.value);
    const selectedIndex = parseInt(e.detail.value, 10); //确保是数字
    const selectedMonthData = this.data.monthPickerRange[selectedIndex];

    if (selectedMonthData) {
      this.setData({
        currentYear: selectedMonthData.year,
        currentMonth: selectedMonthData.month,
        billGroups: [], 
        monthSummary: { totalExpense: "0.00", totalIncome: "0.00" } 
      });
      this.loadBillData();
    }
  },

  bindViewTap() {
    routerOptimizer.navigate({
      url: '../logs/logs',
      type: 'navigateTo'
    }).catch(err => {
      console.error('导航失败:', err);
    });
  },

  /**
   * 选择头像
   */
  onChooseAvatar(e) {
    const { avatarUrl } = e.detail
    const { nickName } = this.data.userInfo
    this.setData({
      "userInfo.avatarUrl": avatarUrl,
      hasUserInfo: nickName && avatarUrl && avatarUrl !== DEFAULT_AVATAR.WECHAT_USER,
    })
  },

  /**
   * 昵称输入变化
   */
  onInputChange(e) {
    const nickName = e.detail.value
    const { avatarUrl } = this.data.userInfo
    this.setData({
      "userInfo.nickName": nickName,
      hasUserInfo: nickName && avatarUrl && avatarUrl !== DEFAULT_AVATAR.WECHAT_USER,
    })
  },

  getUserProfile(e) {
    wx.getUserProfile({
      desc: '展示用户信息',
      success: (res) => {
        this.setData({
          userInfo: res.userInfo,
          hasUserInfo: true
        });
        app.globalData.userInfo = res.userInfo;
      }
    })
  },

  toggleViewMode: function() {
    this.setData({
      showDiagonalView: !this.data.showDiagonalView
    });
  },

  toggleSummaryView: function() {
    this.setData({
      showExpense: !this.data.showExpense
    });
  },

  /**
   * 加载账单数据
   */
  loadBillData: function() {
    // 检查登录状态
    if (!wx.getStorageSync(STORAGE_KEYS.TOKEN)) {
      // 未登录时显示友好提示
      this.setData({
        billGroups: [],
        monthSummary: { totalExpense: "0.00", totalIncome: "0.00" },
        isLoading: false
      });
      return;
    }

    if (this.data.isLoading) {
      return;
    }

    // 账单数据不使用缓存，确保实时性
    const year = this.data.currentYear;
    const month = this.data.currentMonth;

    this.setData({ isLoading: true });
    showLoading('加载中...');

    const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
    const lastDayOfMonth = new Date(year, month, 0).getDate();
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDayOfMonth).padStart(2, '0')}`;

    const params = {
      startDate: startDate,
      endDate: endDate
    };

    try {
      api.getBaseURL(); // 验证API配置
    } catch (error) {
      console.error("API base URL not configured");
      hideLoading();
      this.setData({ isLoading: false });
      showError(error, '配置错误');
      return;
    }

    if (!app.globalData.token) {
      console.error("User token not found.");
      hideLoading();
      this.setData({ isLoading: false });
      return;
    }

    api.request({
      endpoint: '/bill',
      method: 'GET',
      data: params
    }).then((response) => {
      console.log("API Response for /bill:", response);
      if (response && response.code === 200) {
        // 兼容两种返回格式：
        // 1. 分页对象: { records: [...], total: 10, ... }
        // 2. 直接数组: [...]
        const billsFromApi = Array.isArray(response.data)
          ? response.data
          : (response.data.records || []);
        console.log("Bills from API:", billsFromApi);

        // 账单数据不缓存，确保实时性
        this.processBillData(billsFromApi);
      } else {
        let errorMsg = response.message || "请求失败";
        console.error("加载账单数据失败:", errorMsg, response);
        showError(errorMsg, '加载失败');
        this.setData({ billGroups: [], monthSummary: { totalExpense: "0.00", totalIncome: "0.00" } });
      }
    }).catch((err) => {
      console.error("请求账单数据接口失败:", err);
      showError(err, '网络错误，请重试');
      this.setData({ billGroups: [], monthSummary: { totalExpense: "0.00", totalIncome: "0.00" } });
    }).finally(() => {
      hideLoading();
      this.setData({ isLoading: false });
    });
  },

  /**
   * 处理账单数据
   */
  processBillData: function(apiBills) {
    // 检查数据是否为空
    if (!apiBills || apiBills.length === 0) {
      console.log("没有账单数据");
      this.setData({
        billGroups: [],
        monthSummary: { totalExpense: "0.00", totalIncome: "0.00" },
        isLoading: false
      });
      hideLoading();
      return;
    }

    // 使用异步处理避免阻塞UI
    wx.nextTick(() => {
      this.processBillDataAsync(apiBills);
    });
  },

  processBillDataAsync: function(apiBills) {
    let totalExpenseMonth = 0;
    let totalIncomeMonth = 0;
    const groupedByDate = {};

    apiBills.forEach(bill => {
      const amount = parseFloat(bill.totalAmount) || 0; 
      let issueDateOnlyString; 

      if (bill.issueDate) { // issueDate from backend should be "YYYY-MM-DD" string for LocalDate
        if (typeof bill.issueDate === 'string' && bill.issueDate.match(/^\d{4}-\d{2}-\d{2}$/)) {
            issueDateOnlyString = bill.issueDate;
        } else if (Array.isArray(bill.issueDate) && bill.issueDate.length >= 3) { // Fallback for old Jackson LocalDate array
            const year = bill.issueDate[0];
            const monthVal = String(bill.issueDate[1]).padStart(2, '0');
            const dayVal = String(bill.issueDate[2]).padStart(2, '0');
            issueDateOnlyString = `${year}-${monthVal}-${dayVal}`;
        } else {
            console.warn('Bill with unparseable/unexpected issueDate format skipped:', bill.issueDate, bill);
            return; 
        }
      } else {
        console.warn('Bill with undefined/null issueDate skipped:', bill);
        return; 
      }

      if (bill.transactionType === 'expense') {
        totalExpenseMonth += amount;
      } else if (bill.transactionType === 'income') {
        totalIncomeMonth += amount;
      }

      if (!groupedByDate[issueDateOnlyString]) {
        groupedByDate[issueDateOnlyString] = {
          date: issueDateOnlyString, 
          dayLabel: getDayLabel(issueDateOnlyString),
          expense: 0,
          income: 0,
          bills: []
        };
      }

      const formattedBill = {
        id: bill.id,
        category: bill.billType || '其他', 
        description: bill.name || '无描述', 
        amount: bill.transactionType === 'expense' ? -amount : amount,
        type: bill.transactionType 
      };
      groupedByDate[issueDateOnlyString].bills.push(formattedBill);

      if (bill.transactionType === 'expense') {
        groupedByDate[issueDateOnlyString].expense += amount;
      } else if (bill.transactionType === 'income') {
        groupedByDate[issueDateOnlyString].income += amount;
      }
    });

    const billGroupsArray = Object.values(groupedByDate).sort((a, b) => {
      return new Date(b.date) - new Date(a.date);
    });

    // 批量更新数据，减少setData调用
    const updateData = {
      monthSummary: {
        totalExpense: formatCurrency(totalExpenseMonth),
        totalIncome: formatCurrency(totalIncomeMonth)
      },
      billGroups: billGroupsArray.map(group => ({
        ...group,
        expense: formatCurrency(group.expense),
        income: formatCurrency(group.income),
        bills: group.bills.map(b => ({
          ...b,
          amount: b.type === 'expense' ? `-${formatCurrency(Math.abs(b.amount))}` : formatCurrency(b.amount)
        }))
      })),
      isLoading: false
    };

    this.setData(updateData);
    hideLoading();
  },


  /**
   * 导航到账单详情
   */
  navigateToBillDetail: function(e) {
    const billId = e.currentTarget.dataset.id;
    console.log('导航到账单详情，ID:', billId, '类型:', typeof billId);

    // 后端现在返回字符串类型的ID，直接使用即可
    routerOptimizer.navigate({
      url: `/pages/bill-detail/bill-detail?id=${encodeURIComponent(billId)}`,
      type: 'navigateTo'
    }).catch(err => {
      console.error('导航到账单详情失败:', err);
      showError(err, '页面跳转失败');
    });
  },

  /**
   * 导航到添加账单页面
   */
  navigateToAddBill: function() {
    // wx.navigateTo({
    //   url: '/pages/add-bill/add-bill'
    // });
  }
})