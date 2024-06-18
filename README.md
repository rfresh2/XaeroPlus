# XaeroPlus

<p align="center">
  <a href="https://discord.gg/nJZrSaRKtb">
  <img alt="Discord" src="https://dcbadge.vercel.app/api/server/nJZrSaRKtb">
  </a>
</p>

<p align="center">
  <a href=https://modrinth.com/mod/xaeroplus ><img alt="Modrinth Downloads" src="https://img.shields.io/modrinth/dt/EnPUzSTg?style=for-the-badge&logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href=https://legacy.curseforge.com/minecraft/mc-mods/xaeroplus ><img alt="CurseForge Downloads" src="https://cf.way2muchnoise.eu/866084.svg?badge_style=for_the_badge"></a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/MC-1.12.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.19.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.19.4-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.4-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.6-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.21-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/github/languages/code-size/rfresh2/XaeroPlus.svg" alt="Code size"/>
  <img src="https://img.shields.io/github/repo-size/rfresh2/XaeroPlus.svg" alt="GitHub repo size"/>
  <img src="https://tokei.rs/b1/github/rfresh2/XaeroPlus?category=code&style=flat" alt="Lines of Code"/>
</p>

XaeroPlus is a client-side Minecraft mod that depends on and modifies the Xaero's WorldMap and Minimap mods with extra
features and performance improvements - particularly for use on anarchy servers like 2b2t.

XaeroPlus is not affiliated or endorsed by xaero96. Please report issues to XaeroPlus's [Github](https://github.com/rfresh2/XaeroPlus/issues) or [discord server](https://discord.gg/nJZrSaRKtb).

<details>
<summary>Example Map</summary>
<p align="center">
  <img src="https://i.imgur.com/oYYhDoS.jpeg">
</p>
</details>

# Download

Available on:

