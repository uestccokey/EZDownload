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

    private DownloadFileRequest mDownloadRequest;
    private OnProgressUpdateListener mProgressUpdateListener;
    private OnCompleteListener mCompleteListener;

    public DownloadFileTask(DownloadFileRequest request) {
        this.mDownloadRequest = request;
    }

    public DownloadFileTask copy() throws CloneNotSupportedException {
        DownloadFileTask downloadFileTask = new DownloadFileTask(mDownloadRequest.clone());
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
        return mDownloadRequest.toString();
    }

    private void startDownload() {
        HttpURLConnection connection = null;
        try {
            long offset = mDownloadRequest.isSupportRange() ? mDownloadRequest.getCurrentLength() : 0;
            long start = mDownloadRequest.getStartPosition() + offset;
            long end = mDownloadRequest.getEndPosition();

            URL url = new URL(mDownloadRequest.getUrl());
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("Accept", "*, */*");
            connection.setRequestProperty("accept-charset", "utf-8");
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();

            if (isCancelled()) {
                return;
            }

            Log.e("DownloadFileTask", "onConnected:" + code + " " + mDownloadRequest.getUrl() + " " + mDownloadRequest.toString());
            if (code == HTTP_STATE_SC_OK || code == HTTP_STATE_SC_PARTIAL_CONTENT) {
                mDownloadRequest.setStatus(DOWNLOADING);

                RandomAccessFile randomAccessFile = null;
                InputStream inputStream = null;
                try {
                    randomAccessFile = new RandomAccessFile(mDownloadRequest.getPath(), "rwd");
                    randomAccessFile.seek(start);

                    inputStream = connection.getInputStream();
                    int length;
                    long currentLength = offset;
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
                            float totalProgress = currentLength * 100f / mDownloadRequest.getTotalContentLength();
                            mDownloadRequest.setProgress(totalProgress);
                            publishProgress(blockProgress, totalProgress);
                        }
                        randomAccessFile.write(buffer, 0, length);
                        mDownloadRequest.setCurrentLength(currentLength);
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
        mDownloadRequest.setStatus(SUSPEND);
        if (mDownloadRequest.shouldRetry()) {
            mDownloadRequest.addRetryCount();
            startDownload();
        }
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        super.onProgressUpdate(values);
        if (mProgressUpdateListener != null) {
            mProgressUpdateListener.onProgressUpdated(mDownloadRequest.getBlockPosition(), values[0], values[1]);
        }
    }

    @Override
    protected void onPostExecute(Object object) {
        super.onPostExecute(object);
        if (mDownloadRequest.getStatus() != SUSPEND) {
            mDownloadRequest.setStatus(COMPLETED);
//            Log.e("DownloadFileTask", "onCompleted:" + mDownloadRequest.getUrl() + " " + mDownloadRequest.toString());
            if (mCompleteListener != null) {
                mCompleteListener.onCompleted();
            }
        } else {
//            Log.e("DownloadFileTask", "onSuspend:" + mDownloadRequest.getUrl() + " " + mDownloadRequest.toString());
            if (mCompleteListener != null) {
                mCompleteListener.onSuspend();
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mDownloadRequest.setStatus(SUSPEND);
//        Log.e("DownloadFileTask", "onSuspend:" + mDownloadRequest.getUrl() + " " + mDownloadRequest.toString());
        if (mCompleteListener != null) {
            mCompleteListener.onSuspend();
        }
    }

    public DownloadFileRequest getDownloadFileRequest() {
        return mDownloadRequest;
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
