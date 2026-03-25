package com.camexpert.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers and manages {@link CamPlugin} implementations.
 *
 * <p>On startup the host application calls {@link #loadFromDirectory(File)} with a
 * designated plug-in directory. Every {@code .jar} found there is added to a
 * {@link URLClassLoader} and scanned for {@link CamPlugin} implementations via
 * the Java {@link ServiceLoader} SPI mechanism.
 *
 * <p>The standard classpath is also scanned, so plugins bundled directly with the
 * application (e.g. built-in toolpath algorithms) are discovered automatically.
 */
public final class PluginLoader {

    private static final Logger LOG = Logger.getLogger(PluginLoader.class.getName());

    private final List<CamPlugin> plugins = new ArrayList<>();

    /**
     * Loads plugins from the application classpath via SPI.
     * Call this once during application boot.
     */
    public void loadFromClasspath() {
        ServiceLoader<CamPlugin> loader = ServiceLoader.load(CamPlugin.class);
        for (CamPlugin plugin : loader) {
            register(plugin);
        }
    }

    /**
     * Scans {@code directory} for {@code .jar} files, loads each one, and
     * discovers {@link CamPlugin} implementations inside them via SPI.
     *
     * @param directory directory to scan; must exist and be a directory
     * @throws IllegalArgumentException if {@code directory} is not a valid directory
     */
    public void loadFromDirectory(File directory) {
        if (directory == null || !directory.isDirectory()) {
            throw new IllegalArgumentException("Plugin directory must be a valid directory: " + directory);
        }

        File[] jars = directory.listFiles(f -> f.getName().endsWith(".jar"));
        if (jars == null || jars.length == 0) {
            LOG.info("No plugin JARs found in: " + directory.getAbsolutePath());
            return;
        }

        URL[] urls = new URL[jars.length];
        try {
            for (int i = 0; i < jars.length; i++) {
                urls[i] = jars[i].toURI().toURL();
                LOG.info("Loading plugin JAR: " + jars[i].getName());
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to build plugin class loader URLs", e);
            return;
        }

        URLClassLoader pluginClassLoader = new URLClassLoader(urls, getClass().getClassLoader());
        ServiceLoader<CamPlugin> loader = ServiceLoader.load(CamPlugin.class, pluginClassLoader);
        for (CamPlugin plugin : loader) {
            register(plugin);
        }
    }

    /**
     * Returns an unmodifiable view of all currently registered plugins.
     *
     * @return registered plugins
     */
    public List<CamPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    /**
     * Shuts down all registered plugins and clears the registry.
     * Call this during application teardown.
     */
    public void shutdownAll() {
        for (CamPlugin plugin : plugins) {
            try {
                plugin.shutdown();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Plugin shutdown failed: " + plugin.getPluginId(), e);
            }
        }
        plugins.clear();
    }

    private void register(CamPlugin plugin) {
        try {
            plugin.initialize();
            plugins.add(plugin);
            LOG.info("Registered plugin: " + plugin.getPluginId() + " (" + plugin.getDisplayName() + ")");
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Plugin initialization failed, skipping: " + plugin.getPluginId(), e);
        }
    }
}
