package org.example;

import io.github.cdimascio.dotenv.Dotenv;
import org.json.JSONObject;
import redis.clients.jedis.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Connections {
    public static AtomicReference<Optional<Integer>> EventId = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<String>> EventStatus = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<Integer>> FightId = new AtomicReference<>(Optional.empty());

    private static final JedisPool jedisPool;
    private static final Dotenv dotenv = Dotenv.load();  // Load once and use everywhere
    private static final String backendServer = dotenv.get("BACKEND_SEVER");

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setMaxWaitMillis(30000);

        jedisPool = new JedisPool(
                poolConfig,
                dotenv.get("REDIS_HOST"),
                Integer.parseInt(dotenv.get("REDIS_PORT")),
                30000,
                dotenv.get("REDIS_PASSWORD"),
                true
        );
    }

    private Jedis getJedisFromPool() {
        return jedisPool.getResource();
    }

    public JSONObject currentEventEndpoint() throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendServer + "/info/get-current-event"))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(res.body());

        if (res.statusCode() == 200) {
            EventId.set(json.isNull("event_id") ? Optional.empty() : Optional.of(json.getInt("event_id")));
            EventStatus.set(json.isNull("event_status") ? Optional.empty() : Optional.of(json.getString("event_status")));
        }

        if (EventId.get().isPresent() && EventStatus.get().isPresent()) {
            System.out.println(EventStatus.get() + " " + EventId.get());
        } else {
            System.out.println("Event data incomplete or missing");
        }

        return json;
    }

    public JSONObject getFightEndPoint(int eventId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendServer + "/info/get-fight?event_id=" + eventId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        JSONObject json = new JSONObject(res.body());

        if (res.statusCode() == 200) {
            FightId.set(json.isNull("currentFight") ? Optional.empty() : Optional.of(json.getInt("currentFight")));
        }

        System.out.println("Response: " + res.body());
        return json;
    }

    public JSONObject getRoundInfoEndpoint(int eventId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendServer + "/info/get-round-info?event_id=" + eventId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + res.body());
        return new JSONObject(res.body());
    }

    public JSONObject getVotesDataEndpoint(int fightId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(backendServer + "/voting/get-votes-data?fight_id=" + fightId))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + res.body());
        return new JSONObject(res.body());
    }

    public void sendToRedis(String key, JSONObject json) {
        try (Jedis jedis = getJedisFromPool()) {
            jedis.set(key, json.toString());
            jedis.publish(key, json.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFromRedis(String key) {
        try (Jedis jedis = getJedisFromPool()) {
            return jedis.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void closePool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
