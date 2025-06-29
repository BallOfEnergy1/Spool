package com.gamma.spool.util;

public interface ISimulationDistanceWorld {

    void hodgepodge$preventChunkSimulation(long packedChunkPos, boolean prevent);

    SimulationDistanceHelper hodgepodge$getSimulationDistanceHelper();
}
