package dev.sghimire;

import dev.sghimire.model.DataTask;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Worker implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(Worker.class.getName());

    private final int workerId;
    private final TaskQueue queue;
    private final List<String> sharedResults;

    public Worker(int workerId, TaskQueue queue, List<String> sharedResults) {
        this.workerId = workerId;
        this.queue = queue;
        this.sharedResults = sharedResults;
    }

    @Override
    public void run() {
        LOGGER.info(() -> "Worker " + workerId + " started.");
        try {
            while (true) {
                DataTask task = queue.getTask(); // may block / throw InterruptedException

                if (task.isPoisonPill()) {
                    LOGGER.info(() -> "Worker " + workerId + " received poison pill. Stopping.");
                    break;
                }
                try {
                    String result = process(task);
                    // write to shared result list in a thread-safe way
                    synchronized (sharedResults) {
                        sharedResults.add(result);
                    }
                    LOGGER.info(() -> "Worker " + workerId + " completed task " + task.getId());
                } catch (Exception ex) {
                    // Handle any processing errors
                    LOGGER.log(Level.SEVERE,"Worker " + workerId + " failed to process task " + task.getId(), ex);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Worker " + workerId + " interrupted.", e);
        }

        LOGGER.info(() -> "Worker " + workerId + " finished.");
    }

    /** Simulates some computational work with delay. */
    private String process(DataTask task) throws InterruptedException {
        long start = System.currentTimeMillis();

        // Simulate computation delay
        long sleepMs = ThreadLocalRandom.current().nextLong(100, 500);
        Thread.sleep(sleepMs);

        // Simple “processing”: convert payload to upper case
        String processedPayload = task.getPayload().toUpperCase(Locale.ROOT);

        long duration = System.currentTimeMillis() - start;
        return "worker=" + workerId +
                ", taskId=" + task.getId() +
                ", payload=" + processedPayload +
                ", durationMs=" + duration;
    }
}
