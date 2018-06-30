package eu.interiot.intermw.bridge.example;

/**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union’s Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2016-2018, by (Author's company of this file):
 * - XLAB d.o.o.
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Service;

import java.net.URL;
import java.util.*;

public class ExamplePlatformEmulator {
    private final Logger logger = LoggerFactory.getLogger(ExamplePlatformEmulator.class);
    private static final int EMULATOR_PORT = 4568;
    private Set<String> managedDevices = new HashSet<>();
    private Map<String, Subscription> subscriptions = new HashMap<>();
    private Map<String, Thread> subscriptionThreads = new HashMap<>();
    private Service spark;

    public ExamplePlatformEmulator() {
    }

    public void start() throws Exception {
        logger.debug("ExamplePlatformEmulator is initializing...");

        spark = Service.ignite()
                .port(EMULATOR_PORT);

        setUpListeners();

        logger.info("ExamplePlatformEmulator is listening on port {}.", EMULATOR_PORT);
    }

    private void setUpListeners() {
        spark.post("/register-platform", (request, response) -> {
            logger.debug("Received Register-Platform request.");
            try {
                String platformId = request.queryParams("platformId");
                logger.debug("Platform {} has been registered with Inter MW.", platformId);
                response.status(204);
                return "";

            } catch (Exception e) {
                logger.error("Failed to register platform: " + e.getMessage(), e);
                response.status(500);
                return e.getMessage();
            }
        });

        spark.post("/create-device", (request, response) -> {
            try {
                logger.debug("Received Create-Device request.");
                String deviceId = request.queryParams("deviceId");
                managedDevices.add(deviceId);
                logger.debug("Started to manage device {}.", deviceId);
                response.status(204);
                return "";

            } catch (Exception e) {
                logger.error("Failed to create device: " + e.getMessage(), e);
                response.status(500);
                return e.getMessage();
            }
        });

        spark.post("/subscribe", (request, response) -> {
            logger.debug("Received Subscribe request.");
            try {
                String callbackUrl = request.queryParams("callbackUrl");
                String conversationId = request.queryParams("conversationId");
                String deviceIdsString = request.queryParams("deviceIds");
                if (callbackUrl == null || deviceIdsString == null || conversationId == null ||
                        !deviceIdsString.startsWith("[") || !deviceIdsString.endsWith("]")) {
                    response.status(400);
                    return "Invalid request.";
                }
                List<String> deviceIds = Arrays.asList(deviceIdsString.substring(1, deviceIdsString.length() - 1).split(",\\s*"));

                if (subscriptions.containsKey(conversationId)) {
                    response.status(409);
                    return "Already subscribed.";
                }

                Subscription subscription = new Subscription(deviceIds, callbackUrl);
                subscriptions.put(conversationId, subscription);

                SubscriptionWorker worker = new SubscriptionWorker(subscription);
                Thread workerThread = new Thread(worker);
                workerThread.start();
                subscriptionThreads.put(conversationId, workerThread);

                logger.debug("Created subscription to devices '{}' with conversationId {}.", deviceIds, conversationId);
                response.status(204);
                return "";

            } catch (Exception e) {
                logger.error("Failed to subscribe: " + e.getMessage(), e);
                response.status(500);
                return e.getMessage();
            }
        });

        spark.post("/unsubscribe", (request, response) -> {
            logger.debug("Received Unsubscribe request.");
            try {
                String conversationId = request.queryParams("conversationId");
                Subscription subscription = subscriptions.get(conversationId);
                if (subscription == null) {
                    response.status(400);
                    return "Subscription doesn't exist.";
                }

                Thread thread = subscriptionThreads.get(conversationId);
                thread.interrupt();
                subscriptions.remove(conversationId);

                logger.debug("Subscription {} has been canceled.", conversationId);
                response.status(204);
                return "";

            } catch (Exception e) {
                logger.error("Failed to unsubscribe: " + e.getMessage(), e);
                response.status(500);
                return e.getMessage();
            }
        });

        spark.post("/unregister-platform", (request, response) -> {
            logger.debug("Received Unregister-Platform request.");
            try {
                String platformId = request.queryParams("platformId");

                Iterator<String> iterator = subscriptionThreads.keySet().iterator();
                while (iterator.hasNext()) {
                    String conversationId = iterator.next();
                    Thread thread = subscriptionThreads.get(conversationId);
                    thread.interrupt();
                    iterator.remove();
                }

                subscriptions.clear();

                logger.debug("Platform {} has been unregistered from Inter MW.", platformId);
                response.status(204);
                return "";

            } catch (Exception e) {
                logger.error("Failed to unregister platform: " + e.getMessage(), e);
                response.status(500);
                return e.getMessage();
            }
        });
    }

    public void stop() {
        spark.stop();
        for (Thread thread : subscriptionThreads.values()) {
            thread.interrupt();
        }
        logger.debug("ExamplePlatformEmulator has stopped.");
    }

    public static void main(String[] args) throws Exception {
        new ExamplePlatformEmulator().start();
    }

    class SubscriptionWorker implements Runnable {
        private String observationN3Template;
        private Subscription subscription;

        public SubscriptionWorker(Subscription subscription) throws Exception {
            this.subscription = subscription;
            URL url = Resources.getResource("example-sensor-observation.n3");
            observationN3Template = Resources.toString(url, Charsets.UTF_8);
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return;
            }
            HttpClient httpClient = HttpClientBuilder.create().build();

            while (!Thread.interrupted()) {
                for (String deviceId : subscription.getDeviceIds()) {
                    try {
                        HttpPost httpPost = new HttpPost(subscription.getCallbackUrl());
                        String observationN3 = observationN3Template
                                .replaceAll("%TIMESTAMP%", Long.toString(new Date().getTime()))
                                .replaceAll("%DEVICE_ID%", deviceId);
                        HttpEntity httpEntity = new StringEntity(observationN3, ContentType.getByMimeType("text/n3"));
                        httpPost.setEntity(httpEntity);
                        HttpResponse response = httpClient.execute(httpPost);
                        if (response.getStatusLine().getStatusCode() != 204) {
                            throw new Exception("Invalid response from the Inter MW: " + response.getStatusLine());
                        }
                        logger.debug("Observation for the device {} has been sent to {}.",
                                deviceId, subscription.getCallbackUrl());

                    } catch (Exception e) {
                        logger.error(String.format("Failed to send observation for device %s to %s: %s",
                                deviceId, subscription.getCallbackUrl(), e.getMessage()));
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    static class Subscription {
        private List<String> deviceIds;
        private String callbackUrl;

        public Subscription(List<String> deviceIds, String callbackUrl) {
            this.deviceIds = deviceIds;
            this.callbackUrl = callbackUrl;
        }

        public String getCallbackUrl() {
            return callbackUrl;
        }

        public List<String> getDeviceIds() {
            return deviceIds;
        }
    }
}