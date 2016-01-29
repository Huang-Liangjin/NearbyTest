package jp.co.drecom.spice.nearby;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.PublishCallback;
import com.google.android.gms.nearby.messages.PublishOptions;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

import java.util.ArrayList;

public class NearbyMessage {
    public static final String TAG = NearbyMessage.class.getSimpleName();
    private static final int PUBLISH_RETRY_TIMES = 3;

    //*********************SINGLETON*********************//
    private NearbyMessage(){}
    private static NearbyMessage ourInstance = new NearbyMessage();
    private static NearbyMessage getInstance() {
        return ourInstance;
    }
    //*********************SINGLETON********************//

    // 最初はApplicationContextにするつもりだったが、
    // applicationContextだと、実行時に
    // attempting to perform a high-power operation from a non-activity context
    // というエラーメッセージが表示されて、publish/subscribeできなくなります。
    private Context mContext = null;

    //Nearbyクラスはsingletonなので、内部クラスをstaticにする必要がない
    public class NearbyApiConnectionListener implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "google api client connected");
            checkNearbyPermission();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "google api client suspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "google api client connected");
        }
    }

    private NearbyApiConnectionListener mApiConnectionListener =
            new NearbyApiConnectionListener();
    private NearbyPublishCallback mPublishCallback = null;
    private NearbySubscribeCallback mSubscribeCallback = null;

    private GoogleApiClient mGoogleApiClient = null;

    private int mPublishRetryTimes = PUBLISH_RETRY_TIMES;
    //publishのTTLは、30秒にします。
    private int mPublishTTL = 30;
    //subscriberのTTLは、default 5minに設定します。
    private int mSubscribeTTL = Strategy.TTL_SECONDS_DEFAULT;

    //for subscribe
    private MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onFound(Message message) {
            if (mSubscribeCallback != null) {
                Log.d(TAG, "message namespace type is " + message.getNamespace() + " " + message.getType());
                mSubscribeCallback.onGet(message.getContent());
            }
        }

        @Override
        public void onLost(Message message) {
            super.onLost(message);
            if (mSubscribeCallback != null) {
                mSubscribeCallback.onLost();
            }
        }
    };


    //***********************************************************//
    //**************************API******************************//
    static public void setPublishCallback(NearbyPublishCallback nearbyPublishCallback) {
        getInstance().setPublishCallbackInternal(nearbyPublishCallback);
    }
    static public void setSubscribeCallback(NearbySubscribeCallback nearbySubscribeCallback) {
        getInstance().setSubscribeCallbackInternal(nearbySubscribeCallback);
    }
    static public void setPublishTTL(int messageTimeToLive) {
        getInstance().setPublishTTLInternal(messageTimeToLive);
    }
    static public void setSubscribeTTL(int messageTimeToLive) {
        getInstance().setSubscribeTTLInternal(messageTimeToLive);
    }
    public static void setPublishRetryTimes(int retryTimes) {
        getInstance().setPublishRetryTimesInternal(retryTimes);
    }
    public static void setup(Activity context) {
        getInstance().setupInternal(context);
    }
    public static void close() {
        //TODO set all to null;
        getInstance().closeInternal();
    }
    public static void publish(byte[] message) {
        getInstance().publishInternal(message, null);
    }
    public static void publish(byte[] message, String type) {
        getInstance().publishInternal(message, type);
    }
    public static void subscribe() {
        getInstance().subscribeInternal(null);
    }
    public static void subscribe(ArrayList<String> types) {
        getInstance().subscribeInternal(types);
    }
    public static void unsubscribe() {
        getInstance().unsubscribeInternal();
    }
    //**************************API******************************//
    //***********************************************************//

    //実際試したところ、discoveryModeが要らないほうがコントロールしやすい
    //http://etherpad.drecom.dc/p/spice-nearby#lineNumber=55
    //NOTICE
    //ここのパラメータはActivityにしちゃいます。
    //Activity以外のcontextの場合、(例えばApplicationContext)、
    // "attempting to perform a high-power operation from a non-activity context"
    //というエラーになるので、Activity限定で。
    //TODO mContextの解放が忘れずに。
    private void setupInternal(Activity context) {
        if (mGoogleApiClient == null) {
            mContext = context;
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(com.google.android.gms.nearby.Nearby.MESSAGES_API)
                    .addConnectionCallbacks(mApiConnectionListener)
                    .addOnConnectionFailedListener(mApiConnectionListener)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    private void setPublishCallbackInternal(NearbyPublishCallback nearbyPublishCallback) {
        mPublishCallback = nearbyPublishCallback;
    }

    private void setSubscribeCallbackInternal(NearbySubscribeCallback nearbySubscribeCallback) {
        mSubscribeCallback = nearbySubscribeCallback;
    }

    private void setPublishTTLInternal(int messageTimeToLive) {
        mPublishTTL = messageTimeToLive;
    }

    private void setSubscribeTTLInternal(int messageTimeToLive) {
        mSubscribeTTL = messageTimeToLive;
    }

    //retryはまだ
    private void setPublishRetryTimesInternal(int retryTimes) {
        mPublishRetryTimes = retryTimes;
    }

    //check permissionは、内部で使う予定
    //毎回、setupする時に、チェックします
    private void checkNearbyPermission() {
        if (mContext != null) {
            Intent intent = new Intent(mContext, NearbyPermissionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
        }
    }

    private GoogleApiClient getNearbyGoogleApiClientInternal() {
        return mGoogleApiClient;
    }

    public static GoogleApiClient getNearbyGoogleApiClient() {
        return getInstance().getNearbyGoogleApiClientInternal();
    }

    private void closeInternal() {
        //TODO
        //unPublish
        //unSubscribe
        //set local variable to null
        mContext = null;
        mGoogleApiClient = null;
        mPublishCallback = null;
        mSubscribeCallback = null;
    }

    private void publishInternal(final byte[] content, final String type) {
        //TODO callback、strategy objectはdefaultの物を使う
//        int publishRetry = mPublishRetryTimes;
        if (mGoogleApiClient == null) {
            if (mPublishCallback != null) {
                mPublishCallback.onFailed();
            }
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            Message message;
            if (type == null) {
                message = new Message(content);
            } else {
                message = new Message(content, type);
            }
            PublishOptions publishOptions = new PublishOptions.Builder()
                    .setStrategy(new Strategy.Builder()
                            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
                            .setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT)
                            .setTtlSeconds(mPublishTTL)
                            .build())
                    .setCallback(new PublishCallback() {
                        @Override
                        public void onExpired() {
                            super.onExpired();
                            Log.d(TAG, new String(content).toString() + " is expired");
                            if (mPublishCallback != null) {
                                mPublishCallback.onExpired();
                            }
                        }
                    })
                    .build();
            Nearby.Messages.publish(mGoogleApiClient, message, publishOptions)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Published successfully.");
//                                mPublishRetryTimes = 0;
                                if (mPublishCallback != null) {
                                    mPublishCallback.onSucceed();
                                }
                            } else {
                                Log.i(TAG, "Could not publish.");
                                // Check whether consent was given;
                                // if not, prompt the user for consent.
                                //TODO retry
                                checkNearbyPermission();
//                                while (mPublishRetryTimes-- > 0) {
//                                    publishInternal(content, type);
//                                }
                            }
                        }
                    });
        }
    }

    // unpublish:
    // ここは、publishと同じMessageオブジェクトにする必要があります。
    // TODO C++利用するときにどうするのか問題です
    // 一応、unpublish提供しなくても、publishのTTL時間満たしたら自動的にunpublishします。
    // activityがバックブランドになるときも、自動的にunpublishしてくれます。
    private void unpublishInternal() {

    }

    static public void unpublish() {

    }

    private MessageFilter makeMessageFilter(ArrayList<String> types) {
        MessageFilter messageFilter = null;
        if (types != null && !types.isEmpty()) {
            MessageFilter.Builder messageFilterBuilder = new MessageFilter.Builder();
            for (String type: types) {
                //CONFIRM
                messageFilterBuilder.includeNamespacedType("", type);
            }
            messageFilter = messageFilterBuilder.build();
        }
        return messageFilter;
//        return new MessageFilter.Builder().includeNamespacedType("spice", "type1").build();
    }

    private void subscribeInternal(ArrayList<String> types) {
        if (mGoogleApiClient == null) {
            if (mSubscribeCallback != null) {
                mSubscribeCallback.onFailed();
            }
            return;
        }
        if (!mGoogleApiClient.isConnected()) {
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        } else {
            SubscribeOptions.Builder subscribeOptionBuilder = new SubscribeOptions.Builder()
                    .setStrategy(new Strategy.Builder()
                            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
                            .setDistanceType(Strategy.DISTANCE_TYPE_EARSHOT)
                            .setTtlSeconds(mSubscribeTTL)
                            .build())
                    .setCallback(new SubscribeCallback() {
                        @Override
                        public void onExpired() {
                            Log.i(TAG, "subscribe is expired");
                            if (mSubscribeCallback != null) {
                                mSubscribeCallback.onExpired();
                            }
                        }
                    });
            MessageFilter messageFilter = makeMessageFilter(types);
            if (messageFilter != null) {
                subscribeOptionBuilder.setFilter(messageFilter);
            }
            SubscribeOptions options = subscribeOptionBuilder.build();
            Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Subscribed successfully.");
                            } else {
                                Log.i(TAG, "Could not subscribe.");
                                // Check whether consent was given;
                                // if not, prompt the user for consent.
                                checkNearbyPermission();
                            }
                        }
                    });
        }
    }

    private void unsubscribeInternal() {
        Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
    }

}
