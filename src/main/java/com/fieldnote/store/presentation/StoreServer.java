package com.fieldnote.store.presentation;

import com.fieldnote.store.model.Order;
import com.fieldnote.store.model.Product;
import com.fieldnote.store.repository.InMemoryStoreRepository;
import com.fieldnote.store.repository.MySqlStoreRepository;
import com.fieldnote.store.repository.StoreRepository;
import com.fieldnote.store.service.CatalogService;
import com.fieldnote.store.service.CheckoutService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class StoreServer {
    private static final Path PUBLIC_DIR = Path.of("frontend", "dist");
    private final CatalogService catalog;
    private final CheckoutService checkout;

    private StoreServer() throws SQLException {
        String databaseUrl = System.getenv("DB_URL");
        StoreRepository repository;
        if (databaseUrl == null || databaseUrl.isBlank()) {
            repository = new InMemoryStoreRepository();
            System.out.println("Using in-memory product and order storage.");
        } else {
            repository = new MySqlStoreRepository(databaseUrl, System.getenv("DB_USER"),
                    System.getenv("DB_PASSWORD"));
            System.out.println("Using MySQL product and order storage.");
        }
        catalog = new CatalogService(repository);
        checkout = new CheckoutService(repository);
    }

    public static void main(String[] args) throws IOException, SQLException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        StoreServer app = new StoreServer();
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/products", app::handleProducts);
        server.createContext("/api/orders", app::handleOrders);
        server.createContext("/", app::handleStatic);
        server.start();
        System.out.println("Fieldnote Supply is running at http://localhost:" + port);
    }

    private void handleProducts(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
            return;
        }
        StringBuilder json = new StringBuilder("[\n");
        var products = catalog.listProducts();
        for (int i = 0; i < products.size(); i++) {
            Product product = products.get(i);
            json.append("{\"id\":").append(product.id())
                    .append(",\"name\":\"").append(escape(product.name()))
                    .append("\",\"description\":\"").append(escape(product.description()))
                    .append("\",\"category\":\"").append(escape(product.category()))
                    .append("\",\"priceCents\":").append(product.priceCents())
                    .append(",\"stock\":").append(product.stock())
                    .append(",\"imageUrl\":\"").append(escape(product.imageUrl())).append("\"}");
            if (i < products.size() - 1) json.append(',');
            json.append('\n');
        }
        sendJson(exchange, 200, json.append(']').toString());
    }

    private void handleOrders(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed.\"}");
            return;
        }
        try {
            byte[] body = exchange.getRequestBody().readNBytes(4097);
            if (body.length > 4096) {
                throw new IllegalArgumentException("Your order details are too long.");
            }
            Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
            Order order = checkout.checkout(form.get("name"), form.get("email"), form.get("items"));
            sendJson(exchange, 201, "{\"id\":" + order.id() + ",\"totalCents\":" + order.totalCents() + "}");
        } catch (IllegalArgumentException exception) {
            sendJson(exchange, 400, "{\"error\":\"" + escape(exception.getMessage()) + "\"}");
        }
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            sendJson(exchange, 404, "{\"error\":\"Not found.\"}");
            return;
        }
        String requestPath = exchange.getRequestURI().getPath();
        String file = requestPath.equals("/") ? "index.html" : requestPath.substring(1);
        Path publicDirectory = PUBLIC_DIR.toAbsolutePath().normalize();
        Path target = publicDirectory.resolve(file).normalize();
        if (!target.startsWith(publicDirectory) || !Files.isRegularFile(target)) {
            sendJson(exchange, 404, "{\"error\":\"Not found. Build the React storefront before starting the server.\"}");
            return;
        }
        String contentType = contentType(target);
        byte[] content = Files.readAllBytes(target);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            values.put(key, value);
        }
        return values;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, content.length);
        exchange.getResponseBody().write(content);
        exchange.close();
    }
}