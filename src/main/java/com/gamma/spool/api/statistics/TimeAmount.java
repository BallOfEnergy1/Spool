package com.gamma.spool.api.statistics;

import java.util.concurrent.TimeUnit;

// We can't let this be a record. Other mods won't like it if they
// don't use Jabel.
/**
 * A utility class that combines a time and a TimeUnit into one.
 */
@SuppressWarnings("ClassCanBeRecord")
public class TimeAmount {

    private final long time;
    private final TimeUnit timeUnit;

    public TimeAmount(long time, TimeUnit timeUnit) {
        this.time = time;
        this.timeUnit = timeUnit;
    }

    public long getTime() {
        return time;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }
}
