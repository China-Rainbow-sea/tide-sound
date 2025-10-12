package com.rainbowsea.tidesound.live.utils;

import com.rainbowsea.tidesound.live.config.LiveProperties;
import com.rainbowsea.tidesound.vo.live.TencentLiveAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class LiveAddressUtil {

    @Autowired
    private LiveProperties liveProperties;

    /**
     * 根据直播间在腾讯云中的直播间名称 和 过期时间 生成推流和拉流地址（WebRTC）
     * @param streamName
     * @param txTime
     * @return
     */
    public TencentLiveAddressVo getWebRTCLiveAddress(String streamName, Long txTime) {

        String safeUrl = getSafeUrl(liveProperties.getPushKey(), streamName, txTime);

        String pushUrl = "webrtc://" + liveProperties.getPushDomain() + "/" + liveProperties.getAppName() + "/" + streamName + "?" + safeUrl;
        String pullUrl = "webrtc://" + liveProperties.getPullDomain() +"/" + liveProperties.getAppName() + "/" + streamName + "?" + safeUrl;

        TencentLiveAddressVo addressVo = new TencentLiveAddressVo();
        addressVo.setPushWebRtcUrl(pushUrl);
        addressVo.setPullWebRtcUrl(pullUrl);
        return addressVo;
    }

    private static final char[] DIGITS_LOWER =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /*
     * KEY+ streamName + txTime
     */
    private static String getSafeUrl(String key, String streamName, long txTime) {
        String input = new StringBuilder().
                append(key).
                append(streamName).
                append(Long.toHexString(txTime).toUpperCase()).toString();

        String txSecret = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            txSecret  = byteArrayToHexString(
                    messageDigest.digest(input.getBytes("UTF-8")));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return txSecret == null ? "" :
                new StringBuilder().
                        append("txSecret=").
                        append(txSecret).
                        append("&").
                        append("txTime=").
                        append(Long.toHexString(txTime).toUpperCase()).
                        toString();
    }

    private static String byteArrayToHexString(byte[] data) {
        char[] out = new char[data.length << 1];

        for (int i = 0, j = 0; i < data.length; i++) {
            out[j++] = DIGITS_LOWER[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS_LOWER[0x0F & data[i]];
        }
        return new String(out);
    }
}
