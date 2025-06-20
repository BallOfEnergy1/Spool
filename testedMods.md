# Tested Mods List

This is a list of tested mods that are confirmed to be compatible/incompatible with Spool.
This list will be updated as often as possible.

Mods not listed here have not been tested and have a chance to cause world-breaking bugs.

## Compatible

- Angelica (1.0.0-beta46; GTNH)
  - There is an odd issue where the client thread completely locks up with one of the chunk render threads, though I cannot pinpoint the issue. Closing and restarting the game is the only solution.
- AppleCore (3.1.1)
- Applied Energistics 2 (rv3-beta-408-GTNH)
  - This does have minor issues, such as cables not placing correctly when a world is first created, though this can be fixed with relogging.
  - Along with this, items cannot be shift-clicked from a terminal.
  - Aside than the above issues, it is stable.
- Aroma1997Core (1.0.2.16)
- AsieLib (0.4.9)
- bdlib (1.10.0; GTNH)
- BetterBuildersWands (0.8.1-1.7.10r92+aec06c3)
- Biomes O' Plenty (2.1.0.2308-universal)
- bogosorter (1.2.0; GTNH)
- bugtorch (1.2.14; GTNH)
- Chisel (2.15.2; GTNH)
- CodeChickenCore (1.4.3; GTNH)
- Controlling (1.0.0.8)
- CraftTweaker (3.3.1)
- Duradisplay (1.3.2; GTNH)
- EnderCore (0.2.0.40_beta)
- GTNH Lib (0.6.31; GTNH)
- Hodgepodge (2.6.78; GTNH)
  - Read note in the incompatible section about the `B:addSimulationDistance` config setting.
- JourneyMap (5.2.9)
- ModularUI2 (2.1.16; GTNH)
- NEIAddons (1.16.0; GTNH)
- netherportalfix (1.3.0)
- NotEnoughItems (2.7.56; GTNH)
- Spark (1.10.19)
- Walia (1.5.10)
- WaliaHarvestability (1.2.1)
- WaliaPlugins (0.2.0-25)
- Wawla (1.3.0)


## Incompatible
### Hodgepodge
Hodgepodge's `B:addSimulationDistance` setting conflicts with the mixins that Spool uses to thread and add safety to some core classes, so it must be disabled for Spool to work properly.
### Archaic Fix
Archaic Fix has a builtin log trigger for whenever a thread attempts to access the world, something that Spool does *extremely* often (it's the entire point).
### Forge Multipart
Forge Multipart doesn't play nicely with some of the modifications that Spool makes, which causes a crash.
### NotEnoughIDs (GTNH)
NotEnoughIDs makes changes to the way chunk data is saved, causing massive data loss when saving a world.
