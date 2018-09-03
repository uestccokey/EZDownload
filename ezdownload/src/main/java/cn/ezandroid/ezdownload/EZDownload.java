package cn.ezandroid.ezdownload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.ezandroid.ezdownload.DownloadStatus.COMPLETED;

/**
 * 文件下载
 *
 * @author like
 * @date 2018-09-03
 */
public class EZDownload {

    private EZDownload() {
    }

    public static Downloader download(String url) {
        return new Downloader().setUrl(url);
    }

    public static class Downloader {

        private String mUrl;
        private String mPath;

        private int mThreadCount = 1;

        private IDownloadListener mDownloadListener;

        private List<DownloadFileTask> mFileTasks = new ArrayList<>();

        private ExecutorService mExecutorService;

        private DownloadStatus mStatus = DownloadStatus.IDLE;

        private Downloader() {
        }

        public Downloader setUrl(String url) {
            mUrl = url;
            return this;
        }

        public Downloader setPath(String path) {
            mPath = path;
            return this;
        }

        public Downloader setThreadCount(int threadCount) {
            mThreadCount = Math.max(threadCount, 1);
            return this;
        }

        public Downloader setDownloadListener(IDownloadListener downloadListener) {
            mDownloadListener = downloadListener;
            return this;
        }

        /**
         * 获取下载进度
         *
         * @return
         */
        public float getDownloadProgress() {
            float progress = 0;
            for (DownloadFileTask task : mFileTasks) {
                progress += task.getDownloadFileRequest().getProgress();
            }
            return progress;
        }

        /**
         * 获取下载状态
         *
         * @return
         */
        public DownloadStatus getDownloadStatus() {
            return mStatus;
        }

        /**
         * 是否已下载完成
         *
         * @return
         */
        public boolean isDownloadCompleted() {
            boolean isCompleted = true;
            for (DownloadFileTask task : mFileTasks) {
                if (task.getDownloadFileRequest().getStatus() != COMPLETED) {
                    isCompleted = false;
                    break;
                }
            }
            return isCompleted;
        }

        /**
         * 开始下载
         *
         * @return
         */
        public Downloader start() {
            if (mExecutorService == null || mExecutorService.isShutdown()) {
                mExecutorService = Executors.newFixedThreadPool(mThreadCount);
            }

            DownloadSizeTask downloadSizeTask = new DownloadSizeTask();
            downloadSizeTask.setOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onSuspend() {
                    mStatus = DownloadStatus.SUSPEND;
                    if (mDownloadListener != null) {
                        mDownloadListener.onSuspend();
                    }
                }

                @Override
                public void onCompleted(String url, int contentLength) {
                    mStatus = DownloadStatus.DOWNLOADING;
                    long blockSize = (int) Math.ceil((float) contentLength / mThreadCount);

                    for (int position = 0; position < mThreadCount; position++) {
                        DownloadFileTask downloadFileTask
                                = new DownloadFileTask(new DownloadFileRequest(url, mPath, contentLength, blockSize, position));
                        downloadFileTask.setProgressUpdateListener(new OnProgressUpdateListener() {
                            @Override
                            public void onProgressUpdated(int position, float subProgress, float totalProgress) {
                                mFileTasks.get(position).getDownloadFileRequest().setProgress(totalProgress);
                                if (mDownloadListener != null) {
                                    mDownloadListener.onProgressUpdated(getDownloadProgress());
                                }
                            }
                        });
                        downloadFileTask.setCompleteListener(new OnCompleteListener() {
                            @Override
                            public void onSuspend() {
                                mStatus = DownloadStatus.SUSPEND;
                                if (mDownloadListener != null) {
                                    mDownloadListener.onSuspend();
                                }
                            }

                            @Override
                            public void onCompleted(String url, int contentLength) {
                                if (isDownloadCompleted()) {
                                    mStatus = DownloadStatus.COMPLETED;
                                    if (mDownloadListener != null) {
                                        mDownloadListener.onCompleted();
                                    }
                                }
                            }
                        });
                        downloadFileTask.executeOnExecutor(mExecutorService);
                        mFileTasks.add(downloadFileTask);
                    }
                }
            });
            downloadSizeTask.execute(mUrl);
            return this;
        }

        /**
         * 恢复下载
         *
         * @return
         */
        public Downloader resume() {
            if (mFileTasks.isEmpty()) {
                start();
            } else {
                if (mExecutorService == null || mExecutorService.isShutdown()) {
                    mExecutorService = Executors.newFixedThreadPool(mThreadCount);
                }

                mStatus = DownloadStatus.DOWNLOADING;
                try {
                    List<DownloadFileTask> copyTasks = new ArrayList<>();
                    for (DownloadFileTask task : mFileTasks) {
                        copyTasks.add(task.copy());
                    }
                    mFileTasks = copyTasks;

                    for (DownloadFileTask task : mFileTasks) {
                        task.executeOnExecutor(mExecutorService);
                    }
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        /**
         * 暂停下载
         *
         * @return
         */
        public Downloader pause() {
            mStatus = DownloadStatus.SUSPEND;
            for (DownloadFileTask task : mFileTasks) {
                task.cancel(true);
            }

            if (mExecutorService != null) {
                mExecutorService.shutdownNow();
            }
            return this;
        }

        /**
         * 销毁下载器
         */
        public void destroy() {
            mStatus = DownloadStatus.IDLE;
            for (DownloadFileTask task : mFileTasks) {
                task.cancel(true);
            }

            if (mExecutorService != null) {
                mExecutorService.shutdownNow();
            }

            mFileTasks.clear();
        }
    }
}