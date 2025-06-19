# LMTM

The Legacy Multithreading Mod, or LMTM for short, is a mod that aims to bring the joys (and horror if you're a mod developer) of multithreading to older versions. It is fully serverside and does not modify the client in any way except to add F3 debug data (optional).

This mod is a heavy work in progress and is incompatible with the majority of mods.

LMTM requires GTNHLib and UniMixins as dependencies.

## Current Progress
LMTM is very unstable, however for the most part it does not crash whenever not paired with other mods.
Chunk generating/loading may be broken as the system is not threadsafe.

## Performance Improvements
So far from my (admittedly quite brief testing) I am seeing improvements of up to 10x with a 16-chunk render distance world (~10 MSPT -> ~1 MSPT).
I hope to get this even higher, though without large-scale reworking of systems that I can barely begin to comprehend it won't be soon.

## TODO List
- ~~Fix chunk generation.~~
- Fix slowed down chunk loading process during world load/creation.
- Finish compatibility with GTNH's Hodgepodge.
- Debug until 99% of issues have been fixed (concurrency and such).
- Begin the compatibility spree of forking/PRing/mixin-ing other mods in order to increase thread safety.
-   Fix issues with world chunk saving/loading where the *entire world is deleted* (mod conflict).

In the far, far future I would love to port this mod to other versions, hence the naming scheme of "Legacy" Multithreading Mod, though we'll just have to see.


also please dont laugh at my code, its crap i know but im not that smart 3:
