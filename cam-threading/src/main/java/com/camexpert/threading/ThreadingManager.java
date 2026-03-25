package com.camexpert.threading;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central multi-threading manager for CAM-Expert background computations.
 *
 * <p>Toolpath generation, simulation, and other CPU-intensive operations are
 * submitted as {@link ComputationJob}s. Each job runs asynchronously on a
 * managed thread pool, freeing the main (UI) thread to remain responsive.
 *
 * <h2>Architecture</h2>
 * <pre>
 *   UI Thread                ThreadingManager               Worker Threads
 *      |                          |                               |
 *      |-- submit(callable) ----> |                               |
 *      |<- ComputationJob<T> ---- |-- schedule on pool ---------->|
 *      |                          |                     (run callable)
 *      |-- getJobs() -----------> |                               |
 *      |<- [QUEUED, RUNNING, …] - |                               |
 *      |                          |<-- markCompleted / markFailed |
 *      |   (future callback fires)|                               |
 * </pre>
 *
 * <h2>Graceful cancellation</h2>
 * Worker callables must periodically check
 * {@link Thread#isInterrupted()} and throw {@link InterruptedException} (or
 * simply return early) when the flag is set. {@link ComputationJob#cancel()}
 * interrupts the worker thread and transitions the job to
 * {@link JobStatus#CANCELLED}.
 *
 * <h2>Shutdown</h2>
 * Call {@link #shutdown()} during application teardown to wait for running jobs
 * to finish and release thread resources cleanly.
 */
public final class ThreadingManager {

    private static final Logger LOG = Logger.getLogger(ThreadingManager.class.getName());

    private final ExecutorService executor;
    private final Map<String, ComputationJob<?>> jobs = new ConcurrentHashMap<>();

    /**
     * Creates a manager backed by a fixed thread pool with the given number of threads.
     *
     * @param threadCount number of worker threads (must be >= 1)
     */
    public ThreadingManager(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be >= 1, got: " + threadCount);
        }
        this.executor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "cam-worker-" + UUID.randomUUID().toString().substring(0, 8));
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Creates a manager backed by the provided {@link ExecutorService}.
     * Useful for injecting a test-controlled executor.
     *
     * @param executor the executor to use; must not be null
     */
    public ThreadingManager(ExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor must not be null");
        }
        this.executor = executor;
    }

    /**
     * Submits a callable computation as a background job.
     *
     * <p>The callable should periodically check {@link Thread#isInterrupted()} to
     * support graceful cancellation.
     *
     * @param description human-readable label for the Toolpath Manager UI
     * @param callable    the computation to run; must not be null
     * @param <T>         the type of result
     * @return the job handle; use {@link ComputationJob#getFuture()} to react to completion
     */
    public <T> ComputationJob<T> submit(String description, Callable<T> callable) {
        if (callable == null) {
            throw new IllegalArgumentException("callable must not be null");
        }
        String jobId = UUID.randomUUID().toString();
        ComputationJob<T> job = new ComputationJob<>(jobId, description);
        jobs.put(jobId, job);

        executor.submit(() -> {
            if (job.getStatus() == JobStatus.CANCELLED) {
                return;
            }
            job.markRunning(Thread.currentThread());
            try {
                T result = callable.call();
                if (Thread.currentThread().isInterrupted()) {
                    job.cancel();
                } else {
                    job.markCompleted(result);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                job.cancel();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Job failed: " + jobId, e);
                job.markFailed(e);
            }
        });

        LOG.info("Submitted job: " + jobId + " [" + description + "]");
        return job;
    }

    /**
     * Returns an unmodifiable snapshot of all known jobs (queued, running, completed, etc.).
     *
     * @return all jobs
     */
    public List<ComputationJob<?>> getJobs() {
        return Collections.unmodifiableList(new ArrayList<>(jobs.values()));
    }

    /**
     * Returns the job with the given ID, or {@code null} if not found.
     *
     * @param jobId job identifier
     * @return the job or {@code null}
     */
    public ComputationJob<?> getJob(String jobId) {
        return jobs.get(jobId);
    }

    /**
     * Shuts down the thread pool.
     *
     * <p>Waits up to {@code timeoutSeconds} for running jobs to complete. Jobs
     * still running after the timeout are interrupted.
     *
     * @param timeoutSeconds maximum seconds to wait for orderly shutdown
     */
    public void shutdown(long timeoutSeconds) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                LOG.warning("ThreadingManager forced shutdown after " + timeoutSeconds + "s timeout.");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Shuts down the thread pool with a default 30-second timeout.
     */
    public void shutdown() {
        shutdown(30);
    }
}
