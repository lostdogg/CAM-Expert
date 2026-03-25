package com.camexpert.threading;

/**
 * Represents the status of a background computation job managed by
 * {@link ThreadingManager}.
 */
public enum JobStatus {

    /** The job is waiting in the queue to be picked up by a worker thread. */
    QUEUED,

    /** A worker thread is actively computing the result. */
    RUNNING,

    /** The job completed successfully; the result is available. */
    COMPLETED,

    /** The job was cancelled before or during execution. */
    CANCELLED,

    /** The job failed with an exception. */
    FAILED
}
