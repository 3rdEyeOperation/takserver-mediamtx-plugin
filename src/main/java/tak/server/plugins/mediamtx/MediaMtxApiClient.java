/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Thin client for the MediaMTX HTTP API. Currently it only needs to enumerate
 * stream paths via {@code GET /v3/paths/list}, transparently paging through
 * results.
 */
public class MediaMtxApiClient {

    private static final Logger logger = LoggerFactory.getLogger(MediaMtxApiClient.class);

    /** Maximum pages to fetch before giving up, as a safety guard. */
    private static final int MAX_PAGES = 100;

    private final URI baseUri;
    private final String authHeader;
    private final int itemsPerPage;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;

    public MediaMtxApiClient(String baseUrl, String username, String password,
                             int itemsPerPage, Duration requestTimeout) {
        Objects.requireNonNull(baseUrl, "baseUrl");
        // Normalise: strip trailing slash so we can append paths cleanly.
        String trimmed = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.baseUri = URI.create(trimmed);
        this.itemsPerPage = itemsPerPage > 0 ? itemsPerPage : 100;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        if (username != null && !username.isEmpty()) {
            String creds = username + ":" + (password != null ? password : "");
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        } else {
            this.authHeader = null;
        }
    }

    /**
     * Fetches every page of {@code /v3/paths/list} and returns the aggregated
     * list. Network failures are logged and surfaced as
     * {@link IOException} so the scheduler can decide whether to retry on the
     * next tick.
     */
    public List<MediaMtxPath> listPaths() throws IOException, InterruptedException {
        List<MediaMtxPath> all = new ArrayList<>();
        int page = 0;
        long pageCount = 1;
        while (page < pageCount && page < MAX_PAGES) {
            URI uri = URI.create(baseUri + "/v3/paths/list?page=" + page
                    + "&itemsPerPage=" + itemsPerPage);
            HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(30))
                    .header("Accept", "application/json")
                    .GET();
            if (authHeader != null) {
                b.header("Authorization", authHeader);
            }
            HttpResponse<byte[]> resp;
            try {
                resp = httpClient.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException e) {
                logger.warn("MediaMTX request failed: {}", e.toString());
                throw e;
            }
            if (resp.statusCode() / 100 != 2) {
                throw new IOException("MediaMTX returned HTTP " + resp.statusCode()
                        + " for " + uri);
            }
            MediaMtxPathList list = mapper.readValue(resp.body(), MediaMtxPathList.class);
            if (list.getItems() != null) {
                all.addAll(list.getItems());
            }
            pageCount = Math.max(list.getPageCount(), 1);
            page++;
        }
        return Collections.unmodifiableList(all);
    }

    /** Visible for testing. */
    URI getBaseUri() {
        return baseUri;
    }
}
