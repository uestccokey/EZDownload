package cn.ezandroid.ezdownload;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import static cn.ezandroid.ezdownload.DownloadFileRequest.TaskStatus.CANCELED;
import static cn.ezandroid.ezdownload.DownloadFileRequest.TaskStatus.COMPLETED;
import static cn.ezandroid.ezdownload.DownloadFileRequest.TaskStatus.FAILED;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_OK;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_PARTIAL_CONTENT;

/**
 * 下载（分片）文件
 *
 * @author like
 * @date 2018-09-03
 */
public class DownloadFileTask extends AsyncTask<String, Float, Object> {

    private DownloadFileRequest mDownloadFileRequest;
    private OnProgressUpdateListener mProgressUpdateListener;
    private OnCompleteListener mCompleteListener;

    private int mContentLength;

    public DownloadFileTask(DownloadFileRequest request) {
        this.mDownloadFileRequest = request;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        onTaskStatusChanged(DownloadFileRequest.TaskStatus.READY);
    }

    @Override
    protected Object doInBackground(String... strings) {
        startDownload();
        return null;
    }

    @Override
    public String toString() {
        return mDownloadFileRequest.toString();
    }

    private void startDownload() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(mDownloadFileRequest.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "*, */*");
            connection.setRequestProperty("accept-charset", "utf-8");
            connection.setRequestProperty("Range", "bytes=" + mDownloadFileRequest.getStartPosition() + "-" + mDownloadFileRequest.getEndPosition());
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            Log.e("DownloadFileTask", "onConnected:" + code + " " + mDownloadFileRequest.getUrl() +
                    " from " + mDownloadFileRequest.getStartPosition() + " to " + mDownloadFileRequest.getEndPosition());
            if (code == HTTP_STATE_SC_OK || code == HTTP_STATE_SC_PARTIAL_CONTENT) {
                onTaskStatusChanged(DownloadFileRequest.TaskStatus.DOWNLOADING);

                RandomAccessFile randomAccessFile = null;
                InputStream inputStream = null;
                try {
                    randomAccessFile = new RandomAccessFile(mDownloadFileRequest.getPath(), "rwd");
                    randomAccessFile.seek(mDownloadFileRequest.getStartPosition());

                    inputStream = connection.getInputStream();
                    int length;
                    int currentLength = 0;
                    mContentLength = connection.getContentLength();

                    byte[] buffer = new byte[1024 * 1000];
                    while ((length = inputStream.read(buffer)) != -1) {
                        currentLength += length;
                        if (mContentLength > 0) {
                            publishProgress(((float) currentLength / mContentLength * 100),
                                    ((float) currentLength / mDownloadFileRequest.getTotalContentLength() * 100));
                        }
                        randomAccessFile.write(buffer, 0, length);
                    }
                } finally {
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } else {
                onTaskStatusChanged(FAILED);
            }
        } catch (Exception e) {
            onTaskStatusChanged(FAILED);
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        super.onProgressUpdate(values);
        if (mProgressUpdateListener != null) {
            mProgressUpdateListener.onProgressUpdated(mDownloadFileRequest.getBlockPosition(), values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Object object) {
        super.onPostExecute(object);
        if (mDownloadFileRequest.getStatus() != CANCELED
                && mDownloadFileRequest.getStatus() != FAILED) {
            onTaskStatusChanged(COMPLETED);
            Log.e("DownloadFileTask", "onCompleted:" + mDownloadFileRequest.getUrl() +
                    " from " + mDownloadFileRequest.getStartPosition() + " to " + mDownloadFileRequest.getEndPosition());
            if (mCompleteListener != null) {
                mCompleteListener.onCompleted(mDownloadFileRequest.getUrl(), mContentLength);
            }
        } else {
            Log.e("DownloadFileTask", "onFailed:" + mDownloadFileRequest.getUrl() +
                    " from " + mDownloadFileRequest.getStartPosition() + " to " + mDownloadFileRequest.getEndPosition());
            if (mCompleteListener != null) {
                mCompleteListener.onFailed();
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        onTaskStatusChanged(CANCELED);
    }

    private void onTaskStatusChanged(DownloadFileRequest.TaskStatus taskStatus) {
        mDownloadFileRequest.setStatus(taskStatus);
        switch (taskStatus) {
            case CANCELED:
            case FAILED:
                mDownloadFileRequest.addRetryCount();
                if (mDownloadFileRequest.shouldRetry()) {
                    startDownload();
                }
                break;
            default:
                break;
        }
    }

    public DownloadFileRequest getDownloadFileRequest() {
        return mDownloadFileRequest;
    }

    public void setProgressUpdateListener(OnProgressUpdateListener progressUpdateListener) {
        this.mProgressUpdateListener = progressUpdateListener;
    }

    public void setCompleteListener(OnCompleteListener completeListener) {
        this.mCompleteListener = completeListener;
    }
}
