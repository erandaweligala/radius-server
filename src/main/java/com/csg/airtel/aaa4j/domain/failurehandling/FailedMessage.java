package com.csg.airtel.aaa4j.domain.failurehandling;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;

/**
 * Represents a failed message awaiting retry
 */
public class FailedMessage {
    private final AccountingRequestDto request;
    private final String partitionKey;
    private final long failedAt;
    private int retryCount;

    public FailedMessage(AccountingRequestDto request, String partitionKey) {
        this.request = request;
        this.partitionKey = partitionKey;
        this.failedAt = System.currentTimeMillis();
        this.retryCount = 0;
    }

    public AccountingRequestDto getRequest() {
        return request;
    }

    public String getPartitionKey() {
        return partitionKey;
    }

    public long getFailedAt() {
        return failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
