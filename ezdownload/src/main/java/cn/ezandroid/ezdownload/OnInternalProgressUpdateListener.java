package cn.ezandroid.ezdownload;

/**
 * OnInternalProgressUpdateListener
 *
 * @author like
 * @date 2018-09-03
 */
interface OnInternalProgressUpdateListener {

    void onProgressUpdated(int position, float subProgress, float totalProgress);
}
