package org.example;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    // Flags to track which pollers are already running
    private static final AtomicBoolean fightPollerStarted = new AtomicBoolean(false);
    private static final AtomicBoolean roundPollerStarted = new AtomicBoolean(false);
    private static final AtomicBoolean votesPollerStarted = new AtomicBoolean(false);

    public static void main(String[] args) {
        ConnectBasicExample c = new ConnectBasicExample();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        Runnable pollEventInfo = () -> {
            try {
                c.run("EventInfo", c.testEndpoint());

                Optional<Integer> eventIdOpt = ConnectBasicExample.EventId.get();
                Optional<String> eventStatusOpt = ConnectBasicExample.EventStatus.get();

                if (eventIdOpt.isPresent() &&
                        eventStatusOpt.isPresent() &&
                        eventStatusOpt.get().equals("Live")) {

                    if (fightPollerStarted.compareAndSet(false, true)) {
                        System.out.println("Starting fight poller for event: " + eventIdOpt.get());
                        startFightPoller(c, scheduler);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(pollEventInfo, 0, 5, TimeUnit.SECONDS);
    }

    private static void startFightPoller(ConnectBasicExample c, ScheduledExecutorService scheduler) {
        Runnable pollFightInfo = () -> {
            try {
                Optional<Integer> eventIdOpt = ConnectBasicExample.EventId.get();
                if (eventIdOpt.isEmpty()) return;

                int eventId = eventIdOpt.get();
                c.run("FightInfo", c.testEndpoint2(eventId));

                Optional<Integer> fightIdOpt = ConnectBasicExample.FightId.get();
                if (fightIdOpt.isPresent()) {
                    int fightId = fightIdOpt.get();

                    if (roundPollerStarted.compareAndSet(false, true)) {
                        System.out.println("Starting round poller for fight: " + fightId);
                        startRoundPoller(c, scheduler, eventId);
                    }

                    if (votesPollerStarted.compareAndSet(false, true)) {
                        System.out.println("Starting votes poller for fight: " + fightId);
                        startVotesPoller(c, scheduler);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(pollFightInfo, 0, 5, TimeUnit.SECONDS);
    }

    private static void startRoundPoller(ConnectBasicExample c, ScheduledExecutorService scheduler, int eventId) {
        Runnable pollRoundInfo = () -> {
            try {
                c.run("RoundInfo", c.testEndpoint3(eventId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(pollRoundInfo, 0, 5, TimeUnit.SECONDS);
    }

    private static void startVotesPoller(ConnectBasicExample c, ScheduledExecutorService scheduler) {
        Runnable pollVotesData = () -> {
            try {
                Optional<Integer> fightIdOpt = ConnectBasicExample.FightId.get();
                if (fightIdOpt.isEmpty()) return;

                int fightId = fightIdOpt.get();
                c.run("VotesData", c.testEndpoint4(fightId));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(pollVotesData, 0, 1, TimeUnit.SECONDS);
    }
}
