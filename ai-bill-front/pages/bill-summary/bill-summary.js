const app = getApp();
const { billApi } = require('../../utils/api-modules');
const { cacheManager } = require('../../utils/cache');

Page({
  /**
   * 页面的初始数据
   */
  data: {
    pieChartType: 'expense', // 'expense' or 'income'
    dates: [], // 将从真实数据中生成
    dailyRecords: [], // 将从真实数据中生成
    expenseCategoriesData: [], // 将从真实数据中生成
    incomeCategoriesData: [], // 将从真实数据中生成
    isLoading: false,
    totalExpense: 0,
    totalIncome: 0,
    balance: 0, // 结余金额
    balanceType: 'positive', // 结余类型：positive 或 negative
    currentMonth: new Date().getMonth() + 1,
    currentYear: new Date().getFullYear(),
    billsData: [], // 存储从后端获取的原始账单数据
    hasInitialLoad: false, // 标记是否已经初始加载过数据
    monthPickerRange: [], // 月份选择器选项
    monthPickerIndex: 0, // 当前选中的月份索引
    currentPage: 1, // 当前页码
    pageSize: 10, // 每页显示的账单数量
    hasMoreBills: true, // 是否还有更多账单

    // CSS 图表相关数据
    chartDays: [], // 柱状图每日数据
    yAxisLabels: [], // Y轴标签
    currentCategoriesData: [], // 当前显示的分类数据
    pieChartGradient: '', // 饼图渐变色
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    // 初始化月份选择器
    this.initMonthPickerRange();
    // 页面加载时获取账单数据
    this.loadBillData().then(() => {
      this.setData({ hasInitialLoad: true });
    });
  },

  /**
   * 初始化月份选择器范围
   */
  initMonthPickerRange() {
    const months = [];
    const currentDate = new Date();
    for(let i = 0; i < 12; i++) { // 生成12个月的选项
      const date = new Date(currentDate.getFullYear(), currentDate.getMonth() - i, 1);
      const year = date.getFullYear();
      const month = date.getMonth() + 1;
      months.push({ 
        year: year, 
        month: month, 
        text: `${year}年${month}月` 
      });
    }
    
    // 找到当前月份的索引
    let currentIndex = months.findIndex(m => 
      m.year === this.data.currentYear && m.month === this.data.currentMonth
    );
    if (currentIndex === -1) {
      currentIndex = 0; // 如果找不到，默认选择第一个
      // 同时更新当前年月为第一个选项
      this.setData({
        currentYear: months[0].year,
        currentMonth: months[0].month
      });
    }
    
    this.setData({
      monthPickerRange: months,
      monthPickerIndex: currentIndex
    });
  },

  /**
   * 月份选择器变化事件
   */
  onMonthPickerChange(e) {
    console.log('月份选择器发送选择改变，携带值为', e.detail.value);
    const selectedIndex = parseInt(e.detail.value, 10);
    const selectedMonthData = this.data.monthPickerRange[selectedIndex];

    if (selectedMonthData) {
      console.log('选择的月份数据:', selectedMonthData);
      this.setData({
        currentYear: selectedMonthData.year,
        currentMonth: selectedMonthData.month,
        monthPickerIndex: selectedIndex
      });
      // 重新加载数据
      this.loadBillData().then(() => {
        wx.showToast({
          title: `已切换到${selectedMonthData.text}`,
          icon: 'success',
          duration: 1500
        });
      }).catch((error) => {
        console.error('加载数据失败:', error);
        wx.showToast({
          title: '数据加载失败',
          icon: 'none',
          duration: 2000
        });
      });
    }
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {
    // 纯 CSS 实现，无需初始化图表组件
  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    // 只有在未初始加载时才重新加载数据
    if (!this.data.hasInitialLoad) {
      this.loadBillData().then(() => {
        this.setData({ hasInitialLoad: true });
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
    // 纯 CSS 实现，无需清理图表实例
  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {
    this.loadBillData().then(() => {
      wx.stopPullDownRefresh();
    });
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
   * 从后端获取账单数据
   */
  loadBillData() {
    if (this.data.isLoading) {
      return Promise.reject("加载中，请稍后...");
    }

    // 检查登录状态
    if (!wx.getStorageSync('token')) {
      wx.showToast({
        title: '登录后可查看详细账单统计',
        icon: 'none',
        duration: 2000
      });
      // 清空数据并返回空状态的Promise
      this.setData({
        billsData: [],
        totalExpense: 0,
        totalIncome: 0,
        balance: 0,
        balanceType: 'positive',
        dailyRecords: [],
        expenseCategoriesData: [],
        incomeCategoriesData: [],
        chartDays: [], // 柱状图每日数据
        yAxisLabels: [], // Y轴标签
        currentCategoriesData: [],
        isLoading: false
      });
      return Promise.resolve(); // 返回一个已解决的Promise
    }

    this.setData({ isLoading: true });
    wx.showLoading({ title: '加载统计数据...' });

    // 构建API请求参数
    const year = this.data.currentYear;
    const month = this.data.currentMonth;
    const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
    // 计算月末日期
    const lastDayOfMonth = new Date(year, month, 0).getDate();
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDayOfMonth).padStart(2, '0')}`;

    const params = {
      startDate: startDate,
      endDate: endDate,
      current: 1,
      size: 1000 // 获取足够多的数据用于统计
    };

    return new Promise((resolve, reject) => {
      // 使用新的 API 模块
      billApi.getBillList(params)
        .then((response) => {
          console.log("账单统计页面 API Response:", response);
          if (response && response.code === 200) {
            const billsFromApi = response.data.records || [];
            console.log("Bills from API for summary:", billsFromApi);
            this.processBillDataForCharts(billsFromApi);
            // 账单数据不缓存，确保实时性
            // 重置分页数据并保存原始账单数据
            this.setData({
              billsData: billsFromApi,
              currentPage: 1,
              hasMoreBills: billsFromApi.length >= this.data.pageSize
            });
            resolve(billsFromApi);
          } else {
            let errorMsg = response.message || "请求失败";
            console.error("加载账单统计数据失败:", errorMsg, response);
            wx.showToast({
              title: `加载失败: ${errorMsg}`,
              icon: 'none'
            });
            reject(errorMsg);
          }
        })
        .catch((error) => {
          console.error("账单统计API请求异常:", error);
          wx.showToast({
            title: '网络错误，请稍后重试',
            icon: 'none'
          });
          reject(error);
        })
        .finally(() => {
          this.setData({ isLoading: false });
          wx.hideLoading();
        });
    });
  },

  /**
   * 处理账单数据，生成图表所需的数据结构
   */
  processBillDataForCharts(billsData) {
    this.setData({ billsData: billsData });

    // 生成日期数组（当前月份的所有日期）
    const year = this.data.currentYear;
    const month = this.data.currentMonth;
    const daysInMonth = new Date(year, month, 0).getDate();
    const dates = [];
    for (let i = 1; i <= daysInMonth; i++) {
      dates.push(`${String(month).padStart(2, '0')}-${String(i).padStart(2, '0')}`);
    }

    // 初始化每日收支数据
    const dailyRecords = [];
    for (let i = 0; i < daysInMonth; i++) {
      dailyRecords.push({ expense: 0, income: 0 });
    }

    // 统计分类数据
    const expenseCategories = {};
    const incomeCategories = {};
    let totalExpense = 0;
    let totalIncome = 0;

    // 处理账单数据
    billsData.forEach(bill => {
      const billDate = new Date(bill.issueDate);
      const billDay = billDate.getDate();
      const amount = parseFloat(bill.totalAmount) || 0;

      console.log('处理账单:', bill.name, '金额:', amount, '日期:', bill.issueDate, '类型:', bill.transactionType);

      // 统计每日数据
      if (billDay >= 1 && billDay <= daysInMonth) {
        if (bill.transactionType === 'expense') {
          dailyRecords[billDay - 1].expense += amount;
          totalExpense += amount;
          
          // 统计支出分类
          const category = bill.billType || '其他';
          expenseCategories[category] = (expenseCategories[category] || 0) + amount;
        } else if (bill.transactionType === 'income') {
          dailyRecords[billDay - 1].income += amount;
          totalIncome += amount;
          
          // 统计收入分类
          const category = bill.billType || '其他';
          incomeCategories[category] = (incomeCategories[category] || 0) + amount;
        }
      }
    });

    console.log('统计结果 - 总支出:', totalExpense, '总收入:', totalIncome);
    console.log('支出分类:', expenseCategories);
    console.log('收入分类:', incomeCategories);

    // 转换分类数据为图表格式
    const expenseCategoriesData = Object.entries(expenseCategories).map(([name, value]) => ({
      name,
      value: parseFloat(value.toFixed(2))
    }));

    const incomeCategoriesData = Object.entries(incomeCategories).map(([name, value]) => ({
      name,
      value: parseFloat(value.toFixed(2))
    }));

    // 如果没有数据，提供默认显示
    if (expenseCategoriesData.length === 0) {
      expenseCategoriesData.push({ name: '暂无支出数据', value: 0 });
    }
    if (incomeCategoriesData.length === 0) {
      incomeCategoriesData.push({ name: '暂无收入数据', value: 0 });
    }

    // 更新页面数据
    const balance = totalIncome - totalExpense;
    const balanceType = balance >= 0 ? 'positive' : 'negative';
    
    this.setData({
      dates: dates,
      dailyRecords: dailyRecords,
      expenseCategoriesData: expenseCategoriesData,
      incomeCategoriesData: incomeCategoriesData,
      totalExpense: totalExpense.toFixed(2),
      totalIncome: totalIncome.toFixed(2),
      balance: balance.toFixed(2),
      balanceType: balanceType
    });

    // 为 CSS 图表准备数据
    this.prepareChartData();
  },

  /**
   * 为 CSS 图表准备数据
   */
  prepareChartData() {
    // 准备柱状图数据
    this.prepareBarChartData();
    
    // 准备饼图数据
    this.preparePieChartData();
  },

  /**
   * 准备柱状图数据
   */
  prepareBarChartData() {
    const maxExpense = Math.max(...this.data.dailyRecords.map(r => r.expense));
    const maxIncome = Math.max(...this.data.dailyRecords.map(r => r.income));
    const maxValue = Math.max(maxExpense, maxIncome);
    
    // 如果最大值为0，设置默认值避免除以0
    const normalizeValue = maxValue > 0 ? maxValue : 100;

    // 生成 Y 轴标签
    const yAxisLabels = [];
    for (let i = 0; i <= 5; i++) {
      yAxisLabels.push((normalizeValue * i / 5).toFixed(0));
    }

    // 只显示有数据的日期，最多显示15天
    const chartDays = [];
    this.data.dailyRecords.forEach((record, index) => {
      if (record.expense > 0 || record.income > 0) {
        chartDays.push({
          day: index + 1,
          show: true,
          expenseHeight: (record.expense / normalizeValue) * 100,
          incomeHeight: (record.income / normalizeValue) * 100
        });
      }
    });

    // 如果数据太多，只显示前15个有数据的日期
    if (chartDays.length > 15) {
      chartDays.splice(15);
    }

    // 如果没有数据，显示几个示例
    if (chartDays.length === 0) {
      for (let i = 1; i <= 5; i++) {
        chartDays.push({
          day: i,
          show: true,
          expenseHeight: 0,
          incomeHeight: 0
        });
      }
    }

    this.setData({
      chartDays: chartDays,
      yAxisLabels: yAxisLabels
    });
  },

  /**
   * 准备饼图数据
   */
  preparePieChartData() {
    const currentData = this.data.pieChartType === 'expense' 
      ? this.data.expenseCategoriesData 
      : this.data.incomeCategoriesData;
    
    const total = currentData.reduce((sum, item) => sum + item.value, 0);
    
    // 预定义颜色 - 支出用红色系，收入用绿色系
    const expenseColors = [
      '#FF6B6B', '#FF9F43', '#FD79A8', '#FDCB6E', '#FF7675',
      '#E17055', '#E84393', '#F39C12', '#E74C3C', '#C0392B',
      '#FF5722', '#FF9800', '#FF5252', '#FF1744', '#D32F2F'
    ];
    
    const incomeColors = [
      '#4ECDC4', '#00D68F', '#2ECC71', '#27AE60', '#16A085',
      '#48C9B0', '#58D68D', '#52C41A', '#389E0D', '#237804',
      '#67B279', '#52C41A', '#95DE64', '#B7EB8F', '#73D13D'
    ];
    
    const colors = this.data.pieChartType === 'expense' ? expenseColors : incomeColors;

    // 添加颜色和百分比
    const categoriesWithColor = currentData.map((item, index) => ({
      ...item,
      color: colors[index % colors.length],
      percentage: total > 0 ? ((item.value / total) * 100).toFixed(1) : '0.0'
    }));

    // 生成饼图渐变色字符串
    let gradientString = '';
    let currentAngle = 0;
    
    categoriesWithColor.forEach((item, index) => {
      const percentage = parseFloat(item.percentage);
      const angle = (percentage / 100) * 360;
      
      if (index === 0) {
        gradientString += `${item.color} 0deg ${angle}deg`;
      } else {
        gradientString += `, ${item.color} ${currentAngle}deg ${currentAngle + angle}deg`;
      }
      
      currentAngle += angle;
    });

    // 如果没有数据，显示灰色
    if (total === 0) {
      gradientString = '#e0e0e0 0deg 360deg';
    }

    this.setData({
      currentCategoriesData: categoriesWithColor,
      pieChartGradient: gradientString
    });
  },

  /**
   * 切换饼图类型
   */
  onPieChartTypeChange(e) {
    const type = e.currentTarget.dataset.type;
    if (type !== this.data.pieChartType) {
      this.setData({
        pieChartType: type
      });
      // 重新准备饼图数据
      this.preparePieChartData();
    }
  },

  /**
   * 点击账单条目，查看详情
   */
  onBillItemTap(e) {
    const billId = e.currentTarget.dataset.id;
    if (!billId) {
      wx.showToast({
        title: '账单ID无效',
        icon: 'none'
      });
      return;
    }
    
    // 跳转到账单详情页
    wx.navigateTo({
      url: `/pages/bill-detail/bill-detail?id=${billId}`
    });
  },

  /**
   * 编辑账单
   */
  onEditBillTap(e) {
    const billId = e.currentTarget.dataset.id;
    if (!billId) {
      wx.showToast({
        title: '账单ID无效',
        icon: 'none'
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/bill-detail/bill-detail?id=${billId}`
    });
  },

  /**
   * 加载更多账单
   */
  loadMoreBills() {
    if (!this.data.hasMoreBills || this.data.isLoading) {
      return;
    }

    this.setData({ isLoading: true });

    const nextPage = this.data.currentPage + 1;
    const year = this.data.currentYear;
    const month = this.data.currentMonth;
    const startDate = `${year}-${String(month).padStart(2, '0')}-01`;
    const lastDayOfMonth = new Date(year, month, 0).getDate();
    const endDate = `${year}-${String(month).padStart(2, '0')}-${String(lastDayOfMonth).padStart(2, '0')}`;

    const params = {
      startDate: startDate,
      endDate: endDate,
      current: nextPage,
      size: this.data.pageSize
    };

    // 使用新的 API 模块
    billApi.getBillList(params)
      .then((response) => {
        if (response && response.code === 200) {
          const newBills = response.data.records || [];

          if (newBills.length > 0) {
            // 追加新数据
            this.setData({
              billsData: [...this.data.billsData, ...newBills],
              currentPage: nextPage,
              hasMoreBills: newBills.length >= this.data.pageSize
            });
          } else {
            this.setData({
              hasMoreBills: false
            });
            wx.showToast({
              title: '已加载全部数据',
              icon: 'none'
            });
          }
        } else {
          wx.showToast({
            title: '加载失败',
            icon: 'none'
          });
        }
      })
      .catch((error) => {
        console.error('加载更多账单失败:', error);
        wx.showToast({
          title: '网络错误，请稍后重试',
          icon: 'none'
        });
      })
      .finally(() => {
        this.setData({ isLoading: false });
      });
  },
}); 