package com.gamma.spool.util;

import java.util.Collection;
import java.util.concurrent.Phaser;

import com.gamma.spool.core.SpoolLogger;
import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import cpw.mods.fml.common.eventhandler.Event;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

// TODO: Optimize this class down to use less HashMap calls. Not super critical as of now, but potentially can use
// Atomics of some kind.
public class BusLatch {

    public static final Multimap<Integer, BusLatch> perWorldBusLatches = Multimaps
        .synchronizedMultimap(HashMultimap.create());

    public ObjectList<Latch> pre;
    public Latch post;

    public BusLatch() {
        this(true);
    }

    private BusLatch(boolean registerOverworld) {
        pre = ObjectLists.synchronize(new ObjectArrayList<>());
        if (registerOverworld) {
            perWorldBusLatches.put(0, this); // Add to overworld.
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
        for (BusLatch busLatch : perWorldBusLatches.get(dim)) {
            busLatch.pre.forEach(latch -> {
                // Skip awaiting; no need to trigger since that'll be done after to restrict other threads.
                if (event != latch.event && busLatch.post.event == event) latch.await();
            });
        }
    }

    public static void postEvent(Class<? extends Event> event, int dim) {
        // Allow dependencies to progress.
        for (BusLatch busLatch : perWorldBusLatches.get(dim)) {
            busLatch.pre.forEach(latch -> latch.eventTrigger(event));
        }
    }

    public static void onWorldLoad(int dim) {
        if (dim == 0) {
            SpoolLogger.info("Bus latching ignoring overworld load...");
            return;
        }
        ObjectList<BusLatch> overworldLatches;
        synchronized (perWorldBusLatches) {
            // TODO: figure out some better way to do this...
            // Realistically, its only on world load, so it shouldn't be too bad.
            overworldLatches = new ObjectArrayList<>(perWorldBusLatches.get(0));
        }
        for (BusLatch busLatch : overworldLatches) { // Pull from overworld; always loaded.
            perWorldBusLatches.put(dim, busLatch.copy());
        }
    }

    public static void onWorldUnload(int dim) {
        if (dim == 0) {
            SpoolLogger.warn(
                "Bus latching detected overworld unloading, this is unexpected behavior. This can safely be ignored if the server is shutting down.");
            return;
        }
        perWorldBusLatches.removeAll(dim);
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
}
