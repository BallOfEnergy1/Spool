package com.gamma.spool.thread;

public abstract class RollingAverageWrapper implements IThreadManager {

    private final ExponentialMovingAverage emaExecuting = new ExponentialMovingAverage(0.02);
    private final ExponentialMovingAverage emaOverhead = new ExponentialMovingAverage(0.02);
    private final ExponentialMovingAverage emaWaiting = new ExponentialMovingAverage(0.02);

    void updateTimes() {
        emaExecuting.next(getTimeExecuting());
        emaOverhead.next(getTimeOverhead());
        emaWaiting.next(getTimeWaiting());
    }

    @Override
    public long getAvgTimeExecuting() {
        return emaExecuting.getValue();
    }

    @Override
    public long getAvgTimeOverhead() {
        return emaOverhead.getValue();
    }

    @Override
    public long getAvgTimeWaiting() {
        return emaWaiting.getValue();
    }

    private static class ExponentialMovingAverage {

        private final double alpha;
        private double ema = -1;

        public ExponentialMovingAverage(double alpha) {
            this.alpha = alpha;
        }

        public void next(double value) {
            if (ema == -1) {
                ema = value;
            } else {
                ema = alpha * value + (1 - alpha) * ema;
            }
        }

        public long getValue() {
            return (long) ema;
        }
    }
}
