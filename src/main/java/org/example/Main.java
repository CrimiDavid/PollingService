package org.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Main {
    public static void main(String[] args) {
        Connections client = new Connections();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

        EventInfoPoller eventInfoPoller = new EventInfoPoller(client, scheduler, 5);
        eventInfoPoller.startPolling();

    }
}