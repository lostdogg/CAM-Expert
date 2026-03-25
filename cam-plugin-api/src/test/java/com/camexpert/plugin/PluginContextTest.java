package com.camexpert.plugin;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PluginContextTest {

    @Test
    void get_returnsValueForKnownKey() {
        PluginContext ctx = PluginContext.builder()
                .put("filePath", "/home/user/part.cam")
                .build();
        assertEquals("/home/user/part.cam", ctx.get("filePath"));
    }

    @Test
    void get_returnsNullForUnknownKey() {
        PluginContext ctx = PluginContext.builder().build();
        assertNull(ctx.get("missing"));
    }

    @Test
    void getOrDefault_returnsDefaultForMissingKey() {
        PluginContext ctx = PluginContext.builder().build();
        assertEquals("default", ctx.getOrDefault("missing", "default"));
    }

    @Test
    void getAll_returnsAllEntries() {
        PluginContext ctx = PluginContext.builder()
                .put("a", "1")
                .put("b", "2")
                .build();
        Map<String, String> all = ctx.getAll();
        assertEquals(2, all.size());
        assertEquals("1", all.get("a"));
        assertEquals("2", all.get("b"));
    }

    @Test
    void getAll_isUnmodifiable() {
        PluginContext ctx = PluginContext.builder().put("k", "v").build();
        assertThrows(UnsupportedOperationException.class, () -> ctx.getAll().put("x", "y"));
    }
}
