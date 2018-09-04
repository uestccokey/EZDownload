package cn.ezandroid.ezdownload.demo;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Locale;

import cn.ezandroid.ezdownload.EZDownload;
import cn.ezandroid.ezdownload.IDownloadListener;
import cn.ezandroid.ezpermission.EZPermission;
import cn.ezandroid.ezpermission.Permission;
import cn.ezandroid.ezpermission.PermissionCallback;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private Button mDownloadButton;

    private EZDownload.Downloader mDownloader;

    // 测试小文件 http://116.62.9.17:8080/examples/2.mp4
    // 测试大文件 http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4
    // 测试不支持断点续传 https://github.com/lingochamp/FileDownloader/archive/v1.7.4.zip
    // 测试重定向 http://t.cn/RCSKsoX

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setMessage("Downloading");
        mProgressDialog.setMax(100);
        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Pause",
                (dialog, which) -> {
                    if (mDownloader != null) {
                        mDownloader.pause();
                        mDownloadButton.setText("Resume");
                    }
                });

        mDownloadButton = findViewById(R.id.start);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 申请权限
                EZPermission.permissions(Permission.STORAGE).apply(MainActivity.this, new PermissionCallback() {
                    @Override
                    public void onAllPermissionsGranted() {
                        if (mDownloader == null) {
                            mProgressDialog.show();
                            long time = System.currentTimeMillis();
                            mDownloader = EZDownload.download("https://github.com/lingochamp/FileDownloader/archive/v1.7.4.zip")
                                    .setPath("/sdcard/3")
                                    .setThreadCount(2)
                                    .setDownloadListener(new IDownloadListener() {
                                        @Override
                                        public void onSuspend() {
                                            mDownloadButton.setText("Resume");
                                            mProgressDialog.dismiss();
                                            Log.e("MainActivity", "onSuspend");
                                        }

                                        @Override
                                        public void onProgressUpdated(float progress) {
                                            Log.e("MainActivity", "onProgressUpdated:" + progress);
                                            mProgressDialog.setProgress(Math.round(progress));
                                            float total = mDownloader.getDownloadTotalSize() / 1024f / 1024f;
                                            float current = mDownloader.getDownloadCurrentSize() / 1024f / 1024f;
                                            mProgressDialog.setProgressNumberFormat(String.format(Locale.getDefault(),
                                                    "%.2fMB/%.2fMB", current, total));
                                        }

                                        @Override
                                        public void onCompleted() {
                                            mDownloadButton.setText("Completed");
                                            mProgressDialog.dismiss();
                                            Log.e("MainActivity", "onCompleted:" + (System.currentTimeMillis() - time));
                                        }
                                    }).start();
                            mDownloadButton.setText("Pause");
                        } else {
                            switch (mDownloader.getDownloadStatus()) {
                                case SUSPEND:
                                    mProgressDialog.show();
                                    mDownloader.resume();
                                    mDownloadButton.setText("Pause");
                                    break;
                                case DOWNLOADING:
                                    mDownloader.pause();
                                    mDownloadButton.setText("Resume");
                                    break;
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (mDownloader != null) {
            mDownloader.destroy();
        }
    }
}
