package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperPlayerApiContractTest {

    @Test
    void exposesPaperRespawnLocationDescriptorsOnTheInterfaceAndImplementation() throws Exception {
        assertMethod(PaperPlayerApiMixin.class, "getRespawnLocation", Location.class);
        assertMethod(PaperPlayerApiMixin.class, "setRespawnLocation", void.class, Location.class);
        assertMethod(PaperPlayerApiMixin.class, "setRespawnLocation", void.class,
            Location.class, boolean.class);

        assertMethod(CraftPlayerMixin.class, "getRespawnLocation", Location.class);
        assertMethod(CraftPlayerMixin.class, "setRespawnLocation", void.class, Location.class);
        assertMethod(CraftPlayerMixin.class, "setRespawnLocation", void.class,
            Location.class, boolean.class);
    }

    @Test
    void registersThePlayerApiMixin() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("mixins.arclight.bukkit.json")) {
            assertNotNull(stream);
            var config = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(config.contains("\"PaperPlayerApiMixin\""));
        }
    }

    private static void assertMethod(Class<?> owner, String name, Class<?> returnType,
                                     Class<?>... parameters) throws Exception {
        var method = owner.getDeclaredMethod(name, parameters);
        assertEquals(returnType, method.getReturnType());
    }
}
