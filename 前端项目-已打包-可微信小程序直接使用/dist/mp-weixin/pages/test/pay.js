"use strict";
const common_vendor = require("../../common/vendor.js");
const common_uqrcode = require("../../common/uqrcode.js");
require("../../stores/user.js");
const api_order_order = require("../../api/order/order.js");
require("../../utils/constant.js");
require("../../config/confjg.js");
require("../../utils/utils.js");
require("../../api/user/user.js");
require("../../utils/request.js");
const _sfc_main = {
  data() {
    return {
      orderNo: ""
    };
  },
  onLoad(options) {
  },
  methods: {
    qrCode(url) {
      this.qrShow = true;
      common_uqrcode.uQRCode.make({
        canvasId: "qrcode",
        componentInstance: this,
        text: url,
        size: 170,
        margin: 0,
        backgroundColor: "#ffffff",
        foregroundColor: "#000000",
        fileType: "jpg",
        errorCorrectLevel: common_uqrcode.uQRCode.errorCorrectLevel.H,
        success: (res) => {
        }
      });
    },
    async fun_createNative() {
      if (this.orderNo === "") {
        common_vendor.index.showToast({
          title: "请输入测试订单号"
        });
        return;
      }
      let res = await api_order_order.order.createNative("1301", this.orderNo);
      console.log(res);
      this.qrCode(res.data.codeUrl);
      this.paySuccess();
    },
    // 查询订单支付状态
    async paySuccess() {
      try {
        console.log("轮询查询订单支付状态---start");
        const res = await api_order_order.order.queryOrderPayStatus(this.orderNo);
        if (res.data) {
          common_vendor.index.navigateTo({
            url: `/pages/orderDetail/orderDetail?orderNo=` + this.orderNo
          });
        } else {
          console.log("查询支付信息失败，继续查询-----------");
          setTimeout(() => {
            this.paySuccess();
          }, 2e3);
        }
      } catch (error) {
        console.log(error);
      }
    },
    onTapReturn(mode) {
      console.log(mode);
      if (mode === 1) {
        if (this.orderNo === "") {
          common_vendor.index.showToast({
            title: "请输入测试订单号"
          });
          return;
        }
        common_vendor.index.navigateTo({
          url: `/pages/orderDetail/orderDetail?orderNo=` + this.orderNo
        });
      } else {
        common_vendor.index.navigateTo({
          url: `/pages/order/order`
        });
      }
    }
  }
};
if (!Array) {
  const _component_t_icon = common_vendor.resolveComponent("t-icon");
  const _easycom_uni_easyinput2 = common_vendor.resolveComponent("uni-easyinput");
  (_component_t_icon + _easycom_uni_easyinput2)();
}
const _easycom_uni_easyinput = () => "../../uni_modules/uni-easyinput/components/uni-easyinput/uni-easyinput.js";
if (!Math) {
  _easycom_uni_easyinput();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.p({
      name: "check-circle-filled",
      size: "60rpx",
      color: "#47D368"
    }),
    b: common_vendor.o(($event) => $data.orderNo = $event),
    c: common_vendor.p({
      type: "text",
      placeholder: "请输入测试订单号",
      width: "100%",
      modelValue: $data.orderNo
    }),
    d: common_vendor.o((...args) => $options.fun_createNative && $options.fun_createNative(...args)),
    e: common_vendor.o(($event) => $options.onTapReturn(1)),
    f: common_vendor.o(($event) => $options.onTapReturn(2))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-dbd3e5f3"], ["__file", "D:/workspace/vsworkspace/ts/base/ListenToBooks/ts-front/pages/test/pay.vue"]]);
wx.createPage(MiniProgramPage);
