/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tak.server.plugins.MessageSenderBase;
import tak.server.plugins.ReservedConfigurationException;
import tak.server.plugins.TakServerPlugin;
import tak.server.plugins.mediamtx.model.Feed;
import tak.server.plugins.mediamtx.model.VideoConnections;

/**
 * TAK Server plugin that bridges the MediaMTX HTTP API to TAK Server's
 * video-manager endpoint, so that ATAK clients automatically see every
 * MediaMTX stream as a video feed.
 *
 * <p>On {@link #start()} the plugin schedules a periodic poll of MediaMTX's
 * {@code /v3/paths/list} endpoint and POSTs the resulting set of feeds to
 * the configured TAK Server VCM URL (typically {@code /Marti/vcm}). The
 * plugin extends {@link MessageSenderBase} so that the Plugin Manager picks
 * it up; no CoT messages are actually sent unless future work adds them.
 */
@TakServerPlugin(
        name = "MediaMTX Video Bridge",
        description = "Periodically imports MediaMTX streams as TAK Server video feeds."
)
public class MediaMtxVideoPlugin extends MessageSenderBase {

    private static final Logger logger = LoggerFactory.getLogger(MediaMtxVideoPlugin.class);

    // --- configuration keys (read from conf/plugins/<FQCN>.yaml) ---
    static final String CFG_MEDIAMTX_URL          = "mediamtx.apiUrl";
    static final String CFG_MEDIAMTX_USER         = "mediamtx.username";
    static final String CFG_MEDIAMTX_PASS         = "mediamtx.password";
    static final String CFG_MEDIAMTX_PAGE_SIZE    = "mediamtx.pageSize";
    static final String CFG_MEDIAMTX_ONLY_READY   = "mediamtx.onlyReady";

    static final String CFG_STREAM_PROTOCOL       = "stream.protocol";
    static final String CFG_STREAM_HOST           = "stream.host";
    static final String CFG_STREAM_PORT           = "stream.port";
    static final String CFG_STREAM_CLASSIFICATION = "stream.classification";

    static final String CFG_TAK_VCM_URL           = "takserver.vcmUrl";
    static final String CFG_TAK_USER              = "takserver.username";
    static final String CFG_TAK_PASS              = "takserver.password";
    static final String CFG_TAK_CLIENT_CERT       = "takserver.clientCertPath";
    static final String CFG_TAK_CLIENT_CERT_PW    = "takserver.clientCertPassword";
    static final String CFG_TAK_TRUSTSTORE        = "takserver.trustStorePath";
    static final String CFG_TAK_TRUSTSTORE_PW     = "takserver.trustStorePassword";
    static final String CFG_TAK_TRUST_ALL         = "takserver.trustAll";

    static final String CFG_POLL_INTERVAL_SECONDS = "pollIntervalSeconds";

    /** Default values for the configuration keys above. */
    private static final long DEFAULT_POLL_SECONDS = 30L;

    private ScheduledExecutorService scheduler;
    private MediaMtxApiClient mediaMtxClient;
    private TakServerVideoClient takClient;
    private FeedFactory feedFactory;

    public MediaMtxVideoPlugin() throws ReservedConfigurationException {
        super();
    }

