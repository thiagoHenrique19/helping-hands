package com.helpinghands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Starts all tasks at the same moment and collects each outcome, thrown exceptions included. */
public final class ConcurrentRunner {

    public record Outcome<T>(T value, Throwable error) {
        public boolean succeeded() {
            return error == null;
        }
    }

    private ConcurrentRunner() {
    }

    public static <T> List<Outcome<T>> runAll(List<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Outcome<T>>> futures = new ArrayList<>();
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return new Outcome<>(task.call(), null);
                    } catch (Throwable error) {
                        return new Outcome<>(null, error);
                    }
                }));
            }
            ready.await(10, TimeUnit.SECONDS);
            go.countDown();
            List<Outcome<T>> outcomes = new ArrayList<>();
            for (Future<Outcome<T>> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            executor.shutdownNow();
        }
    }
}
