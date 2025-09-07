"use strict";
const common_vendor = require("../../common/vendor.js");
const Grace6_js_grace = require("../../Grace6/js/grace.js");
require("../../stores/user.js");
const utils_utils = require("../../utils/utils.js");
const api_albums_albums = require("../../api/albums/albums.js");
const stores_player = require("../../stores/player.js");
require("../../Grace6/js/md5.js");
require("../../utils/constant.js");
require("../../config/confjg.js");
require("../../api/user/user.js");
require("../../utils/request.js");
require("../../api/order/order.js");
if (!Array) {
  const _easycom_gui_switch_navigation2 = common_vendor.resolveComponent("gui-switch-navigation");
  const _easycom_uni_icons2 = common_vendor.resolveComponent("uni-icons");
  const _easycom_CommentList2 = common_vendor.resolveComponent("CommentList");
  const _easycom_uni_popup2 = common_vendor.resolveComponent("uni-popup");
  (_easycom_gui_switch_navigation2 + _easycom_uni_icons2 + _easycom_CommentList2 + _easycom_uni_popup2)();
}
const _easycom_gui_switch_navigation = () => "../../Grace6/components/gui-switch-navigation.js";
const _easycom_uni_icons = () => "../../uni_modules/uni-icons/components/uni-icons/uni-icons.js";
const _easycom_CommentList = () => "../../components/CommentList/CommentList.js";
const _easycom_uni_popup = () => "../../uni_modules/uni-popup/components/uni-popup/uni-popup.js";
if (!Math) {
  (_easycom_gui_switch_navigation + _easycom_uni_icons + _easycom_CommentList + ZPaging + _easycom_uni_popup)();
}
const ZPaging = () => "../../uni_modules/z-paging/components/z-paging/z-paging.js";
const _sfc_main = /* @__PURE__ */ common_vendor.defineComponent({
  __name: "player",
  props: {
    albumId: {
      type: String,
      required: true
    }
  },
  setup(__props) {
    const playerStore = stores_player.usePlayerStore();
    const systemHeight = common_vendor.ref(0);
    const swiperHeight = common_vendor.computed(() => {
      return systemHeight.value - common_vendor.index.upx2px(113);
    });
    const scrollHeight = common_vendor.computed(() => {
      return systemHeight.value - common_vendor.index.upx2px(113) - common_vendor.index.upx2px(135);
    });
    const currentIndex = common_vendor.ref(0);
    const bgAudioManager = common_vendor.index.getBackgroundAudioManager();
    const isCollect = common_vendor.ref(false);
    let trackInfo = common_vendor.ref();
    let trackStaVo = common_vendor.ref();
    let album = common_vendor.ref();
    const audios = common_vendor.reactive({
      /** 音频总时长 */
      duration: "00:00",
      /** 当前进度 */
      currentTime: "00:00",
      /** 音频播放状态 */
      playStatus: false,
      /** 正在播放的音频id */
      trackId: 0,
      /** 专辑id */
      albumId: 0,
      /** 跳转进度 */
      breakSecond: 0
    });
    const audioList = common_vendor.ref([]);
    const sliders = common_vendor.reactive({
      /** 是否正在拖动进度条 */
      isDraging: false,
      /** 进度条时间 */
      progressTime: 0,
      /** 进度条总长度 */
      max: 0
    });
    const albumPopupRef = common_vendor.ref();
    const navchange = (index) => {
      currentIndex.value = index;
    };
    const openAlbumPopup = () => {
      albumPopupRef.value.open();
    };
    const closeAlbumPopup = () => {
      albumPopupRef.value.close();
    };
    const sliderChange = (e) => {
      const position = e.detail.value;
      seekAudio(position);
    };
    const handleSliderMoveStart = () => {
      sliders.isDraging = true;
    };
    const handleSliderMoveEnd = () => {
      sliders.isDraging = false;
    };
    const seekAudio = (position) => {
      bgAudioManager.seek(position*1);
      audios.currentTime = utils_utils.formatTime(position);
      sliders.progressTime = position;
    };
    const handleJump = () => {
      common_vendor.index.navigateBack();
    };
    const changeAudio = (item) => {
      if (item.trackId !== audios.trackId) {
        if (playTimer.value != 0) {
          clearInterval(playTimer.value);
          playTimer.value = 0;
        }
        common_vendor.index.redirectTo({
          url: `/pages/player/player?albumId=${album.value.id}&trackId=${item.trackId}`
        });
      }
    };
    const prevAudio = () => {
      var _a;
      const firstId = (_a = audioList.value[0]) == null ? void 0 : _a.trackId;
      if (firstId === audios.trackId) {
        common_vendor.index.showToast({
          title: "当前已经是第一首了",
          icon: "none"
        });
        return;
      }
      console.log(audios.trackId);
      let id = 0;
      audioList.value.forEach((item, index) => {
        var _a2;
        if (item.trackId === audios.trackId) {
          id = (_a2 = audioList.value[index - 1]) == null ? void 0 : _a2.trackId;
        }
      });
      if (playTimer.value != 0) {
        clearInterval(playTimer.value);
        playTimer.value = 0;
      }
      common_vendor.index.redirectTo({
        url: `/pages/player/player?albumId=${album.value.id}&trackId=${id}`
      });
    };
    const nextAudio = () => {
      var _a;
      const len = audioList.value.length;
      const lastId = (_a = audioList.value[len - 1]) == null ? void 0 : _a.trackId;
      if (lastId === audios.trackId) {
        common_vendor.index.showToast({
          title: "当前播放列表已是最新的了，请加载更多",
          icon: "none"
        });
        return;
      }
      console.log(audios.trackId);
      let id = 0;
      audioList.value.forEach((item, index) => {
        var _a2;
        if (item.trackId === audios.trackId) {
          id = (_a2 = audioList.value[index + 1]) == null ? void 0 : _a2.trackId;
        }
      });
      if (playTimer.value != 0) {
        clearInterval(playTimer.value);
        playTimer.value = 0;
      }
      common_vendor.index.redirectTo({
        url: `/pages/player/player?albumId=${album.value.id}&trackId=${id}`
      });
    };
    const createBgAudioManager = () => {
      var _a, _b, _c;
      if (bgAudioManager) {
        if (!bgAudioManager.paused) {
          bgAudioManager.stop();
        }
        bgAudioManager.title = (_a = trackInfo.value) == null ? void 0 : _a.trackTitle;
        bgAudioManager.coverImgUrl = (_b = trackInfo.value) == null ? void 0 : _b.coverUrl;
        bgAudioManager.src = (_c = trackInfo.value) == null ? void 0 : _c.mediaUrl;
        initAudio(bgAudioManager);
      }
    };
    const pauseAudio = () => {
      bgAudioManager.paused = true;
      bgAudioManager.pause();
    };
    const playAudio = () => {
      getBreakSecond();
      setTimeout(() => {
        bgAudioManager.paused = false;
        bgAudioManager.play();
      }, 500);
    };
    let playTimer = common_vendor.ref(0);
    const initAudio = (ctx) => {
      ctx.onTimeUpdate(() => {
        if (!sliders.isDraging) {
          const currentTime = ctx.currentTime;
          if (currentTime) {
            sliders.progressTime = ctx.currentTime;
            audios.currentTime = utils_utils.formatTime(currentTime);
          }
        }
      });
      ctx.onCanplay(() => {
        setTimeout(() => {
          console.log("音频长度", bgAudioManager.duration);
          const duration = bgAudioManager.duration;
          audios.duration = utils_utils.formatTime(duration);
          sliders.max = duration;
        }, 300);
      });
      ctx.onPlay(() => {
        audios.playStatus = true;
        playerStore.changePlayStatus(true);
        seekAudio(audios.breakSecond);
        console.log("播放循环：------------------");
        playTimer.value = setInterval(async () => {
          console.log("播放循环1：", audios.trackId, "playTimer", playTimer.value);
          if (playTimer.value != 0) {
            const params = {
              albumId: audios.albumId,
              trackId: audios.trackId,
              breakSecond: sliders.progressTime
            };
            if (bgAudioManager.paused)
              return;
            await api_albums_albums.albumsService.updateListenProcess(params);
            console.log("播放循环2：", audios.trackId, "playTimer", playTimer.value);
          }
        }, 3e3);
      });
      ctx.onPause(() => {
        audios.playStatus = false;
        playerStore.changePlayStatus(false);
      });
      ctx.onEnded(() => {
        nextAudio();
      });
      ctx.onError(() => {
        audios.playStatus = false;
        playerStore.changePlayStatus(false);
      });
    };
    const handleComment = () => {
      currentIndex.value = 1;
    };
    const getIsCollect = async () => {
      const res = await api_albums_albums.albumsService.isCollectTrack(audios.trackId);
      isCollect.value = res.data;
    };
    const handleCollect = async () => {
      const res = await api_albums_albums.albumsService.collectTrack(audios.trackId);
      isCollect.value = res.data;
      common_vendor.index.showToast({
        title: !res.data ? "取消收藏成功" : "收藏成功",
        icon: "none"
      });
    };
    const getTrackInfo = async (trackId) => {
      var _a;
      try {
        const res = await api_albums_albums.albumsService.getTrackInfo(trackId);
        trackInfo.value = res.data;
        audios.trackId = (_a = res.data) == null ? void 0 : _a.id;
        createBgAudioManager();
      } catch (error) {
        console.log(error);
      }
    };
    const getTrackStatVo = async () => {
      try {
        const res = await api_albums_albums.albumsService.getTrackStaVo(audios.trackId);
        trackStaVo.value = res.data;
      } catch (error) {
        console.log(error);
      }
    };
    const zPagingRef = common_vendor.ref();
    const getAblumAudioList = async (page, limit) => {
      const params = {
        page,
        limit,
        albumId: audios.albumId
      };
      if (audios.albumId != 0) {
        const res = await api_albums_albums.albumsService.getAlbumTrackList(params);
        zPagingRef.value.complete(res.data.records);
      }
    };
    const getAlbumDetail = async (id) => {
      const res = await api_albums_albums.albumsService.getAlbumInfo(id);
      album.value = res.data;
    };
    const getBreakSecond = async () => {
      const res = await api_albums_albums.albumsService.getTrackBreakSecond(audios.trackId);
      audios.breakSecond = res.data;
    };
    common_vendor.onLoad(async (options) => {
      console.log("options", options);
      if (JSON.stringify(options) !== "{}") {
        audios.trackId = options.trackId;
        audios.albumId = options.albumId;
        console.log("初始化1：", options);
      } else {
        const { data } = await api_albums_albums.albumsService.getLatelyTrack();
        console.log("初始化2：", data.trackId);
        audios.trackId = data.trackId;
        audios.albumId = data.albumId;
      }
      getBreakSecond();
      getAlbumDetail(audios.albumId);
      getTrackInfo(audios.trackId);
      getAblumAudioList(1, 10);
      getTrackStatVo();
      getIsCollect();
    });
    common_vendor.onMounted(() => {
      var systemInfo = Grace6_js_grace.graceJS.system();
      systemHeight.value = systemInfo.safeArea.height;
      common_vendor.onBeforeUnmount(() => {
        if (playTimer.value != 0) {
          clearInterval(playTimer.value);
          playTimer.value = 0;
        }
      });
    });
    return (_ctx, _cache) => {
      var _a, _b, _c, _d, _e, _f, _g, _h;
      return common_vendor.e({
        a: common_vendor.o(handleJump),
        b: common_vendor.o(navchange),
        c: common_vendor.p({
          activeLineClass: ["gui-nav-scale", "gui-bg-white"],
          titleClass: ["gui-color-white"],
          items: [{
            id: 0,
            name: "播放"
          }, {
            id: 1,
            name: "评论"
          }],
          activeLineWidth: "50rpx",
          size: 100,
          width: 200,
          currentIndex: currentIndex.value
        }),
        d: common_vendor.t((_a = common_vendor.unref(trackStaVo)) == null ? void 0 : _a.playStatNum),
        e: (_b = common_vendor.unref(trackInfo)) == null ? void 0 : _b.coverUrl,
        f: common_vendor.t((_c = common_vendor.unref(trackInfo)) == null ? void 0 : _c.trackTitle),
        g: common_vendor.t(audios.currentTime),
        h: sliders.max,
        i: sliders.progressTime,
        j: common_vendor.o(sliderChange),
        k: common_vendor.o(
          //@ts-ignore
          (...args) => handleSliderMoveStart && handleSliderMoveStart(...args)
        ),
        l: common_vendor.o(handleSliderMoveEnd),
        m: common_vendor.t(audios.duration),
        n: common_vendor.o(openAlbumPopup),
        o: common_vendor.o(prevAudio),
        p: audios.playStatus
      }, audios.playStatus ? {
        q: common_vendor.o(pauseAudio),
        r: common_vendor.p({
          ["custom-prefix"]: "iconfont",
          type: "pause",
          size: "80rpx",
          color: "#fff"
        })
      } : {
        s: common_vendor.o(playAudio)
      }, {
        t: common_vendor.o(nextAudio),
        v: (_d = common_vendor.unref(album)) == null ? void 0 : _d.coverUrl,
        w: common_vendor.t((_e = common_vendor.unref(album)) == null ? void 0 : _e.albumTitle),
        x: common_vendor.t((_f = common_vendor.unref(trackStaVo)) == null ? void 0 : _f.collectStatNum),
        y: common_vendor.unref(scrollHeight) + "px",
        z: common_vendor.o(handleComment),
        A: isCollect.value
      }, isCollect.value ? {} : {}, {
        B: common_vendor.t(((_g = common_vendor.unref(trackStaVo)) == null ? void 0 : _g.collectStatNum) || 0),
        C: common_vendor.o(handleCollect),
        D: common_vendor.t(((_h = common_vendor.unref(trackStaVo)) == null ? void 0 : _h.commentStatNum) || 0),
        E: common_vendor.o(handleComment),
        F: common_vendor.unref(scrollHeight) + "px",
        G: common_vendor.p({
          albumId: __props.albumId,
          trackId: audios.trackId
        }),
        H: currentIndex.value,
        I: common_vendor.unref(swiperHeight) + "px",
        J: common_vendor.o(closeAlbumPopup),
        K: common_vendor.f(audioList.value, (item, k0, i0) => {
          return common_vendor.e({
            a: common_vendor.t(item.orderNum),
            b: common_vendor.n((item == null ? void 0 : item.trackId) == audios.trackId ? "track-item-title-checked" : "gui-color-grey1"),
            c: common_vendor.t(item.trackTitle),
            d: common_vendor.n((item == null ? void 0 : item.trackId) == audios.trackId ? "track-item-title-checked" : "gui-primary-text "),
            e: (item == null ? void 0 : item.trackId) == audios.trackId
          }, (item == null ? void 0 : item.trackId) == audios.trackId ? {} : {}, {
            f: common_vendor.t(item.playStatNum),
            g: common_vendor.t(item == null ? void 0 : item.albumCommentStatNum),
            h: common_vendor.t(common_vendor.unref(utils_utils.formatTime)(item.mediaDuration)),
            i: item.trackId,
            j: common_vendor.o(($event) => changeAudio(item), item.trackId)
          });
        }),
        L: common_vendor.sr(zPagingRef, "6ae8dc9e-4,6ae8dc9e-3", {
          "k": "zPagingRef"
        }),
        M: common_vendor.o(getAblumAudioList),
        N: common_vendor.o(($event) => audioList.value = $event),
        O: common_vendor.p({
          ["paging-style"]: {
            height: "500px"
          },
          fixed: false,
          modelValue: audioList.value
        }),
        P: common_vendor.sr(albumPopupRef, "6ae8dc9e-3", {
          "k": "albumPopupRef"
        }),
        Q: common_vendor.p({
          safeArea: true,
          type: "bottom",
          backgroundColor: "#fff"
        })
      });
    };
  }
});
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["__file", "D:/workspace/vsworkspace/ts/base/ListenToBooks/ts-front/pages/player/player.vue"]]);
wx.createPage(MiniProgramPage);
