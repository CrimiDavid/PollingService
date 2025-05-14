package org.example;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class RoundInfoPoller extends AbstractPoller {
    private final int eventId;

    public RoundInfoPoller(ConnectBasicExample client, ScheduledExecutorService scheduler, int eventId, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
        this.eventId = eventId;
    }

    @Override
    public void run() {
        try {
            // Check if the event is still live before polling
            Optional<String> eventStatusOpt = ConnectBasicExample.EventStatus.get();
            if (eventStatusOpt.isEmpty() || eventStatusOpt.get().equals("Final")) {
                // Event is over or status unknown, stop polling
                System.out.println("Round poller stopping: Event status is " +
                        (eventStatusOpt.isPresent() ? eventStatusOpt.get() : "unknown"));
                stopPolling();
                return;
            }

            // Continue with the polling operation if event is still active
            sendToRedis("RoundInfo", client.testEndpoint3(eventId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}