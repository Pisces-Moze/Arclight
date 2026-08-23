package io.izzel.arclight.boot.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.LinkedHashMap;
import java.util.Map;

/** Adds Paper's static scheduler facade after Mixin has transformed Bukkit. */
public final class PaperApiImplementer implements Implementer {

    static final PaperApiImplementer INSTANCE = new PaperApiImplementer();

    private static final String BUKKIT = "org/bukkit/Bukkit";
    private static final String SERVER_BRIDGE =
        "io/izzel/arclight/common/bridge/bukkit/PaperSchedulerBridge";
    private static final Map<String, String> SCHEDULER_METHODS = schedulerMethods();

    private PaperApiImplementer() {
    }

    @Override
    public boolean processClass(ClassNode node,
                                ILaunchPluginService.ITransformerLoader transformerLoader) {
        if (!BUKKIT.equals(node.name)) {
            return false;
        }

        boolean changed = false;
        for (var entry : SCHEDULER_METHODS.entrySet()) {
            String descriptor = "()L" + entry.getValue() + ";";
            boolean present = node.methods.stream().anyMatch(method ->
                method.name.equals(entry.getKey()) && method.desc.equals(descriptor));
            if (!present) {
                node.methods.add(createSchedulerGetter(entry.getKey(), descriptor));
                changed = true;
            }
        }
        return changed;
    }

    private static MethodNode createSchedulerGetter(String name, String descriptor) {
        var method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            name, descriptor, null, null);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BUKKIT,
            "getServer", "()Lorg/bukkit/Server;", false));
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, SERVER_BRIDGE));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, SERVER_BRIDGE,
            name, descriptor, true));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 1;
        method.maxLocals = 0;
        return method;
    }

    private static Map<String, String> schedulerMethods() {
        var methods = new LinkedHashMap<String, String>();
        methods.put("getRegionScheduler",
            "io/papermc/paper/threadedregions/scheduler/RegionScheduler");
        methods.put("getAsyncScheduler",
            "io/papermc/paper/threadedregions/scheduler/AsyncScheduler");
        methods.put("getGlobalRegionScheduler",
            "io/papermc/paper/threadedregions/scheduler/GlobalRegionScheduler");
        return methods;
    }
}
