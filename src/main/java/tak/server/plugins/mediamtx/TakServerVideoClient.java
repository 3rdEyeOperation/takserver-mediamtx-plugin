/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/* See InsecureTrustManager nested class below for the opt-in trustAll mode. */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

import tak.server.plugins.mediamtx.model.VideoConnections;

/**
 * Publishes the current {@link VideoConnections} snapshot to a TAK Server
 * video-manager endpoint (typically {@code /Marti/vcm}). Supports either HTTP
 * basic auth or PKCS#12 / JKS client-certificate authentication, since most
 * production TAK Servers require a certificate-authenticated administrator.
 */
public class TakServerVideoClient {

    private static final Logger logger = LoggerFactory.getLogger(TakServerVideoClient.class);

    private final URI endpoint;
    private final String authHeader;
    private final HttpClient httpClient;
    private final JAXBContext jaxbContext;

    public TakServerVideoClient(String endpointUrl,
                                String username, String password,
                                String clientCertPath, String clientCertPassword,
                                String trustStorePath, String trustStorePassword,
                                boolean trustAll,
                                Duration requestTimeout) throws Exception {
        Objects.requireNonNull(endpointUrl, "endpointUrl");
        this.endpoint = URI.create(endpointUrl);
        this.jaxbContext = JAXBContext.newInstance(VideoConnections.class);

        if (username != null && !username.isEmpty()) {
            String creds = username + ":" + (password != null ? password : "");
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(creds.getBytes(StandardCharsets.UTF_8));
        } else {
            this.authHeader = null;
        }

        HttpClient.Builder b = HttpClient.newBuilder()
                .connectTimeout(requestTimeout)
                .followRedirects(HttpClient.Redirect.NORMAL);

        if ((clientCertPath != null && !clientCertPath.isEmpty())
                || (trustStorePath != null && !trustStorePath.isEmpty())
                || trustAll) {
            b.sslContext(buildSslContext(clientCertPath, clientCertPassword,
                    trustStorePath, trustStorePassword, trustAll));
        }
        this.httpClient = b.build();
    }

    /**
     * POST the supplied feed snapshot to the TAK Server video manager endpoint.
     *
     * @return the HTTP status code returned by the server.
     */
    public int publish(VideoConnections connections) throws IOException, InterruptedException {
        byte[] body;
        try {
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            m.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            m.marshal(connections, out);
            body = out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to marshal videoConnections XML", e);
        }

        HttpRequest.Builder rb = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/xml")
                .header("Accept", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body));
        if (authHeader != null) {
            rb.header("Authorization", authHeader);
        }
        HttpResponse<String> resp = httpClient.send(rb.build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() / 100 != 2) {
            logger.warn("TAK Server VCM publish returned HTTP {}: {}",
                    resp.statusCode(), resp.body());
        } else if (logger.isDebugEnabled()) {
            logger.debug("TAK Server VCM publish succeeded ({} feeds, HTTP {})",
                    connections.getFeeds().size(), resp.statusCode());
        }
        return resp.statusCode();
    }

    /**
     * JVM system property that MUST be set to {@code true} for
     * {@code takserver.trustAll} configuration to take effect. Requiring an
     * additional out-of-band opt-in (not just a YAML toggle) prevents an
     * accidentally-committed config file from silently disabling TLS
     * verification on a production server.
     */
    static final String INSECURE_TLS_SYS_PROP = "mediamtx.plugin.allowInsecureTls";

    private static SSLContext buildSslContext(String clientCertPath, String clientCertPassword,
                                              String trustStorePath, String trustStorePassword,
                                              boolean trustAll) throws Exception {
        KeyManagerFactory kmf = null;
        if (clientCertPath != null && !clientCertPath.isEmpty()) {
            char[] pw = clientCertPassword != null ? clientCertPassword.toCharArray() : new char[0];
            KeyStore ks = KeyStore.getInstance(guessKeyStoreType(clientCertPath));
            try (FileInputStream in = new FileInputStream(clientCertPath)) {
                ks.load(in, pw);
            }
            kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, pw);
        }

        boolean insecureAllowed = Boolean.parseBoolean(
                System.getProperty(INSECURE_TLS_SYS_PROP, "false"));
        if (trustAll && !insecureAllowed) {
            logger.error("takserver.trustAll=true was requested in plugin config, but the "
                    + "JVM system property -D{}=true is not set. TLS verification will "
                    + "remain enabled.", INSECURE_TLS_SYS_PROP);
            trustAll = false;
        }

        TrustManager[] tms;
        if (trustAll) {
            logger.warn("TAK Server video client configured with trustAll=true and "
                    + "-D{}=true; TLS certificates will not be validated. Use only for "
                    + "local development.", INSECURE_TLS_SYS_PROP);
            // Intentional opt-in insecure TrustManager: gated behind BOTH a config
            // flag (takserver.trustAll) and a JVM system property
            // (mediamtx.plugin.allowInsecureTls). Required for self-signed dev
            // TAK Servers; never enable in production.
            // lgtm[java/insecure-trustmanager]
            tms = new TrustManager[]{ new InsecureTrustManager() };
        } else if (trustStorePath != null && !trustStorePath.isEmpty()) {
            char[] pw = trustStorePassword != null ? trustStorePassword.toCharArray() : new char[0];
            KeyStore ts = KeyStore.getInstance(guessKeyStoreType(trustStorePath));
            try (FileInputStream in = new FileInputStream(trustStorePath)) {
                ts.load(in, pw);
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);
            tms = tmf.getTrustManagers();
        } else {
            tms = null; // use JVM defaults
        }

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf != null ? kmf.getKeyManagers() : null, tms, new SecureRandom());
        return ctx;
    }

    private static String guessKeyStoreType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".p12") || lower.endsWith(".pfx")) {
            return "PKCS12";
        }
        return "JKS";
    }

    /**
     * X509TrustManager that accepts any certificate. Only constructed when
     * BOTH {@code takserver.trustAll=true} (in plugin config) AND the JVM
     * system property {@value #INSECURE_TLS_SYS_PROP} is set to {@code true}.
     * Used exclusively for local development against self-signed TAK Server
     * instances. Operators are warned at startup.
     */
    @SuppressWarnings("java:S4830") // Intentional: gated insecure trust manager for dev only.
    private static final class InsecureTrustManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] x, String s) { }
        @Override public void checkServerTrusted(X509Certificate[] x, String s) { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
}
