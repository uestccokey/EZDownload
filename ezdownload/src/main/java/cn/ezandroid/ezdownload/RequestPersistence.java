package cn.ezandroid.ezdownload;

/**
 * 持久化接口
 *
 * @author like
 * @date 2018-09-06
 */
public interface RequestPersistence {

    /**
     * 寻找是否有已持久化的文件下载请求
     *
     * @param request
     * @return
     */
    DownloadFileRequest find(DownloadFileRequest request);

    /**
     * 保存传入的文件下载请求
     *
     * @param request
     */
    void save(DownloadFileRequest request);
}
