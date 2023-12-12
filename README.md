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
  <img src="https://img.shields.io/badge/MC-1.20.1-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.2-brightgreen.svg" alt="Minecraft"/>
  <img src="https://img.shields.io/badge/MC-1.20.4-brightgreen.svg" alt="Minecraft"/>
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
* https://modrinth.com/mod/xaeros-world-map/versions
* https://modrinth.com/mod/xaeros-minimap/versions

Or a modpack zip with these jars is included in the Github releases.

# Modifications

* [Dimension hot-switching on WorldMap and Minimap.](https://cdn.discordapp.com/attachments/971140948593635335/1109733484144107572/Temurin-1.8.0_352_2023.03.31_-_20.11.27.50.mp4)
  * Recommended: Create keybinds for Minimap dimension switching.
* [Adjustable minimap scaling that increases how many chunks are visible](https://cdn.discordapp.com/attachments/971140948593635335/1109734742842146937/Temurin-1.8.0_352_2023.03.29_-_16.16.08.32.DVR.mp4)
* [NewChunks Highlighting in MiniMap and WorldMap.](https://cdn.discordapp.com/attachments/971140948593635335/1109735633045434408/Base_Profile_2023.01.02_-_11.26.22.02.DVR.mp4)
* [Baritone](https://github.com/cabaletta/baritone) integration
  * Baritone Goals synced as temporary waypoints
  * [Point and Click Travel](https://cdn.discordapp.com/attachments/1005598555186139156/1125306712300204082/Base_Profile_2023.07.02_-_23.04.34.09.DVR.mp4)
* [Waystones](https://legacy.curseforge.com/minecraft/mc-mods/waystones) and [Fabric Waystones](https://legacy.curseforge.com/minecraft/mc-mods/fabric-waystones) integration
  * Syncs Waystones as temporary waypoints
* [Portals Highlighting in Minimap and WorldMap](https://cdn.discordapp.com/attachments/1127463054804779028/1133251771217752175/Base_Profile_2023.07.24_-_21.02.36.03.mp4)
* [Portal Skip Highlighting in Minimap and WorldMap](https://cdn.discordapp.com/attachments/1029572347818151947/1109656254265163816/Base_Profile_2023.05.20_-_18.34.34.34.DVR.mp4). Detects chunks where a portal could have been loaded.
* OldChunks Highlighting in Minimap and WorldMap.
  * Intended for use on 2b2t. Highlights chunks that were generated in 1.12.2 or lower in the Overworld and Nether based on the natural blocks present in the chunk.
* [Transparent minimap background instead of wasted black screen space.](https://cdn.discordapp.com/attachments/963821382569979904/1088651890335686716/2023-03-23_19.26.36.png)
* [Fast map region writes](https://cdn.discordapp.com/attachments/963821382569979904/1049947847467995196/Temurin-1.8.0_345_2022.12.06_-_22.44.28.05.DVR.mp4). Prevent missed chunks in map while traveling at very high speeds.
* Allow multiple MC instances to read/write to the same map concurrently
* Transparent obsidian roof. Useful for mapping 2b2t spawn.
* Setting to always view and create waypoints in the Overworld when in Nether.
* [Render server view distance square around the player.](https://cdn.discordapp.com/attachments/971140948593635335/1109733753686851594/Temurin-1.8.0_352_2023.03.29_-_21.37.26.35.DVR.mp4)
* WorldMap GUI:
  * WorldMap zoom unlocked
  * GUI on WorldMap to pan the map to user entered coordinates.
  * WorldMap Follow mode and GUI button
  * F1 on WorldMap hides GUI and overlays
* Waypoints GUI:
  * [Search](https://cdn.discordapp.com/attachments/971140948593635335/1109736194163617842/Base_Profile_2023.05.04_-_16.48.38.73.DVR.mp4)
  * Display distance to waypoints on Waypoints GUI
  * Always sort enabled waypoints before disabled waypoints
  * GUI button to enable/disable all waypoints
* [Waypoint Beacons](https://cdn.discordapp.com/attachments/971140948593635335/1125611814089134180/2023-07-03_19.18.51.png)
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
