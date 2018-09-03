package cn.ezandroid.ezdownload.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import cn.ezandroid.ezdownload.EZDownload;
import cn.ezandroid.ezdownload.OnCompleteListener;
import cn.ezandroid.ezdownload.OnProgressUpdateListener;
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
                EZDownload.request("http://116.62.9.17:8080/examples/2.mp4").setCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onFailed() {
                    }

                    @Override
                    public void onCompleted() {
                    }
                }).setProgressUpdateListener(new OnProgressUpdateListener() {
                    @Override
                    public void onProgressUpdated(float progress) {
                    }
                }).download();
            }
        });
    }
}
