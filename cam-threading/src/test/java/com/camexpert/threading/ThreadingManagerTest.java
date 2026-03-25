package com.camexpert.threading;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(value = 10, unit = TimeUnit.SECONDS)
class ThreadingManagerTest {

    private ThreadingManager manager;

    @BeforeEach
    void setUp() {
        manager = new ThreadingManager(2);
    }

    @AfterEach
    void tearDown() {
        manager.shutdown(5);
    }

    // ------------------------------------------------------------------
    // Constructor validation
    // ------------------------------------------------------------------

    @Test
    void constructor_rejectsZeroThreads() {
        assertThrows(IllegalArgumentException.class, () -> new ThreadingManager(0));
    }

    @Test
    void constructor_rejectsNullExecutor() {
        assertThrows(IllegalArgumentException.class,
                () -> new ThreadingManager((java.util.concurrent.ExecutorService) null));
    }

    // ------------------------------------------------------------------
    // Submit and completion
    // ------------------------------------------------------------------

    @Test
    void submit_nullCallableThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> manager.submit("test", null));
    }

    @Test
    void submit_completesSuccessfully() throws Exception {
        ComputationJob<Integer> job = manager.submit("add", () -> 1 + 2);
        assertEquals(3, job.getFuture().get(5, TimeUnit.SECONDS));
        assertEquals(JobStatus.COMPLETED, job.getStatus());
    }

    @Test
    void submit_failedJobTransitionsToFailed() throws Exception {
        ComputationJob<Integer> job = manager.submit("fail", () -> {
            throw new RuntimeException("intentional failure");
        });
        // Future should complete exceptionally
        assertThrows(ExecutionException.class, () -> job.getFuture().get(5, TimeUnit.SECONDS));
        assertEquals(JobStatus.FAILED, job.getStatus());
    }

    @Test
    void submit_jobAppearsInGetJobs() throws Exception {
        ComputationJob<String> job = manager.submit("label", () -> "result");
        job.getFuture().get(5, TimeUnit.SECONDS);
        List<ComputationJob<?>> jobs = manager.getJobs();
        assertTrue(jobs.stream().anyMatch(j -> j.getJobId().equals(job.getJobId())));
    }

    @Test
    void submit_multipleJobsRunConcurrently() throws Exception {
        CountDownLatch latch = new CountDownLatch(2);

        ComputationJob<Integer> j1 = manager.submit("job1", () -> {
            latch.countDown();
            latch.await(5, TimeUnit.SECONDS);
            return 1;
        });
        ComputationJob<Integer> j2 = manager.submit("job2", () -> {
            latch.countDown();
            latch.await(5, TimeUnit.SECONDS);
            return 2;
        });

        assertEquals(1, j1.getFuture().get(5, TimeUnit.SECONDS));
        assertEquals(2, j2.getFuture().get(5, TimeUnit.SECONDS));
        assertEquals(JobStatus.COMPLETED, j1.getStatus());
        assertEquals(JobStatus.COMPLETED, j2.getStatus());
    }

    // ------------------------------------------------------------------
    // Cancellation / interruption
    // ------------------------------------------------------------------

    @Test
    void cancel_queuedJobTransitionsToCancelled() {
        // Use a single-thread executor so the second job stays QUEUED
        ThreadingManager singleThread = new ThreadingManager(1);
        try {
            CountDownLatch started = new CountDownLatch(1);
            CountDownLatch hold = new CountDownLatch(1);

            // First job occupies the thread
            singleThread.submit("blocker", () -> {
                started.countDown();
                hold.await();
                return null;
            });

            // Give thread time to pick up the blocker
            assertTrue(started.await(5, TimeUnit.SECONDS));

            // Second job should be QUEUED
            ComputationJob<Integer> queued = singleThread.submit("queued", () -> 42);
            assertTrue(queued.cancel());
            assertEquals(JobStatus.CANCELLED, queued.getStatus());

            hold.countDown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        } finally {
            singleThread.shutdown(5);
        }
    }

    @Test
    void cancel_runningJobInterruptsThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        ComputationJob<Integer> job = manager.submit("long-calc", () -> {
            started.countDown();
            // Worker respects interruption
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(10);
            }
            throw new InterruptedException("cancelled by test");
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        boolean cancelled = job.cancel();
        assertTrue(cancelled);
        // Wait for the future to resolve
        assertThrows(java.util.concurrent.CancellationException.class,
                () -> job.getFuture().get(5, TimeUnit.SECONDS));
    }

    // ------------------------------------------------------------------
    // ComputationJob state machine
    // ------------------------------------------------------------------

    @Test
    void computationJob_initialStatusIsQueued() {
        ComputationJob<Void> job = new ComputationJob<>("id", "desc");
        assertEquals(JobStatus.QUEUED, job.getStatus());
    }

    @Test
    void computationJob_cancelFromQueued() {
        ComputationJob<Void> job = new ComputationJob<>("id", "desc");
        assertTrue(job.cancel());
        assertEquals(JobStatus.CANCELLED, job.getStatus());
        // Second cancel returns false
        assertFalse(job.cancel());
    }

    @Test
    void computationJob_blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ComputationJob<>("", "desc"));
        assertThrows(IllegalArgumentException.class, () -> new ComputationJob<>(null, "desc"));
    }

    // ------------------------------------------------------------------
    // getJob by ID
    // ------------------------------------------------------------------

    @Test
    void getJob_returnsCorrectJob() throws Exception {
        ComputationJob<String> job = manager.submit("lookup", () -> "found");
        job.getFuture().get(5, TimeUnit.SECONDS);
        assertSame(job, manager.getJob(job.getJobId()));
    }

    @Test
    void getJob_unknownIdReturnsNull() {
        assertNull(manager.getJob("nonexistent-id"));
    }
}
