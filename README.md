# PackMetaFix

A tiny Fabric mod that stops modern Minecraft from throwing away resource packs built for older versions.

## The problem

Join a server whose resource pack was authored for an older Minecraft version — very common when
connecting through [ViaFabricPlus](https://github.com/ViaVersion/ViaFabricPlus) — and the pack can
vanish entirely. Custom fonts render as tofu boxes (`□□□`), custom HUDs and menu backgrounds fall
back to vanilla textures, and the log shows:

```
com.google.gson.JsonParseException: Overlay "ia_overlay_1_20_5_plus" declares support for version
newer than 64, but is missing mandatory fields min_format and max_format
...
[Render thread/WARN]: Invalid pack metadata in ...\downloads\..., ignoring all
```

Since pack format 64, a pack or overlay whose declared range reaches past 64 must state
`min_format` and `max_format`. The older `pack_format`, `supported_formats` and `formats` keys are no
longer sufficient on their own. Minecraft does not skip the offending overlay — it discards **the
entire pack**, which is what makes a single stale line cost you every font and graphic in it.

Pack generators such as ItemsAdder, Nexo and BetterHUD emit the legacy keys, and a server running an
older version has no reason to update them. Nothing is wrong on your machine, and the pack itself is
intact — only its version declaration is stale.

## What the mod does

Every `pack.mcmeta` reaches Minecraft through `ResourceMetadata.fromJsonStream`. PackMetaFix
intercepts it there, fills in `min_format` / `max_format` from whatever legacy range the document
already declares, and hands the result to the untouched vanilla parser.

The translation is faithful: the legacy range is copied across unchanged, so each overlay keeps
exactly the versions its author declared. Documents that already carry both explicit keys are left
alone, as are ones the mod cannot parse — those pass through byte for byte so Minecraft reports them
exactly as it would without the mod.

It covers built-in, local and server-supplied packs alike, and needs no configuration.

## Install

Requires Fabric Loader and Minecraft 26.1+. No Fabric API dependency.

Download the jar from [Releases](https://github.com/needitem/packmetafix/releases) and drop it in
your `mods` folder.

## Build

```bash
./gradlew build
```

The jar lands in `build/libs/`. Requires JDK 25.

## Verifying it works

With the mod installed, joining a server with an affected pack logs a line like:

```
[PackMetaFix] Repaired 7 legacy pack format declaration(s) in pack.mcmeta
```

and the `Invalid pack metadata ..., ignoring all` warning no longer appears.

## License

[MIT](LICENSE)
