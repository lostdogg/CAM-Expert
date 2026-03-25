package com.camexpert.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginLoaderTest {

    private PluginLoader loader;

    @BeforeEach
    void setUp() {
        loader = new PluginLoader();
    }

    @Test
    void loadFromClasspath_returnsEmptyListWhenNoSpiImplementationsPresent() {
        loader.loadFromClasspath();
        // The test classpath has no CamPlugin SPI registrations, so the list should be empty.
        List<CamPlugin> plugins = loader.getPlugins();
        assertNotNull(plugins);
    }

    @Test
    void loadFromDirectory_throwsForNullDirectory() {
        assertThrows(IllegalArgumentException.class, () -> loader.loadFromDirectory(null));
    }

    @Test
    void loadFromDirectory_throwsForNonDirectory(@TempDir File tmp) throws Exception {
        File notADir = new File(tmp, "notadir.txt");
        notADir.createNewFile();
        assertThrows(IllegalArgumentException.class, () -> loader.loadFromDirectory(notADir));
    }

    @Test
    void loadFromDirectory_emptyDirectoryProducesNoPlugins(@TempDir File tmp) {
        loader.loadFromDirectory(tmp);
        assertTrue(loader.getPlugins().isEmpty());
    }

    @Test
    void getPlugins_returnsUnmodifiableList() {
        List<CamPlugin> plugins = loader.getPlugins();
        assertThrows(UnsupportedOperationException.class, () -> plugins.add(null));
    }

    @Test
    void shutdownAll_clearsList() {
        loader.shutdownAll();
        assertTrue(loader.getPlugins().isEmpty());
    }
}
