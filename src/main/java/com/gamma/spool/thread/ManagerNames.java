package com.gamma.spool.thread;

public enum ManagerNames {

    ENTITY("entityManager"),
    BLOCK("blockManager"),
    DIMENSION("dimensionManager"),
    DISTANCE("distanceManager");

    final String name;

    ManagerNames(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
