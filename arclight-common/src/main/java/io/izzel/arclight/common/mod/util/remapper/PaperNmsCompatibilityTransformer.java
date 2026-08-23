package io.izzel.arclight.common.mod.util.remapper;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Repairs binary-level NMS differences between Paper 1.20.1 plugins and the
 * vanilla/Forge 1.20.1 runtime used by Arclight.
 */
public final class PaperNmsCompatibilityTransformer implements PluginTransformer {

    public static final PaperNmsCompatibilityTransformer INSTANCE = new PaperNmsCompatibilityTransformer();

    private static final String LEVEL_CHUNK_SECTION =
        "net/minecraft/world/level/chunk/LevelChunkSection";
    private static final String PAPER_SECTION_CONSTRUCTOR =
        "(Lnet/minecraft/world/level/chunk/PalettedContainer;" +
        "Lnet/minecraft/world/level/chunk/PalettedContainer;)V";
    private static final String FORGE_SECTION_CONSTRUCTOR =
        "(Lnet/minecraft/world/level/chunk/PalettedContainer;" +
        "Lnet/minecraft/world/level/chunk/PalettedContainerRO;)V";

    private PaperNmsCompatibilityTransformer() {
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper, ArclightRemapConfig config) {
        if (!config.remap()) {
            return;
        }
        for (var method : node.methods) {
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                    && LEVEL_CHUNK_SECTION.equals(call.owner)
                    && "<init>".equals(call.name)
                    && PAPER_SECTION_CONSTRUCTOR.equals(call.desc)) {
                    call.desc = FORGE_SECTION_CONSTRUCTOR;
                }
            }
        }
    }
}
