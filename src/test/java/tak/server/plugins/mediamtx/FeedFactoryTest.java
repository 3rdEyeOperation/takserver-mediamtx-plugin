/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tak.server.plugins.mediamtx.model.Feed;

class FeedFactoryTest {

    private final FeedFactory.StreamProfile profile =
            new FeedFactory.StreamProfile("rtsp", "video.example.com", 8554);

    @Test
    void mapsPathsToFeedsAndFiltersNotReadyWhenRequested() {
        MediaMtxPath ready = new MediaMtxPath();
        ready.setName("camera1");
        ready.setReady(true);

        MediaMtxPath idle = new MediaMtxPath();
        idle.setName("camera2");
        idle.setReady(false);

        FeedFactory factory = new FeedFactory(profile, "UNCLASSIFIED", true);
        List<Feed> feeds = factory.toFeeds(Arrays.asList(ready, idle));
        assertEquals(1, feeds.size(), "only ready paths should be published");

        Feed f = feeds.get(0);
        assertEquals("camera1", f.getAlias());
        assertEquals("rtsp", f.getProtocol());
        assertEquals("video.example.com", f.getAddress());
        assertEquals("8554", f.getPort());
        assertEquals("/camera1", f.getPath());
        assertEquals("UNCLASSIFIED", f.getClassification());
        assertTrue(f.getActive());
        assertNotNull(f.getUuid());
    }

    @Test
    void uuidIsDeterministicForSamePath() {
        MediaMtxPath p = new MediaMtxPath();
        p.setName("camera1");
        p.setReady(true);

        FeedFactory factory = new FeedFactory(profile, null, false);
        String first = factory.toFeed(p).getUuid();
        String second = factory.toFeed(p).getUuid();
        assertEquals(first, second, "same path must always produce the same UID");
    }

    @Test
    void includesNotReadyPathsWhenOnlyReadyDisabled() {
        MediaMtxPath idle = new MediaMtxPath();
        idle.setName("camera2");
        idle.setReady(false);

        FeedFactory factory = new FeedFactory(profile, null, false);
        List<Feed> feeds = factory.toFeeds(java.util.Collections.singletonList(idle));
        assertEquals(1, feeds.size());
        // Inactive paths still get published, but are marked inactive on the feed.
        assertEquals(false, feeds.get(0).getActive());
    }

    @Test
    void skipsPathsWithoutName() {
        MediaMtxPath blank = new MediaMtxPath();
        blank.setReady(true);
        FeedFactory factory = new FeedFactory(profile, null, false);
        assertTrue(factory.toFeeds(java.util.Collections.singletonList(blank)).isEmpty());
    }
}
