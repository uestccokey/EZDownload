package cn.ezandroid.ezdownload;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_OK;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_PARTIAL_CONTENT;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_REDIRECT;

/**
 * 下载第一步
 * 1.获取要下载的文件大小，以便后续进行分片多线程下载
 * 2.处理重定向，获取最终的下载文件地址
 *
 * @author like
 * @date 2018-09-03
 */
public class DownloadInfoTask extends AsyncTask<String, Integer, Object> {

    private static final int MAX_REDIRECT_COUNT = 3; // 最大重定向次数
    private static final int MAX_RETRY_COUNT = 3;

    private int mRedirectCount;
    private int mRetryCount;

    private OnCompleteListener mCompleteListener;

    @Override
    protected Object doInBackground(String... params) {
        if (isCancelled()) {
            if (mCompleteListener != null) {
                mCompleteListener.onSuspend();
            }
            return null;
        }

        HttpURLConnection connection = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "*, */*");
            connection.setRequestProperty("Accept-Charset", "utf-8");
            connection.setRequestProperty("Range", "bytes=0-"); // 用来判断是否支持断点续传
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();

            if (isCancelled()) {
                if (mCompleteListener != null) {
                    mCompleteListener.onSuspend();
                }
                return null;
            }

            Log.i("DownloadInfoTask", "onConnected:" + code + " " + params[0]);
            if (code == HTTP_STATE_SC_OK || code == HTTP_STATE_SC_PARTIAL_CONTENT) {
                long contentLength;
                long contentRange;
                String rangeString = connection.getHeaderField("Content-Range");
                if (!TextUtils.isEmpty(rangeString)) {
                    contentLength = Long.parseLong(rangeString.substring(rangeString.indexOf('/') + 1));
                    contentRange = Long.parseLong(rangeString.substring(rangeString.indexOf("-") + 1, rangeString.indexOf('/'))) + 1;
                } else {
                    contentLength = connection.getContentLength();
                    contentRange = -1;
                }
                Log.i("DownloadInfoTask", "ContentLength:" + contentLength + " ContentRange:" + contentRange);
                if (contentLength <= 0) {
                    retry(params);
                } else {
                    if (mCompleteListener != null) {
                        mCompleteListener.onCompleted(params[0], contentLength, contentRange);
                    }
                }
            } else if (code == HTTP_STATE_SC_REDIRECT) {
                mRedirectCount++;
                String location = connection.getHeaderField("Location");
                Log.i("DownloadInfoTask", "Redirect url:" + location + " " + mRedirectCount);
                if (TextUtils.isEmpty(location) || mRedirectCount > MAX_REDIRECT_COUNT) {
                    retry(params);
                } else {
                    doInBackground(location);
                }
            } else {
                retry(params);
            }
        } catch (Exception e) {
            retry(params);
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    private void retry(String... params) {
        if (mRetryCount < MAX_RETRY_COUNT) {
            mRetryCount++;
            doInBackground(params);
        } else {
            if (mCompleteListener != null) {
                mCompleteListener.onSuspend();
            }
        }
    }

    public void setOnCompleteListener(OnCompleteListener listener) {
        this.mCompleteListener = listener;
    }

    interface OnCompleteListener {

        void onSuspend();

        void onCompleted(String url, long contentLength, long contentRange);
    }
}
