package cn.ezandroid.ezdownload;

/**
 * IDownloadListener
 *
 * @author like
 * @date 2018-09-03
 */
public interface IDownloadListener {

    void onFailed();

    default void onProgressUpdated(float progress) {
    }

    void onCompleted();
}
