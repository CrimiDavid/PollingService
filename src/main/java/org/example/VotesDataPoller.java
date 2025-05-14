package org.example;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;

public class VotesDataPoller extends AbstractPoller {

    public VotesDataPoller(Connections client, ScheduledExecutorService scheduler, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
    }

    @Override
    public void run() {
        try {
            // Check if we have a fight ID to poll
            Optional<Integer> fightIdOpt = Connections.FightId.get();
            if (fightIdOpt.isEmpty()) {
                // No fight ID available, wait for next cycle
                return;
            }
            // Continue with the polling operation
            int fightId = fightIdOpt.get();
            sendToRedis("VotesData", client.getVotesDataEndpoint(fightId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}