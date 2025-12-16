package com.gamma.spool.asm.util;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class PhaseChain<T> {

    private int index = 0;
    private int end = 0;
    private boolean requireImmediatelyAfterPrevious = false;

    private boolean finishedEarly = false;
    private boolean cantFindResult = false;

    private Phase<T> first;
    private Phase<T> last;

    private final ObjectList<Phase<T>> phaseList;

    public PhaseChain() {
        phaseList = new ObjectArrayList<>();
    }

    public Phase<T> getLast() {
        return last;
    }

    public Phase<T> getFirst() {
        return first;
    }

    public PhaseChain<T> nextPhaseAnchor(ObjectObject2BooleanFunction<Phase<T>, T> phaseCriteria) {
        return nextPhase(-2, false, phaseCriteria);
    }

    public PhaseChain<T> nextPhaseAnchor(boolean requireImmediatelyAfterPrevious,
        ObjectObject2BooleanFunction<Phase<T>, T> phaseCriteria) {
        return nextPhase(-2, requireImmediatelyAfterPrevious, phaseCriteria);
    }

    public PhaseChain<T> nextPhase(ObjectObject2BooleanFunction<Phase<T>, T> phaseCriteria) {
        return nextPhase(-1, false, phaseCriteria);
    }

    public PhaseChain<T> nextPhase(boolean requireImmediatelyAfterPrevious,
        ObjectObject2BooleanFunction<Phase<T>, T> phaseCriteria) {
        return nextPhase(-1, requireImmediatelyAfterPrevious, phaseCriteria);
    }

    public PhaseChain<T> nextPhase(int anchorIndex, boolean requireImmediatelyAfterPrevious,
        ObjectObject2BooleanFunction<Phase<T>, T> phaseCriteria) {
        end++;
        Phase<T> phase = new Phase<>(this, anchorIndex, phaseCriteria);
        if (phaseList.size() == 1) first = phase;
        last = phase;
        this.requireImmediatelyAfterPrevious = requireImmediatelyAfterPrevious;
        return this;
    }

    public T getSuccessfulInput(int phase) {
        return phaseList.get(phase)
            .getSuccessfulInput();
    }

    public boolean next(T node) {
        if (isDone()) return true;
        if (phaseList.get(index)
            .check(node)) index++;
        else if (requireImmediatelyAfterPrevious) reset(true);
        return isDone();
    }

    public boolean cantFindResult() {
        return cantFindResult;
    }

    public boolean isDone() {
        if (cantFindResult) return true;
        if (finishedEarly) return true;
        return index >= end;
    }

    private void reset(boolean indexOnly) {
        int anchorIndex = phaseList.get(index >= end ? index - 1 : index).anchorIndex;
        index = anchorIndex == -1 ? 0 : -1;
        if (indexOnly) {
            requireImmediatelyAfterPrevious = false;
            cantFindResult = false;
            finishedEarly = false;
        }
    }

    public void reset() {
        reset(false);
    }

    public void setCantFindResult() {
        cantFindResult = true;
    }

    public void finish() {
        finishedEarly = true;
    }

    @Override
    public String toString() {
        return "PhaseChain [" + "index="
            + index
            + " end="
            + end
            + " requireImmediatelyAfterPrevious="
            + requireImmediatelyAfterPrevious
            + "]";
    }

    public static final class Phase<T> {

        private final PhaseChain<T> chain;

        private Phase<T> next = null;
        private final Phase<T> previous;
        private final int phaseIndex;
        private final int anchorIndex;

        private final ObjectObject2BooleanFunction<Phase<T>, T> criteria;
        private T satisfiedInput;

        public Phase(PhaseChain<T> chain, int anchorIndex, ObjectObject2BooleanFunction<Phase<T>, T> criteria) {
            this.chain = chain;
            chain.phaseList.add(this);
            this.phaseIndex = chain.phaseList.indexOf(this);
            if (phaseIndex == 0) {
                this.previous = null;
                this.anchorIndex = anchorIndex;
            } else {
                this.previous = chain.phaseList.get(phaseIndex - 1);
                this.previous.next = this;
                if (this.previous.anchorIndex > -1 && anchorIndex == -1) this.anchorIndex = this.previous.anchorIndex;
                else if (anchorIndex == -2) this.anchorIndex = this.phaseIndex;
                else this.anchorIndex = anchorIndex;
            }
            this.criteria = criteria;
        }

        public PhaseChain<T> getChain() {
            return chain;
        }

        public Phase<T> getNext() {
            return next;
        }

        public Phase<T> getPrevious() {
            return previous;
        }

        public int getPhaseIndex() {
            return phaseIndex;
        }

        public T getSuccessfulInput() {
            return satisfiedInput;
        }

        public boolean check(T input) {
            if (!criteria.getBoolean(this, input)) return false;

            satisfiedInput = input;
            return true;
        }
    }

    @FunctionalInterface
    public interface ObjectObject2BooleanFunction<T, U> {

        boolean getBoolean(T arg1, U arg2);
    }
}