* [Github Releases](https://github.com/rfresh2/XaeroPlus/releases)
* [Modrinth](https://modrinth.com/mod/xaeroplus)
* [CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/xaeroplus)
* [GitHub Actions](https://github.com/rfresh2/XaeroPlus/actions?query=branch%3Amainline+)

# Xaero Versions

Each XaeroPlus release is only compatible with a specific version of Xaero's WorldMap and Minimap.
Xaero's Minimap can be swapped out for [BetterPVP](https://chocolateminecraft.com/betterpvp2.php)

Download and include these mods **in addition** to `XaeroPlus-*.jar` (3 jars total).

You can find download links to Xaero's mods here:
* https://modrinth.com/mod/xaeros-world-map/versions?g=1.20.1
* https://modrinth.com/mod/xaeros-minimap/versions?g=1.20.1

Or a modpack zip with these jars is included in the Github releases.

# Modifications

* [1.5-3x your FPS by limiting the framerate the minimap is rendered at!](https://youtu.be/hIG-VyGQLao)
  * Tip: For least visual impact, lock the minimap's north
* [Adjustable minimap scaling that increases how many chunks are visible](https://youtu.be/dNqxGzGAHyk)
* [NewChunks Highlighting in MiniMap and WorldMap.](https://youtu.be/n-Tf6TJSsiA)
* [Baritone](https://github.com/cabaletta/baritone) integration
  * Baritone Goals synced as temporary waypoints
  * [Point and Click Travel](https://youtu.be/gbguyfXLgi0)
* [Waystones](https://legacy.curseforge.com/minecraft/mc-mods/waystones) and [Fabric Waystones](https://legacy.curseforge.com/minecraft/mc-mods/fabric-waystones) integration
  * Syncs Waystones as temporary waypoints
* [WorldTools](https://modrinth.com/mod/worldtools/) World Downloader integration
  * [Highlights saved chunks while downloading](https://youtu.be/mtCqwJ_RGcc)
* [Portals Highlighting in Minimap and WorldMap](https://youtu.be/zstGVfVRrAs)
* [Portal Skip Highlighting in Minimap and WorldMap](https://youtu.be/g_yQ8D95RY0). Detects chunks where a portal could have been loaded.
* OldChunks Highlighting in Minimap and WorldMap.
  * Intended for use on 2b2t. Highlights chunks that were generated in 1.12.2 or lower in the Overworld and Nether based on the natural blocks present in the chunk.
* [Transparent minimap background instead of wasted black screen space.](https://imgur.com/a/jGgHqL4)
* [Fast map region writes](https://youtu.be/B5d7FaHXDCk). Prevent missed chunks in map while traveling at very high speeds.
* Allow multiple MC instances to read/write to the same map concurrently
* Transparent obsidian roof. Useful for mapping 2b2t spawn.
* Setting to always view and create waypoints in the Overworld when in Nether.
* [Render server view distance square around the player.](https://youtu.be/iY_JTGFK6Yg)
* [Dimension hot-switching on WorldMap and Minimap.](https://youtu.be/hXZQtX2df3I)
  * Recommended: Create keybinds for Minimap dimension switching.
* WorldMap GUI:
  * WorldMap zoom unlocked
  * GUI on WorldMap to pan the map to user entered coordinates.
  * WorldMap Follow mode and GUI button
  * F1 on WorldMap hides GUI and overlays
* Waypoints GUI:
  * [Search](https://youtu.be/7DRMUsmZDxc)
  * Display distance to waypoints on Waypoints GUI
  * Always sort enabled waypoints before disabled waypoints
  * GUI button to enable/disable all waypoints
* [Waypoint Beacons](https://imgur.com/a/jGgHqL4)
* Minecraft world always renders in background while in a Xaero GUI for client travel mods compatibility
* Minimap entity dot fix. Hides the entity dot when arrow mode is selected.
* WorldMap and Waypoint directories optionally indexed by:
  * Multiplayer server list name.
  * Base Server Domain Name
  * Server IP (Xaero's default)
  * **Changing this setting requires you to manually rename existing folders in `.minecraft/XaeroWaypoints/` and `.minecraft/XaeroWorldMap/`**
* Overworld dimension optionally stored in the "DIM0" directory instead of "null"
  * **Changing this setting requires you to manually rename existing folders in `.minecraft/XaeroWorldMap/Multiplayer_<server name>/`**
* WorldMap 1.30.0 added cave data saving and rendering. There is a setting on by default in XaeroPlus that changes how the nether is rendered with cave mode off to be as it was previously.
  * This removes the need to manually move existing world data files.

Configurations are in the Xaero WorldMap and Minimap settings GUI.

Toggleable settings support keybinds through the standard Minecraft Controls GUI.

# Language Translations

PR's are welcomed for language translations. 

Language files are located in `src/main/resources/assets/xaeroplus/lang/`

# Other Useful Tools

* Convert JourneyMap World Files to Xaero: [JMToXaero](https://github.com/Entropy5/JMtoXaero)
* Convert JourneyMap Waypoints to Xaero: [JMWaypointsToXaero](https://github.com/rfresh2/JMWaypointsToXaero)
* 2b2t Atlas Waypoints to Xaero: [JMWaypointsToXaero/atlas](https://github.com/rfresh2/JMWaypointsToXaero/tree/atlas)
* Convert MC Region Files to Xaero: [JMToXaero/Region-Scripts](https://github.com/Entropy5/JMtoXaero/blob/Region-Scripts/src/main/java/com/github/entropy5/RegionToXaero.java)
* 2b2t 256k WDL Xaero Map (20GB): [mc-archive](https://data.mc-archive.org/s/eFDEy2XKof83Kez)
* Xaero World
  Merger: [JMToXaero/Region-Scripts](https://github.com/Entropy5/JMtoXaero/blob/Region-Scripts/src/main/java/com/github/entropy5/XaeroRegionMerger.java)
  * Can be used to merge 256k WDL into an existing world. With optional darkening only on tiles from 256K
