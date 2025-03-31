package com.sneha.download_manager;

import java.util.ArrayList;
import java.util.List;

public class ProgressNotifier {
    private final List<DownloadProgressListener> listeners = new ArrayList<>();

    public void registerListener(DownloadProgressListener listener) {
        listeners.add(listener);
    }

    public void notifyProgress(String downloadId, int progress) {
        for (DownloadProgressListener listener : listeners) {
            listener.onProgressUpdate(downloadId, progress);
        }
    }

    public void notifyCompletion(String downloadId) {
        for (DownloadProgressListener listener : listeners) {
            listener.onComplete(downloadId);
        }
    }

    public void notifyFailure(String downloadId, Exception e) {
        for (DownloadProgressListener listener : listeners) {
            listener.onFailure(downloadId, e);
        }
    }
}
