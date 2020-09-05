package ru.kildishev;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.locks.ReentrantLock;

@State(Scope.Benchmark)
public class LockBenchmark {
    private ReentrantLock lock;
    private NonThreadSafeCounter counter;

    @Setup(Level.Iteration)
    public void setUpCounter() {
        lock = new ReentrantLock();
        counter = new NonThreadSafeCounter();
    }

    private void doMeasure() {
        lock.lock();
        counter.increment();
        lock.unlock();
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
