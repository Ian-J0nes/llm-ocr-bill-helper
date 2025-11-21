/**
 * 聊天页面 - AI智能记账助手
 * @description 与AI助手对话，自动识别账单信息
 */

const app = getApp();
const api = require('../../utils/api');
const { cacheManager } = require('../../utils/cache');
const { tokenManager } = require('../../utils/token-manager');
const { loginHelper } = require('../../utils/login-helper');
const { formatDate, formatCurrency } = require('../../utils/formatters');
const { showError, showSuccess, showLoading, hideLoading } = require('../../utils/error-handler');
const { validateBillData } = require('../../utils/validators');
const {
  DEFAULT_BILL_CATEGORIES,
  DEFAULT_AVATAR,
  FILE_SIZE,
  TOAST_DURATION,
  HTTP_STATUS,
  UI_CONFIG
} = require('../../utils/constants');

Page({
  data: {
    messages: [],
    inputMessage: '',
    scrollTop: 0,
    isLoading: false,
    token: '',
    streamingMessageIndex: -1,
    scrollToMessageId: null,
    userInfo: {
      avatarUrl: DEFAULT_AVATAR.WECHAT_USER,
      nickName: '微信用户'
    },
    billTypeOptions: [],
    transactionTypeOptions: [
      { value: 'expense', name: '支出' },
      { value: 'income', name: '收入' }
    ],
  },

  // --- Lifecycle Methods ---
  onLoad(options) {
    const token = this._getAuthToken();
    this.setData({ token });

    try {
      api.getBaseURL(); // 验证API配置是否正确
    } catch (error) {
      console.error("CRITICAL ERROR: BASE_URL is not set!");
      this._showErrorModal('应用基础URL未配置，无法发送请求。');
    }

    // 异步加载账单分类，避免阻塞页面
    wx.nextTick(() => {
      this.loadBillTypeOptions();
    });
  },

  onReady() {
    this._updateTabBarSelected();
  },

  onShow() {
    this._updateTabBarSelected();

    // 检查登录状态但不强制跳转
    const token = this._getAuthToken();
    const updateData = {};

    // 只有当token有效时才更新
    if (token && (!this.data.token || this.data.token !== token)) {
      updateData.token = token;
    }

    // 批量更新用户信息
    const userInfo = this._getUserInfoSync();
    if (userInfo && (userInfo.avatarUrl !== this.data.userInfo.avatarUrl || userInfo.nickName !== this.data.userInfo.nickName)) {
      updateData.userInfo = userInfo;
    }

    // 一次性更新所有数据
    if (Object.keys(updateData).length > 0) {
      this.setData(updateData);
    }

    // 如果账单分类为空，异步重新加载
    if (this.data.billTypeOptions.length === 0) {
      wx.nextTick(() => {
        this.loadBillTypeOptions();
      });
    }
  },

  onHide() { /* Placeholder */ },
  onUnload() { /* Placeholder */ },
  onPullDownRefresh() { wx.stopPullDownRefresh(); },
  onReachBottom() { /* Placeholder for infinite scroll if needed */ },

  onShareAppMessage() {
    return {
      title: '小咩智能记账',
      path: '/pages/chat/chat'
    };
  },
  
  /**
   * 加载账单分类选项
   */
  loadBillTypeOptions() {
    if (!this.data.token) {
      console.warn('未登录，无法获取账单分类');
      return;
    }

    api.request({
      endpoint: '/bill-category/names',
      method: 'GET',
      useCache: false
    }).then((response) => {
      console.log('分类名称API响应:', response);

      if (response && response.code === HTTP_STATUS.SUCCESS) {
        let categories = response.data || [];

        if (!categories || categories.length === 0) {
          categories = DEFAULT_BILL_CATEGORIES;
        }

        this.setData({ billTypeOptions: categories });
      }
    }).catch((error) => {
      console.error('加载分类选项失败:', error);
      this.setData({ billTypeOptions: DEFAULT_BILL_CATEGORIES });
    });
  },

  // --- User Info and Auth ---
  _getAuthToken() {
    const token = tokenManager.getValidToken();
    if (!token) {
      // 使用统一的登录提示框
      loginHelper.checkLoginWithPrompt({
        content: '请先登录后再使用聊天功能'
      });
      return null;
    }
    return token;
  },

  _getUserId() {
    return wx.getStorageSync('userId') || app.globalData.userInfo?.id || app.globalData.userInfoFromApi?.id || null;
  },

  _loadUserInfo() {
    const userInfo = this._getUserInfoSync();
    if (userInfo && (userInfo.avatarUrl !== this.data.userInfo.avatarUrl || userInfo.nickName !== this.data.userInfo.nickName)) {
      this.setData({
        userInfo: userInfo
      });
    }
  },

  _getUserInfoSync() {
    const cachedAvatarUrl = wx.getStorageSync('avatarUrl');
    const cachedNickName = wx.getStorageSync('nickName');
    return {
      avatarUrl: cachedAvatarUrl || DEFAULT_AVATAR.WECHAT_USER,
      nickName: cachedNickName || '微信用户'
    };
  },

  // --- UI Helpers ---
  _updateTabBarSelected() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 });
    }
  },

  /**
   * 显示提示信息
   * @deprecated 使用 showError 或 showSuccess 替代
   */
  _showToast(title, callback, icon = 'none', duration = TOAST_DURATION.NORMAL) {
    if (icon === 'success') {
      showSuccess(title, callback);
    } else {
      showError(title);
      if (callback) {
        setTimeout(callback, duration);
      }
    }
  },

  /**
   * 显示错误弹窗
   * @deprecated 使用 showError 替代
   */
  _showErrorModal(content, title = '错误') {
    showError(content);
  },

  _scrollToBottom() {
    if (this.data.scrollToMessageId) {
      // 使用 nextTick 确保DOM更新后再滚动
      wx.nextTick(() => {
        this.setData({ scrollTop: this.data.messages.length * 200 });
      });
    }
  },

  // --- Event Handlers ---
  onInputChange(e) {
    this.setData({ inputMessage: e.detail.value });
  },

  // --- Message Sending Logic ---
  sendTextMessage() {
    const { inputMessage, isLoading } = this.data;
    if (!inputMessage.trim()) {
      this._showToast('消息不能为空哦~');
      return;
    }
    if (isLoading) return;
    if (!this._ensureApiPrerequisites()) return;

    this._sendChatMessage({ text: inputMessage.trim() });
    this.setData({ inputMessage: '' });
  },

  chooseImageAndSend() {
    if (this.data.isLoading) return;
    if (!this._ensureApiPrerequisites()) return;

    wx.chooseMedia({
      count: 1,
      mediaType: ['image'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const file = res.tempFiles[0];
        if (file.size > FILE_SIZE.MAX_IMAGE_SIZE) {
          showError('图片大小超过20MB限制');
          return;
        }
        this._sendChatMessage({ text: this.data.inputMessage.trim(), imagePath: file.tempFilePath });
        this.setData({ inputMessage: '' });
      },
      fail: (err) => {
        if (err.errMsg !== "chooseMedia:fail cancel") {
          console.error("chooseImageAndSend: chooseMedia failed", err);
          showError('选择图片失败');
        }
      }
    });
  },

  _ensureApiPrerequisites() {
    // 获取最新的token
    const currentToken = this._getAuthToken();
    if (!currentToken) {
      // _getAuthToken 已经处理了登录提示，这里直接返回false
      return false;
    }

    // 更新页面的token数据
    if (this.data.token !== currentToken) {
      this.setData({ token: currentToken });
    }

    if (!api.getBaseURL()) {
      this._showErrorModal('应用配置错误(URL缺失)，无法发送消息。');
      return false;
    }
    return true;
  },

  _sendChatMessage(payload) {
    if (!payload.text && !payload.imagePath) {
      this._showToast('不能发送空内容');
      return;
    }

    this.setData({ isLoading: true });

    const userMessageContent = payload.text || "";
    const userMessageId = this._generateMessageId('user');
    this._addMessage({
      id: userMessageId,
      type: 'user',
      content: userMessageContent,
      localImagePath: payload.imagePath || null,
      uploading: !!payload.imagePath, // True if there's an image to upload
    });

    const systemMessageId = this._generateMessageId('system');
    const systemMessageIndex = this.data.messages.length;
    this._addMessage({
      id: systemMessageId,
      type: 'system',
      isLoading: true, // System message starts in loading state
    });
    this.setData({ streamingMessageIndex: systemMessageIndex });

    if (payload.imagePath) {
      // 图片上传仍然使用wx.uploadFile，因为api.request不支持文件上传
      const requestUrl = `${api.getBaseURL()}/aio/messages`;
      // 确保使用最新的token
      const currentToken = this._getAuthToken();
      if (!currentToken) {
        console.error('图片上传失败：无法获取有效token');
        this._handleApiError({errMsg: '无法获取有效token'}, '图片上传失败', systemMessageIndex, systemMessageId, userMessageId);
        return;
      }
      const headers = { 'Authorization': `Bearer ${currentToken}` };
      
      wx.uploadFile({
        url: requestUrl,
        filePath: payload.imagePath,
        name: 'files',
        header: headers,
        formData: { 'message': userMessageContent },
        dataType: 'text',
        success: (res) => {
          // 需要手动处理uploadFile的响应格式
          try {
            const responseData = JSON.parse(res.data);
            // 构造与api.request相似的响应格式
            const formattedResponse = {
              code: res.statusCode,
              data: responseData,
              message: responseData.message || responseData.msg || ''
            };
            this._handleApiResponse(formattedResponse, systemMessageIndex, systemMessageId, userMessageId);
          } catch (error) {
            console.error('解析上传响应失败:', error);
            this._handleApiError({errMsg: '解析响应失败'}, '图片上传响应格式错误', systemMessageIndex, systemMessageId, userMessageId);
          }
        },
        fail: (err) => this._handleApiError(err, '图片上传请求失败', systemMessageIndex, systemMessageId, userMessageId),
        complete: () => this._updateMessageById(userMessageId, { uploading: false })
      });
    } else {
      // 使用api.request替代wx.request
      api.request({
        endpoint: '/aio/messages',
        method: 'POST',
        header: { 'Content-Type': 'application/x-www-form-urlencoded' },
        data: { 'message': userMessageContent }
      }).then(response => {
        this._handleApiResponse(response, systemMessageIndex, systemMessageId, userMessageId);
      }).catch(err => {
        this._handleApiError(err, '消息请求失败', systemMessageIndex, systemMessageId, userMessageId);
      });
    }
  },

  // --- API Response Handling ---
  _handleApiResponse(response, messageIndex, systemMessageId, userMessageId) {
    this.setData({ isLoading: false });
    if (userMessageId) this._updateMessageById(userMessageId, { uploading: false, error: false });

    // 检查响应状态
    if (!response) {
      this._updateSystemMessage(messageIndex, systemMessageId, { content: "未收到有效响应", isError: true });
      return;
    }

    // 处理纯文本响应
    if (typeof response === 'string') {
      console.log("接收到纯文本响应，直接处理:", response.substring(0, 100) + (response.length > 100 ? '...' : ''));
      this._processSuccessfulResponse(response, messageIndex, systemMessageId);
      this.setData({ streamingMessageIndex: -1 });
      this._scrollToBottom();
      return;
    }

    // 处理直接返回的账单对象
    if (typeof response === 'object' && !response.code &&
        (this._isBillData(response) || 
         (response.name && response.totalAmount))) {
      console.log("接收到直接返回的账单对象:", response);
      this._processSuccessfulResponse(response, messageIndex, systemMessageId);
      this.setData({ streamingMessageIndex: -1 });
      this._scrollToBottom();
      return;
    }

    // 处理标准格式响应
    if (response.code === HTTP_STATUS.UNAUTHORIZED) {
      api.handleUnauthorized(response);
      return;
    } else if (response.code === HTTP_STATUS.SUCCESS) {
      this._processSuccessfulResponse(response.data, messageIndex, systemMessageId);
    } else {
      console.error("请求失败，状态码:", response.code, "响应数据:", response);
      let errorMsg = response.message || `服务器错误 (状态码: ${response.code})`;
      this._updateSystemMessage(messageIndex, systemMessageId, { content: errorMsg, isError: true });
    }
    
    this.setData({ streamingMessageIndex: -1 });
    this._scrollToBottom();
  },

  _handleApiError(err, defaultMessage, systemMessageIndex, systemMessageId, userMessageId) {
    this.setData({ isLoading: false });
    
    let errInfo = '';
    if (typeof err === 'string') {
      errInfo = err;
    } else if (err.errMsg) {
      errInfo = err.errMsg;
    } else if (err.message) {
      errInfo = err.message;
    } else {
      errInfo = JSON.stringify(err).substring(0, 100);
    }
    
    console.error(`${defaultMessage}:`, err, '错误信息:', errInfo);
    
    let displayErrorMsg = '未知错误';
    if (errInfo.includes('SyntaxError') && errInfo.includes('JSON')) {
      displayErrorMsg = '响应格式错误，请稍后再试';
    } else if (errInfo.includes('timeout')) {
      displayErrorMsg = '请求超时，请稍后再试';
    } else if (errInfo.includes('request:fail')) {
      displayErrorMsg = '网络连接失败，请检查网络';
    } else {
      displayErrorMsg = errInfo || '未知错误';
    }
    
    this._showErrorModal(`${defaultMessage}: ${displayErrorMsg}`);
    this._updateSystemMessage(systemMessageIndex, systemMessageId, { 
      content: "网络请求失败，请稍后再试", 
      isError: true,
      isLoading: false 
    });
    
    if (userMessageId) {
      this._updateMessageById(userMessageId, { 
        uploading: false, 
        error: true, 
        errorMessage: '发送失败' 
      });
    }
    
    this.setData({ streamingMessageIndex: -1 });
    this._scrollToBottom();
  },

  _processSuccessfulResponse(responseData, messageIndex, systemMessageId) {
    console.log("处理API成功响应, 类型:", typeof responseData, 
                "长度:", typeof responseData === 'string' ? responseData.length : '-',
                "预览:", typeof responseData === 'string' ? 
                       (responseData.length > 50 ? responseData.substring(0, 50) + '...' : responseData) : 
                       '非字符串');

    let parsedData;
    let isJson = false;
    
    // 处理字符串响应
    if (typeof responseData === 'string') {
      if (!responseData.trim()) {
        this._updateSystemMessage(messageIndex, systemMessageId, { 
          content: "收到空响应", 
          isError: true,
          isLoading: false,
          isText: true
        });
        return;
      }
      
      // 如果以{开头且以}结尾，可能是JSON，尝试解析
      if ((responseData.trim().startsWith('{') && responseData.trim().endsWith('}')) ||
          (responseData.trim().startsWith('[') && responseData.trim().endsWith(']'))) {
        try {
          parsedData = JSON.parse(responseData);
          isJson = true;
          console.log("成功解析JSON字符串");
        } catch (e) {
          console.log("看起来像JSON但解析失败，按纯文本处理:", e.message);
          parsedData = responseData.trim();
        }
      } else {
        // 明显不是JSON的字符串，直接作为纯文本
        parsedData = responseData.trim();
        console.log("直接按纯文本处理");
      }
    } 
    // 处理对象响应
    else if (typeof responseData === 'object' && responseData !== null) {
      parsedData = responseData;
      isJson = true;
      console.log("响应已是对象类型");
    } 
    // 处理其他类型响应
    else {
      console.error("收到意外的响应数据类型:", typeof responseData);
      this._updateSystemMessage(messageIndex, systemMessageId, { 
        content: `收到意外的回复格式: ${typeof responseData}`, 
        isError: true,
        isLoading: false,
        isText: true
      });
      return;
    }

    // 根据解析结果处理
    if (isJson && this._isBillData(parsedData)) {
      // 账单数据处理
      const billData = this._processBillDataForDisplay(parsedData);
      this._updateSystemMessage(messageIndex, systemMessageId, { billData: billData });
    } else if (isJson) {
      // 其他JSON数据，提取文本内容
      const aiTextContent = parsedData.message || parsedData.text || parsedData.answer || 
                            parsedData.response || parsedData.content || JSON.stringify(parsedData);
      this._simulateStreamingText(aiTextContent, messageIndex, systemMessageId);
    } else {
      // 纯文本响应
      this._simulateStreamingText(parsedData, messageIndex, systemMessageId);
    }
  },

  _isBillData(data) {
    if (typeof data !== 'object' || data === null) return false;
    // Check for presence of core bill fields and the new transactionType
    const billKeys = ['totalAmount', 'issueDate', 'transactionType']; // Add more if necessary
    return billKeys.every(key => data.hasOwnProperty(key)) || // All core keys present OR
           ( (data.hasOwnProperty('name') || data.hasOwnProperty('supplierName')) && data.hasOwnProperty('totalAmount')); // Common combination
  },

  /**
   * 处理账单数据用于显示
   */
  _processBillDataForDisplay(billData) {
    const processed = { ...billData };

    // 日期格式化 - 使用统一工具函数
    if (processed.issueDate) {
      processed.issueDate_display = formatDate(processed.issueDate);
    }

    // 金额格式化 - 使用统一工具函数
    ['totalAmount', 'taxAmount', 'netAmount'].forEach(key => {
      if (processed[key] !== undefined && processed[key] !== null) {
        let num = parseFloat(processed[key]);
        if (!isNaN(num)) {
          processed[key] = Math.abs(num);
          processed[`${key}_display`] = formatCurrency(processed[key]);
        } else {
          processed[`${key}_display`] = String(billData[key]);
        }
      } else if (processed[key] === null && (key === 'totalAmount' || key === 'taxAmount')) {
        processed[`${key}_display`] = formatCurrency(0);
      }
    });

    // 默认交易类型
    if (!processed.transactionType) {
      processed.transactionType = 'expense';
      console.warn("账单数据缺少交易类型，默认为'支出'", processed);
    }

    return processed;
  },

  /**
   * 模拟流式文本显示效果
   */
  _simulateStreamingText(fullText, messageIndex, messageId) {
    if (!fullText || typeof fullText !== 'string') {
      this._updateSystemMessage(messageIndex, messageId, { content: fullText || "", isLoading: false, isText: true });
      this.setData({ isLoading: false, streamingMessageIndex: -1 });
      this._scrollToBottom();
      return;
    }

    let currentText = '';
    let charIndex = 0;
    this._updateSystemMessage(messageIndex, messageId, { content: "", isLoading: true, isText: true });

    const streamInterval = setInterval(() => {
      if (charIndex < fullText.length) {
        currentText += fullText[charIndex];
        this._updateSystemMessage(messageIndex, messageId, { content: currentText, isLoading: true, isText: true });
        charIndex++;
      } else {
        clearInterval(streamInterval);
        this._updateSystemMessage(messageIndex, messageId, { content: fullText, isLoading: false, isText: true });
        this.setData({ isLoading: false, streamingMessageIndex: -1 });
        this._scrollToBottom();
      }
    }, UI_CONFIG.STREAMING_SPEED);
  },

  // --- Message Array Management ---
  _generateMessageId(type) {
    return `msg-${type}-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`;
  },

  _addMessage(msgData) {
    const defaults = {
      type: 'system', // 'user' or 'system'
      content: '',
      imageUrl: null, // URL for display after upload (if any)
      localImagePath: null, // Temporary local path for user image before upload
      uploading: false, // For user image messages
      isLoading: false, // For system messages awaiting response, or text streaming
      error: false,
      errorMessage: '',
      isBill: false,
      billData: null, // Parsed and processed bill data from LLM
      isEditing: false, // Bill form is in edit mode
      billDataEditable: null, // A deep copy of billData for editing
      originalBillData: null, // To revert changes if editing is cancelled
      alreadySaved: false, // If this bill has been saved to backend
      isText: true, // True if it's a plain text message, false for bill card
    };

    // Determine isText based on content/type
    if (msgData.type === 'user') {
      defaults.isText = !!msgData.content && !msgData.localImagePath;
    } else if (msgData.type === 'system') {
      defaults.isText = msgData.isLoading || (!!msgData.content && !msgData.billData);
    }
    
    const newMessage = { ...defaults, ...msgData };
    if (newMessage.billData) newMessage.isText = false; // If there's billData, it's not a plain text bubble

    const messages = [...this.data.messages, newMessage];
    this.setData({
      messages: messages,
      scrollToMessageId: newMessage.id
    }, () => {
      this._scrollToBottom();
    });
    return newMessage.id;
  },

  _updateMessageById(id, updates) {
    const messageIndex = this.data.messages.findIndex(msg => msg.id === id);
    if (messageIndex !== -1) {
      const updatedMessage = { ...this.data.messages[messageIndex], ...updates };
      this.setData({
        [`messages[${messageIndex}]`]: updatedMessage
      });
    } else {
      console.warn("_updateMessageById: Message with ID not found:", id);
    }
  },

  /**
   * Updates a system message, typically an AI response.
   * @param {number} index - The index of the message in the array.
   * @param {string} id - The ID of the message.
   * @param {object} data - Object containing updates: { content, billData, isLoading, isError, isText }
   */
  _updateSystemMessage(index, id, data) {
    if (index < 0 || index >= this.data.messages.length || this.data.messages[index].id !== id) {
      index = this.data.messages.findIndex(msg => msg.id === id);
      if (index === -1) {
        console.warn("_updateSystemMessage: Message not found by index or id", index, id);
        return;
      }
    }

    const currentMessage = { ...this.data.messages[index] };
    currentMessage.isLoading = data.isLoading !== undefined ? data.isLoading : false;
    currentMessage.error = data.isError || false;

    if (data.isError) {
      currentMessage.content = data.content || "发生错误";
      currentMessage.billData = null;
      currentMessage.isBill = false;
      currentMessage.isText = true;
    } else if (data.billData) {
      currentMessage.billData = data.billData; // Assumed to be processed by _processBillDataForDisplay
      currentMessage.originalBillData = JSON.parse(JSON.stringify(data.billData));
      currentMessage.billDataEditable = JSON.parse(JSON.stringify(data.billData));
      currentMessage.content = "";
      currentMessage.isBill = true;
      currentMessage.isText = false;
      currentMessage.isEditing = false;
      currentMessage.alreadySaved = false; // New bill from LLM is not saved yet
    } else { // Text content
      currentMessage.content = data.content !== undefined ? data.content : currentMessage.content;
      currentMessage.billData = null;
      currentMessage.isBill = false;
      currentMessage.isText = data.isText !== undefined ? data.isText : true;
    }
    
    this.setData({
      [`messages[${index}]`]: currentMessage
    });
  },

  // --- Image Preview ---
  previewImage(e) {
    const currentUrl = e.currentTarget.dataset.url;
    if (!currentUrl) return;

    const previewUrls = this.data.messages
      .map(msg => msg.localImagePath || msg.imageUrl)
      .filter(url => url && typeof url === 'string' && (url.startsWith('http') || url.startsWith('wxfile') || url.startsWith('blob')));
    
    // Ensure currentUrl is in the list if it's a valid previewable URL
    let finalUrls = [...new Set(previewUrls)]; // Deduplicate
    if (!finalUrls.includes(currentUrl) && (currentUrl.startsWith('http') || currentUrl.startsWith('wxfile') || currentUrl.startsWith('blob'))) {
        finalUrls.push(currentUrl);
    }
    if (finalUrls.length === 0 && currentUrl) {
        finalUrls = [currentUrl];
    }

    if (finalUrls.length > 0) {
      wx.previewImage({
        current: finalUrls.includes(currentUrl) ? currentUrl : finalUrls[0],
        urls: finalUrls
      });
    }
  },

  // --- Bill Form Handling ---
  handleBillInputChange(e) {
    const { index, field } = e.currentTarget.dataset;
    const value = e.detail.value;
    this.setData({
      [`messages[${index}].billDataEditable.${field}`]: value
    });
    // For amount fields, update display version if needed
    if (field === 'totalAmount' || field === 'taxAmount' || field === 'netAmount') {
        let num = parseFloat(value);
        if(!isNaN(num)) {
            this.setData({
                [`messages[${index}].billDataEditable.${field}_display`]: Math.abs(num).toFixed(2)
            });
        } else {
             this.setData({
                [`messages[${index}].billDataEditable.${field}_display`]: value // Or '0.00' or ''
            });
        }
    }
  },

  handleBillDateChange(e) {
    const { index } = e.currentTarget.dataset;
    const value = e.detail.value;
    this.setData({
      [`messages[${index}].billDataEditable.issueDate`]: value,
      [`messages[${index}].billDataEditable.issueDate_display`]: value,
    });
  },

  handleBillTypeChange(e) {
    const { index } = e.currentTarget.dataset;
    const value = this.data.billTypeOptions[e.detail.value];
    this.setData({
      [`messages[${index}].billDataEditable.billType`]: value,
    });
  },

  handleTransactionTypeChange(e) {
    const { index } = e.currentTarget.dataset;
    const value = this.data.transactionTypeOptions[e.detail.value].value;
    this.setData({
      [`messages[${index}].billDataEditable.transactionType`]: value,
    });
  },

  handleEditBill(e) {
    const { index } = e.currentTarget.dataset;
    // billDataEditable should already be a deep copy from originalBillData or initial billData
    this.setData({
      [`messages[${index}].isEditing`]: true
    });
  },

  handleCancelEditBill(e) {
    const { index } = e.currentTarget.dataset;
    this.setData({
      [`messages[${index}].isEditing`]: false,
      // Revert to original by deep copying again
      [`messages[${index}].billDataEditable`]: JSON.parse(JSON.stringify(this.data.messages[index].originalBillData))
    });
  },

  handleSaveChangesBill(e) { // This is for when "Save" is clicked while editing
    const { index } = e.currentTarget.dataset;
    const message = this.data.messages[index];
    const billToSave = message.billDataEditable;

    if (!this._validateBillData(billToSave)) return;
    
    // Ensure fileId is carried over if it existed on original billData
    if (!billToSave.fileId && message.originalBillData && message.originalBillData.fileId) {
        billToSave.fileId = message.originalBillData.fileId;
    }

    if (message.alreadySaved && billToSave.id) {
      this._submitBillData('PUT', billToSave, index);
    } else {
      this._submitBillData('POST', billToSave, index);
    }
  },

  handleConfirmSaveBill(e) { // This is for the initial "Confirm & Save"
    const { index } = e.currentTarget.dataset;
    const message = this.data.messages[index];
    const billToSave = message.billData; // Use the non-editable billData here

    if (!this._validateBillData(billToSave)) return;

    this._submitBillData('POST', billToSave, index);
  },

  /**
   * 验证账单数据
   */
  _validateBillData(billData) {
    const validationResult = validateBillData(billData);
    if (!validationResult.valid) {
      showError(validationResult.message);
      return false;
    }
    return true;
  },
  
  _prepareBillDtoForApi(billDataFromForm) {
    const dto = { ...billDataFromForm };
    const userId = this._getUserId();
    if (userId) {
      dto.userId = userId;
    } else {
      console.warn("Submit Bill: userId is missing. Backend might reject or misassign.");
    }

    // Ensure amounts are numbers
    dto.totalAmount = parseFloat(dto.totalAmount) || 0;
    dto.taxAmount = dto.taxAmount ? parseFloat(dto.taxAmount) : null;
    dto.netAmount = dto.netAmount ? parseFloat(dto.netAmount) : null;
    
    // Remove display-only fields and ensure correct types for id/fileId
    Object.keys(dto).forEach(key => {
      if (key.endsWith('_display')) {
        delete dto[key];
      }
      if ((key === 'id' || key === 'fileId') && dto[key] !== null && dto[key] !== undefined) {
        dto[key] = String(dto[key]); // Ensure IDs are strings if they exist
      }
    });
    
    // Clean items array if it exists (not shown in current UI, but from prompt)
    if (dto.items && Array.isArray(dto.items)) {
      dto.items = dto.items.map(item => {
        const cleanItem = { ...item };
        Object.keys(cleanItem).forEach(subKey => {
          if (subKey.endsWith('_display')) delete cleanItem[subKey];
          if (subKey === 'itemPrice') cleanItem.itemPrice = parseFloat(cleanItem.itemPrice) || 0;
        });
        return cleanItem;
      });
    }
    return dto;
  },

  /**
   * 提交账单数据到服务器
   */
  async _submitBillData(method, billData, messageIndex) {
    if (!this._ensureApiPrerequisites()) return false;

    const billDTO = this._prepareBillDtoForApi(billData);
    console.log(`提交账单数据 (${method}):`, billDTO);

    showLoading(method === 'POST' ? '保存中...' : '更新中...');

    // 根据方法构造正确的endpoint
    let endpoint;
    if (method === 'POST') {
      endpoint = '/bill';
    } else if (method === 'PUT' && billDTO.id) {
      endpoint = `/bill/${billDTO.id}`;
    } else {
      showError('更新账单时缺少ID');
      hideLoading();
      return false;
    }

    try {
      const response = await api.request({
        endpoint: endpoint,
        method: method,
        data: billDTO
      });

      hideLoading();
      console.log(`账单 ${method} 响应:`, response);

      if (response && response.code === HTTP_STATUS.SUCCESS) {
        showSuccess(method === 'POST' ? '保存成功!' : '更新成功!');
        const messages = [...this.data.messages];

        if (messages[messageIndex]) {
          let finalSavedBillData = { ...billData };

          if (response.data && typeof response.data === 'object') {
            finalSavedBillData = { ...finalSavedBillData, ...response.data };
            if (response.data.id) finalSavedBillData.id = String(response.data.id);
            if (response.data.fileId) finalSavedBillData.fileId = String(response.data.fileId);
          }

          const displayData = this._processBillDataForDisplay(finalSavedBillData);
          messages[messageIndex].billData = displayData;
          messages[messageIndex].originalBillData = JSON.parse(JSON.stringify(displayData));
          messages[messageIndex].billDataEditable = JSON.parse(JSON.stringify(displayData));
          messages[messageIndex].isEditing = false;
          messages[messageIndex].alreadySaved = true;
          messages[messageIndex].error = false;

          this.setData({ messages });
        }
        return true;
      } else {
        const errorMessage = response?.message || (method === 'POST' ? '保存失败' : '更新失败');
        showError(errorMessage);
        return false;
      }
    } catch (err) {
      hideLoading();
      console.error(`账单 ${method} 请求失败:`, err);
      showError(method === 'POST' ? '保存请求异常' : '更新请求异常');
      return false;
    }
  },
});