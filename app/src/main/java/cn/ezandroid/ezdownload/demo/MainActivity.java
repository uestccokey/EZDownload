package cn.ezandroid.ezdownload.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import cn.ezandroid.ezdownload.EZDownload;
import cn.ezandroid.ezdownload.IDownloadListener;
import cn.ezandroid.ezpermission.EZPermission;
import cn.ezandroid.ezpermission.Permission;
import cn.ezandroid.ezpermission.PermissionCallback;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 申请权限
        EZPermission.permissions(Permission.STORAGE).apply(this, new PermissionCallback() {
            @Override
            public void onAllPermissionsGranted() {
                EZDownload.request("http://116.62.9.17:8080/examples/2.mp4").setPath("/sdcard/2.mp4").setDownloadListener(new IDownloadListener() {
                    @Override
                    public void onFailed() {
                        Log.e("MainActivity", "onFailed");
                    }

                    @Override
                    public void onProgressUpdated(float progress) {
                        Log.e("MainActivity", "onProgressUpdated:" + progress);
                    }

                    @Override
                    public void onCompleted() {
                        Log.e("MainActivity", "onCompleted");
                    }
                }).download();
            }
        });
    }
}
