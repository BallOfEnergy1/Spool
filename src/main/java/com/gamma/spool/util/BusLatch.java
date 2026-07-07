package com.gamma.spool.util;

import java.util.Collection;
import java.util.concurrent.Phaser;
import java.util.concurrent.locks.StampedLock;

import net.minecraft.world.World;

import com.gamma.spool.core.SpoolLogger;
import com.google.common.base.Throwables;

import cpw.mods.fml.common.eventhandler.Event;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

public class BusLatch {

    public static final Int2ObjectMap<LatchesPerWorld> perWorldBusLatches = Int2ObjectMaps
        .synchronize(new Int2ObjectOpenHashMap<>());

    public final ObjectSet<Latch> pre;
    public Latch post;

    public BusLatch() {
        this(true);
    }

    private BusLatch(boolean registerOverworld) {
        pre = ObjectSets.synchronize(new ObjectArraySet<>());
        if (registerOverworld) {
            // Add to overworld.
            perWorldBusLatches.computeIfAbsent(0, LatchesPerWorld::new)
                .add(this);
        }
    }

    public void addPreEvent(Class<? extends Event> event) {
        this.pre.add(new Latch(event));
    }

    public void addPostEvent(Class<? extends Event> event) {
        this.post = new Latch(event);
    }

    public void addPreEvent(Collection<Class<? extends Event>> events) {
        for (Class<? extends Event> event : events) {
            addPreEvent(event);
        }
    }

    public static void preEvent(Class<? extends Event> event, int dim) {
        // Wait until we're ready to progress.
        LatchesPerWorld lpw = perWorldBusLatches.get(dim);
        if (lpw == null) return;
        for (BusLatch busLatch : lpw.latches) {
            for (Latch latch : busLatch.pre) {
                // Skip awaiting; no need to trigger since that'll be done after to restrict other threads.
                if (event != latch.event && busLatch.post.event == event) latch.await();
            }
        }
    }

    public static void postEvent(Class<? extends Event> event, int dim) {
        // Allow dependencies to progress.
        LatchesPerWorld lpw = perWorldBusLatches.get(dim);
        if (lpw == null) return;
        for (BusLatch busLatch : lpw.latches) {
            for (Latch latch : busLatch.pre) {
                latch.eventTrigger(event);
            }
        }
    }

    public static void onWorldLoad(World world) {
        if (world.isRemote) return;
        int dim = world.provider.dimensionId;
        if (dim == 0) {
            SpoolLogger.debugWarn("Bus latching ignoring overworld load...");
            return;
        }
        LatchesPerWorld overworldLPW = perWorldBusLatches.get(0);
        LatchesPerWorld newLPW = perWorldBusLatches.computeIfAbsent(dim, LatchesPerWorld::new);
        for (BusLatch busLatch : overworldLPW.latches) { // Pull from overworld; always loaded.
            newLPW.add(busLatch.copy()); // deep copy
        }
    }

    public static void onWorldUnload(World world) {
        if (world.isRemote) return;
        int dim = world.provider.dimensionId;
        if (dim == 0) {
            SpoolLogger.debugWarn(
                "Bus latching detected overworld unloading, this is unexpected behavior. This can safely be ignored if the server is shutting down.");
            return;
        }
        perWorldBusLatches.remove(dim);
    }

    public BusLatch copy() {
        BusLatch copy = new BusLatch(false);
        for (Latch latch : this.pre) {
            copy.addPreEvent(latch.event);
        }
        copy.addPostEvent(this.post.event);
        return copy;
    }

    // Per world, same objects across busses on the same world.
    public static class Latch {

        private final Phaser lock = new Phaser(0);
        public final Class<? extends Event> event;

        public Latch(Class<? extends Event> event) {
            this.event = event;
            lock.register(); // Stay locked until the event is triggered.
        }

        public void eventTrigger(Class<? extends Event> event) {
            if (this.event != event) return;
            lock.arriveAndDeregister(); // Leave unlocked forever; when the world unloads, it'll be destroyed anyway.
        }

        public void await() {
            try {
                lock.register();
                lock.awaitAdvanceInterruptibly(lock.arriveAndDeregister()); // Lock then unlock the read-lock, basically
                                                                            // checking that the write-lock is unlocked.
            } catch (InterruptedException e) {
                Throwables.propagate(e);
            }
        }
    }

    public static class LatchesPerWorld {

        private final ObjectSet<BusLatch> latches = new ObjectArraySet<>();
        private final StampedLock lock = new StampedLock();

        // optimize for computeIfAbsent lambdas (unused key).
        public LatchesPerWorld(int unused) {}

        public void add(BusLatch busLatch) {
            long stamp = lock.writeLock();
            try {
                latches.add(busLatch);
            } finally {
                lock.unlockWrite(stamp);
            }
        }

        public ObjectSet<BusLatch> getLatches() {
            return latches;
        }
    }
}
