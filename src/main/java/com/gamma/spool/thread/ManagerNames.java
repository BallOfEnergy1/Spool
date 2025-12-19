package com.gamma.spool.thread;

public enum ManagerNames {

    ENTITY("entityManager"),
    BLOCK("blockManager"),
    DIMENSION("dimensionManager"),
    DISTANCE("distanceManager"),
    ENTITY_AI("entityAIManager"),
    CHUNK_LOAD("chunkLoadingManager"),
    THREAD_MANAGER_TIMER("threadManagerTimer"),

    // Various caches.
    HIERARCHY("higherarchyUtil");

    final String name;

    ManagerNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
