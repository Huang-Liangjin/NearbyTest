package jp.co.drecom.spice.nearby;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;

import java.lang.ref.WeakReference;

public class NearbyPermissionActivity extends Activity {
    public static final String TAG = NearbyPermissionActivity.class.getSimpleName();

    private NearbyPermissionCallback mPermissionCallback = new NearbyPermissionCallback(this);

    public static final int NEARBY_PERMISSION_REQUEST = 1001;

    private static final String STATE_DIALOG_FLAG = "PermissionDialogFlag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        // 全画面表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 画面回転を禁止する
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        super.onCreate(savedInstanceState);
        //重複ダイアログフラグの読み込み（もしActivity再建された場合）
        if (savedInstanceState != null) {
            mPermissionCallback.mResolvingNearbyPermissionError = savedInstanceState.getBoolean(STATE_DIALOG_FLAG);
        }

        GoogleApiClient client = NearbyMessage.getNearbyGoogleApiClient();
        if (client != null) {
            Nearby.Messages.getPermissionStatus(client).setResultCallback(mPermissionCallback);
        } else {
            finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //flag保存
        outState.putBoolean(STATE_DIALOG_FLAG, mPermissionCallback.mResolvingNearbyPermissionError);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult " + resultCode);
        mPermissionCallback.finishedResolvingNearbyPermissionError();
        if (requestCode == NEARBY_PERMISSION_REQUEST) {
            switch (resultCode) {
                case Activity.RESULT_OK:
                    //TODO add init callback to notice the permission is approved
                    Log.d(TAG, "permission approved");
                    break;
                case Activity.RESULT_CANCELED:
                    //TODO add init callback to notice the permission is denied
                    Log.d(TAG, "permission denied");
                    break;
                default:
                    Log.d(TAG, "permission something else");
                    break;
            }
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    static private class NearbyPermissionCallback implements ResultCallback<Status> {
        private WeakReference<Activity> activityReference;

        //複数のパミーションダイアログ表示しないようにするためのflag
        public boolean mResolvingNearbyPermissionError = false;

        public NearbyPermissionCallback(Activity activity) {
            activityReference = new WeakReference<Activity>(activity);
        }

        private void finishedResolvingNearbyPermissionError() {
            mResolvingNearbyPermissionError = false;
        }

        @Override
        public void onResult(Status status) {
            Activity activity = activityReference.get();
            if (mResolvingNearbyPermissionError) {
                //他のパミーションダイアログが表示されている
                return;
            }
            //承諾された場合
            if (status.getStatusCode() != NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
                //0 is success
                //他のstatus codeもチェックしたほうがいいですが、ここの場合、パミーションチェックしかしないので、
                //statusCode == APP_NOT_OPTED_IN以外の場合、全部成功とする
                //https://developers.google.com/android/reference/com/google/android/gms/common/api/CommonStatusCodes
                Log.d(TAG, "already allowed " + status.getStatusCode());
                activityReference.get().finish();
                //TODO add init callback to notice the permission is approved
            } else {
                Log.d(TAG, status.getStatusMessage());
                try {
                    mResolvingNearbyPermissionError = true;
                    status.startResolutionForResult(activity, NEARBY_PERMISSION_REQUEST);
                } catch (IntentSender.SendIntentException e) {
                    mResolvingNearbyPermissionError = false;
                    e.printStackTrace();
                }
            }
        }
    }
}
