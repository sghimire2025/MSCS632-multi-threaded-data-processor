package main

import (
	"context"
	"errors"
	"log"
	"math/rand"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"
)

// Job represents a unit of work to be processed.
type Job struct {
	ID    int
	Value int
}

// Result represents the output of processing a Job.
type Result struct {
	JobID  int
	Output int
	Err    error
}

func main() {
	// ----- Logging setup -----
	logger := log.New(os.Stdout, "[DataProcessor] ", log.LstdFlags|log.Lmicroseconds)

	// Seed RNG for demo purposes
	rand.Seed(time.Now().UnixNano())

	// ----- Concurrency management: context with cancel -----
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// Graceful shutdown on Ctrl+C
	sigCh := make(chan os.Signal, 1)
	signal.Notify(sigCh, syscall.SIGINT, syscall.SIGTERM)

	go func() {
		sig := <-sigCh
		logger.Printf("Received signal %v, initiating graceful shutdown...", sig)
		cancel()
	}()

	// ----- Shared resource queues -----
	const (
		numJobs    = 20
		numWorker = 4
	)

	jobs := make(chan Job, numJobs)     // Shared job queue
	results := make(chan Result, numJobs) // Shared results queue

	var wg sync.WaitGroup

	// ----- Start worker threads (goroutines) -----
	for i := 1; i <= numWorker; i++ {
		wg.Add(1)
		go worker(ctx, i, jobs, results, &wg, logger)
	}

	// ----- Producer: enqueue jobs -----
	go func() {
		defer close(jobs)
		for i := 1; i <= numJobs; i++ {
			// Random values, some negative to trigger errors
			v := rand.Intn(25) - 5 // range: -5 .. 19
			job := Job{ID: i, Value: v}
			logger.Printf("Enqueuing job %d (value=%d)", job.ID, job.Value)

			select {
			case <-ctx.Done():
				logger.Printf("Producer: context cancelled, stopping job enqueue")
				return
			case jobs <- job:
			}
		}
	}()

	// ----- Collect results in a separate goroutine -----
	var collectWg sync.WaitGroup
	collectWg.Add(1)

	go func() {
		defer collectWg.Done()
		for res := range results {
			if res.Err != nil {
				logger.Printf("Result: job %d failed: %v", res.JobID, res.Err)
			} else {
				logger.Printf("Result: job %d succeeded, output=%d", res.JobID, res.Output)
			}
		}
		logger.Println("Result collector finished (results channel closed)")
	}()

	// ----- Wait for workers to finish -----
	wg.Wait()
	// All workers exited; safe to close results
	close(results)
	// Wait for collector to finish
	collectWg.Wait()

	logger.Println("All jobs processed. Exiting.")
}

// worker is a worker thread (goroutine) that processes jobs from the shared queue.
func worker(
	ctx context.Context,
	id int,
	jobs <-chan Job,
	results chan<- Result,
	wg *sync.WaitGroup,
	logger *log.Logger,
) {
	defer wg.Done()

	logger.Printf("[worker-%d] started", id)
	defer logger.Printf("[worker-%d] stopped", id)

	for {
		select {
		case <-ctx.Done():
			logger.Printf("[worker-%d] context cancelled: %v", id, ctx.Err())
			return

		case job, ok := <-jobs:
			if !ok {
				// No more jobs; queue is closed
				logger.Printf("[worker-%d] jobs channel closed, exiting", id)
				return
			}

			logger.Printf("[worker-%d] processing job %d (value=%d)", id, job.ID, job.Value)

			// Safe processing with panic recovery
			output, err := safeProcess(job.Value, logger, id, job.ID)

			// Non-blocking respect to context when sending result
			select {
			case <-ctx.Done():
				logger.Printf("[worker-%d] context cancelled before sending result for job %d", id, job.ID)
				return
			case results <- Result{JobID: job.ID, Output: output, Err: err}:
			}
		}
	}
}

// safeProcess wraps process() with panic recovery for "exceptional" conditions.
func safeProcess(value int, logger *log.Logger, workerID, jobID int) (int, error) {
	defer func() {
		if r := recover(); r != nil {
			logger.Printf("[worker-%d] PANIC while processing job %d: %v", workerID, jobID, r)
		}
	}()

	return process(value)
}

// process simulates some CPU-bound work and may return errors.
func process(value int) (int, error) {
	// Simulate processing time
	time.Sleep(time.Duration(rand.Intn(400)+100) * time.Millisecond)

	// Example of "exceptional" error conditions
	if value < 0 {
		return 0, errors.New("negative values are not allowed")
	}

	// Simulate a rare panic for demonstration
	if value == 13 {
		panic("unlucky value 13 encountered")
	}

	// Normal processing: square the value
	return value * value, nil
}
