package tak.server.plugins;

import java.util.Collections;
import java.util.List;

/** Compile-only stub. */
public class PluginConfiguration {

    public PluginConfiguration() { }

    public PluginConfiguration(Class<?> clazz) { }

    public Object getProperty(String key) { return null; }

    public boolean containsProperty(String property) { return false; }

    public List<String> getProperties() { return Collections.emptyList(); }

    public PluginConfiguration reloadPluginConfiguration(Class<?> pluginClazz) {
        return new PluginConfiguration(pluginClazz);
    }
}
