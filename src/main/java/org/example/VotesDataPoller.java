package org.example;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class VotesDataPoller extends AbstractPoller {

    public VotesDataPoller(ConnectBasicExample client, ScheduledExecutorService scheduler, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
    }

    @Override
    public void run() {
        try {
            // Check if the event is still live
            Optional<String> eventStatusOpt = ConnectBasicExample.EventStatus.get();
            if (eventStatusOpt.isEmpty() || eventStatusOpt.get().equals("Final")) {
                // Event is over or status unknown, stop polling
                System.out.println("Votes poller stopping: Event status is " +
                        (eventStatusOpt.orElse("unknown")));
                stopPolling();
                return;
            }

            // Check if we have a fight ID to poll
            Optional<Integer> fightIdOpt = ConnectBasicExample.FightId.get();
            if (fightIdOpt.isEmpty()) {
                // No fight ID available, wait for next cycle
                return;
            }

            // Continue with the polling operation
            int fightId = fightIdOpt.get();
            sendToRedis("VotesData", client.testEndpoint4(fightId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}