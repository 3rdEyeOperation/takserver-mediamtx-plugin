package tak.server.plugins;

/** Compile-only stub. */
public abstract class PluginBase implements PluginLifecycle {

    protected PluginConfiguration config;
    protected PluginInfo pluginInfo;

    protected PluginBase() throws ReservedConfigurationException {
        this.config = new PluginConfiguration(getClass());
    }

    @Override
    public void internalStart() { start(); }

    @Override
    public void internalStop() { stop(); }

    @Override
    public PluginInfo getPluginInfo() { return pluginInfo; }

    @Override
    public void setPluginInfo(PluginInfo pluginInfo) { this.pluginInfo = pluginInfo; }

    @Override
    public void selfStop() { }
}
