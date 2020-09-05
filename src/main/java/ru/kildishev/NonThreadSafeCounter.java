package ru.kildishev;

public class NonThreadSafeCounter {
    private volatile int i = 0;

    public void increment() {
        i++;
    }
}
