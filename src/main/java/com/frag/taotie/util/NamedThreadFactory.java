package com.frag.taotie.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liuhj
 */
public class NamedThreadFactory implements ThreadFactory {

    private String threadNamePrefix;
    private int threadPriority = 5;
    private boolean daemon = false;
    private ThreadGroup threadGroup;
    private final AtomicInteger threadCount = new AtomicInteger(0);

    public NamedThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(this.getThreadGroup(), r, this.nextThreadName());
        thread.setPriority(this.getThreadPriority());
        thread.setDaemon(this.isDaemon());
        return thread;
    }

    public String getThreadNamePrefix() {
        return this.threadNamePrefix;
    }

    public void setThreadPriority(int threadPriority) {
        this.threadPriority = threadPriority;
    }

    public int getThreadPriority() {
        return this.threadPriority;
    }

    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    public boolean isDaemon() {
        return this.daemon;
    }

    public void setThreadGroupName(String name) {
        this.threadGroup = new ThreadGroup(name);
    }

    public void setThreadGroup(ThreadGroup threadGroup) {
        this.threadGroup = threadGroup;
    }

    public ThreadGroup getThreadGroup() {
        return this.threadGroup;
    }

    protected String nextThreadName() {
        return this.getThreadNamePrefix() + this.threadCount.incrementAndGet();
    }

}
