package dev.sghimire;

import dev.sghimire.model.DataTask;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataProcessingApp {

    private static final Logger LOGGER = Logger.getLogger(DataProcessingApp.class.getName());

    public static void main(String[] args) {
        configureLogging();

        int numberOfWorkers = 4;
        int numberOfTasks = 20;

        TaskQueue queue = new TaskQueue();
        List<String> sharedResults = Collections.synchronizedList(new ArrayList<>());

        // Start worker threads
        List<Thread> workers = new ArrayList<>();
        for (int i = 1; i <= numberOfWorkers; i++) {
            Thread t = new Thread(new Worker(i, queue, sharedResults), "worker-" + i);
            workers.add(t);
            t.start();
        }

        // Enqueue tasks
        LOGGER.info("Main: Enqueuing tasks.");
        for (int i = 1; i <= numberOfTasks; i++) {
            queue.addTask(new DataTask(i, "payload-" + i));
        }

        // Add poison pills so workers can terminate safely
        for (int i = 0; i < numberOfWorkers; i++) {
            queue.addTask(DataTask.poisonPill());
        }

        // Wait for all workers to finish
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.log(Level.WARNING, "Main thread interrupted while joining workers.", e);
            }
        }

        // Write results to a shared output file
        Path outputFile = Path.of("processing_results.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            synchronized (sharedResults) {
                for (String line : sharedResults) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            LOGGER.info("Main: Results written to " + outputFile.toAbsolutePath());
        } catch (IOException e) {
            // File I/O exception handling
            LOGGER.log(Level.SEVERE, "Failed to write results file.", e);
        }

        LOGGER.info("Main: Application completed.");
    }

    /**
     * Simple console logger setup.
     */
    private static void configureLogging() {
        Logger root = Logger.getLogger("");
        root.setLevel(Level.INFO);
        for (var handler : root.getHandlers()) {
            root.removeHandler(handler);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.INFO);
        root.addHandler(handler);
    }
}
