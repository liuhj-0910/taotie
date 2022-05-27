package com.frag.taotie;

import com.frag.taotie.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author liuhj
 */
public class TaotieActuator<T> {

    private final Logger log = LoggerFactory.getLogger(TaotieActuator.class);

    private final static int QUEUE_POWER_MIN = 0;
    private final static int QUEUE_POWER_MAX = 10;

    /**
     * 从数据对象T中获取分区键的方法
     */
    private final Function<T, Number> getPartitionKey;

    /**
     * 每个数据队列，每benchCommitNum个数据批量提交一次
     */
    private final int benchCommitNum;

    /**
     * 数据队列容量因子，用来控制每个队列的容量
     * benchCommitNum * queueCapacityFactor = 队列的容量
     */
    private final float queueCapacityFactor;

    /**
     * 批量提交数据的业务方法
     */
    private final Consumer<List<T>> benchCommit;

    /**
     * 数据入队的超时时间，单位为毫秒
     * 超时入队表示系统存在数据积压，会有告警日志。需要调整参数或人工介入。
     */
    private final int offerTimeout;

    /**
     * 数据出队的超时时间，单位为毫秒
     * 超时出队表示数据上报速度慢，属于正常现象，只打印debug日志。
     */
    private final int pollTimeout;

    /**
     * 数据队列数量，为2的queuePower次方
     */
    private final int queueNum;

    /**
     * 处理出队数据的线程数，为2的pollThreadPower次方
     */
    private final int pollThreadNum;

    /**
     * 数据队列只有单队列时，保存结构退化为队列
     */
    private final BlockingQueue<T> singleQueue;

    /**
     * 数据队列为多队列，保存结构为Map
     * key值：业务数据<T>根据lambda表达式getPartitionKey获取业务分区键，与(队列数量 - 1)做&运算的结果
     * value：保存对应分区后业务数据的队列
     */
    private final Map<Integer, BlockingQueue<T>> multipleQueue;

    /**
     * 处理出队数据线程为单线程，用于出队数据的数据结构退化为List
     */
    private final List<BlockingQueue<T>> singlePollThreadList;

    /**
     * 处理出队数据线程为多线程，用于出队数据的数据结构为Map
     * key：multipleQueue.key，与(出队数据线程数 - 1)做&运算的结果
     * value：hash后对应的数据队列集合
     */
    private final Map<Integer, List<BlockingQueue<T>>> pollList;

    /**
     * 处理批量提交数据的线程池
     */
    private final ExecutorService commitThreadPool;

    /**
     * 处理出队数据的线程池
     */
    private final ExecutorService pollThreadPool;

    /**
     * 每个数据队列的容量
     */
    private final int queueCapacity;

    /**
     * @param queuePower      该值为2的幂数，计算2的幂后的结果为内部保存数据队列的数量。该数量与业务分库分表的表数量一致。
     *                        业务数据<T>根据lambda表达式getPartitionKey获取业务分区键，与(队列数量 - 1)做&运算后，保存到对应的数据队列中。
     *                        例：分库分表1024张表，对应1024的队列，则queuePower=10
     * @param benchCommitNum  批量提交数量
     * @param pollThreadPower 该值为2的幂数，计算2的幂后的结果为用于从数据队列中出队数据的线程数
     *                        例：queuePower=10表示有1024个数据队列，pollThreadNum=2表示处理出队数据的线程为4个，则初始化时为每个线程分配固定256个队列
     * @param commitThreadNum 用于处理批量提交数据的线程数量，特别提醒该值不是2的幂数
     *                        每个pollThread在每个数据队列出队benchCommitNum后，或pollTimeout毫秒后没出队数据，则调用commitThread异步批量提交
     * @param offerTimeout    数据入队的超时时间，单位为毫秒
     * @param pollTimeout     数据出队的超时时间，单位为毫秒
     * @param benchCommit     批量提交数据的业务方法
     * @param getPartitionKey 从数据对象T中获取分区键的方法
     */
    public TaotieActuator(int queuePower, int benchCommitNum, int pollThreadPower, int commitThreadNum, float queueCapacityFactor, int offerTimeout, int pollTimeout, Consumer<List<T>> benchCommit, Function<T, Number> getPartitionKey) {
        if (queuePower < QUEUE_POWER_MIN || queuePower > QUEUE_POWER_MAX) {
            log.error("参数queuePower设置异常 :: {}", queuePower);
            throw new IllegalArgumentException("参数queuePower设置异常");
        }

        if (benchCommitNum < 1) {
            log.error("参数benchCommitNum设置异常 :: {}", benchCommitNum);
            throw new IllegalArgumentException("参数benchCommitNum设置异常");
        }

        // pollThread数不能超过队列数
        if (pollThreadPower < QUEUE_POWER_MIN || pollThreadPower > QUEUE_POWER_MAX || pollThreadPower > queuePower) {
            log.error("参数pollThreadPower设置异常 :: {}", pollThreadPower);
            throw new IllegalArgumentException("参数pollThreadPower设置异常");
        }

        if (commitThreadNum < 1) {
            log.error("参数commitThreadNum设置异常 :: {}", commitThreadNum);
            throw new IllegalArgumentException("参数commitThreadNum设置异常");
        }

        if (null == benchCommit) {
            log.error("参数benchCommit必须设置");
            throw new IllegalArgumentException("参数benchCommit必须设置");
        }

        if (queuePower > 0 && null == getPartitionKey) {
            log.error("多数据队列，参数getPartitionKey必须设置");
            throw new IllegalArgumentException("多数据队列，参数getPartitionKey必须设置");
        }

        this.benchCommitNum = benchCommitNum;
        this.offerTimeout = offerTimeout;
        this.pollTimeout = pollTimeout;
        this.benchCommit = benchCommit;
        this.queueCapacityFactor = queueCapacityFactor;
        this.getPartitionKey = getPartitionKey;

        this.queueNum = 1 << queuePower;
        this.pollThreadNum = 1 << pollThreadPower;
        int pollListCapacity = queueNum >> pollThreadPower;
        this.queueCapacity = ((Float) (this.benchCommitNum * this.queueCapacityFactor)).intValue() + 1;

        // 初始化出队数据结构
        if (pollThreadNum == 1) {
            // pollThreadNum=1，则一个线程处理全部队列，退化为单list
            this.singlePollThreadList = new ArrayList<>(queueNum);
            this.pollList = null;
        } else {
            this.singlePollThreadList = null;
            this.pollList = new ConcurrentHashMap<>(pollThreadNum);

            for (int i = 0; i < pollThreadNum; i++) {
                List<BlockingQueue<T>> list = new ArrayList<BlockingQueue<T>>(pollListCapacity);
                pollList.put(i, list);
            }
        }

        // 初始化数据队列
        if (queueNum == 1) {
            this.singleQueue = newQueue();
            this.multipleQueue = null;
        } else {
            this.singleQueue = null;
            this.multipleQueue = new ConcurrentHashMap<>(queueNum);
            for (int i = 0; i < queueNum; i++) {
                BlockingQueue<T> q = newQueue();
                multipleQueue.put(i, q);

                if (pollThreadNum == 1) {
                    // pollThreadNum=1，则一个线程处理全部队列，退化为单list
                    singlePollThreadList.add(q);
                } else {
                    pollList.get(i & (pollThreadNum - 1)).add(q);
                }
            }
        }

        this.pollThreadPool = Executors.newFixedThreadPool(pollThreadNum, new NamedThreadFactory("pollThread-pool-"));
        this.commitThreadPool = Executors.newFixedThreadPool(commitThreadNum, new NamedThreadFactory("commitThread-pool-"));

        process();
    }

