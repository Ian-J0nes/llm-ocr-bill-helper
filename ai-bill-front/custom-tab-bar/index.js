Component({
    data: {
      selected: 0,
      list: [
        {
          pagePath: "/pages/index/index",
          text: "首页",
          id: "index"
        },
        {
          pagePath: "/pages/chat/chat",
          text: "记账",
          id: "chat"
        },
        {
          pagePath: "/pages/myset/myset",
          text: "我的",
          id: "myset"
        }
      ]
    },
    methods: {
      switchTab(e) {
        const data = e.currentTarget.dataset;
        const url = data.path;
        wx.switchTab({ url });
      }
    }
  });