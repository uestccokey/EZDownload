package cn.ezandroid.ezdownload;

import android.os.Handler;
import android.os.Looper;

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

        private DownloadInfoTask mInfoTask = new DownloadInfoTask();
        private List<DownloadFileTask> mFileTasks = new ArrayList<>();

        private ExecutorService mExecutorService;

        private DownloadStatus mStatus = DownloadStatus.IDLE;

        private Handler mMainHandler = new Handler(Looper.getMainLooper());

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
         * 获取要下载的文件总大小
         *
         * @return
         */
        public long getDownloadTotalSize() {
            if (mFileTasks.isEmpty()) {
                return 0;
            } else {
                return mFileTasks.get(0).getDownloadFileRequest().getTotalContentLength();
            }
        }

        /**
         * 获取当前已下载的文件总大小
         *
         * @return
         */
        public long getDownloadCurrentSize() {
            if (mFileTasks.isEmpty()) {
                return 0;
            } else {
                long size = 0;
                for (DownloadFileTask task : mFileTasks) {
                    size += task.getDownloadFileRequest().getCurrentLength();
                }
                return size;
            }
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

            mInfoTask = new DownloadInfoTask();
            mInfoTask.setOnCompleteListener(new DownloadInfoTask.OnCompleteListener() {
                @Override
                public void onSuspend() {
                    mStatus = DownloadStatus.SUSPEND;
                    mMainHandler.post(() -> {
                        if (mDownloadListener != null) {
                            mDownloadListener.onSuspend();
                        }
                    });
                }

                @Override
                public void onCompleted(String url, long contentLength, boolean supportRange) {
                    if (!supportRange) {
                        mThreadCount = 1; // 不支持断点续传时，退化为单线程下载
                    }

                    mStatus = DownloadStatus.DOWNLOADING;
                    long blockSize = (int) Math.ceil((float) contentLength / mThreadCount);
                    for (int i = 0; i < mThreadCount; i++) {
                        DownloadFileTask downloadFileTask
                                = new DownloadFileTask(new DownloadFileRequest(url, mPath, contentLength, blockSize, i, supportRange));
                        downloadFileTask.setProgressUpdateListener((position, subProgress, totalProgress) -> {
                            if (mDownloadListener != null) {
                                mDownloadListener.onProgressUpdated(getDownloadProgress());
                            }
                        });
                        downloadFileTask.setCompleteListener(new DownloadFileTask.OnCompleteListener() {
                            @Override
                            public void onSuspend() {
                                mStatus = DownloadStatus.SUSPEND;
                                mMainHandler.post(() -> {
                                    if (mDownloadListener != null) {
                                        mDownloadListener.onSuspend();
                                    }
                                });
                            }

                            @Override
                            public void onCompleted() {
                                if (isDownloadCompleted()) {
                                    mStatus = DownloadStatus.COMPLETED;
                                    mMainHandler.post(() -> {
                                        if (mDownloadListener != null) {
                                            mDownloadListener.onCompleted();
                                        }
                                    });
                                }
                            }
                        });
                        downloadFileTask.executeOnExecutor(mExecutorService);
                        mFileTasks.add(downloadFileTask);
                    }
                }
            });
            mInfoTask.execute(mUrl);
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
                        task.cancel(true);
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
            if (mInfoTask != null) {
                mInfoTask.cancel(true);
            }
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
            if (mInfoTask != null) {
                mInfoTask.cancel(true);
            }
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