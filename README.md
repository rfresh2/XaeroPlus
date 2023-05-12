# XaeroPlus

XaeroPlus is a Forge 1.12.2 mod that depends on and modifies Xaero's WorldMap and Minimap with extra 
features and performance improvements - particularly for use on anarchy servers like 2b2t.

<details>
<summary>Example Map</summary>
<p align="center">
  <img src="https://i.imgur.com/oYYhDoS.jpeg">
</p>
</details>

# Download

Download from [Github Releases](https://github.com/rfresh2/XaeroPlus/releases) 

Or from [GitHub actions](https://github.com/rfresh2/XaeroPlus/actions?query=branch%3Amainline+)

# Xaero Versions

Xaero WorldMap version: [1.30.1](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map/files/4516852)

Xaero Minimap version: [23.4.1](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap/files/4516832)

Download and include these mods in your Forge mods folder **in addition** to `xaeroplus-xx.jar` (3 jars total).

Any other Xaero WorldMap/Minimap version may cause crashes.

# Modifications

* WorldMap zoom unlocked
* Dimension hot-switching on WorldMap and Minimap
* Fast map region writes. Prevent missed chunks in map while traveling at very high speeds.
* WorldMap Follow mode and GUI button
* Adjustable minimap scaling that increases how many chunks are visible
* NewChunks Highlighting in MiniMap and WorldMap.
* [WorldDownloader 4.1.1.0](https://github.com/Pokechu22/WorldDownloader/) integration (does not work with Future's Forge WDL jar)
  * Highlights chunks as they are downloaded in the Minimap and WorldMap.
* Transparent minimap background instead of wasted black screen space.
* Allow multiple MC instances to read/write to the same map concurrently
* Transparent obsidian roof. Useful for mapping 2b2t spawn.
* Option to always view and create waypoints in the Overworld when in Nether.
* GUI on WorldMap to pan the map to user entered coordinates.
* F1 on WorldMap hides GUI and overlays
* Waypoints GUI:
  * Search
  * Display distance to waypoints on Waypoints GUI
  * Always sort enabled waypoints before disabled waypoints
  * GUI button to enable/disable all waypoints
* Minecraft world always renders in background while in a Xaero GUI (for client travel mods compatibility)
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

# Other Useful Tools

* Convert JourneyMap World Files to Xaero: [JMToXaero](https://github.com/Entropy5/JMtoXaero)
* Convert JourneyMap Waypoints to Xaero: [JMWaypointsToXaero](https://github.com/rfresh2/JMWaypointsToXaero)
* 2b2t Atlas Waypoints to Xaero: [JMWaypointsToXaero/atlas](https://github.com/rfresh2/JMWaypointsToXaero/tree/atlas)
* Convert MC Region Files to Xaero: [JMToXaero/Region-Scripts](https://github.com/Entropy5/JMtoXaero/blob/Region-Scripts/src/main/java/com/github/entropy5/RegionToXaero.java)
* 2b2t 256k WDL Xaero Map (20GB): [mc-archive](https://data.mc-archive.org/s/eFDEy2XKof83Kez)
* Xaero World Merger: [JMToXaero/Region-Scripts](https://github.com/Entropy5/JMtoXaero/blob/Region-Scripts/src/main/java/com/github/entropy5/XaeroRegionMerger.java) 
  * Can be used to merge 256k WDL into an existing world. With optional darkening only on tiles from 256K

# Support

Message me on Discord: `rfresh#2222`