    @Override
    public void start() {
        logger.info("MediaMtxVideoPlugin starting");

        String mediaMtxUrl = stringProp(CFG_MEDIAMTX_URL, "http://127.0.0.1:9997");
        String mediaMtxUser = stringProp(CFG_MEDIAMTX_USER, null);
        String mediaMtxPass = stringProp(CFG_MEDIAMTX_PASS, null);
        int pageSize = intProp(CFG_MEDIAMTX_PAGE_SIZE, 100);
        boolean onlyReady = boolProp(CFG_MEDIAMTX_ONLY_READY, true);

        String protocol = stringProp(CFG_STREAM_PROTOCOL, "rtsp");
        String host = stringProp(CFG_STREAM_HOST, "127.0.0.1");
        int port = intProp(CFG_STREAM_PORT, defaultPortFor(protocol));
        String classification = stringProp(CFG_STREAM_CLASSIFICATION, null);

        String takUrl = stringProp(CFG_TAK_VCM_URL, null);
        if (takUrl == null || takUrl.isEmpty()) {
            logger.error("Required configuration property '{}' is missing; plugin will not run.",
                    CFG_TAK_VCM_URL);
            return;
        }
        String takUser = stringProp(CFG_TAK_USER, null);
        String takPass = stringProp(CFG_TAK_PASS, null);
        String certPath = stringProp(CFG_TAK_CLIENT_CERT, null);
        String certPass = stringProp(CFG_TAK_CLIENT_CERT_PW, null);
        String tsPath   = stringProp(CFG_TAK_TRUSTSTORE, null);
        String tsPass   = stringProp(CFG_TAK_TRUSTSTORE_PW, null);
        boolean trustAll = boolProp(CFG_TAK_TRUST_ALL, false);

        long pollSeconds = longProp(CFG_POLL_INTERVAL_SECONDS, DEFAULT_POLL_SECONDS);

        Duration timeout = Duration.ofSeconds(10);
        this.mediaMtxClient = new MediaMtxApiClient(mediaMtxUrl, mediaMtxUser, mediaMtxPass,
                pageSize, timeout);
        try {
            this.takClient = new TakServerVideoClient(takUrl, takUser, takPass,
                    certPath, certPass, tsPath, tsPass, trustAll, timeout);
        } catch (Exception e) {
            logger.error("Failed to build TAK Server video client: {}", e.toString(), e);
            return;
        }
        this.feedFactory = new FeedFactory(
                new FeedFactory.StreamProfile(protocol, host, port), classification, onlyReady);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(namedDaemonFactory());
        scheduler.scheduleWithFixedDelay(this::syncSafely, 0L, pollSeconds, TimeUnit.SECONDS);
        logger.info("MediaMtxVideoPlugin started: mediamtx={} -> tak={} every {}s",
                mediaMtxUrl, takUrl, pollSeconds);
    }

    @Override
    public void stop() {
        logger.info("MediaMtxVideoPlugin stopping");
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
    }

    /** Runs one sync cycle, swallowing exceptions so the scheduler keeps ticking. */
    void syncSafely() {
        try {
            sync();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.warn("MediaMTX sync cycle failed: {}", e.toString());
            if (logger.isDebugEnabled()) {
                logger.debug("MediaMTX sync stack trace", e);
            }
        }
    }

    private void sync() throws IOException, InterruptedException {
        List<MediaMtxPath> paths = mediaMtxClient.listPaths();
        List<Feed> feeds = feedFactory.toFeeds(paths);
        if (logger.isDebugEnabled()) {
            logger.debug("Publishing {} MediaMTX feed(s) to TAK Server", feeds.size());
        }
        takClient.publish(new VideoConnections(feeds));
    }

    // ---- configuration helpers ------------------------------------------------

    private String stringProp(String key, String dflt) {
        Object v = config != null ? config.getProperty(key) : null;
        return (v == null) ? dflt : v.toString();
    }

    private int intProp(String key, int dflt) {
        Object v = config != null ? config.getProperty(key) : null;
        if (v == null) return dflt;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString().trim()); }
        catch (NumberFormatException e) { return dflt; }
    }

    private long longProp(String key, long dflt) {
        Object v = config != null ? config.getProperty(key) : null;
        if (v == null) return dflt;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString().trim()); }
        catch (NumberFormatException e) { return dflt; }
    }

    private boolean boolProp(String key, boolean dflt) {
        Object v = config != null ? config.getProperty(key) : null;
        if (v == null) return dflt;
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(v.toString().trim());
    }

    private static int defaultPortFor(String protocol) {
        if (protocol == null) return 8554;
        switch (protocol.toLowerCase()) {
            case "rtsp":  return 8554;
            case "rtsps": return 8322;
            case "rtmp":  return 1935;
            case "rtmps": return 1936;
            case "hls":
            case "https": return 8888;
            case "http":  return 8889;
            case "srt":   return 8890;
            default:      return 8554;
        }
    }

    private static ThreadFactory namedDaemonFactory() {
        return new ThreadFactory() {
            private final AtomicInteger n = new AtomicInteger(1);
            @Override public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "mediamtx-sync-" + n.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
    }
}