    private BlockingQueue<T> newQueue() {
        return new ArrayBlockingQueue<>(this.queueCapacity);
    }

    public void offer(T data) {
        // 单队列
        if (1 == queueNum) {
            offer(data, this.singleQueue);
        } else {
            BlockingQueue<T> queue = multipleQueue.get((getPartitionKey.apply(data).intValue() & (this.queueNum - 1)));
            offer(data, queue);
        }
    }

    private void offer(T data, BlockingQueue<T> queue) {
        try {
            if (!queue.offer(data, offerTimeout, TimeUnit.MILLISECONDS)) {
                log.warn("{}-taotie处理器队列阻塞 :: {}", Thread.currentThread().getName(), data);
            }
        } catch (InterruptedException e) {
            log.error("{}-taotie处理器offer队列发生InterruptedException :: {}", Thread.currentThread().getName(), data);
        }
    }

    private void process() {
        // 单队列 单线程
        if (1 == queueNum) {
            pollThreadPool.execute(() -> {
                while (true) {
                    processQueue(singleQueue);
                }
            });
        } else {
            if (pollThreadNum == 1) {
                // 多队列 单线程
                pollThreadPool.execute(() -> {
                    while (true) {
                        for (BlockingQueue<T> ts : singlePollThreadList) {
                            processQueue(ts);
                        }
                    }
                });
            } else {
                for (int i = 0; i < pollThreadNum; i++) {
                    List<BlockingQueue<T>> list = pollList.get(i);
                    pollThreadPool.execute(() -> {
                        while (true) {
                            for (BlockingQueue<T> ts : list) {
                                processQueue(ts);
                            }
                        }
                    });
                }
            }
        }
    }

    private void processQueue(BlockingQueue<T> queue) {
        try {
            List<T> list = new ArrayList<>(benchCommitNum);
            long pollStart = System.currentTimeMillis();
            for (int i = 0; i < benchCommitNum; i++) {
                try {
                    T data = queue.poll(pollTimeout, TimeUnit.MILLISECONDS);
                    if (data == null) {
                        log.debug("{}-taotie处理器: {}毫秒没有新的消息，则退出本次循环", Thread.currentThread().getName(), pollTimeout);
                        break;
                    } else {
                        log.trace("{}-taotie处理器: {}", Thread.currentThread().getName(), data);
                    }
                    list.add(data);
                } catch (InterruptedException e) {
                    log.error("{}-taotie处理器: 获取消息中断", Thread.currentThread().getName());
                }
            }
            long pollEnd = System.currentTimeMillis();

            int result = list.size();
            if (result > 0) {
                commitThreadPool.execute(() -> {
                    long commitStart = System.currentTimeMillis();
                    benchCommit.accept(list);
                    long commitEnd = System.currentTimeMillis();
                    log.info("{}-taotie处理器: 成功提交{}条记录，出队耗时{}毫秒，等待线程耗时{}毫秒，insert耗时{}毫秒，总耗时{}毫秒", Thread.currentThread().getName(), result, pollEnd - pollStart, commitStart - pollEnd, commitEnd - commitStart, commitEnd - pollStart);
                });
            }
        } catch (Exception exception) {
            log.error("{}-taotie处理器循环处理异常:: {}", Thread.currentThread().getName(), exception.getMessage());
            throw exception;
        }
    }

    public int getQueueNum() {
        return queueNum;
    }

}
