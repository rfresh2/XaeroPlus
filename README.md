# XaeroPlus
Xaero runtime modifications

This is still WIP. There may be bugs or weirdness.

Xaero WorldMap version: XaerosWorldMap_1.26.2_Forge_1.12

Xaero Minimap version: Xaeros_Minimap_22.13.0_Forge_1.12

Both must be present. Using any version other than these two will most likely cause issues.

# Modifications

* WorldMap zoom unlocked
* WorldMap and Waypoint directories are indexed by Multiplayer server name instead of IP
* Increase speed of loading minimap and worldmap regions on world/dimension change.
  * Most noticeable when you have 50k+ regions per dimension
* GUI to enter coordinates to pan the world map to.

# WIP

* Improve WorldMap performance with very small zooms
  * With the unlocked zoom, loading many regions in view is very time (and VRAM) intensive. 
