package org.example;

import org.json.JSONObject;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.json.Path2;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.ServerSocketChannel;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

public class ConnectBasicExample {
    public static AtomicReference<Optional<Integer>> EventId = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<String>> EventStatus = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<Integer>> FightId = new AtomicReference<>(Optional.empty());

    private JSONObject test;
    private JedisClientConfig config;
    private UnifiedJedis jedis;

    public ConnectBasicExample() {
        config = DefaultJedisClientConfig.builder()
                .user("default")
                .password("v9QrT2nzxM0qDAv6Mw2RjTj2WNoqAP43")
                .build();

        jedis = new UnifiedJedis(
                new HostAndPort("redis-19041.c14.us-east-1-2.ec2.redns.redis-cloud.com", 19041),
                config
        );
    }

    public JSONObject testEndpoint() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/info/get-current-event"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(res.body());
        if (res.statusCode() == 200) {
            if (!json.isNull("event_id")) {
                EventId.set(Optional.of(json.getInt("event_id")));
            } else {
                EventId.set(Optional.empty());
            }

            if (!json.isNull("event_status")) {
                EventStatus.set(Optional.of(json.getString("event_status")));
            } else {
                EventStatus.set(Optional.empty());
            }
        }

        if (EventId.get().isPresent() && EventStatus.get().isPresent()) {
            System.out.println(EventStatus.get() + " " + EventId.get());
        } else {
            System.out.println("Event data incomplete or missing");
        }

        return json;
    }

    public JSONObject testEndpoint2(int eventId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/info/get-fight?event_id=" + eventId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());

        JSONObject json = new JSONObject(res.body());
        if (res.statusCode() == 200) {
            if (!json.isNull("fightId")) {
                FightId.set(Optional.of(json.getInt("fightId")));
            } else {
                FightId.set(Optional.empty());
            }
        }

        System.out.println("Response: " + res.body());
        return json;
    }

    public JSONObject testEndpoint3(int eventId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/info/get-round-info?event_id=" + eventId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + res.body());
        return new JSONObject(res.body());
    }

    public JSONObject testEndpoint4(int fightId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8000/voting/get-votes-data?fight_id="+fightId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + res.body());

        return new JSONObject(res.body());
    }

    public void run(String key, JSONObject json) {
        try {
            jedis.jsonSet(key, json);
        } catch (Exception e) {
            jedis.close();
            e.printStackTrace();
        }
    }
}