<template>
	<view class="pay-result">
		<view class="pay-status">
			<t-icon name="check-circle-filled" size="60rpx" color="#47D368" />
			<text>订单支付测试</text>
		</view>
		<view style="width: 95%;font-size: 28rpx;color: #333333;margin-top: 5px; margin-bottom: 10px;">
			说明：<br/>
			微信小程序支付，需加入微信开发者，才可以测试支付；由于学员未加入开发者，故不能测试微信小程序支付；<br/>
			微信Native支付无限制，二者只是微信下单方式不一样，支付回调接口一致，因此未加入微信开发者人员，可在此测试微信支付。<br/>
		</view>
		<view class="pay-order" style="width: 95%;">
			<uni-easyinput type="text" v-model="orderNo" placeholder="请输入测试订单号" width="100%"/>
			<button type="text" style="margin-left: 10px;font-size: 14px;" @click="fun_createNative">生成支付</button>
		</view>
		<view class="qr-box">
		     <canvas canvas-id="qrcode" style="width: 340rpx;height: 340rpx;;margin: 0 auto;" />
		</view>
		<view class="btn-wrapper">
			<view class="status-btn" data-type="orderList" @click="onTapReturn(1)">订单详情</view>
			<view class="status-btn" data-type="home" @click="onTapReturn(2)">订单列表</view>
		</view>
	</view>
</template>
<script>
    import uQRCode from '@/common/uqrcode.js' //引入uqrcode.js
	import { order } from "../../api"
	import { ref } from 'vue';
	
    export default {
        data() {
            return {
				orderNo: ''
            }
        },
        onLoad(options) {
            //this.qrCode('66')
        }, 
        methods: {
          　　qrCode(url) {
                this.qrShow = true
                uQRCode.make({
                    canvasId: 'qrcode',
                    componentInstance: this,
                    text: url,
                    size: 170,
                    margin: 0,
                    backgroundColor: '#ffffff',
                    foregroundColor: '#000000',
                    fileType: 'jpg',
                    errorCorrectLevel: uQRCode.errorCorrectLevel.H,
                    success: res => {}
                })
            },
			
			async fun_createNative() {
				if(this.orderNo === '') {
					uni.showToast({
						title: '请输入测试订单号' 
					})
					return;
				}
				
				let res = await order.createNative('1301', this.orderNo)
				console.log(res)
				this.qrCode(res.data.codeUrl);
				
				this.paySuccess()
			},
			
			// 查询订单支付状态
			async paySuccess() {
			  try {
				console.log("轮询查询订单支付状态---start");
				const res = await order.queryOrderPayStatus(this.orderNo);
				if (res.data) {
				  uni.navigateTo({
				  	url: `/pages/orderDetail/orderDetail?orderNo=` + this.orderNo,
				  });
				} else {
				  console.log("查询支付信息失败，继续查询-----------");
				  setTimeout(() => {
				    this.paySuccess();
				  }, 2000);
				}
			  } catch (error) {
				console.log(error);
			  }
			},
			
			onTapReturn (mode)  {
				console.log(mode);
				if (mode === 1) {
					if(this.orderNo === '') {
						uni.showToast({
							title: '请输入测试订单号' 
						})
						return;
					}
					uni.navigateTo({
						url: `/pages/orderDetail/orderDetail?orderNo=` + this.orderNo,
					});
				} else {
					uni.navigateTo({
						url: `/pages/order/order`
					});
				}
			}
        }
    }
</script>

<style lang="scss" scoped>
.qr-box {
        width: 400rpx;
        height: 460rpx;
        margin: 0 auto;
        margin-top: 20rpx;
    }
.pay-result {
	height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
	justify-content: center;
  width: 100%;
}

.pay-result .pay-status {
  margin-top: 30rpx;
  margin-bottom: 30rpx;
  font-size: 48rpx;
  line-height: 72rpx;
  font-weight: bold;
  color: #333333;
  display: flex;
  align-items: center;
}
.pay-result .pay-order {
  margin-top: 20rpx;
  font-size: 20rpx;
  line-height: 72rpx;
  // font-weight: bold;
  color: #333333;
  display: flex;
  align-items: flex-start;
}
.pay-result .pay-status text {
  padding-left: 12rpx;
}
.pay-result .pay-money {
  color: #666666;
  font-size: 28rpx;
  line-height: 48rpx;
  margin-top: 28rpx;
  display: flex;
  align-items: baseline;
}

.pay-result .pay-money .pay-money__price {
  font-size: 36rpx;
  line-height: 48rpx;
  color: #fa4126;
}
.pay-result .btn-wrapper {
  margin-top: 200rpx;
  padding: 12rpx 32rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  box-sizing: border-box;

}

.pay-result .btn-wrapper .status-btn {
  height: 88rpx;
  width: 334rpx;
  border-radius: 44rpx;
  border: 2rpx solid #fa4126;
  color: #fa4126;
  font-size: 28rpx;
  font-weight: bold;
  line-height: 88rpx;
  text-align: center;
}
</style>
