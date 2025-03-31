package com.sneha.download_manager;

public interface DownloadProgressListener {
    void onProgressUpdate(String downloadId, int progress);
    void onComplete(String downloadId);
    void onFailure(String downloadId, Exception e);
}
