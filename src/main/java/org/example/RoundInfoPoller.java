package org.example;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class RoundInfoPoller extends AbstractPoller {
    private final int eventId;

    public RoundInfoPoller(Connections client, ScheduledExecutorService scheduler, int eventId, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
        this.eventId = eventId;
    }

    @Override
    public void run() {
        try {
            Optional<Integer> fightIdOpt = Connections.FightId.get();
            if (fightIdOpt.isEmpty()) {
                // No fight ID available, wait for next cycle
                return;
            }
            sendToRedis("RoundInfo", client.getRoundInfoEndpoint(eventId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}