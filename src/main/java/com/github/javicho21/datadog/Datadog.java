package com.github.javicho21.datadog;

import com.github.javicho21.rabbitmq.Payload;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.TimeUnit;

/**
 * Publishes data to Datadog.
 *
 * @author aaronzhang
 */
public class Datadog implements Observer {

    private final String apiKey;
    private final URL url;

    /**
     * Connects to Datadog using the given API key.
     *
     * @param apiKey API key
     * @throws IOException if invalid API key
     */
    public Datadog(String apiKey) throws IOException {
        this.apiKey = apiKey;
        url = new URL(
            "https://app.datadoghq.com/api/v1/series?api_key=" + apiKey);
    }

    @Override
    public void update(Observable o, Object arg) {
        if (!(arg instanceof Payload)) {
            throw new IllegalArgumentException(
                "Datadog must be updated with payload");
        }
        Payload payload = (Payload) arg;
        // Format payload data for post request
        long timestamp = TimeUnit.SECONDS.convert(
            payload.getTimestampValue(), payload.getTimestampUnit());
        double value;
        try {
            value = Double.parseDouble(
                payload.getFields().values().iterator().next().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return;
        }
        StringBuilder tagsBuilder = new StringBuilder().append('[');
        payload.getTags().forEach((t, v) -> {
            tagsBuilder
                .append('"')
                .append(t)
                .append(':')
                .append(v)
                .append('"')
                .append(',');
        });
        if (tagsBuilder.charAt(tagsBuilder.length() - 1) == ',') {
            tagsBuilder.deleteCharAt(tagsBuilder.length() - 1);
        }
        String tags = tagsBuilder.append(']').toString();
        String body = String.format("{\"series\" :\n"
            + "[{\"metric\":\"%s\",\n"
            + "\"points\":[[%d, %f]],\n"
            + "\"type\":\"gauge\",\n"
            + "\"tags\":%s}\n"
            + "]\n"
            + "}", payload.getMetric(), timestamp, value, tags);
        // Make post request
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            try (DataOutputStream out =
                new DataOutputStream(conn.getOutputStream())) {
                out.write(body.getBytes());
            }
            conn.getResponseCode();
            conn.getResponseMessage();
        } catch (IOException e) {
            // Replace with logging
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return String.format("[Datadog: apiKey=%s]", apiKey);
    }

    public String getApiKey() {
        return apiKey;
    }
}
