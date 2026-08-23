package io.izzel.arclight.boot.mod;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IEnvironment;
import io.izzel.arclight.api.Unsafe;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Protects ModLauncher's per-class transformation audit trail from parallel class loading.
 *
 * <p>ModLauncher 10 stores the audit table in a concurrent map, but creates a plain
 * {@link java.util.ArrayList} for each class. Forge constructs mods in parallel, so two class
 * loaders can append to the same list and corrupt its backing array. Audit entries are tiny,
 * startup-only diagnostic data, making copy-on-write lists a safe fit without affecting game
 * logic or runtime tick performance.</p>
 */
public final class TransformerAuditGuard {

    private static final String MODLAUNCHER_AUDIT_CLASS = "cpw.mods.modlauncher.TransformerAuditTrail";

    private TransformerAuditGuard() {
    }

    @SuppressWarnings("unchecked")
    public static void install() {
        var auditTrail = Launcher.INSTANCE.environment()
            .getProperty(IEnvironment.Keys.AUDITTRAIL.get())
            .orElseThrow(() -> new IllegalStateException("ModLauncher audit trail is unavailable"));

        install(auditTrail);
    }

    @SuppressWarnings("unchecked")
    static void install(Object auditTrail) {

        // A different implementation may already provide its own concurrency guarantees.
        if (!MODLAUNCHER_AUDIT_CLASS.equals(auditTrail.getClass().getName())) {
            return;
        }

        try {
            Field auditField = auditTrail.getClass().getDeclaredField("audit");
            long auditOffset = Unsafe.objectFieldOffset(auditField);
            var current = (Map<String, List<Object>>) Unsafe.getObject(auditTrail, auditOffset);
            if (current instanceof ConcurrentAuditMap<?>) {
                return;
            }
            Unsafe.putObjectVolatile(auditTrail, auditOffset, new ConcurrentAuditMap<>(current));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to protect the ModLauncher audit trail", e);
        }
    }

    static final class ConcurrentAuditMap<E> extends ConcurrentHashMap<String, List<E>> {

        ConcurrentAuditMap(Map<String, List<E>> source) {
            source.forEach((key, value) -> super.put(key, stable(value)));
        }

        @Override
        public List<E> computeIfAbsent(String key,
                                       Function<? super String, ? extends List<E>> mappingFunction) {
            return super.computeIfAbsent(key, candidate -> stable(mappingFunction.apply(candidate)));
        }

        private static <E> List<E> stable(List<E> values) {
            if (values == null || values instanceof CopyOnWriteArrayList<?>) {
                return values;
            }
            return new CopyOnWriteArrayList<>(values);
        }
    }
}
