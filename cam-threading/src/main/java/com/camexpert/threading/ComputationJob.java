package com.camexpert.threading;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a single background computation job submitted to {@link ThreadingManager}.
 *
 * <p>A {@code ComputationJob} wraps a unit of work as a {@link java.util.concurrent.Callable}
 * and exposes:
 * <ul>
 *   <li>A {@link CompletableFuture} so callers can attach completion callbacks
 *       and the UI can be notified when the result is ready.</li>
 *   <li>A {@link #cancel()} method that sets the interruption flag on the worker
 *       thread and transitions the job to {@link JobStatus#CANCELLED}.</li>
 *   <li>A human-readable {@link #getDescription()} for display in the Toolpath Manager.</li>
 * </ul>
 *
 * @param <T> the type of result produced by this job
 */
public final class ComputationJob<T> {

    private final String jobId;
    private final String description;
    private final CompletableFuture<T> future;
    private final AtomicReference<JobStatus> status;
    private volatile Thread workerThread;

    /**
     * Creates a new job.
     *
     * @param jobId       unique job identifier
     * @param description human-readable label shown in the Toolpath Manager
     */
    public ComputationJob(String jobId, String description) {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be null or blank");
        }
        this.jobId = jobId;
        this.description = description != null ? description : jobId;
        this.future = new CompletableFuture<>();
        this.status = new AtomicReference<>(JobStatus.QUEUED);
    }

    /** @return unique identifier for this job */
    public String getJobId() {
        return jobId;
    }

    /** @return human-readable description shown in the Toolpath Manager UI */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the {@link CompletableFuture} that will be completed with the job's
     * result, or completed exceptionally if the job fails or is cancelled.
     *
     * @return future result
     */
    public CompletableFuture<T> getFuture() {
        return future;
    }

    /**
     * Returns the current status of this job.
     *
     * @return current status
     */
    public JobStatus getStatus() {
        return status.get();
    }

    /**
     * Attempts to cancel this job.
     *
     * <p>If the job is {@link JobStatus#RUNNING}, the worker thread is interrupted
     * so it can check {@link Thread#isInterrupted()} and exit cleanly. The status
     * transitions to {@link JobStatus#CANCELLED} and the future is cancelled.
     *
     * @return {@code true} if the cancel request was accepted (job was not already
     *         completed or failed)
     */
    public boolean cancel() {
        if (status.compareAndSet(JobStatus.QUEUED, JobStatus.CANCELLED)
                || status.compareAndSet(JobStatus.RUNNING, JobStatus.CANCELLED)) {
            Thread t = workerThread;
            if (t != null) {
                t.interrupt();
            }
            future.cancel(true);
            return true;
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Package-private state-transition helpers used by ThreadingManager
    // -------------------------------------------------------------------------

    void markRunning(Thread thread) {
        this.workerThread = thread;
        status.compareAndSet(JobStatus.QUEUED, JobStatus.RUNNING);
    }

    void markCompleted(T result) {
        if (status.compareAndSet(JobStatus.RUNNING, JobStatus.COMPLETED)) {
            future.complete(result);
        }
    }

    void markFailed(Throwable cause) {
        if (status.compareAndSet(JobStatus.RUNNING, JobStatus.FAILED)) {
            future.completeExceptionally(cause);
        }
    }
}
