package io.izzel.arclight.common.mixin.forge;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitecturyNetworkMixinContractTest {

    private static final String TARGET = "dev/architectury/networking/forge/NetworkManagerImpl.class";
    private static final String TARGET_OWNER = "dev/architectury/networking/forge/NetworkManagerImpl";
    private static final Map<String, String> GUARDED_FIELDS = Map.of(
        "S2C", "Ljava/util/Map;",
        "C2S", "Ljava/util/Map;",
        "S2C_TRANSFORMERS", "Ljava/util/Map;",
        "C2S_TRANSFORMERS", "Ljava/util/Map;",
        "serverReceivables", "Ljava/util/Set;",
        "clientReceivables", "Lcom/google/common/collect/Multimap;"
    );

    @Test
    void architecturyTargetStillMatchesTheGuardedLayout() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream(TARGET)) {
            assertNotNull(stream, "Architectury 9.2.14 test target is missing");
            var node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

            var fields = node.fields.stream()
                .filter(field -> GUARDED_FIELDS.containsKey(field.name))
                .collect(Collectors.toMap(field -> field.name, field -> field.desc));
            assertEquals(GUARDED_FIELDS, fields);

            var clinit = node.methods.stream().filter(method -> method.name.equals("<clinit>"))
                .findFirst().orElseThrow();
            int captureCalls = 0;
            var initializedBeforeCapture = new HashSet<String>();
            for (var instruction : clinit.instructions) {
                if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTSTATIC
                    && field.owner.equals(TARGET_OWNER)
                    && GUARDED_FIELDS.containsKey(field.name)) {
                    initializedBeforeCapture.add(field.name);
                }
                if (instruction instanceof MethodInsnNode method
                    && method.owner.equals(TARGET_OWNER)
                    && method.name.equals("createPacketHandler")
                    && method.desc.equals("(Ljava/lang/Class;Ljava/util/Map;)Ljava/util/function/Consumer;")) {
                    captureCalls++;
                    assertEquals(GUARDED_FIELDS.keySet(), initializedBeforeCapture,
                        "all guarded state must exist before the packet listener captures it");

                    var argumentLoad = instruction.getPrevious();
                    while (argumentLoad != null && argumentLoad.getOpcode() < 0) {
                        argumentLoad = argumentLoad.getPrevious();
                    }
                    assertTrue(argumentLoad instanceof FieldInsnNode);
                    var capturedField = (FieldInsnNode) argumentLoad;
                    assertEquals(Opcodes.GETSTATIC, capturedField.getOpcode());
                    assertEquals(TARGET_OWNER, capturedField.owner);
                    assertEquals("C2S_TRANSFORMERS", capturedField.name);
                }
            }
            assertEquals(1, captureCalls);
        }
    }
}
