package cn.ezandroid.ezdownload;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_OK;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_REDIRECT;

/**
 * 下载第一步
 * 1.获取要下载的文件大小，以便后续进行分片多线程下载
 * 2.处理重定向，获取最终的下载文件地址
 *
 * @author like
 * @date 2018-09-03
 */
public class DownloadSizeTask extends AsyncTask<String, Integer, Object> {

    private static final int MAX_REDIRECT_COUNT = 3; // 最大重定向次数

    private int mRedirectCount;

    private OnInternalCompleteListener mCompleteListener;

    @Override
    protected Object doInBackground(String... params) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "*, */*");
            connection.setRequestProperty("accept-charset", "utf-8");
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            Log.d("DownloadSizeTask", "code:" + code + " msg:" + connection.getResponseMessage());
            if (code == HTTP_STATE_SC_OK) {
                if (mCompleteListener != null) {
                    mCompleteListener.onCompleted(params[0], connection.getContentLength());
                }
            } else if (code == HTTP_STATE_SC_REDIRECT) {
                // 处理重定向
                mRedirectCount++;
                String location = connection.getHeaderField("Location");
                if (TextUtils.isEmpty(location) || mRedirectCount > MAX_REDIRECT_COUNT) {
                    if (mCompleteListener != null) {
                        mCompleteListener.onFailed();
                    }
                } else {
                    doInBackground(location);
                }
            } else {
                if (mCompleteListener != null) {
                    mCompleteListener.onFailed();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public void setOnCompleteListener(OnInternalCompleteListener listener) {
        this.mCompleteListener = listener;
    }
}
