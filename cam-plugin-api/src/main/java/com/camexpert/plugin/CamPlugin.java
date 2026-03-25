package com.camexpert.plugin;

/**
 * Service Provider Interface for CAM-Expert plugins.
 *
 * <p>Third-party developers implement this interface and package it in a JAR.
 * The {@link PluginLoader} discovers and registers all implementations on startup
 * via Java's {@link java.util.ServiceLoader} mechanism.
 *
 * <p>Typical use cases:
 * <ul>
 *   <li>Custom toolpath algorithms (e.g. gear-machining)</li>
 *   <li>ERP/MES integration</li>
 *   <li>Automated setup-sheet generation</li>
 * </ul>
 */
public interface CamPlugin {

    /**
     * Returns the unique identifier for this plugin.
     *
     * @return a non-null, non-empty plugin ID (e.g. {@code "com.acme.gear-mill"})
     */
    String getPluginId();

    /**
     * Returns a human-readable display name shown in the UI.
     *
     * @return plugin display name
     */
    String getDisplayName();

    /**
     * Returns a brief description of what this plugin does.
     *
     * @return plugin description
     */
    String getDescription();

    /**
     * Called by the framework after the plugin is loaded and before it is used.
     * Implementations should perform any one-time initialisation here (e.g.
     * loading configuration, opening connections).
     */
    void initialize();

    /**
     * Called by the framework when the application shuts down or the plugin is
     * explicitly unloaded. Implementations must release all held resources.
     */
    void shutdown();

    /**
     * Executes the plugin's primary action with the supplied context.
     *
     * @param context a key/value map of contextual data provided by the host
     *                application (e.g. active file path, selected operations)
     */
    void execute(PluginContext context);
}
