package com.sneha.download_manager;

public class RetryPolicy {
    private final int maxRetries;
    private int retryCount;

    public RetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
        this.retryCount = 0;
    }

    public boolean shouldRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetryCount() {
        retryCount++;
    }
}