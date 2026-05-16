/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Wrapper element mirroring TAK Server's {@code <videoConnections>} payload.
 * It is the body that ATAK clients ultimately consume from {@code /Marti/vcm}.
 */
@XmlRootElement(name = "videoConnections")
@XmlAccessorType(XmlAccessType.NONE)
public class VideoConnections {

    @XmlElement(name = "feed")
    private List<Feed> feeds = new ArrayList<>();

    public VideoConnections() { }

    public VideoConnections(List<Feed> feeds) {
        if (feeds != null) {
            this.feeds = feeds;
        }
    }

    public List<Feed> getFeeds() { return feeds; }
    public void setFeeds(List<Feed> feeds) {
        this.feeds = (feeds != null) ? feeds : new ArrayList<>();
    }
}
