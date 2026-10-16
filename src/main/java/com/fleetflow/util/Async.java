package com.fleetflow.util;

import javafx.concurrent.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Runs computations on a daemon thread pool and delivers the outcome on the JavaFX thread. */
public final class Async {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "fleetflow-worker");
        t.setDaemon(true);
        return t;
    });

    private Async() {
    }

    /**
     * Executes {@code work} off the UI thread.
     *
     * @param onSuccess called on the JavaFX thread with the result
     * @param onError   called on the JavaFX thread if the work threw
     */
    public static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException()));
        POOL.execute(task);
    }
}
