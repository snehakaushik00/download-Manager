package com.sneha.download_manager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadManager {
    private final Map<String, DownloadTask> downloads = new HashMap<>();
    private final PriorityQueue<DownloadTask> downloadQueue;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public DownloadManager() {
        this.downloadQueue = new PriorityQueue<>(
                Comparator.comparingInt(DownloadTask::getPriority).reversed());
    }

    public void addDownload(DownloadTask downloadTask) {
        downloads.put(downloadTask.getDownloadId(), downloadTask);
        downloadQueue.offer(downloadTask);
    }

    public void startDownload(String downloadId) {
        DownloadTask task = downloads.get(downloadId);
        if (task == null) {
            throw new IllegalArgumentException("Invalid download ID");
        }
        if (task.getStatus() == DownloadStatus.COMPLETED) {
            System.out.println("Download already completed: " + downloadId);
            return;
        }
        if (task.getStatus() == DownloadStatus.PAUSED) {
            task.resume();
        } else if (task.getStatus() == DownloadStatus.CREATED) {
            executorService.submit(task);
        }
    }

    public void pauseDownload(String downloadId) {
        DownloadTask task = downloads.get(downloadId);
        if (task != null) {
            task.pause();
        }
    }

    public void resumeDownload(String downloadId) {
        DownloadTask task = downloads.get(downloadId);
        if (task != null) {
            task.resume();
        }
    }

    public void cancelDownload(String downloadId) {
        DownloadTask task = downloads.get(downloadId);
        if (task != null) {
            task.cancel();
        }
    }

    public DownloadStatus getDownloadStatus(String downloadId) {
        DownloadTask task = downloads.get(downloadId);
        return task != null ? task.getStatus() : null;
    }

    public void shutdown() {
        synchronized (this) {
            while (downloads.values().stream().anyMatch(task -> task.getStatus() == DownloadStatus.IN_PROGRESS ||
                    task.getStatus() == DownloadStatus.PAUSED)) {
                try {
                    wait(1000); // Check for active tasks periodically
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            executorService.shutdown();
            System.out.println("Download manager shut down.");
        }
    }

}
