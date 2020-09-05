package ru.kildishev;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class SynchronizedSectionBenchmark {
    private Object lock;
    private NonThreadSafeCounter counter;

    @Setup(Level.Iteration)
    public void setUpCounter() {
        lock = new Object();
        counter = new NonThreadSafeCounter();
    }

    private void doMeasure() {
        synchronized (lock) {
            counter.increment();
        }
    }


    @Threads(1)

    @Benchmark
    public void singleThreadMeasure() {
        doMeasure();
    }


    @Threads(2)

    @Benchmark
    public void twoThreadMeasure() {
        doMeasure();
    }

    @Threads(8)

    @Benchmark
    public void coreCountThreadMeasure() {
        doMeasure();
    }

}
