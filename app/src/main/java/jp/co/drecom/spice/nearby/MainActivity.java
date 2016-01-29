package jp.co.drecom.spice.nearby;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();

    private class NearbyPublishCallbackImp implements NearbyPublishCallback {

        @Override
        public void onSucceed() {

        }

        @Override
        public void onFailed() {

        }

        @Override
        public void onExpired() {

        }
    }

    private class NearbySubscribeCallbackImp implements NearbySubscribeCallback {

        @Override
        public void onGet(byte[] content) {
            String contents = new String(content);
            Log.d(TAG, "received message is " + contents);
        }

        @Override
        public void onLost() {

        }

        @Override
        public void onExpired() {

        }

        @Override
        public void onFailed() {

        }
    }

    private NearbyPublishCallbackImp publishCallbackImp = new NearbyPublishCallbackImp();
    private NearbySubscribeCallbackImp subscribeCallbackImp = new NearbySubscribeCallbackImp();
    private ArrayList<String> types = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnClick(View v) {
        switch (v.getId()) {
            case R.id.btn_nearby_init:
                NearbyMessage.setup(this);
                NearbyMessage.setPublishCallback(publishCallbackImp);
                NearbyMessage.setSubscribeCallback(subscribeCallbackImp);
                break;
            case R.id.btn_publish_msg1:
                NearbyMessage.publish(new String("message1").getBytes());
                break;
            case R.id.btn_publish_msg1_type1:
                NearbyMessage.publish(new String("message1").getBytes(), "type1");
                break;
            case R.id.btn_publish_msg2:
                NearbyMessage.publish(new String("message2").getBytes());
                break;
            case R.id.btn_publish_msg2_type2:
                NearbyMessage.publish(new String("message2").getBytes(), "type2");
                break;
            case R.id.btn_publish_msg3:
                NearbyMessage.publish(new String("message3").getBytes());
                break;
            case R.id.btn_subscribe:
                NearbyMessage.subscribe();
                break;
            case R.id.btn_subscribe_type1:
                types.clear();
                types.add("type1");
                NearbyMessage.subscribe(types);
                break;
            case R.id.btn_subscribe_type12:
                types.clear();
                types.add("type1");
                types.add("type2");
                NearbyMessage.subscribe(types);
                break;
            case R.id.btn_unsubscribe:
                NearbyMessage.unsubscribe();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NearbyMessage.close();
    }
}
