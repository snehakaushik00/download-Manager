package com.sneha.download_manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadTask implements Runnable {
    private final String downloadId;
    private final URL downloadUrl;
    private final String destinationPath;
    private volatile DownloadStatus status;
    private final int chunks;
    private int progress;
    private final int priority;
    private long bytesDownloaded = 0;
    private long totalBytes = 0;
    private final RetryPolicy retryPolicy;
    private ExecutorService chunkExecutor;

    private final List<DownloadProgressListener> listeners = new ArrayList<>();

    private volatile boolean isPaused = false;

    public DownloadTask(String downloadId, URL downloadUrl, String destinationPath, int priority, int chunks,
            RetryPolicy retryPolicy) {
        this.downloadId = downloadId;
        this.downloadUrl = downloadUrl;
        this.destinationPath = destinationPath;
        this.priority = priority;
        this.status = DownloadStatus.CREATED;
        this.retryPolicy = retryPolicy;
        this.chunks = chunks;
    }

    public void addListener(DownloadProgressListener listener) {
        listeners.add(listener);
    }

    private void notifyProgress() {
        for (DownloadProgressListener listener : listeners) {
            listener.onProgressUpdate(downloadId, progress);
        }
    }

    private void notifyCompletion() {
        for (DownloadProgressListener listener : listeners) {
            listener.onComplete(downloadId);
        }
    }

    private void notifyFailure(Exception e) {
        for (DownloadProgressListener listener : listeners) {
            listener.onFailure(downloadId, e);
        }
    }

    public String getDownloadId() {
        return downloadId;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public int getPriority() {
        return priority;
    }

    public synchronized void pause() {
        isPaused = true;
        status = DownloadStatus.PAUSED;
    }

    public synchronized void resume() {
        isPaused = false;
        status = DownloadStatus.IN_PROGRESS;
        notifyAll(); // Resume the download logic
    }

    public void cancel() {
        status = DownloadStatus.CANCELLED;
    }

    @Override
    public void run() {
        if (status == DownloadStatus.CANCELLED)
            return;

        status = DownloadStatus.IN_PROGRESS;
        chunkExecutor = Executors.newFixedThreadPool(chunks);

        try {
            HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
            connection.setRequestMethod("HEAD");
            long fileSize = connection.getContentLengthLong();

            long chunkSize = fileSize / chunks;
            long start, end;

            CountDownLatch latch = new CountDownLatch(chunks);
            for (int i = 0; i < chunks; i++) {
                int chunkIdx = i;
                start = i * chunkSize;
                end = (i == chunks - 1) ? fileSize - 1 : (start + chunkSize - 1);

                long finalStart = start;
                long finalEnd = end;

                chunkExecutor.submit(() -> {
                    try {
                        downloadChunk(finalStart, finalEnd, chunkIdx, downloadId);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                        status = DownloadStatus.FAILED;
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            if (status == DownloadStatus.IN_PROGRESS) {
                mergeChunks();
                status = DownloadStatus.COMPLETED;
                System.out.println("Download " + downloadId + " completed.");
            }

        } catch (IOException | InterruptedException e) {
            status = DownloadStatus.FAILED;
            e.printStackTrace();
        } finally {
            if (chunkExecutor != null) {
                chunkExecutor.shutdown();
                System.out.println("Chunk Executor Completed.");
            }
        }

        // try (InputStream in = downloadUrl.openStream();
        // FileOutputStream out = new FileOutputStream(destinationPath)) {

        // totalBytes = downloadUrl.openConnection().getContentLengthLong();
        // byte[] buffer = new byte[4096];
        // int bytesRead;

        // while ((bytesRead = in.read(buffer)) != -1 && status ==
        // DownloadStatus.IN_PROGRESS) {
        // synchronized (this) {
        // while (isPaused) {
        // this.wait(); // Pause the thread until resume is called
        // }
        // }
        // out.write(buffer, 0, bytesRead);
        // bytesDownloaded += bytesRead;
        // progress = (int) ((bytesDownloaded * 100) / totalBytes);
        // System.out.println("Download ID: " + downloadId + " Progress: " + progress +
        // "%");
        // notifyProgress();
        // }

        // if (status == DownloadStatus.IN_PROGRESS) {
        // status = DownloadStatus.COMPLETED;
        // notifyCompletion();
        // System.out.println("Download ID: " + downloadId + " Completed!");
        // }
        // } catch (IOException | InterruptedException e) {
        // status = DownloadStatus.FAILED;
        // notifyFailure(e);
        // System.out.println("Download ID: " + downloadId + " Failed!");
        // if (retryPolicy.shouldRetry()) {
        // retryPolicy.incrementRetryCount();
        // System.out.println("Retrying download: " + downloadId);
        // run(); // Retry logic
        // }
        // }
    }

    private void downloadChunk(long start, long end, int chunkIndex, String downloadId) throws IOException, InterruptedException {
        HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
        connection.connect();

        File chunkFile = new File(destinationPath + ".part" + chunkIndex);
        try (InputStream in = connection.getInputStream();
                FileOutputStream out = new FileOutputStream(chunkFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                synchronized (this) {
                    while (isPaused) {
                        wait();
                    }
                }
                out.write(buffer, 0, bytesRead);
            }

            if (isPaused) {
                System.out.println("Chunk " + chunkIndex + " of download task " + downloadId + " paused.");
            } else {
                System.out.println("Chunk " + chunkIndex + " of download task " + downloadId + " downloaded.");
            }
        }
    }

    private void mergeChunks() throws IOException {
        try (FileOutputStream out = new FileOutputStream(destinationPath)) {
            for (int i = 0; i < chunks; i++) {
                File chunkFile = new File(destinationPath + ".part" + i);
                try (FileInputStream in = new FileInputStream(chunkFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                chunkFile.delete(); // Delete chunk file after merging
            }
        }
    }

}
