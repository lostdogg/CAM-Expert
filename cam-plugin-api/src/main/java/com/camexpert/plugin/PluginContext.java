package com.camexpert.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable key/value context passed to a plugin when it is executed.
 *
 * <p>The host application populates this context with data relevant to the
 * current state (e.g. active CAM file path, selected operations, part name).
 * Plugins read values via {@link #get(String)} and {@link #getOrDefault(String, String)}.
 */
public final class PluginContext {

    private final Map<String, String> values;

    private PluginContext(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(new HashMap<>(values));
    }

    /**
     * Returns the value associated with {@code key}, or {@code null} if absent.
     *
     * @param key context key
     * @return associated value or {@code null}
     */
    public String get(String key) {
        return values.get(key);
    }

    /**
     * Returns the value associated with {@code key}, or {@code defaultValue} if absent.
     *
     * @param key          context key
     * @param defaultValue fallback value
     * @return associated value or {@code defaultValue}
     */
    public String getOrDefault(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    /**
     * Returns an unmodifiable view of all context entries.
     *
     * @return all context entries
     */
    public Map<String, String> getAll() {
        return values;
    }

    /**
     * Creates a new {@link Builder} for constructing a {@link PluginContext}.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link PluginContext}. */
    public static final class Builder {

        private final Map<String, String> values = new HashMap<>();

        private Builder() {}

        /**
         * Adds a context entry.
         *
         * @param key   entry key
         * @param value entry value
         * @return this builder
         */
        public Builder put(String key, String value) {
            values.put(key, value);
            return this;
        }

        /**
         * Builds and returns the {@link PluginContext}.
         *
         * @return new immutable context
         */
        public PluginContext build() {
            return new PluginContext(values);
        }
    }
}
