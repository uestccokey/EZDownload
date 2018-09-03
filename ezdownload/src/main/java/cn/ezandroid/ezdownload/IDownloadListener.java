package cn.ezandroid.ezdownload;

/**
 * IDownloadListener
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
