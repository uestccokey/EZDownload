package cn.ezandroid.ezdownload;

/**
 * 下载监听器
 *
 * @author like
 * @date 2018-09-03
 */
public interface IDownloadListener {

    default void onSuspend() {
    }

    default void onProgressUpdated(float progress) {
    }

    default void onCompleted() {
    }
}
