package com.needitem.packmetafix.mixin;

import com.google.gson.JsonObject;
import com.needitem.packmetafix.PackMetaFix;
import com.needitem.packmetafix.PackMetaSanitizer;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Repairs legacy pack format declarations before Minecraft parses them.
 *
 * <p>Every {@code pack.mcmeta} reaches Minecraft through {@code ResourceMetadata.fromJsonStream}, so
 * intercepting it here covers built-in, local and server-supplied packs alike. The document is
 * repaired and then handed back to the untouched vanilla parser, which keeps the error handling and
 * the returned implementation exactly as they were.
 */
// Mixin forbids non-shadow fields in an interface mixin (they would become public static final on
// the target), so anything constant-like lives in PackMetaSanitizer instead.
@Mixin(ResourceMetadata.class)
public interface ResourceMetadataMixin {

    @Inject(method = "fromJsonStream", at = @At("HEAD"), cancellable = true)
    private static void packmetafix$repairLegacyFormats(InputStream stream, CallbackInfoReturnable<ResourceMetadata> cir) throws IOException {
        // Inner call from the delegation below: let vanilla do its job.
        if (PackMetaFix.isReentrant()) {
            return;
        }

        byte[] original = stream.readAllBytes();
        byte[] repaired = original;

        try {
            JsonObject root = PackMetaSanitizer.parse(original);
            int sections = PackMetaSanitizer.repair(root);
            if (sections > 0) {
                repaired = root.toString().getBytes(StandardCharsets.UTF_8);
                PackMetaFix.LOGGER.info("Repaired {} legacy pack format declaration(s) in pack.mcmeta", sections);
            }
        } catch (RuntimeException malformed) {
            // Not something we can repair. Pass the original bytes through so vanilla reports it as usual.
            PackMetaFix.LOGGER.debug("Left pack.mcmeta untouched: {}", malformed.toString());
        }

        PackMetaFix.enterReentrant();
        try {
            cir.setReturnValue(ResourceMetadata.fromJsonStream(new ByteArrayInputStream(repaired)));
        } finally {
            PackMetaFix.exitReentrant();
        }
    }
}
