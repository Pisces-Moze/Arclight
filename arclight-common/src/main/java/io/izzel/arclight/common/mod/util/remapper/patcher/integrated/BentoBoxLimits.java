package io.izzel.arclight.common.mod.util.remapper.patcher.integrated;

import io.izzel.arclight.api.PluginPatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Map;

import static java.util.Map.entry;

public class BentoBoxLimits {

    private static final Map<String, String> ENTITY_TYPE_ALIASES = Map.ofEntries(
        entry("ITEM", "DROPPED_ITEM"),
        entry("EXPERIENCE_BOTTLE", "THROWN_EXP_BOTTLE"),
        entry("EYE_OF_ENDER", "ENDER_SIGNAL"),
        entry("FIREWORK_ROCKET", "FIREWORK"),
        entry("LEASH_KNOT", "LEASH_HITCH"),
        entry("MOOSHROOM", "MUSHROOM_COW"),
        entry("SNOW_GOLEM", "SNOWMAN"),
        entry("TNT", "PRIMED_TNT"),
        entry("END_CRYSTAL", "ENDER_CRYSTAL"),
        entry("FISHING_BOBBER", "FISHING_HOOK"),
        entry("LIGHTNING_BOLT", "LIGHTNING"),
        entry("COMMAND_BLOCK_MINECART", "MINECART_COMMAND"),
        entry("CHEST_MINECART", "MINECART_CHEST"),
        entry("FURNACE_MINECART", "MINECART_FURNACE"),
        entry("TNT_MINECART", "MINECART_TNT"),
        entry("HOPPER_MINECART", "MINECART_HOPPER"),
        entry("SPAWNER_MINECART", "MINECART_MOB_SPAWNER")
    );

    public static void handleEntityTypeAliases(ClassNode node, PluginPatcher.ClassRepo repo) {
        if (!node.name.startsWith("world/bentobox/limits/")) {
            return;
        }
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode instruction : method.instructions) {
                if (instruction.getOpcode() == Opcodes.GETSTATIC && instruction instanceof FieldInsnNode field
                    && field.owner.equals("org/bukkit/entity/EntityType")
                    && field.desc.equals("Lorg/bukkit/entity/EntityType;")) {
                    String replacement = ENTITY_TYPE_ALIASES.get(field.name);
                    if (replacement != null) {
                        field.name = replacement;
                    }
                }
            }
        }
    }
}
