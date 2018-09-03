package cn.ezandroid.ezdownload;

/**
 * OnInternalCompleteListener
 *
 * @author like
 * @date 2018-09-03
 */
interface OnInternalCompleteListener {

    void onFailed();

    void onCompleted(String url, int contentLength);
}
