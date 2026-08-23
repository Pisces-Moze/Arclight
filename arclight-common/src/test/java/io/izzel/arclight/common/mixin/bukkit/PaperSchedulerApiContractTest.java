package io.izzel.arclight.common.mixin.bukkit;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperSchedulerApiContractTest {

    @Test
    void exposesThePaperSchedulerMethodDescriptorsExpectedByPlugins() throws Exception {
        var entityMethod = PaperEntityApiMixin.class.getDeclaredMethod("getScheduler");
        assertEquals(EntityScheduler.class, entityMethod.getReturnType());

        var implementation = CraftEntityMixin.class.getDeclaredMethod("getScheduler");
        assertEquals(EntityScheduler.class, implementation.getReturnType());
    }

    @Test
    void registersBothApiSurfaceMixins() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("mixins.arclight.bukkit.json")) {
            assertNotNull(stream);
            var config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertFalse(config.contains("\"PaperBukkitApiMixin\""));
            assertTrue(config.contains("\"PaperEntityApiMixin\""));
            assertTrue(config.contains("\"PaperServerApiMixin\""));
        }
    }
}
