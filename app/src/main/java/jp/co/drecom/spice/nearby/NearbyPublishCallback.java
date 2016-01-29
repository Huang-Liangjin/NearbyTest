package jp.co.drecom.spice.nearby;

/**
 * Created by huang_liangjin on 2016/01/28.
 */
public interface NearbyPublishCallback {
    //実装しなくてもいい
    void onSucceed();
    void onFailed();
    void onExpired();
}
