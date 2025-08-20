package com.gamma.spool.api.statistics;

/**
 * Classes should implement this interface if they wish to receive
 * statistics about Spool (and the game itself through Spool).
 */
public interface IStatisticReceiver {

    /**
     * Function that is called when a new statistic is sent by Spool.
     * <p>
     * The statistic objects may be reused; do not store them
     * across statistics iterations.
     * </p>
     * <p>
     * This function should be thread-safe (safe with other
     * functions it may call), as Spool may create a new thread
     * for statistics creation and invocation. This function will
     * *never* be run twice at the same time by the same thread.
     * </p>
     * 
     * @param statistic The statistic (provided by Spool).
     */
    void onNewStatistics(Statistic statistic);

    /**
     * The interval at which Spool should send statistics data.
     * <p>
     * If the interval is set <100 μs, Spool will treat the time amount
     * as 100 μs.
     * </p>
     * 
     * @return The time interval.
     */
    TimeAmount interval();

    /**
     * If the statistic receiver is enabled. This allows mods to tell Spool not to send them
     * data.
     * 
     * @return If the statistic receiver should be sent data (is enabled).
     */
    boolean isEnabled();
}
