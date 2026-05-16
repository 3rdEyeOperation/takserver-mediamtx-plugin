# takserver-mediamtx-plugin

A [TAK Server](https://github.com/TAK-Product-Center/Server) Plugin Manager
plugin that turns a [MediaMTX](https://github.com/bluenviron/mediamtx) media
server into a live video catalog for ATAK / WinTAK / iTAK clients.

The plugin periodically calls MediaMTX's HTTP API (`GET /v3/paths/list`),
converts each active stream path into a TAK Server `feed`, and POSTs the
resulting `<videoConnections>` XML to TAK Server's video-manager endpoint
(`/Marti/vcm`). ATAK clients then receive the stream list via the standard
Video → Video Library refresh, with no client-side configuration.

```
+----------+    GET /v3/paths/list     +-----------------+   POST /Marti/vcm   +-------------+
| MediaMTX | <-----------------------  | takserver-      |  ----------------> | TAK Server  | -> ATAK clients
+----------+                           | mediamtx-plugin |                    +-------------+
                                       +-----------------+
```

## Features

* Polls MediaMTX `/v3/paths/list` on a configurable interval (default 30 s),
  transparently paging through results.
* Optional MediaMTX HTTP Basic auth (`mediamtx.username` / `mediamtx.password`).
* Publishes feeds to TAK Server using either HTTP Basic auth or a PKCS#12 /
  JKS client certificate (the common production setup).
* Builds deterministic `uid`s per stream so re-publishing is idempotent and
  does not create duplicate feeds in TAK Server.
* Configurable `protocol`, `host`, `port` for the URL ATAK clients use to
  reach MediaMTX (RTSP / RTSPS / RTMP / RTMPS / HLS / HTTP / SRT).
* Filters out streams that are not currently `ready` (toggleable).

## Build

This is a standard Gradle project (Java 17, Gradle 8.x). Because the TAK
Server plugin SDK (`gov.tak:takserver-plugins`) is **not** published to any
public Maven repository, you must first build and publish it from the TAK
Server source tree to your local Maven cache:

```bash
git clone https://github.com/TAK-Product-Center/Server.git takserver
cd takserver/src
./gradlew :takserver-plugins:publishToMavenLocal
```

This installs `gov.tak:takserver-plugins:<version>` under `~/.m2/repository`.
Adjust `takserverPluginsVersion` in [`gradle.properties`](gradle.properties)
to match the version you published, then build the plugin:

```bash
cd takserver-mediamtx-plugin
gradle wrapper      # one-time, if you do not already have a wrapper
./gradlew clean build
```

The shaded plugin JAR is written to
`build/libs/takserver-mediamtx-plugin-<version>.jar`.

## Configure

Copy [`example/conf/plugins/tak.server.plugins.mediamtx.MediaMtxVideoPlugin.yaml`](example/conf/plugins/tak.server.plugins.mediamtx.MediaMtxVideoPlugin.yaml)
to TAK Server's plugin-manager working directory at:

```
conf/plugins/tak.server.plugins.mediamtx.MediaMtxVideoPlugin.yaml
```

The file is loaded by `PluginConfiguration` exactly as documented in the TAK
Server plugin SDK. Required keys are:

| Key                     | Description                                                       |
| ----------------------- | ----------------------------------------------------------------- |
| `mediamtx.apiUrl`       | Base URL of MediaMTX's HTTP API, e.g. `http://127.0.0.1:9997`.    |
| `stream.protocol`       | Protocol ATAK clients use to play streams (`rtsp`, `rtmp`, …).    |
| `stream.host`           | Public hostname / IP of the MediaMTX media plane.                 |
| `stream.port`           | Port of the MediaMTX media plane for the chosen protocol.         |
| `takserver.vcmUrl`      | TAK Server VCM endpoint, e.g. `https://tak:8443/Marti/vcm`.       |

Optional keys (auth, TLS, paging, poll interval) are documented in the
example YAML.

## Deploy

1. Stop the TAK Server Plugin Manager service.
2. Copy the shaded plugin JAR into the Plugin Manager's plugin directory
   (typically `/opt/tak/lib/`).
3. Copy your configured YAML into `/opt/tak/conf/plugins/` next to the JAR.
4. Restart the Plugin Manager service and tail its log; on startup you
   should see:

   ```
   plugin class name: tak.server.plugins.mediamtx.MediaMtxVideoPlugin
   MediaMtxVideoPlugin started: mediamtx=... -> tak=... every 30s
   ```

5. Open ATAK → Video → Video Library and pull-to-refresh. MediaMTX streams
   appear as feeds named after each MediaMTX path.

## Project layout

```
build.gradle                      # Gradle build (Java 17, shadow jar)
settings.gradle, gradle.properties
src/main/java/tak/server/plugins/mediamtx/
  MediaMtxVideoPlugin.java        # @TakServerPlugin, schedules sync
  MediaMtxApiClient.java          # MediaMTX /v3/paths/list client
  MediaMtxPath, MediaMtxPathList  # Jackson DTOs
  FeedFactory.java                # MediaMtxPath -> TAK Feed mapping
  TakServerVideoClient.java       # Publishes <videoConnections> to /Marti/vcm
  model/Feed.java                 # JAXB feed (matches TAK Server schema)
  model/VideoConnections.java     # JAXB wrapper
src/test/java/...                 # JUnit 5 unit tests
example/conf/plugins/...yaml      # Example plugin configuration
```

## License

MIT. See the per-file headers.
