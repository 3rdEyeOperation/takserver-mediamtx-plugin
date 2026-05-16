/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class MediaMtxApiClientTest {

    private HttpServer server;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v3/paths/list", this::handle);
        server.setExecutor(null);
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange ex) throws IOException {
        int page = parseQueryParamInt(ex.getRequestURI(), "page", 0);
        String json;
        if (page == 0) {
            json = "{\"pageCount\":2,\"itemCount\":3,\"items\":["
                    + "{\"name\":\"camera1\",\"ready\":true},"
                    + "{\"name\":\"camera2\",\"ready\":false}"
                    + "]}";
        } else {
            json = "{\"pageCount\":2,\"itemCount\":3,\"items\":["
                    + "{\"name\":\"camera3\",\"ready\":true}"
                    + "]}";
        }
        requestCount.incrementAndGet();
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static int parseQueryParamInt(URI uri, String key, int dflt) {
        String q = uri.getQuery();
        if (q == null) return dflt;
        for (String part : q.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0 && key.equals(part.substring(0, eq))) {
                try { return Integer.parseInt(part.substring(eq + 1)); }
                catch (NumberFormatException ignored) { return dflt; }
            }
        }
        return dflt;
    }

    @Test
    void listPathsAggregatesAllPages() throws Exception {
        MediaMtxApiClient client = new MediaMtxApiClient(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                null, null, 100, Duration.ofSeconds(5));
        List<MediaMtxPath> paths = client.listPaths();
        assertEquals(2, requestCount.get(), "client should page through results");
        assertEquals(3, paths.size());
        assertEquals("camera1", paths.get(0).getName());
        assertEquals("camera3", paths.get(2).getName());
    }
}
