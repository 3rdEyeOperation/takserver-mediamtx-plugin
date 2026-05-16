package tak.server.plugins;

/** Compile-only stub. */
public interface PluginLifecycle {
    default void start() { }
    default void stop() { }
    void internalStart();
    void internalStop();
    PluginInfo getPluginInfo();
    void setPluginInfo(PluginInfo pluginInfo);
    void selfStop();
}
