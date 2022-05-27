package com.frag.taotie;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class TaotieExecutors {

    private final static int OFFER_TIMEOUT_DEFAULT = 50;
    private final static int POLL_TIMEOUT_DEFAULT = 10;

    /**
     * 单队列默认配置
     */
    private final static int SINGLE_QUEUE_BENCH_COMMIT_NUM_DEFAULT = 10000;
    private final static float SINGLE_QUEUE_CAPACITY_FACTOR_DEFAULT = 0.25f;

    /**
     * 多队列默认配置
     */
    private final static float MULTIPLE_QUEUE_CAPACITY_FACTOR_DEFAULT = 1.0f;


    /**
     * 下面是单队列构造器
     */
    public static <T> TaotieActuator singleQueueProcessor(Consumer<List<T>> benchCommit) {
        return singleQueueProcessor(SINGLE_QUEUE_BENCH_COMMIT_NUM_DEFAULT, 1, benchCommit);
    }

    public static <T> TaotieActuator singleQueueProcessor(int benchCommitNum, int commitThreadNum, Consumer<List<T>> benchCommit) {
        return singleQueueProcessor(benchCommitNum, commitThreadNum, SINGLE_QUEUE_CAPACITY_FACTOR_DEFAULT, benchCommit);
    }

    public static <T> TaotieActuator singleQueueProcessor(int benchCommitNum, int commitThreadNum, float queueCapacityFactor, Consumer<List<T>> benchCommit) {
        return singleQueueProcessor(benchCommitNum, commitThreadNum, queueCapacityFactor, OFFER_TIMEOUT_DEFAULT, POLL_TIMEOUT_DEFAULT, benchCommit);
    }

    public static <T> TaotieActuator singleQueueProcessor(int benchCommitNum, int commitThreadNum, float queueCapacityFactor, int offerTimeout, int pollTimeout, Consumer<List<T>> benchCommit) {
        return new TaotieActuator(0, benchCommitNum, 0, commitThreadNum, queueCapacityFactor, offerTimeout, pollTimeout, benchCommit, null);
    }

    /**
     * 下面是多队列构造器
     */
    public static <T> TaotieActuator multipleQueueProcessor(int queuePower, int benchCommitNum, int pollThreadPower, int commitThreadNum, Consumer<List<T>> benchCommit, Function<T, Number> getHashKey) {
        return multipleQueueProcessor(queuePower, benchCommitNum, pollThreadPower, commitThreadNum, MULTIPLE_QUEUE_CAPACITY_FACTOR_DEFAULT, benchCommit, getHashKey);
    }

    public static <T> TaotieActuator multipleQueueProcessor(int queuePower, int benchCommitNum, int pollThreadPower, int commitThreadNum, float queueCapacityFactor, Consumer<List<T>> benchCommit, Function<T, Number> getHashKey) {
        return multipleQueueProcessor(queuePower, benchCommitNum, pollThreadPower, commitThreadNum, queueCapacityFactor, OFFER_TIMEOUT_DEFAULT, POLL_TIMEOUT_DEFAULT, benchCommit, getHashKey);
    }

    public static <T> TaotieActuator multipleQueueProcessor(int queuePower, int benchCommitNum, int pollThreadPower, int commitThreadNum, float queueCapacityFactor, int offerTimeout, int pollTimeout, Consumer<List<T>> benchCommit, Function<T, Number> getHashKey) {
        return new TaotieActuator(queuePower, benchCommitNum, pollThreadPower, commitThreadNum, queueCapacityFactor, offerTimeout, pollTimeout, benchCommit, getHashKey);
    }

}
