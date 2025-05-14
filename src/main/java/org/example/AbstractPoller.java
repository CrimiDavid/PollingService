package org.example;

import org.json.JSONObject;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPoller implements Runnable {
    protected final ConnectBasicExample client;
    protected final ScheduledExecutorService scheduler;
    protected final int pollIntervalSeconds;
    protected ScheduledFuture<?> scheduledFuture;  // Track the scheduled task

    protected AbstractPoller(ConnectBasicExample client, ScheduledExecutorService scheduler, int pollIntervalSeconds) {
        this.client = client;
        this.scheduler = scheduler;
        this.pollIntervalSeconds = pollIntervalSeconds;
    }

    public void startPolling() {
        scheduledFuture = scheduler.scheduleAtFixedRate(
                this, 0, pollIntervalSeconds, TimeUnit.SECONDS);
    }

    public void stopPolling() {
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(false);  // Don't interrupt if running
        }
    }

    // Helper method to send data to Redis
    protected void sendToRedis(String key, JSONObject data) {
        client.sendToRedis(key, data);
    }
}