package cn.ezandroid.ezdownload.demo;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
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

    private String mUrl = "https://www.baidupcs.com/rest/2.0/pcs/file?method=batchdownload&app_id=250528&zipcontent=%7B%22fs_id%22%3A" +
            "%5B107809464443833%5D%7D&sign=DCb740ccc5511e5e8fedcff06b081203:KjmIACJQRWsN1XCmGuX1Ys9bDFo%3D&uid=1678264183&time=1536208193&dp-logid" +
            "=5740705277993713336&dp-callid=0&vuk=1678264183&zipname=%E3%80%90%E6%96%87%E4%BB%B6%E3%80%91AQWeight.zip.zip";

    // 测试小文件 http://116.62.9.17:8080/examples/2.mp4
    // 测试大文件 http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4
    // 测试不支持断点续传的Url https://github.com/lingochamp/FileDownloader/archive/v1.7.4.zip
    // 测试重定向的Url http://t.cn/RCSKsoX
    // 测试不支持断点续传的Url https://www.baidupcs.com/rest/2.0/pcs/file?method=batchdownload&app_id=250528&zipcontent=%7B%22fs_id%22%3A%5B3132666163%5D%7D
    // &sign
    // =DCb740ccc5511e5e8fedcff06b081203:Me9HDWoT%2B5TDoSbAWUnwdeCJJM4%3D&uid=1678264183&time=1536134849&dp-logid=5721017085837119746&dp-callid=0
    // &vuk=1678264183&zipname=%E3%80%90%E6%96%87%E4%BB%B6%E3%80%91%E5%BD%A2%E5%BD%A2%E8%89%B2%E8%89%B2%E7%9A%84%E8%83%9C%E8%B4%9F%E6%89%8B.pdf.zip

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
                            mDownloader = EZDownload.download(mUrl)
                                    .setPath("/sdcard/AQWeight.zip.tmp")
                                    .setThreadCount(Runtime.getRuntime().availableProcessors() - 1)
                                    .setDownloadListener(new IDownloadListener() {
                                        @Override
                                        public void onSuspend() {
                                            mDownloadButton.setText("Resume");
                                            mProgressDialog.dismiss();
                                            Log.e("MainActivity", "onSuspend");
                                        }

                                        @Override
                                        public void onProgressUpdated(float progress) {
//                                            Log.e("MainActivity", "onProgressUpdated:" + progress);
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
                                            try {
                                                ZipUtil.unZip("/sdcard/AQWeight.zip.tmp", "/sdcard/AQWeight");
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
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
