package org.example;

import org.json.JSONObject;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class EventInfoPoller extends AbstractPoller {
    private final AtomicBoolean fightPollerStarted = new AtomicBoolean(false);
    private final AtomicReference<FightInfoPoller> fightPollerRef = new AtomicReference<>(null);

    public EventInfoPoller(ConnectBasicExample client, ScheduledExecutorService scheduler, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
    }

    @Override
    public void run() {
        try {
            // Get event info and send to Redis
            JSONObject eventInfo = client.testEndpoint();
            sendToRedis("EventInfo", eventInfo);

            Optional<Integer> eventIdOpt = ConnectBasicExample.EventId.get();
            Optional<String> eventStatusOpt = ConnectBasicExample.EventStatus.get();

            // Manage FightInfoPoller based on event status
            if (eventIdOpt.isPresent() && eventStatusOpt.isPresent()) {
                int eventId = eventIdOpt.get();
                String status = eventStatusOpt.get();

                if (status.equals("Live")) {
                    // Start the FightInfoPoller if not already started
                    if (fightPollerStarted.compareAndSet(false, true)) {
                        System.out.println("Starting fight poller for event: " + eventId);
                        FightInfoPoller fightPoller = new FightInfoPoller(client, scheduler, 10);
                        fightPollerRef.set(fightPoller);
                        fightPoller.startPolling();
                    }
                } else{
                    // Stop the FightInfoPoller if it was started
                    if (fightPollerStarted.compareAndSet(true, false)) {
                        FightInfoPoller fightPoller = fightPollerRef.getAndSet(null);
                        if (fightPoller != null) {
                            System.out.println("Stopping fight poller for event: " + eventId + " (status: Final)");
                            fightPoller.stopPolling();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}