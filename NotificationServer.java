package com.app.happytails.backend;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class NotificationServer {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        String serviceAccountPath = "rational-photon-380817-firebase-adminsdk-kq8br-ac84eaf87b.json"; // Path to your service account JSON

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/send-notification", new NotificationHandler(serviceAccountPath));
        server.setExecutor(null);
        System.out.println("Notification server started on port " + port);
        server.start();
    }

    static class NotificationHandler implements HttpHandler {
        private final String serviceAccountPath;
        public NotificationHandler(String serviceAccountPath) {
            this.serviceAccountPath = serviceAccountPath;
        }
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                return;
            }
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JSONObject json = new JSONObject(body);
                String fcmToken = json.getString("fcmToken");
                String title = json.getString("title");
                String message = json.getString("body");
                String fcmResponse = FcmV1Sender.sendNotification(serviceAccountPath, fcmToken, title, message);
                byte[] responseBytes = fcmResponse.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                String error = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
                exchange.sendResponseHeaders(500, error.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(error.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
    }
} 