package com.gamma.spool.thread;

public enum ManagerNames {

    DIMENSION("dimensionManager"),
    DISTANCE("distanceManager"),
    ENTITY_AI("entityAIManager"),
    CHUNK_LOAD("chunkLoadingManager"),
    THREAD_MANAGER_TIMER("threadManagerTimer");

    final String name;

    ManagerNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
