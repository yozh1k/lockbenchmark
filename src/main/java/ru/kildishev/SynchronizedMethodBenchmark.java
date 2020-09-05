package ru.kildishev;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class SynchronizedMethodBenchmark {
    private NonThreadSafeCounter counter;
    private SyncronizedCounterWrapper syncronizedCounterWrapper;

    @Setup(Level.Iteration)
    public void setUpCounter() {
        syncronizedCounterWrapper = new SyncronizedCounterWrapper();
        counter = new NonThreadSafeCounter();
    }

    private class SyncronizedCounterWrapper {
        private synchronized void doMeasure() {
            counter.increment();
        }
    }


    @Threads(1)
    @Benchmark
    public void singleThreadMeasure() {
        syncronizedCounterWrapper.doMeasure();
    }


    @Threads(2)
    @Benchmark
    public void twoThreadMeasure() {
        syncronizedCounterWrapper.doMeasure();
    }

    @Threads(8)
    @Benchmark
    public void coreCountThreadMeasure() {
        syncronizedCounterWrapper.doMeasure();
    }

}
