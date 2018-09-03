package cn.ezandroid.ezdownload;

/**
 * 下载状态枚举
 *
 * @author like
 * @date 2018-09-03
 */
public enum DownloadStatus {
    IDLE, // 空闲状态
    DOWNLOADING, // 正在下载
    SUSPEND, // 下载已挂起（暂停或失败）
    COMPLETED // 下载已完成
}
