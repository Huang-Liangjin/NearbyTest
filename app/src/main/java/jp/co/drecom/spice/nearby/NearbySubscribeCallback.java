package jp.co.drecom.spice.nearby;

/**
 * Created by huang_liangjin on 2016/01/28.
 */
public interface NearbySubscribeCallback {
    void onGet(byte[] content);
    void onLost();
    void onExpired();
    void onFailed();
}
