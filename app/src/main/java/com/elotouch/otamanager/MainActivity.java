package com.elotouch.otamanager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.eloview.homesdk.accountManager.AccountManager;
import com.eloview.homesdk.otaManager.OTA;

import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private static String TAG = MainActivity.class.getSimpleName();
    private String accesstoken ="";
    private String OTA_STATUS_URL = "http://10.51.1.137:8080/ota.json";
    private String OTA_DOWNLOAD_URL ="http://10.51.1.137:8080/ota.zip";
    private  int downloadId;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };

    private TextView txt_result;


    /*
     * android 动态权限申请
     * */
    public static void verifyStoragePermissions(Activity activity) {
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_result = findViewById(R.id.txt_result);
        verifyStoragePermissions(this);
        init_token();
// Setting timeout globally for the download network requests:
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(10_000)
                .setConnectTimeout(10_000)
                .setDatabaseEnabled(true)
                .build();

        PRDownloader.initialize(getApplicationContext(), config);
        PRDownloader.shutDown();
        PRDownloader.getStatus(downloadId);



        findViewById(R.id.button_check).setOnClickListener(v -> OTA.instance.checkOTAStatus(MainActivity.this, accesstoken, OTA_STATUS_URL, otaHandler ));


        findViewById(R.id.button_update).setOnClickListener(v -> download_apply_OTA());

        findViewById(R.id.button_stop).setOnClickListener(v -> stopOTADownload());
        findViewById(R.id.button_resume).setOnClickListener(v -> resumeOTADownload());
    }

    private void init_token() {
        Properties credentials = new Properties();
        try {
            credentials.load(getApplicationContext().getAssets().open("com.elotouch.otamanager_jwt.prop"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        accesstoken = credentials.getProperty("jwt");
        AccountManager.instance.verifyEloToken(this, accesstoken,accessTokenHandler );
    }

    private void stopOTADownload() {
        PRDownloader.pause(downloadId);
    }

    private void resumeOTADownload() {
        PRDownloader.resume(downloadId);
    }

    @SuppressLint({"SdCardPath", "SetTextI18n"})
    private void download_apply_OTA() {
         downloadId = PRDownloader.download(OTA_DOWNLOAD_URL, "/sdcard/Download/", "ota.zip")
                .build()
                .setOnStartOrResumeListener(() -> {

                    txt_result.setText("OTA is Resumed");
                })
                .setOnPauseListener(() -> {

                    txt_result.setText("OTA is paused");
                })
                .setOnCancelListener(() -> {

                    txt_result.setText("OTA is cancelled");
                })
                .setOnProgressListener(progress -> {
                    long p = progress.currentBytes * 100 / progress.totalBytes;
                    txt_result.setText("OTA is updating .Already Downloaded " + p + "%");
                })
                .start(new OnDownloadListener() {

                    @Override
                    public void onDownloadComplete() {
                        OTA.instance.downloadApplyABOTA(MainActivity.this, accesstoken, "",
                                "/sdcard/Download/ota.zip","", otaHandler);
                    }

                    @Override
                    public void onError(Error error) {

                        txt_result.setText("OTA download is error, " + error);
                    }
                });

    }

    Handler accessTokenHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Bundle b = msg.getData();
            String key = Integer.toString(msg.what);
            String val = b.getString(key, "");
            Log.d(TAG, "handleMessage: " + val);
            switch (msg.what) {
                case AccountManager.TOKEN_VERIFY_FAIL:
                    Log.d(TAG, "handleMessage: Invalid Token");
                    val = "INVALID TOKEN\n" + val;
                    txt_result.setText(val);
                    break;
                case AccountManager.ACCESS_TOKEN_INVALID:
                    Log.d(TAG, "handleMessage: Failed to get aN access accessToken");
                    txt_result.setText(val);
                    break;
                case AccountManager.TOKEN_VERIFY_SUCCESS:
                    Log.d(TAG, "handleMessage: accessToken >> " + val);
                    txt_result.setText(val);
                    break;
                case AccountManager.GENERIC_ERROR:
                    txt_result.setText(val);
                    break;
            }
            return false;
        }
    });

    @SuppressLint("HandlerLeak")
    public Handler otaHandler = new Handler(Looper.myLooper()) {
        @SuppressLint("SetTextI18n")
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Bundle b = msg.getData();
            String key = Integer.toString(msg.what);
            String val = b.getString(key);
            switch (msg.what) {
                case OTA.OTA_DOWNLOAD_REMOTE_FILE_PERCENTAGE:
                    Log.i(TAG, "Remote file download percentage := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_REMOTE_FILE_PERCENTAGE \n\n Percentage : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_REMOTE_FILE_SUCCESS:
                    Log.i(TAG, "Remote file download success := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_REMOTE_FILE_SUCCESS \n\n Info : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_REMOTE_FILE_ERROR:
                    Log.i(TAG, "Remote file download error := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_REMOTE_FILE_ERROR \n\n Error : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_LOCAL_FILE_PERCENTAGE:
                    Log.i(TAG, "Local file download percentage := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_LOCAL_FILE_PERCENTAGE \n\n Percentage : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_LOCAL_FILE_SUCCESS:
                    Log.i(TAG, "Local file download success := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_LOCAL_FILE_SUCCESS \n\n Info : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_LOCAL_FILE_ERROR:
                    Log.i(TAG, "Local file download error := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_LOCAL_FILE_ERROR \n\n Error : " + val);
                    break;
                case OTA.OTA_DOWNLOAD_GENERIC_ERROR:
                    Log.i(TAG, "Download error := " + val);
                    txt_result.setText("Code : OTA_DOWNLOAD_GENERIC_ERROR \n\n Error : " + val);
                    break;
                case OTA.OTA_VERIFY_SIGN_PERCENTAGE:
                    Log.i(TAG, "Verify sign percentage := " + val);
                    txt_result.setText("Code : OTA_VERIFY_SIGN_PERCENTAGE \n\n Percentage : " + val);
                    break;
                case OTA.OTA_VERIFY_SIGN_SUCCESS:
                    Log.i(TAG, "Verify sign success := " + val);
                    txt_result.setText("Code : OTA_VERIFY_SIGN_SUCCESS \n\n Info : " + val);
                    break;
                case OTA.OTA_VERIFY_SIGN_ERROR:
                    Log.i(TAG, "Verify sign error := " + val);
                    txt_result.setText("Code : OTA_VERIFY_SIGN_ERROR \n\n Error : " + val);
                    break;
                case OTA.OTA_VERIFY_FILE_ERROR:
                    Log.i(TAG, "Verify file error := " + val);
                    txt_result.setText("Code : OTA_VERIFY_FILE_ERROR \n\n Error : " + val);
                    break;
                case OTA.OTA_CHECK_STATUS_OLD:
                    Log.i(TAG, "OTA check : Version is old := " + val);
                    txt_result.setText("Code : OTA_CHECK_STATUS_OLD \n\n Status : " + val);
                    break;
                case OTA.OTA_CHECK_STATUS_NEW:
                    Log.i(TAG, "OTA Check : Version is new := " + val);
                    txt_result.setText("Code : OTA_CHECK_STATUS_NEW \n\n Status : " + val);
                    break;
                case OTA.OTA_CHECK_STATUS_SAME:
                    Log.i(TAG, "OTA Check : Version is same  := " + val);
                    txt_result.setText("Code : OTA_CHECK_STATUS_SAME \n\n Status : " + val);
                    break;
                case OTA.OTA_CHECK_STATUS_GENERIC_ERROR:
                    Log.i(TAG, "OTA Check : Status error  := " + val);
                    txt_result.setText("Code : OTA_CHECK_STATUS_GENERIC_ERROR \n\n Status : " + val);
                    break;
                case OTA.OTA_APPLY_FILE_ERROR:
                    Log.i(TAG, "OTA apply file error := " + val);
                    txt_result.setText("Code : OTA_APPLY_FILE_ERROR \n\n Status : " + val);
                    break;
                case OTA.OTA_APPLY_IN_PROGRESS:
                    Log.i(TAG, "OTA apply file progress := " + val);
                    txt_result.setText("Code : OTA_APPLY_IN_PROGRESS \n\n Status : " + val);
                    break;
                case AccountManager.TOKEN_VERIFY_FAIL:
                    Log.v(TAG, "Invalid accessToken");
                    txt_result.setText("INVALID TOKEN\n" + val);
                    break;
                case OTA.OTA_VERSION_RESPONSE_AVAILABLE:
                    Log.v(TAG, "OTA_VERSION_RESPONSE_AVAILABLE");
                    txt_result.setText("OTA versions:\n" + val);
                    break;
                case OTA.GENERIC_ERROR:
                    Log.v(TAG, "OTA_GENERIC_ERROR");
                    txt_result.setText("OTA_GENERIC_ERROR: OTA Error:\n" + val);
                    break;
                case AccountManager.TOKEN_VERIFY_SUCCESS:
                    Log.v(TAG, "TOKEN_VERIFY_RESPONSE_AVAILABLE");
                    txt_result.setText(val);
                    break;
                /*case OTA.ANDROID_HOME_SET:
                    Log.v(LOG_TAG, "ANDROID_HOME_SET");
                    txt_result.setText(val);
                    break;*/
                case OTA.OTA_DOWNLOAD_REMOTE_FILE_ALREADY_DOWNLOADED:
                    Log.v(TAG, "OTA_DOWNLOAD_REMOTE_FILE_ALREADY_DOWNLOADED");
                    txt_result.setText("Code : OTA_DOWNLOAD_REMOTE_FILE_ALREADY_DOWNLOADED \n\n Status : " + val);
                    break;
            }
        }
    };
}