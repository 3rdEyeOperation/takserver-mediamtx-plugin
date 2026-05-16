package tak.server.plugins;

import java.util.UUID;

/** Compile-only stub. */
public class PluginInfo {
    private String name;
    private String description;
    private String className;
    private UUID id;
    private boolean enabled;
    private boolean started;
    private boolean archiveEnabled;
    private boolean sender;
    private boolean receiver;
    private boolean interceptor;
    private String version;
    private String tag;
    private String exceptionMessage;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isStarted() { return started; }
    public void setStarted(boolean started) { this.started = started; }
    public boolean isArchiveEnabled() { return archiveEnabled; }
    public void setArchiveEnabled(boolean archiveEnabled) { this.archiveEnabled = archiveEnabled; }
    public boolean isSender() { return sender; }
    public void setSender(boolean sender) { this.sender = sender; }
    public boolean isReceiver() { return receiver; }
    public void setReceiver(boolean receiver) { this.receiver = receiver; }
    public boolean isInterceptor() { return interceptor; }
    public void setInterceptor(boolean interceptor) { this.interceptor = interceptor; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getExceptionMessage() { return exceptionMessage; }
    public void setExceptionMessage(String exceptionMessage) { this.exceptionMessage = exceptionMessage; }
}
