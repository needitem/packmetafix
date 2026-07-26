package com.needitem.packmetafix.mixin;

import com.needitem.packmetafix.LegacyShaderFilter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;

/**
 * Keeps outdated core shaders inside a server-supplied pack from reaching the renderer.
 *
 * <p>Server packs arrive as a zip, which Minecraft reads through {@link FilePackResources}. Reporting
 * the shader files as absent makes the lookup fall through to the client's own shaders, exactly as if
 * the pack had never contained them.
 */
@Mixin(FilePackResources.class)
public abstract class FilePackResourcesMixin implements PackResources {

    @Inject(
        method = "getResource(Lnet/minecraft/server/packs/PackType;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/server/packs/resources/IoSupplier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void packmetafix$hideLegacyShaders(PackType type, Identifier identifier, CallbackInfoReturnable<IoSupplier<InputStream>> cir) {
        if (LegacyShaderFilter.hides(this.packId(), type, identifier.getPath())) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "listResources", at = @At("HEAD"), cancellable = true)
    private void packmetafix$hideLegacyShaders(PackType type, String namespace, String path, PackResources.ResourceOutput output, CallbackInfo ci) {
        if (LegacyShaderFilter.hides(this.packId(), type, path)) {
            ci.cancel();
        }
    }
}
