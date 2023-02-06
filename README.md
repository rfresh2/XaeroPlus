# XaeroPlus

This is a Forge 1.12.2 mod that depends on and modifies Xaero's WorldMap and Minimap.

# Download

Download from [Github Releases](https://github.com/rfresh2/XaeroPlus/releases) 

Or from [GitHub actions](https://github.com/rfresh2/XaeroPlus/actions?query=branch%3Amainline+)

# Xaero Versions

Xaero WorldMap version: [1.28.9](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map/files/4381131)

Xaero Minimap version: [23.1.0](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap/files/4381108)

You need to download and include these mods in your Forge mods folder **in addition** to `xaeroplus.jar` (3 jars total).

Using any official Xaero mod version other than these two may cause crashes.

# Modifications

* WorldMap and Waypoint directories are indexed by Multiplayer server name instead of IP. 
  * e.g. "Multiplayer_connect.2b2t.org" -> "Multiplayer_2b2t"
  * **You will need to manually change the name of any existing folder.** 
    * In both `.minecraft/XaeroWaypoints/` and `.minecraft/XaeroWorldMap/`
* Overworld dimension is stored in the "DIM0" directory instead of "null"
  * **You will need to manually change the name of any existing folder** 
    * In `.minecraft/XaeroWorldMap/Multiplayer_<server name>/`
* WorldMap zoom unlocked
* Worldmap Follow mode and GUI button
* NewChunks Highlighting in MiniMap and WorldMap.
* [WorldDownloader 4.1.1.0](https://github.com/Pokechu22/WorldDownloader/) integration
  * Highlights chunks as they are downloaded in the Minimap and WorldMap.
* Faster map tile zip reads/writes
* Allow multiple MC instances to read/write to the same map concurrently
* Transparent obsidian roof (optional). Useful for mapping 2b2t spawn.
* Faster region writes to prevent missed chunks in map while traveling at very high speeds.
* Option to always view and create waypoints in the Overworld when in Nether.
* GUI on WorldMap to pan the map to user entered coordinates.
* Display distance to waypoints on Waypoints GUI (like JourneyMap)
* Always sort enabled waypoints before disabled waypoints
* GUI button to enable/disable all waypoints
* Minecraft world always renders in background while in a Xaero GUI (for client travel mods compatibility)
* ~~Increased region loading performance for large worlds. Fixes "hangs" on joining a server or switching dimensions.~~
  * Now included in XaeroWorldMap 1.26.4

Configurations are in the normal Xaero WorldMap settings GUI.

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
