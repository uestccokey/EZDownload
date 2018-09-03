package cn.ezandroid.ezdownload;

/**
 * 下载进度监听器
 *
 * @author like
 * @date 2018-09-03
 */
interface OnProgressUpdateListener {

    void onProgressUpdated(int position, float subProgress, float totalProgress);
}
