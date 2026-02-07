package com.gamma.spool.util;

import java.util.Arrays;

import com.gamma.spool.core.SpoolLogger;

import cpw.mods.fml.common.eventhandler.Event;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class BusLatchBuilder {

    private final ObjectList<Class<? extends Event>> pre = new ObjectArrayList<>();
    private Class<? extends Event> post;

    public BusLatchBuilder addPrerequisite(Class<? extends Event> event) {
        pre.add(event);
        return this;
    }

    public BusLatchBuilder setDependent(Class<? extends Event> event) {
        post = event;
        return this;
    }

    public void build() {
        BusLatch latch = new BusLatch();
        latch.addPreEvent(pre);
        latch.addPostEvent(post);
        SpoolLogger.info("Built new bus latch: (dependent: {}, prereqs: {})", post, Arrays.toString(pre.toArray()));
    }
}
