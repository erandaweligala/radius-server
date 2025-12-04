package com.csg.airtel.aaa4j.domain.failurehandling;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe, bounded in-memory store for failed messages awaiting retry.
 *
 * Features:
 * - Lock-free concurrent queue for high performance
 * - Bounded capacity to prevent memory issues
 * - FIFO ordering for fair retry processing
 */
@ApplicationScoped
public class FailedMessageStore {

    private static final Logger logger = Logger.getLogger(FailedMessageStore.class);

    @ConfigProperty(name = "accounting.failure.queue.max-size", defaultValue = "10000")
    int maxQueueSize;

    private final ConcurrentLinkedQueue<FailedMessage> queue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger(0);

    /**
     * Offer a failed message to the store.
     * Returns false if the queue is full.
     */
    public boolean offer(AccountingRequestDto request, String partitionKey) {
        if (size.get() >= maxQueueSize) {
            logger.warnf("Failed message store is full (size=%d), rejecting message for session=%s",
                size.get(), request.sessionId());
            return false;
        }

        FailedMessage failedMessage = new FailedMessage(request, partitionKey);
        if (queue.offer(failedMessage)) {
            int currentSize = size.incrementAndGet();
            if (currentSize % 100 == 0) {
                logger.infof("Failed message store size: %d", currentSize);
            }
            return true;
        }

        return false;
    }

    /**
     * Poll a failed message from the store.
     * Returns null if the queue is empty.
     */
    public FailedMessage poll() {
        FailedMessage message = queue.poll();
        if (message != null) {
            size.decrementAndGet();
        }
        return message;
    }

    /**
     * Get the current size of the store
     */
    public int size() {
        return size.get();
    }

    /**
     * Check if the store is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Check if the store is full
     */
    public boolean isFull() {
        return size.get() >= maxQueueSize;
    }

    /**
     * Clear all messages from the store
     */
    public void clear() {
        queue.clear();
        size.set(0);
        logger.info("Failed message store cleared");
    }
}
