package com.spinn3r.activemq;

/**
 *
 */
public class QueueMeta {

    private final String queueName;

    private final long queueSize;

    private final long consumerCount;

    public QueueMeta(String queueName, long queueSize, long consumerCount) {
        this.queueName = queueName;
        this.queueSize = queueSize;
        this.consumerCount = consumerCount;
    }

    public String getQueueName() {
        return queueName;
    }

    public long getQueueSize() {
        return queueSize;
    }

    public long getConsumerCount() {
        return consumerCount;
    }

    @Override
    public String toString() {
        return String.format( "[queueSize:%s consumerCount:%s]", queueSize, consumerCount );
    }

}
