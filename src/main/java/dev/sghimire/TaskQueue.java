package dev.sghimire;

import dev.sghimire.model.DataTask;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Shared queue accessed by all worker threads.
 * addTask() and getTask() are synchronized to avoid race conditions.
 */
public class TaskQueue {
    private final Queue<DataTask> queue = new LinkedList<>();

    public synchronized void addTask(DataTask task) {
        queue.add(task);
        notifyAll(); // wake up waiting workers
    }

    public synchronized DataTask getTask() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // wait until something is available
        }
        return queue.remove();
    }
}

