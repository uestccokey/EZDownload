package cn.ezandroid.ezdownload.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import cn.ezandroid.ezdownload.EZDownload;
import cn.ezandroid.ezdownload.IDownloadListener;
import cn.ezandroid.ezpermission.EZPermission;
import cn.ezandroid.ezpermission.Permission;
import cn.ezandroid.ezpermission.PermissionCallback;

public class MainActivity extends AppCompatActivity {

    private TextView mProgressText;
    private Button mDownloadButton;

    private EZDownload.Downloader mDownloader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressText = findViewById(R.id.progress);
        mDownloadButton = findViewById(R.id.start);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 申请权限
                EZPermission.permissions(Permission.STORAGE).apply(MainActivity.this, new PermissionCallback() {
                    @Override
                    public void onAllPermissionsGranted() {
                        if (mDownloader == null) {
                            long time = System.currentTimeMillis();
                            // http://116.62.9.17:8080/examples/2.mp4
                            // http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4
                            mDownloader = EZDownload.download("http://116.62.9.17:8080/examples/2.mp4")
                                    .setPath("/sdcard/3.mp4")
                                    .setThreadCount(4)
                                    .setDownloadListener(new IDownloadListener() {
                                        @Override
                                        public void onFailed() {
                                            mDownloadButton.setText("继续");
                                            Log.e("MainActivity", "onFailed");
                                        }

                                        @Override
                                        public void onProgressUpdated(float progress) {
                                            Log.e("MainActivity", "onProgressUpdated:" + progress);
                                            mProgressText.setText(String.valueOf(progress));
                                        }

                                        @Override
                                        public void onCompleted() {
                                            mDownloadButton.setText("已完成");
                                            Log.e("MainActivity", "onCompleted:" + (System.currentTimeMillis() - time));
                                        }
                                    }).start();
                            mDownloadButton.setText("暂停");
                        } else {
                            switch (mDownloader.getDownloadStatus()) {
                                case FAILED:
                                case CANCELED:
                                    mDownloader.resume();
                                    mDownloadButton.setText("暂停");
                                    break;
                                case DOWNLOADING:
                                    mDownloader.pause();
                                    mDownloadButton.setText("继续");
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
        if (mDownloader != null) {
            mDownloader.destroy();
        }
    }
}
