/*
 * Copyright 2025 the takserver-mediamtx-plugin contributors.
 * Licensed under the MIT License.
 */
package tak.server.plugins.mediamtx.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Video feed descriptor consumed by the TAK Server video manager. The element
 * names and structure intentionally mirror the schema used by
 * {@code com.bbn.marti.video.Feed} in TAK Server, so the XML produced by this
 * plugin can be POSTed to {@code /Marti/vcm} and is in turn served verbatim
 * to ATAK clients.
 */
@XmlRootElement(name = "feed")
@XmlAccessorType(XmlAccessType.NONE)
public class Feed {

    private String uuid;
    private boolean active = true;
    private String alias;
    private String address;
    private String port;
    private String path;
    private String protocol;
    private String latitude;
    private String longitude;
    private String classification;

    public Feed() { }

    @XmlElement(name = "uid")
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    @XmlElement(name = "active")
    public boolean getActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @XmlElement(name = "alias")
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }

    @XmlElement(name = "address")
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @XmlElement(name = "port")
    public String getPort() { return port; }
    public void setPort(String port) { this.port = port; }

    @XmlElement(name = "path")
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    @XmlElement(name = "protocol")
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    @XmlElement(name = "latitude")
    public String getLatitude() { return latitude; }
    public void setLatitude(String latitude) { this.latitude = latitude; }

    @XmlElement(name = "longitude")
    public String getLongitude() { return longitude; }
    public void setLongitude(String longitude) { this.longitude = longitude; }

    @XmlElement(name = "classification")
    public String getClassification() { return classification; }
    public void setClassification(String classification) { this.classification = classification; }
}
