package org.example;

import org.json.JSONObject;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FightInfoPoller extends AbstractPoller {
    private final AtomicBoolean roundPollerStarted = new AtomicBoolean(false);
    private final AtomicBoolean votesPollerStarted = new AtomicBoolean(false);
    private final AtomicReference<RoundInfoPoller> roundPollerRef = new AtomicReference<>(null);
    private final AtomicReference<VotesDataPoller> votesPollerRef = new AtomicReference<>(null);

    public FightInfoPoller(Connections client, ScheduledExecutorService scheduler, int pollIntervalSeconds) {
        super(client, scheduler, pollIntervalSeconds);
    }

    @Override
    public void run() {
        try {
            // Check if we should even be running
            Optional<String> eventStatusOpt = Connections.EventStatus.get();
            if (eventStatusOpt.isPresent() && eventStatusOpt.get().equals("Final")) {
                // Event is over, stop this poller and its children
                stopAllPollers();
                return;
            }

            Optional<Integer> eventIdOpt = Connections.EventId.get();
            if (eventIdOpt.isEmpty()) return;

            int eventId = eventIdOpt.get();
            JSONObject fightInfo = client.getFightEndPoint(eventId);
            sendToRedis("FightInfo", fightInfo);

            Optional<Integer> fightIdOpt = Connections.FightId.get();
            if (fightIdOpt.isPresent()) {
                int fightId = fightIdOpt.get();

                // Start round poller if not already started
                if (roundPollerStarted.compareAndSet(false, true)) {
                    System.out.println("Starting round poller for fight: " + fightId);
                    RoundInfoPoller roundPoller = new RoundInfoPoller(client, scheduler, eventId, 9);
                    roundPollerRef.set(roundPoller);
                    roundPoller.startPolling();
                }

                // Start votes poller if not already started
                if (votesPollerStarted.compareAndSet(false, true)) {
                    System.out.println("Starting votes poller for fight: " + fightId);
                    VotesDataPoller votesPoller = new VotesDataPoller(client, scheduler, 1);
                    votesPollerRef.set(votesPoller);
                    votesPoller.startPolling();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Stop all child pollers and mark them as stopped
    private void stopAllPollers() {
        if (roundPollerStarted.compareAndSet(true, false)) {
            RoundInfoPoller roundPoller = roundPollerRef.getAndSet(null);
            if (roundPoller != null) {
                System.out.println("Stopping round poller (event status: Final)");
                roundPoller.stopPolling();
            }
        }

        if (votesPollerStarted.compareAndSet(true, false)) {
            VotesDataPoller votesPoller = votesPollerRef.getAndSet(null);
            if (votesPoller != null) {
                System.out.println("Stopping votes poller (event status: Final)");
                votesPoller.stopPolling();
            }
        }
    }

    // Public method to stop this poller and all child pollers
    @Override
    public void stopPolling() {
        stopAllPollers();
        super.stopPolling();
    }
}