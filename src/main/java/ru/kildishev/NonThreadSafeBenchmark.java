package ru.kildishev;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class NonThreadSafeBenchmark {

    private NonThreadSafeCounter counter;

    @Setup(Level.Iteration)
    public void setUpCounter() {

        counter = new NonThreadSafeCounter();
    }


    @Threads(1)

    @Benchmark
    public void singleThreadMeasure() {
        counter.increment();
    }


}
