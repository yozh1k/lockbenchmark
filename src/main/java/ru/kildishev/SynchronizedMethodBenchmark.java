package ru.kildishev;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class SynchronizedMethodBenchmark {
    private NonThreadSafeCounter counter;
    private SynchronizedCounterWrapper synchronizedCounterWrapper;

    @Setup(Level.Iteration)
    public void setUpCounter() {
        synchronizedCounterWrapper = new SynchronizedCounterWrapper();
        counter = new NonThreadSafeCounter();
    }

    private class SynchronizedCounterWrapper {
        private synchronized void doMeasure() {
            counter.increment();
        }
    }

    @Threads(1)
    @Benchmark
    public void singleThreadMeasure() {
        synchronizedCounterWrapper.doMeasure();
    }


    @Threads(2)
    @Benchmark
    public void twoThreadMeasure() {
        synchronizedCounterWrapper.doMeasure();
    }

    @Threads(8)
    @Benchmark
    public void coreCountThreadMeasure() {
        synchronizedCounterWrapper.doMeasure();
    }

}
