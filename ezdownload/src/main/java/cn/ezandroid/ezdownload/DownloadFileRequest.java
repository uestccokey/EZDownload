package cn.ezandroid.ezdownload;

import java.io.Serializable;

/**
 * 文件下载请求
 *
 * @author like
 * @date 2018-09-03
 */
public class DownloadFileRequest implements Serializable, Cloneable {

    private static final long serialVersionUID = 42L;

    private static final int MAX_RETRY_COUNT = 3;

    private long mTotalContentLength;
    private long mBlockSize;
    private int mBlockPosition;

    private long mStartPosition;
    private long mEndPosition;

    private long mCurrentLength;

    private float mProgress;
    private int mRetryCount;
    private DownloadStatus mStatus = DownloadStatus.IDLE;
    private long mContentRange;

    private String mUrl;
    private String mPath;

    public DownloadFileRequest(String url, String path) {
        this.mUrl = url;
        this.mPath = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DownloadFileRequest)) return false;

        DownloadFileRequest that = (DownloadFileRequest) o;

        if (mTotalContentLength != that.mTotalContentLength) return false;
        if (mBlockSize != that.mBlockSize) return false;
        if (mBlockPosition != that.mBlockPosition) return false;
        if (mStartPosition != that.mStartPosition) return false;
        if (mEndPosition != that.mEndPosition) return false;
        if (mContentRange != that.mContentRange) return false;
        if (!mUrl.equals(that.mUrl)) return false;
        return mPath != null ? mPath.equals(that.mPath) : that.mPath == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (mTotalContentLength ^ (mTotalContentLength >>> 32));
        result = 31 * result + (int) (mBlockSize ^ (mBlockSize >>> 32));
        result = 31 * result + mBlockPosition;
        result = 31 * result + (int) (mStartPosition ^ (mStartPosition >>> 32));
        result = 31 * result + (int) (mEndPosition ^ (mEndPosition >>> 32));
        result = 31 * result + (int) (mContentRange ^ (mContentRange >>> 32));
        result = 31 * result + mUrl.hashCode();
        result = 31 * result + (mPath != null ? mPath.hashCode() : 0);
        return result;
    }

    @Override
    protected DownloadFileRequest clone() throws CloneNotSupportedException {
        return (DownloadFileRequest) super.clone();
    }

    @Override
    public String toString() {
        return "from " + mStartPosition +
                " to " + mEndPosition +
                " > " + mCurrentLength +
                " " + mStatus;
    }

    public long getContentRange() {
        return mContentRange;
    }

    public void setContentRange(long contentRange) {
        this.mContentRange = contentRange;
    }

    public void addRetryCount() {
        mRetryCount++;
    }

    public boolean shouldRetry() {
        return mRetryCount < MAX_RETRY_COUNT;
    }

    public long getCurrentLength() {
        return mCurrentLength;
    }

    public void setCurrentLength(long currentLength) {
        mCurrentLength = currentLength;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public long getTotalContentLength() {
        return mTotalContentLength;
    }

    public void setTotalContentLength(long totalContentLength) {
        this.mTotalContentLength = totalContentLength;
    }

    public long getBlockSize() {
        return mBlockSize;
    }

    public void setBlockSize(long blockSize) {
        this.mBlockSize = blockSize;
    }

    public int getBlockPosition() {
        return mBlockPosition;
    }

    public void setBlockPosition(int blockPosition) {
        this.mBlockPosition = blockPosition;
    }

    public long getStartPosition() {
        return mStartPosition;
    }

    public void setStartPosition(long startPosition) {
        this.mStartPosition = startPosition;
    }

    public long getEndPosition() {
        return mEndPosition;
    }

    public void setEndPosition(long endPosition) {
        this.mEndPosition = endPosition;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        this.mProgress = progress;
    }

    public DownloadStatus getStatus() {
        return mStatus;
    }

    public void setStatus(DownloadStatus status) {
        this.mStatus = status;
    }
}
