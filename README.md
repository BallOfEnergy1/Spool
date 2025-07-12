# Spool

Spool is a mod that aims to bring the joys (and horror if you're a mod developer) of multithreading to older versions. It is fully serverside and does not modify the client in any way except to add F3 debug data (optional).

This mod is a heavy work in progress and is incompatible with the majority of mods.

Spool requires GTNHLib and UniMixins as dependencies.

## Current Progress
Spool is very unstable, however for the most part it does not crash whenever not paired with other mods.
Chunk generating/loading may be broken as the system is not threadsafe.

## Performance Improvements
So far from my (admittedly quite brief testing) I am seeing improvements of up to 10x with a 16-chunk render distance world (~10 MSPT -> ~1 MSPT).
I hope to get this even higher, though without large-scale reworking of systems that I can barely begin to comprehend it won't be soon.

## TODO List
- ~~Fix chunk generation.~~
- ~~Fix slowed down chunk loading process during world load/creation.~~
- ~~Finalize fixing slow world saving (15s world save with 32 chunk render distance as of now)~~
  - As it turns out, the above few are (even larger) issues without Spool, so I don't particularly need to worry about them.
  - I'm considering making an additional mod that can be paired with Spool to help enhance the speed of the world noise generation, however since it would change worldgen entirely, it will be separate from Spool.
- ~~Finish compatibility with GTNH's Hodgepodge.~~
  - This has been done with my [fork of Hodgepodge](https://github.com/BallOfEnergy1/Hodgepodge).
- ~~Editable configs in-game.~~
- ~~Finish dimension-based threading config option.~~
  - Still need to test this a bit, though this is now the default config option.
- ~~Distance-based threading!~~
  - All that's left for this is testing long-distance updates and forced chunks.
  - Other than the above, completely finished and polished!
- ~~Fix the builtin MC profiler (crashes the game).~~
  - Not only fixed crashes, but also added an extra config option for viewing the section times inside threads (see: `B:"Use better task profiling?"`).
- Truly make concurrent world access :ayo:
- Debug until 99% of issues have been fixed (concurrency and such).
- Begin the compatibility spree of forking/PRing/mixin-ing other mods in order to increase thread safety.
  - ~~Fix issues with world chunk saving/loading where the *entire world is deleted* (mod conflict).~~
    - Issue caused by NEID.

In the far, far future I would love to port this mod to other versions, though we'll just have to see.

# Tested Mods
Tested mods can be found in Spool's GitHub wiki; these are mods that have confirmed compatibility/incompatibility with Spool (versions specified).


also please dont laugh at my code, its crap i know but im not that smart 3:
