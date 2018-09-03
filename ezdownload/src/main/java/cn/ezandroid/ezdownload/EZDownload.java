package cn.ezandroid.ezdownload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.ezandroid.ezdownload.DownloadFileRequest.TaskStatus.COMPLETED;

/**
 * EZDownload
 *
 * @author like
 * @date 2018-09-03
 */
public class EZDownload {

    private EZDownload() {
    }

    public static Builder request(String url) {
        return new Builder().setPath(url);
    }

    public static class Builder {

        private String mUrl;
        private String mPath;

        private int mThreadCount = Runtime.getRuntime().availableProcessors(); // 默认为手机核数

        private OnCompleteListener mCompleteListener;
        private OnProgressUpdateListener mProgressUpdateListener;

        private List<DownloadFileTask> mFileTasks = new ArrayList<>();

        private Builder() {
        }

        public Builder setUrl(String url) {
            mUrl = url;
            return this;
        }

        public Builder setPath(String path) {
            mPath = path;
            return this;
        }

        public Builder setThreadCount(int threadCount) {
            mThreadCount = threadCount;
            return this;
        }

        public Builder setCompleteListener(OnCompleteListener completeListener) {
            mCompleteListener = completeListener;
            return this;
        }

        public Builder setProgressUpdateListener(OnProgressUpdateListener progressUpdateListener) {
            mProgressUpdateListener = progressUpdateListener;
            return this;
        }

        private float getDownloadProgress() {
            float progress = 0;
            for (DownloadFileTask task : mFileTasks) {
                progress += task.getDownloadFileRequest().getProgress();
            }
            return progress;
        }

        private boolean isDownloadCompleted() {
            boolean isCompleted = true;
            for (DownloadFileTask task : mFileTasks) {
                if (task.getDownloadFileRequest().getStatus() != COMPLETED) {
                    isCompleted = false;
                    break;
                }
            }
            return isCompleted;
        }

        public void download() {
            DownloadSizeTask downloadSizeTask = new DownloadSizeTask();
            downloadSizeTask.setOnCompleteListener(new OnInternalCompleteListener() {
                @Override
                public void onFailed() {
                    if (mCompleteListener != null) {
                        mCompleteListener.onFailed();
                    }
                }

                @Override
                public void onCompleted(String url, int contentLength) {
                    long blockSize = (int) Math.ceil((float) contentLength / mThreadCount);

                    for (int position = 0; position < mThreadCount; position++) {
                        DownloadFileTask downloadFileTask
                                = new DownloadFileTask(new DownloadFileRequest(url, mPath, contentLength, blockSize, position));
                        downloadFileTask.setProgressUpdateListener(new OnInternalProgressUpdateListener() {
                            @Override
                            public void onProgressUpdated(int position, float subProgress, float totalProgress) {
                                mFileTasks.get(position).getDownloadFileRequest().setProgress(totalProgress);
                                if (mProgressUpdateListener != null) {
                                    mProgressUpdateListener.onProgressUpdated(getDownloadProgress());
                                }
                            }
                        });
                        downloadFileTask.setCompleteListener(new OnInternalCompleteListener() {
                            @Override
                            public void onFailed() {
                                if (mCompleteListener != null) {
                                    mCompleteListener.onFailed();
                                }
                            }

                            @Override
                            public void onCompleted(String url, int contentLength) {
                                if (isDownloadCompleted()) {
                                    if (mCompleteListener != null) {
                                        mCompleteListener.onCompleted();
                                    }
                                }
                            }
                        });
                        ExecutorService executorService = Executors.newFixedThreadPool(mThreadCount);
                        downloadFileTask.executeOnExecutor(executorService);
                        mFileTasks.add(downloadFileTask);
                    }
                }
            });
            downloadSizeTask.execute(mUrl);
        }
    }
}