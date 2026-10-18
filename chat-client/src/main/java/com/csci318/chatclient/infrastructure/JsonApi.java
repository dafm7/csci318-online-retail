package com.csci318.chatclient.infrastructure;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Shared JSON-over-HTTP plumbing for the clients, using the JDK's built-in HttpClient
public abstract class JsonApi {

    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Thrown for network failures (status 0) and non-2xx responses
    public static class ApiException extends Exception {
        private final int status;

        ApiException(int status, String message, Throwable cause) {
            super(message, cause);
            this.status = status;
        }

        public int status() { return status; }
    }

    protected <T> T post(String url, Object body, Class<T> responseType) throws ApiException {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))   // the LLM can take a while, especially when it calls tools
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)))
                    .build();
            return JSON.readValue(send(request), responseType);
        } catch (IOException e) {
            throw new ApiException(0, "bad JSON from " + url, e);
        }
    }

    protected String send(HttpRequest request) throws ApiException {
        try {
            HttpResponse<String> res = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() / 100 != 2) {
                throw new ApiException(res.statusCode(), request.uri() + " returned " + res.statusCode(), null);
            }
            return res.body();
        } catch (IOException e) {
            throw new ApiException(0, "cannot reach " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "interrupted", e);
        }
    }
}
