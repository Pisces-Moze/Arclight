package io.izzel.arclight.common.mod.util.remapper;

public interface RemappingClassLoader {

    ClassLoaderRemapper getRemapper();

    ArclightRemapConfig getRemapConfig();

    static ClassLoader tryRedirect(ClassLoader classLoader) {
        if (needRemap(classLoader)) {
            return RemappingClassLoader.class.getClassLoader();
        }
        return classLoader;
    }

    static boolean needRemap(ClassLoader cl) {
        // Removing redirect for PlatformClassLoader since only classes
        // in the standard library are loaded by PlatformClassLoader.
        // They are always related only to JDK we're using.
        var now = cl;
        do {
            if (now == ClassLoader.getSystemClassLoader()) {
                return true;
            } else if (now == ClassLoader.getPlatformClassLoader() || now == null) {
                return false;
            }
            now = now.getParent();
        } while (now != null);
        return false;
    }
}
