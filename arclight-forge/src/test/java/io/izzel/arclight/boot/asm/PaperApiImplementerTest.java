package io.izzel.arclight.boot.asm;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaperApiImplementerTest {

    private static final String BRIDGE =
        "io/izzel/arclight/common/bridge/bukkit/PaperSchedulerBridge";
    private static final Map<String, String> EXPECTED = Map.of(
        "getRegionScheduler", "()Lio/papermc/paper/threadedregions/scheduler/RegionScheduler;",
        "getAsyncScheduler", "()Lio/papermc/paper/threadedregions/scheduler/AsyncScheduler;",
        "getGlobalRegionScheduler", "()Lio/papermc/paper/threadedregions/scheduler/GlobalRegionScheduler;"
    );

    @Test
    void addsTheThreePublicStaticSchedulerMethodsToBukkit() {
        var node = bukkitNode();

        assertTrue(PaperApiImplementer.INSTANCE.processClass(node, null));

        var methods = node.methods.stream()
            .filter(method -> EXPECTED.containsKey(method.name))
            .collect(Collectors.toMap(method -> method.name, method -> method));
        assertEquals(EXPECTED.keySet(), methods.keySet());
        for (var entry : EXPECTED.entrySet()) {
            var method = methods.get(entry.getKey());
            assertEquals(entry.getValue(), method.desc);
            assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC));

            var instructions = method.instructions.iterator();
            var getServer = (MethodInsnNode) instructions.next();
            assertEquals(Opcodes.INVOKESTATIC, getServer.getOpcode());
            assertEquals("org/bukkit/Bukkit", getServer.owner);
            assertEquals("getServer", getServer.name);
            var cast = (TypeInsnNode) instructions.next();
            assertEquals(BRIDGE, cast.desc);
            var bridgeCall = (MethodInsnNode) instructions.next();
            assertEquals(Opcodes.INVOKEINTERFACE, bridgeCall.getOpcode());
            assertEquals(BRIDGE, bridgeCall.owner);
            assertEquals(entry.getKey(), bridgeCall.name);
            assertEquals(entry.getValue(), bridgeCall.desc);
        }
    }

    @Test
    void isIdempotentAndDoesNotTouchUnrelatedClasses() {
        var node = bukkitNode();
        assertTrue(PaperApiImplementer.INSTANCE.processClass(node, null));
        int methodCount = node.methods.size();
        assertFalse(PaperApiImplementer.INSTANCE.processClass(node, null));
        assertEquals(methodCount, node.methods.size());

        var unrelated = new ClassNode();
        unrelated.name = "example/Unrelated";
        assertFalse(PaperApiImplementer.INSTANCE.processClass(unrelated, null));
        assertTrue(unrelated.methods.isEmpty());
    }

    private static ClassNode bukkitNode() {
        var node = new ClassNode();
        node.name = "org/bukkit/Bukkit";
        return node;
    }
}
