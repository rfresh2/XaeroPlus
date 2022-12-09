# XaeroPlus

This is a Forge 1.12.2 mod that depends on and modifies Xaero's WorldMap and Minimap.

# Download

Download the latest build from GitHub actions: [https://github.com/rfresh2/XaeroPlus/actions](https://github.com/rfresh2/XaeroPlus/actions) 

Or without a GitHub account: [nightly.link](https://nightly.link/rfresh2/XaeroPlus/workflows/gradle/mainline/xaeroplus-43.zip)

# Xaero Versions

Xaero WorldMap version: [1.28.4](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map/files/4127330)

Xaero Minimap version: [22.16.3](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap/files/4127312)

Both mods must be downloaded and included by you in your Forge mods folder. 

Using any version other than these two may cause crashes. 

# Modifications

* WorldMap and Waypoint directories are indexed by Multiplayer server name instead of IP. 
  * e.g. "Multiplayer_connect.2b2t.org" -> "Multiplayer_2b2t"
  * **You will need to manually change the name of any existing folder.** 
    * In both `.minecraft/XaeroWaypoints/` and `.minecraft/XaeroWorldMap/`
* Overworld dimension is stored in the "DIM0" directory instead of "null"
  * **You will need to manually change the name of any existing folder in** `.minecraft/XaeroWorldMap/Multiplayer_<server name>/`
* WorldMap zoom unlocked
* ~~Increased region loading performance for large worlds. Fixes "hangs" on joining a server or switching dimensions.~~
  * Now included in XaeroWorldMap 1.26.4
* Worldmap Follow mode and GUI button
* GUI on WorldMap to pan the map to user entered coordinates.
* Display distance to waypoints on Waypoints GUI (like JourneyMap)
* Always sort enabled waypoints before disabled waypoints
* GUI button to enable/disable all waypoints
* Minecraft world always renders in background while in a Xaero GUI (for client travel mods compatibility)
* Faster map tile zip reads/writes
* Allow multiple MC instances to read/write to the same map concurrently
* Transparent obsidian roof. Useful for mapping 2b2t spawn.
* Faster region writes to prevent missed chunks in map. Helpful if you're traveling at very high speeds.  

Certain modifications have settings that can be changed in the normal Xaero WorldMap settings GUI.

# Work In Progress

* Improve WorldMap performance with very small zooms
  * With the unlocked zoom, loading many regions in view is very time (and VRAM) intensive.
