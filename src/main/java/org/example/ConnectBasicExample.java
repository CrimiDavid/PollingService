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

public class ConnectBasicExample {
    public static AtomicReference<Optional<Integer>> EventId = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<String>> EventStatus = new AtomicReference<>(Optional.empty());
    public static AtomicReference<Optional<Integer>> FightId = new AtomicReference<>(Optional.empty());

    // Singleton pattern for JedisPool
    private static final JedisPool jedisPool;

    static {
        // Initialize JedisPool at class loading time
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // Set maximum connections
        poolConfig.setMaxTotal(20);
        // Set maximum idle connections
        poolConfig.setMaxIdle(5);
        // Set minimum idle connections
        poolConfig.setMinIdle(1);
        // Set whether or not to test connections on borrow
        poolConfig.setTestOnBorrow(true);
        // Set maximum wait time for a connection (milliseconds)
        poolConfig.setMaxWaitMillis(30000);
        Dotenv dotenv = Dotenv.load(); // Loads from .env

        // Create the pool with the config, host, port, and auth info
        jedisPool = new JedisPool(
                poolConfig,
                dotenv.get("REDIS_HOST"),
                Integer.parseInt(dotenv.get("REDIS_PORT")),
                30000,
                dotenv.get("REDIS_PASSWORD"),
                true // SSL enabled (required by Upstash)
        );
    }

    // Get a Jedis instance from the pool
    private Jedis getJedisFromPool() {
        return jedisPool.getResource();
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

    public void sendToRedis(String key, JSONObject json) {
        // Use try-with-resources to ensure proper resource cleanup
        try (Jedis jedis = getJedisFromPool()) {
            jedis.set(key, String.valueOf(json));
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

    // Cleanup method to close the pool when your application shuts down
    public static void closePool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}