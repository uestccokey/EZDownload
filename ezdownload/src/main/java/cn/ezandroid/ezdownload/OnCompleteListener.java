package cn.ezandroid.ezdownload;

/**
 * OnCompleteListener
 *
 * @author like
 * @date 2018-09-03
 */
interface OnCompleteListener {

    void onSuspend();

    void onCompleted(String url, int contentLength);
}
