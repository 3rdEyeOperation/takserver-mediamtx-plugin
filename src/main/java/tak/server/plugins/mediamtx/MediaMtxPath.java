/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Minimal DTO matching the {@code Path} schema from MediaMTX's
 * {@code /v3/paths/list} response. Only fields the plugin actually needs are
 * declared; everything else is silently ignored via
 * {@link JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaMtxPath {

    private String name;
    private String confName;
    private boolean ready;
    private List<String> tracks;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getConfName() { return confName; }
    public void setConfName(String confName) { this.confName = confName; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public List<String> getTracks() { return tracks; }
    public void setTracks(List<String> tracks) { this.tracks = tracks; }
}
