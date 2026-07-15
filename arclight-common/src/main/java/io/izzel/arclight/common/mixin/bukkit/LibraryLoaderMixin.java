package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.util.remapper.generated.RemappingURLClassLoader;
import org.eclipse.aether.repository.LocalRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

@Mixin(targets = "org.bukkit.plugin.java.LibraryLoader", remap = false)
public class LibraryLoaderMixin {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "org/eclipse/aether/repository/LocalRepository"))
    private LocalRepository arclight$useSimpleLocalRepository(String basedir) {
        // Spigot's LibraryLoader defaults to Maven Resolver's enhanced local repository
        // manager. On Arclight servers the shared "libraries" repository may be left in
        // a bad resolver state after interrupted starts, which makes later plugin
        // library resolution stall until the whole directory is deleted. The simple
        // manager is enough for plugin.yml libraries and avoids enhanced tracking state.
        return new LocalRepository(new File(basedir), "simple");
    }

    @Redirect(method = "createLoader", at = @At(value = "NEW", target = "java/net/URLClassLoader"))
    private URLClassLoader arclight$useRemapped(URL[] urls, ClassLoader loader) {
        return new RemappingURLClassLoader(urls, loader);
    }
}
