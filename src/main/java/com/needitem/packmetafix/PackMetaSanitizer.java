package com.needitem.packmetafix;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;

/**
 * Rewrites legacy pack format declarations in a {@code pack.mcmeta} document into the explicit
 * {@code min_format} / {@code max_format} pair that modern Minecraft requires.
 *
 * <p>Since pack format 64, a pack (or overlay) whose declared range reaches past 64 must state
 * {@code min_format} and {@code max_format}; the older {@code pack_format} / {@code supported_formats}
 * / {@code formats} keys alone are rejected. Minecraft does not degrade gracefully here — one bad
 * overlay entry makes it discard the entire pack ("Invalid pack metadata ..., ignoring all"), which
 * is why packs built for older servers lose all of their custom fonts and HUD graphics.
 *
 * <p>The translation is faithful: the legacy range is copied across unchanged, so an overlay keeps
 * exactly the versions its author declared. Nothing is added to a document that already carries both
 * explicit keys.
 */
public final class PackMetaSanitizer {

    /** Minecraft rejects multi-version packs that declare a lower bound below this. */
    private static final int MIN_MULTI_VERSION_FORMAT = 15;

    /** UTF-8 byte order mark — legal in a pack.mcmeta, but Gson will not parse past it. */
    private static final char BOM = '\uFEFF';

    private PackMetaSanitizer() {
    }

    /**
     * Decodes a {@code pack.mcmeta} document, tolerating a leading byte order mark.
     *
     * @throws RuntimeException if the bytes are not a JSON object
     */
    public static JsonObject parse(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == BOM) {
            text = text.substring(1);
        }
        return JsonParser.parseString(text).getAsJsonObject();
    }

    /**
     * Repairs the document in place.
     *
     * @return how many sections were rewritten; {@code 0} means the document was left untouched
     */
    public static int repair(JsonObject root) {
        int repaired = 0;

        JsonObject pack = asObject(root.get("pack"));
        if (pack != null && repair(pack, "pack_format", "supported_formats")) {
            repaired++;
        }

        JsonObject overlays = asObject(root.get("overlays"));
        if (overlays != null) {
            JsonElement entries = overlays.get("entries");
            if (entries != null && entries.isJsonArray()) {
                for (JsonElement element : entries.getAsJsonArray()) {
                    JsonObject entry = asObject(element);
                    if (entry != null && repair(entry, "format", "formats")) {
                        repaired++;
                    }
                }
            }
        }

        return repaired;
    }

    private static boolean repair(JsonObject section, String singleKey, String rangeKey) {
        if (section.has("min_format") && section.has("max_format")) {
            return false;
        }

        int[] range = readRange(section.get(rangeKey));
        if (range == null) {
            range = readRange(section.get(singleKey));
        }
        if (range == null) {
            return false;
        }

        int min = Math.min(range[0], range[1]);
        int max = Math.max(range[0], range[1]);

        // A single-version pack may sit below the multi-version floor; a range may not.
        if (min != max && min < MIN_MULTI_VERSION_FORMAT) {
            min = Math.min(MIN_MULTI_VERSION_FORMAT, max);
        }

        section.add("min_format", new JsonPrimitive(min));
        section.add("max_format", new JsonPrimitive(max));
        return true;
    }

    /**
     * Reads any of the shapes Minecraft has accepted for a format range: a bare number, a two-element
     * array, or an object with {@code min_inclusive} / {@code max_inclusive}.
     *
     * @return {@code {min, max}}, or {@code null} if the element is absent or not a recognised shape
     */
    private static int[] readRange(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }

        if (isNumber(element)) {
            int value = element.getAsInt();
            return new int[]{value, value};
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 2 && isNumber(array.get(0)) && isNumber(array.get(1))) {
                return new int[]{array.get(0).getAsInt(), array.get(1).getAsInt()};
            }
            return null;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement min = object.get("min_inclusive");
            JsonElement max = object.get("max_inclusive");
            if (isNumber(min) && isNumber(max)) {
                return new int[]{min.getAsInt(), max.getAsInt()};
            }
        }

        return null;
    }

    private static boolean isNumber(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }

    private static JsonObject asObject(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
