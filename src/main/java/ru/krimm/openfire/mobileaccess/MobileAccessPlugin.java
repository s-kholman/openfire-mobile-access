package ru.krimm.openfire.mobileaccess;

import java.io.File;
import java.util.Objects;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the Mobile Access Openfire plugin.
 */
public final class MobileAccessPlugin implements Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MobileAccessPlugin.class);

    private File pluginDirectory;

    @Override
    public void initializePlugin(final PluginManager pluginManager, final File pluginDirectory) {
        Objects.requireNonNull(pluginManager, "pluginManager must not be null");
        this.pluginDirectory = Objects.requireNonNull(pluginDirectory, "pluginDirectory must not be null");

        LOGGER.info("Mobile Access plugin initialized from {}", pluginDirectory.getAbsolutePath());
    }

    @Override
    public void destroyPlugin() {
        LOGGER.info("Mobile Access plugin destroyed");
        pluginDirectory = null;
    }

    File getPluginDirectory() {
        return pluginDirectory;
    }
}
