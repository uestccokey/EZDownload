package cn.ezandroid.ezdownload;

/**
 * 下载结束监听器
 *
 * @author like
 * @date 2018-09-03
 */
interface OnCompleteListener {

    void onSuspend();

    void onCompleted(String url, int contentLength);
}
