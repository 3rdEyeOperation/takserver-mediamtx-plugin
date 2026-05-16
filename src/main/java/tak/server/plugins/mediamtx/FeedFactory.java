/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import tak.server.plugins.mediamtx.model.Feed;

/**
 * Converts {@link MediaMtxPath} entries into TAK Server {@link Feed} objects
 * using a fixed {@link StreamProfile} (protocol + host + port + URL template).
 *
 * <p>The deterministic UUID derived from the protocol+host+port+path makes
 * idempotent updates possible: re-publishing the same set of feeds will
 * always produce the same UIDs, so TAK Server treats the publish as an
 * update rather than creating duplicates.
 */
public final class FeedFactory {

    /** UUID namespace for deterministic per-path UIDs (random but constant). */
    private static final UUID NAMESPACE = UUID.fromString("8c2d4cbe-6f8b-4f4f-9b1a-2c0f7a1e7b21");

    private final StreamProfile profile;
    private final String classification;
    private final boolean onlyReady;

    public FeedFactory(StreamProfile profile, String classification, boolean onlyReady) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.classification = classification;
        this.onlyReady = onlyReady;
    }

    public List<Feed> toFeeds(List<MediaMtxPath> paths) {
        List<Feed> feeds = new ArrayList<>();
        if (paths == null) {
            return feeds;
        }
        for (MediaMtxPath p : paths) {
            if (p == null || p.getName() == null || p.getName().isEmpty()) {
                continue;
            }
            if (onlyReady && !p.isReady()) {
                continue;
            }
            feeds.add(toFeed(p));
        }
        return feeds;
    }

    Feed toFeed(MediaMtxPath p) {
        Feed feed = new Feed();
        feed.setAlias(p.getName());
        feed.setProtocol(profile.protocol);
        feed.setAddress(profile.host);
        feed.setPort(profile.port > 0 ? Integer.toString(profile.port) : null);
        // MediaMTX paths do not include a leading slash; TAK Feed expects one.
        String pathSegment = p.getName().startsWith("/") ? p.getName() : "/" + p.getName();
        feed.setPath(pathSegment);
        feed.setActive(p.isReady());
        feed.setClassification(classification);
        feed.setUuid(deterministicUuid(profile.protocol, profile.host, profile.port, pathSegment));
        return feed;
    }

    private static String deterministicUuid(String protocol, String host, int port, String path) {
        String key = (protocol + "://" + host + ":" + port + path).toLowerCase(Locale.ROOT);
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] name = new byte[16 + bytes.length];
        // Prefix with the namespace bytes so a stray colliding "key" alone
        // cannot collide with someone else's UUIDv3/v5 namespace.
        long msb = NAMESPACE.getMostSignificantBits();
        long lsb = NAMESPACE.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            name[i] = (byte) (msb >>> (56 - i * 8));
            name[8 + i] = (byte) (lsb >>> (56 - i * 8));
        }
        System.arraycopy(bytes, 0, name, 16, bytes.length);
        return UUID.nameUUIDFromBytes(name).toString();
    }

    /** Static description of where MediaMTX streams should be played from. */
    public static final class StreamProfile {
        public final String protocol;
        public final String host;
        public final int port;

        public StreamProfile(String protocol, String host, int port) {
            this.protocol = Objects.requireNonNull(protocol, "protocol");
            this.host = Objects.requireNonNull(host, "host");
            this.port = port;
        }
    }
}
