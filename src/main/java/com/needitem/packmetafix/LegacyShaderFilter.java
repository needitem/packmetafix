package com.needitem.packmetafix;

import net.minecraft.server.packs.PackType;

/**
 * Hides the core shader programs shipped inside server-supplied resource packs.
 *
 * <p>A pack built for an older Minecraft version carries {@code assets/minecraft/shaders/core/*}
 * written against the old uniform layout. On a modern client those sources no longer compile — the
 * engine now declares uniforms the pack redeclares, e.g.
 * {@code error C1038: declaration of "FogColor" conflicts with previous declaration}. When the text
 * pipelines fail to build, Minecraft's safety net fires and it drops <em>every</em> selected pack
 * ("Caught error loading resourcepacks, removing all selected resourcepacks"), so the pack's fonts
 * and HUD graphics disappear along with the shaders.
 *
 * <p>Hiding the shader directory lets the client fall back to its own working shaders and keep the
 * rest of the pack. Custom shader effects are lost; everything else survives, which is the better
 * half of a trade that otherwise costs the whole pack.
 *
 * <p>Only server-supplied packs are filtered. Packs the player installed themselves are left alone —
 * a locally installed pack is a deliberate choice, and a modern one may ship shaders that do compile.
 */
public final class LegacyShaderFilter {

    /** Pack id prefix Minecraft gives to packs pushed by a server. */
    private static final String SERVER_PACK_PREFIX = "server/";

    private static final String SHADER_DIRECTORY = "shaders";

    private LegacyShaderFilter() {
    }

    /** Whether resources under {@code path} should be hidden for the pack with this id. */
    public static boolean hides(String packId, PackType type, String path) {
        return type == PackType.CLIENT_RESOURCES
            && packId != null
            && packId.startsWith(SERVER_PACK_PREFIX)
            && isShaderPath(path);
    }

    private static boolean isShaderPath(String path) {
        if (path == null || !path.startsWith(SHADER_DIRECTORY)) {
            return false;
        }
        // "shaders" itself, or anything below it — but not a sibling such as "shaders_extra/".
        return path.length() == SHADER_DIRECTORY.length() || path.charAt(SHADER_DIRECTORY.length()) == '/';
    }
}
