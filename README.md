# Spool

Spool is a mod that aims to bring the joys (and horror if you're a mod developer) of multithreading to older versions.

Spool is intended to be a server-side mod; however, it *can* be used on the client side. Having the mod on the client is fully optional (server-side can have it while client-side doesn't.)
The client side part of Spool allows for showing debug information in the F3 menu, some simple client chunk performance improvements, among other changes.
Threading only partially works on the client side, and distance-threading is forcefully disabled when running on a client.

This mod is a heavy work in progress and is incompatible with the majority of mods.

Spool requires GTNHLib and UniMixins as dependencies.

## Current Progress
Spool itself is stable; however, issues can arise when paired with other mods.

Chunk loading/generating is *mostly* fixed; however, some issues may arise with incompatible mods.
### Spool can corrupt your worlds!
Be cautious when it comes to using Spool on worlds that you care about, especially if pairing it with potentially incompatible mods!

[![Build and test](https://github.com/BallOfEnergy1/Spool/actions/workflows/build-and-test.yml/badge.svg?branch=master)](https://github.com/BallOfEnergy1/Spool/actions/workflows/build-and-test.yml)

## Performance Improvements
Performance improvements can primarily be seen on servers; however, I have not been able to gather sufficient data to make any solid claims.

The data that I *do* have shows that, on average (in a LAN world with three clients, including the host), Spool saves approximately 10 ms off of the server thread. This data could be biased due to hardware however, and it should
not be taken as a guaranteed performance boost.

What I *can* say for certain is that the overhead is much lower than the performance gains, so you *will* see speed increases in 99% of scenarios.

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
  - ~~Still need to test this a bit, though this is now the default config option.~~
- ~~Distance-based threading!~~
  - All that's left for this is testing long-distance updates and forced chunks.
  - Other than the above, it's finished and polished!
- ~~Fix the builtin MC profiler (crashes the game).~~
  - Not only fixed crashes, but also added an extra config option for viewing the section times inside threads (see: `B:"Use better task profiling?"`).
- ~~Truly make concurrent world access :ayo:~~
  - This is done! ~~Concurrent world may have some bugs.~~
- Debug until 99% of issues have been fixed (concurrency and such).
- Begin the compatibility spree of forking/PRing/mixin-ing other mods in order to increase thread safety.
  - ~~Fix issues with world chunk saving/loading where the *entire world is deleted* (mod conflict).~~
    - ~~Issue caused by NEID.~~
    - EndlessIDs compatibility has since been added, NEID is unsupported.

In the far, far future I would love to port this mod to other versions, though we'll just have to see.

# License Details
Spool uses some shadowed dependencies, as listed below:
 - [ByteBuddy](https://github.com/raphw/byte-buddy) ([Maven](https://mvnrepository.com/artifact/net.bytebuddy/byte-buddy)): Licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
   - [ByteBuddy Agent](https://github.com/raphw/byte-buddy) ([Maven](https://mvnrepository.com/artifact/net.bytebuddy/byte-buddy-agent)): Licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
 - [JCTools](https://github.com/JCTools/JCTools) ([Maven](https://mvnrepository.com/artifact/org.jctools/jctools-core)): Licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)
 - [JDBC](https://github.com/xerial/sqlite-jdbc) ([Maven](https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc)): Licensed under [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)

# Tested Mods
Tested mods can be found in Spool's GitHub wiki; these are mods that have confirmed compatibility/incompatibility with Spool (versions specified).
