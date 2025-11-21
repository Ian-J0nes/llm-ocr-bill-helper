/**
 * 账单详情页
 * @description 展示账单的详细信息，支持编辑和删除
 */

const app = getApp();
const api = require('../../utils/api');
const { routerOptimizer } = require('../../utils/router');
const { formatDate, formatFileSize, formatCurrency } = require('../../utils/formatters');
const { showError, showSuccess, showLoading, hideLoading, showDeleteConfirm } = require('../../utils/error-handler');
const { validateBillData } = require('../../utils/validators');
const { DEFAULT_BILL_CATEGORIES, NAVIGATION } = require('../../utils/constants');

Page({
  data: {
    billId: null,
    billDetail: null,
    billDetailEditable: null, // 用于编辑时的数据
    isEditing: false, // 是否处于编辑模式
    billImage: {
      imageUrl: '',
      fileName: '',
      fileSize: '',
      fileType: ''
    },
    isLoading: true,
    errorMessage: '',
    isSubmitting: false, // 提交编辑状态
    billTypeOptions: [], // 从后端获取的账单分类列表
    transactionTypeOptions: [
      { value: 'expense', name: '支出' },
      { value: 'income', name: '收入' }
    ],
    datePickerValue: '', // 日期选择器的值
    today: new Date().toISOString().split('T')[0] // 当前日期，格式：YYYY-MM-DD
  },

  onLoad: function(options) {
    console.log('账单详情页面加载，参数:', options);

    if (options.id) {
      // 后端现在返回字符串类型的ID，直接使用即可
      console.log('接收到的ID:', options.id, '类型:', typeof options.id);

      this.setData({
        billId: options.id
      });

      // 异步加载数据，避免阻塞页面
      wx.nextTick(() => {
        // 并行加载账单分类和详情，提高加载速度
        Promise.all([
          this.loadCategoryOptionsAsync(),
          this.loadBillDetailAsync()
        ]).catch(err => {
          console.error('数据加载失败:', err);
        });
      });
    } else {
      console.error('缺少账单ID参数');
      this.setData({
        isLoading: false,
        errorMessage: '缺少账单ID参数'
      });
      wx.showToast({
        title: '参数错误',
        icon: 'error'
      });
    }
  },

  /**
   * 加载账单分类选项 - 异步版本
   */
  loadCategoryOptionsAsync() {
    return new Promise((resolve) => {
      // 先设置默认分类，避免页面空白
      this.setData({ billTypeOptions: DEFAULT_BILL_CATEGORIES });

      api.request({
        endpoint: '/bill-category/names',
        method: 'GET',
        useCache: false // 账单相关数据不缓存
      }).then((response) => {
        console.log('分类名称API响应:', response);

        if (response && response.code === 200) {
          let categories = response.data || [];

          // 如果没有返回分类，使用默认分类
          if (!categories || categories.length === 0) {
            categories = DEFAULT_BILL_CATEGORIES;
          }

          this.setData({ billTypeOptions: categories });
        }
        resolve();
      }).catch((error) => {
        console.error('加载分类选项失败:', error);
        // 已经设置了默认分类，不需要再次设置
        resolve();
      });
    });
  },

  /**
   * 加载账单分类选项 - 兼容旧版本
   */
  loadCategoryOptions() {
    return this.loadCategoryOptionsAsync();
  },

  /**
   * 加载账单详情 - 异步版本
   */
  loadBillDetailAsync() {
    return new Promise((resolve, reject) => {
      if (!app.globalData.token) {
        console.error('用户未登录');
        app.navigateToLogin();
        reject(new Error('用户未登录'));
        return;
      }

      this.setData({ isLoading: true });
      showLoading('加载中...');

      this.loadBillDetailCore().then(resolve).catch(reject);
    });
  },

  /**
   * 加载账单详情 - 兼容旧版本
   */
  loadBillDetail: function() {
    return this.loadBillDetailAsync();
  },

  /**
   * 账单详情加载核心逻辑
   */
  loadBillDetailCore() {
    return new Promise((resolve, reject) => {
      // 获取账单详情
      api.request({
        endpoint: `/bill/${this.data.billId}`,
        method: 'GET',
        useCache: false // 账单详情不缓存，确保实时性
      }).then((response) => {
      console.log('账单详情API响应:', response);
      
      if (response && response.code === 200) {
        // 获取账单详情数据
        let billDetail = response.data;

        // 处理可能的直接返回账单对象的情况
        if (!billDetail && typeof response === 'object' && response.id && response.transactionType) {
          console.log('API直接返回了账单对象，而不是包装在data中');
          billDetail = response;
        }
        
        if (billDetail) {
          // 格式化日期
          if (billDetail.issueDate) {
            billDetail.issueDate = formatDate(billDetail.issueDate);
          }
          
          // 为金额添加显示格式
          if (billDetail.totalAmount) {
            billDetail.totalAmount_display = parseFloat(billDetail.totalAmount).toFixed(2);
          }
          if (billDetail.taxAmount) {
            billDetail.taxAmount_display = parseFloat(billDetail.taxAmount).toFixed(2);
          }
          
          // 创建可编辑副本
          const billDetailEditable = JSON.parse(JSON.stringify(billDetail));
          
          this.setData({
            billDetail: billDetail,
            billDetailEditable: billDetailEditable,
            datePickerValue: billDetail.issueDate || this.data.today
          });

          // 如果有关联的文件，异步获取文件信息
          if (billDetail.fileId) {
            this.loadBillImage(billDetail.fileId).then(() => {
              resolve();
            }).catch(() => {
              resolve(); // 文件加载失败不影响主流程
            });
          } else {
            this.setData({ isLoading: false });
            hideLoading();
            resolve();
          }
        } else {
          console.error('响应中无有效账单数据:', response);
          this.setData({
            isLoading: false,
            errorMessage: '无法解析账单数据'
          });
          hideLoading();
          showError('数据格式错误');
          reject(new Error('无法解析账单数据'));
        }
      } else {
        console.error('获取账单详情失败:', response);
        this.setData({
          isLoading: false,
          errorMessage: response?.message || '获取账单详情失败'
        });
        hideLoading();
        showError(response, '获取失败');
        reject(new Error(response?.message || '获取账单详情失败'));
      }
    }).catch((err) => {
      console.error('请求账单详情失败:', err);
      this.setData({
        isLoading: false,
        errorMessage: '网络请求失败'
      });
      hideLoading();
      showError(err, '网络错误');
      reject(err);
    });
    });
  },

  loadBillImage: function(fileId) {
    return new Promise((resolve, reject) => {
      console.log('获取文件信息，fileId:', fileId);

      api.request({
        endpoint: `/files/${fileId}`,
        method: 'GET',
        useCache: false // 文件信息不缓存，确保实时性
      }).then((response) => {
      console.log('文件信息API响应:', response);
      
      let fileData = null;
      
      // 处理多种可能的响应格式
      if (response && response.code === 200) {
        if (response.data) {
          // 标准包装响应，数据在data字段
          fileData = response.data;
        } else if (response.message || response.msg) {
          // 特殊情况：数据在message或msg字段中，可能是JSON字符串
          try {
            const jsonStr = response.message || response.msg;
            if (typeof jsonStr === 'string' && (jsonStr.startsWith('{') || jsonStr.startsWith('['))) {
              console.log('尝试解析message字段中的JSON字符串');
              fileData = JSON.parse(jsonStr);
            }
          } catch (e) {
            console.error('解析message字段中的JSON字符串失败:', e);
          }
        }
      } else if (response && !response.code && response.fileUrl) {
        // 直接返回文件对象
        fileData = response;
      }
      
      if (fileData) {
        try {
          const fileInfo = typeof fileData === 'string' ? 
            JSON.parse(fileData) : fileData;
          
          // 确保fileUrl存在
          if (!fileInfo.fileUrl && fileInfo.id) {
            // 如果没有直接的fileUrl但有id，可以尝试构建URL
            console.log('文件信息中没有直接的fileUrl，尝试构建');
            const baseURL = api.getBaseURL();
            fileInfo.fileUrl = `${baseURL}/file/download/${fileInfo.id}`;
          }
          
          console.log('解析后的文件信息:', fileInfo);
          
          this.setData({
            billImage: {
              imageUrl: fileInfo.fileUrl || '',
              fileName: fileInfo.fileName || '',
              fileSize: formatFileSize(fileInfo.fileSize),
              fileType: fileInfo.fileType || ''
            }
          });
        } catch (e) {
          console.error('解析文件信息失败:', e);
        }
      } else {
        console.warn('文件信息响应格式不符合预期:', response);
      }
    }).catch((err) => {
      console.warn('请求文件信息失败，但不影响账单显示:', err);
      reject(err);
    }).finally(() => {
      this.setData({ isLoading: false });
      hideLoading();
      resolve();
    });
    });
  },

  previewImage: function(e) {
    const imageUrl = e.currentTarget.dataset.url;
    if (imageUrl) {
      wx.previewImage({
        current: imageUrl,
        urls: [imageUrl]
      });
    }
  },

  /**
   * 进入编辑模式
   */
  onEditTap() {
    if (!this.data.billDetail) {
      wx.showToast({
        title: '账单数据未加载',
        icon: 'none'
      });
      return;
    }

    // 创建可编辑的副本
    const billDetailEditable = JSON.parse(JSON.stringify(this.data.billDetail));
    
    this.setData({
      billDetailEditable: billDetailEditable,
      isEditing: true
    });
  },

  /**
   * 取消编辑
   */
  cancelEdit() {
    this.setData({
      isEditing: false,
      // 重置可编辑数据
      billDetailEditable: JSON.parse(JSON.stringify(this.data.billDetail))
    });
  },

  /**
   * 保存编辑的账单
   */
  saveEdit() {
    // 使用验证工具验证
    const validationResult = validateBillData(this.data.billDetailEditable);
    if (!validationResult.valid) {
      showError(validationResult.message);
      return;
    }

    this.setData({ isSubmitting: true });
    showLoading('保存中...');

    // 准备要提交的数据
    const billData = this.prepareBillDataForSave();

    api.request({
      endpoint: '/bill/update',
      method: 'PUT',
      data: billData
    }).then((response) => {
      console.log('更新账单API响应:', response);

      if (response && response.code === 200) {
        showSuccess('更新成功');

        // 更新本地数据
        const updatedBillDetail = response.data || billData;
        
        // 格式化日期和金额
        if (updatedBillDetail.issueDate) {
          updatedBillDetail.issueDate = formatDate(updatedBillDetail.issueDate);
        }
        if (updatedBillDetail.totalAmount) {
          updatedBillDetail.totalAmount_display = parseFloat(updatedBillDetail.totalAmount).toFixed(2);
        }
        if (updatedBillDetail.taxAmount) {
          updatedBillDetail.taxAmount_display = parseFloat(updatedBillDetail.taxAmount).toFixed(2);
        }
        
        this.setData({
          billDetail: updatedBillDetail,
          isEditing: false,
          isSubmitting: false
        });
      } else {
        let errorMsg = response?.message || '更新失败';
        // 检查是否是日期格式错误
        if (errorMsg.includes('LocalDateTime') || errorMsg.includes('parse')) {
          console.error('日期格式错误:', errorMsg);
          errorMsg = '日期格式错误，请联系管理员';
        }
        showError(errorMsg);
      }
    }).catch((error) => {
      console.error('更新账单请求失败:', error);
      let errorMsg = '网络错误，请稍后重试';
      // 检查是否是日期格式错误
      if (error && error.message && (error.message.includes('LocalDateTime') || error.message.includes('parse'))) {
        errorMsg = '日期格式错误，请联系管理员';
      }
      showError(errorMsg);
    }).finally(() => {
      this.setData({ isSubmitting: false });
      hideLoading();
    });
  },

  /**
   * 准备账单数据以便保存
   */
  prepareBillDataForSave() {
    const { billDetailEditable } = this.data;
    const billData = { ...billDetailEditable };
    
    // 确保金额是数值类型
    if (billData.totalAmount) {
      billData.totalAmount = parseFloat(billData.totalAmount);
    }
    if (billData.taxAmount) {
      billData.taxAmount = parseFloat(billData.taxAmount);
    }
    
    // 处理日期格式，确保符合LocalDateTime要求
    if (billData.createTime && typeof billData.createTime === 'string') {
      // 将空格替换为'T'以符合ISO-8601格式
      billData.createTime = billData.createTime.replace(' ', 'T');
    }
    
    if (billData.updateTime && typeof billData.updateTime === 'string') {
      // 将空格替换为'T'以符合ISO-8601格式
      billData.updateTime = billData.updateTime.replace(' ', 'T');
    }
    
    // 其他可能包含日期时间的字段
    const dateTimeFields = ['processingTime', 'completionTime', 'reviewTime'];
    dateTimeFields.forEach(field => {
      if (billData[field] && typeof billData[field] === 'string') {
        billData[field] = billData[field].replace(' ', 'T');
      }
    });
    
    // 移除显示专用字段
    Object.keys(billData).forEach(key => {
      if (key.endsWith('_display')) {
        delete billData[key];
      }
    });
    
    return billData;
  },

  /**
   * 金额输入变化
   */
  onAmountInput(e) {
    const value = e.detail.value;
    // 验证输入的金额格式
    if (/^[0-9]*\.?[0-9]{0,2}$/.test(value) || value === '') {
      this.setData({
        'billDetailEditable.totalAmount': value,
        'billDetailEditable.totalAmount_display': value ? parseFloat(value).toFixed(2) : '0.00'
      });
    }
  },

  /**
   * 税额输入变化
   */
  onTaxInput(e) {
    const value = e.detail.value;
    if (/^[0-9]*\.?[0-9]{0,2}$/.test(value) || value === '') {
      this.setData({
        'billDetailEditable.taxAmount': value,
        'billDetailEditable.taxAmount_display': value ? parseFloat(value).toFixed(2) : '0.00'
      });
    }
  },

  /**
   * 账单名称输入变化
   */
  onNameInput(e) {
    this.setData({
      'billDetailEditable.name': e.detail.value
    });
  },

  /**
   * 备注输入变化
   */
  onNotesInput(e) {
    this.setData({
      'billDetailEditable.notes': e.detail.value
    });
  },

  /**
   * 商家/来源输入变化
   */
  onSupplierInput(e) {
    this.setData({
      'billDetailEditable.supplierName': e.detail.value
    });
  },

  /**
   * 发票号输入变化
   */
  onInvoiceNumberInput(e) {
    this.setData({
      'billDetailEditable.invoiceNumber': e.detail.value
    });
  },

  /**
   * 日期选择变化
   */
  onDateChange(e) {
    const dateValue = e.detail.value;
    this.setData({
      'billDetailEditable.issueDate': dateValue,
      datePickerValue: dateValue
    });
  },

  /**
   * 账单分类选择
   */
  onBillTypeChange(e) {
    const value = this.data.billTypeOptions[e.detail.value];
    this.setData({
      'billDetailEditable.billType': value
    });
  },

  /**
   * 交易类型选择
   */
  onTransactionTypeChange(e) {
    const value = this.data.transactionTypeOptions[e.detail.value].value;
    this.setData({
      'billDetailEditable.transactionType': value
    });
  },

  /**
   * 删除账单
   */
  deleteBill: function() {
    showDeleteConfirm({
      content: '确定要删除这条账单记录吗？删除后无法恢复。',
      onConfirm: () => {
        this.performDeleteBill();
      }
    });
  },

  /**
   * 执行删除账单操作
   */
  performDeleteBill: function() {
    if (!app.globalData.token) {
      app.navigateToLogin();
      return;
    }

    showLoading('删除中...');

    api.request({
      endpoint: `/bill/${this.data.billId}`,
      method: 'DELETE'
    }).then((response) => {
      console.log('删除账单API响应:', response);

      if (response && response.code === 200) {
        showSuccess('删除成功', () => {
          routerOptimizer.smartGoBack();
        });
      } else {
        showError(response, '删除失败');
      }
    }).catch((err) => {
      console.error('删除账单请求失败:', err);
      showError(err, '网络错误');
    }).finally(() => {
      hideLoading();
    });
  },

  onShareAppMessage: function() {
    return {
      title: '账单详情',
      path: `/pages/bill-detail/bill-detail?id=${this.data.billId}`
    };
  }
}); 