package cn.ezandroid.ezdownload;

import android.os.AsyncTask;
import android.util.Log;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

import static cn.ezandroid.ezdownload.DownloadStatus.COMPLETED;
import static cn.ezandroid.ezdownload.DownloadStatus.DOWNLOADING;
import static cn.ezandroid.ezdownload.DownloadStatus.SUSPEND;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_OK;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_PARTIAL_CONTENT;
import static cn.ezandroid.ezdownload.HttpState.HTTP_STATE_SC_REQUESTED_RANGE_NOT_SATISFIABLE;

/**
 * 下载（分片）文件任务
 *
 * @author like
 * @date 2018-09-03
 */
public class DownloadFileTask extends AsyncTask<String, Float, Object> {

    private DownloadFileRequest mDownloadFileRequest;
    private OnProgressUpdateListener mProgressUpdateListener;
    private OnCompleteListener mCompleteListener;

    public DownloadFileTask(DownloadFileRequest request) {
        this.mDownloadFileRequest = request;
    }

    public DownloadFileTask copy() throws CloneNotSupportedException {
        DownloadFileTask downloadFileTask = new DownloadFileTask(mDownloadFileRequest.clone());
        downloadFileTask.setProgressUpdateListener(mProgressUpdateListener);
        downloadFileTask.setCompleteListener(mCompleteListener);
        return downloadFileTask;
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
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "*, */*");
            connection.setRequestProperty("accept-charset", "utf-8");
            connection.setRequestProperty("Range", "bytes=" + (mDownloadFileRequest.getStartPosition() + mDownloadFileRequest.getCurrentLength()) +
                    "-" + mDownloadFileRequest.getEndPosition());
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();

            if (isCancelled()) {
                return;
            }

            Log.e("DownloadFileTask", "onConnected:" + code + " " + mDownloadFileRequest.getUrl() + " " + mDownloadFileRequest.toString());
            if (code == HTTP_STATE_SC_OK || code == HTTP_STATE_SC_PARTIAL_CONTENT) {
                mDownloadFileRequest.setStatus(DOWNLOADING);

                RandomAccessFile randomAccessFile = null;
                InputStream inputStream = null;
                try {
                    randomAccessFile = new RandomAccessFile(mDownloadFileRequest.getPath(), "rwd");
                    randomAccessFile.seek(mDownloadFileRequest.getStartPosition() + mDownloadFileRequest.getCurrentLength());

                    inputStream = connection.getInputStream();
                    int length;
                    long currentLength = mDownloadFileRequest.getCurrentLength();
                    long contentLength = connection.getContentLength();
//                    String contentRange = connection.getHeaderField("Content-Range");
//                    Log.e("DownloadFileTask", "ContentLength:" + contentLength + " ContentRange:" + contentRange);
                    byte[] buffer = new byte[1024 * 1024];
                    while ((length = inputStream.read(buffer)) != -1) {
                        if (isCancelled()) {
                            return;
                        }

                        currentLength += length;
                        if (contentLength > 0) {
                            float blockProgress = currentLength * 100f / contentLength;
                            float totalProgress = currentLength * 100f / mDownloadFileRequest.getTotalContentLength();
                            mDownloadFileRequest.setProgress(totalProgress);
                            publishProgress(blockProgress, totalProgress);
                        }
                        randomAccessFile.write(buffer, 0, length);
                        mDownloadFileRequest.setCurrentLength(currentLength);
                    }
                } finally {
                    if (randomAccessFile != null) {
                        randomAccessFile.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
            } else if (code == HTTP_STATE_SC_REQUESTED_RANGE_NOT_SATISFIABLE) {
                // 只会在分段下载完成后，但程序再次请求时出现
            } else {
                retry();
            }
        } catch (Exception e) {
            retry();
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private void retry() {
        mDownloadFileRequest.setStatus(SUSPEND);
        if (mDownloadFileRequest.shouldRetry()) {
            mDownloadFileRequest.addRetryCount();
            startDownload();
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
        if (mDownloadFileRequest.getStatus() != SUSPEND) {
            mDownloadFileRequest.setStatus(COMPLETED);
//            Log.e("DownloadFileTask", "onCompleted:" + mDownloadFileRequest.getUrl() + " " + mDownloadFileRequest.toString());
            if (mCompleteListener != null) {
                mCompleteListener.onCompleted();
            }
        } else {
//            Log.e("DownloadFileTask", "onSuspend:" + mDownloadFileRequest.getUrl() + " " + mDownloadFileRequest.toString());
            if (mCompleteListener != null) {
                mCompleteListener.onSuspend();
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mDownloadFileRequest.setStatus(SUSPEND);
//        Log.e("DownloadFileTask", "onSuspend:" + mDownloadFileRequest.getUrl() + " " + mDownloadFileRequest.toString());
        if (mCompleteListener != null) {
            mCompleteListener.onSuspend();
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

    interface OnProgressUpdateListener {

        void onProgressUpdated(int position, float subProgress, float totalProgress);
    }

    interface OnCompleteListener {

        void onSuspend();

        void onCompleted();
    }
}
