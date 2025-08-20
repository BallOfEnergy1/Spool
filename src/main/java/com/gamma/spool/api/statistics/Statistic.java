package com.gamma.spool.api.statistics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A general class that holds information about Spool's systems for a certain point in time.
 */
public class Statistic {

    private static final AtomicLong lastStatisticTime = new AtomicLong(0);

    /**
     * The time since the last recorded statistic from Spool.
     * This may be extremely high for the first statistic after a pause;
     * these such values should be considered erroneous and should be ignored.
     */
    public final TimeAmount timeSinceLastStatistic;
    /**
     * The time at which the statistic was recorded. Multiple statistics can happen
     * within the same time (the same ` timeOfStatistic ` value).
     */
    public final TimeAmount timeOfStatistic;

    /**
     * Holds all statistics information about Spool (and the server).
     */
    public final SpoolStatistic spoolStatistic;

    /**
     * External (non-Spool) classes should NEVER use this constructor.
     */
    public Statistic(SpoolStatistic spoolStatistic) {
        long time = System.nanoTime();
        timeSinceLastStatistic = new TimeAmount(time - lastStatisticTime.getAndSet(time), TimeUnit.NANOSECONDS);
        timeOfStatistic = new TimeAmount(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        this.spoolStatistic = spoolStatistic;
    }
}
