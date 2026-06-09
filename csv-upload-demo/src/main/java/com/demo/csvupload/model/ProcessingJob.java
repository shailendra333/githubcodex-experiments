package com.demo.csvupload.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class ProcessingJob {

    public enum Status { QUEUED, PROCESSING, COMPLETED, FAILED }

    /** Processing engine used for this job. */
    public enum Mode { JPA, JDBC }

    private final String jobId;
    private final String fileName;
    private final Mode   mode;              // ← which engine processed this job
    private volatile Status status = Status.QUEUED;

    private volatile long totalRows    = 0;
    private final AtomicLong processedRows = new AtomicLong(0);
    private final AtomicLong insertedRows  = new AtomicLong(0);
    private final AtomicLong updatedRows   = new AtomicLong(0);
    private final AtomicLong failedRows    = new AtomicLong(0);

    private final List<String> errors = new ArrayList<>();
    private static final int MAX_ERRORS = 100;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String fatalError;

    /** Peak heap used (MB) captured during processing. */
    private volatile long peakHeapMb = 0;

    /** Wall-clock duration in milliseconds (set on completion). */
    private volatile long durationMs = 0;

    public ProcessingJob(String jobId, String fileName, Mode mode) {
        this.jobId     = jobId;
        this.fileName  = fileName;
        this.mode      = mode;
        this.startedAt = LocalDateTime.now();
    }

    public void addError(long rowNum, String message) {
        if (errors.size() < MAX_ERRORS) {
            errors.add("Row " + rowNum + ": " + message);
        }
        failedRows.incrementAndGet();
    }

    public void incrementProcessed() { processedRows.incrementAndGet(); }
    public void incrementInserted()  { insertedRows.incrementAndGet();  }
    public void incrementUpdated()   { updatedRows.incrementAndGet();   }

    public int getProgressPercent() {
        if (status == Status.COMPLETED) return 100;
        if (totalRows <= 0) return 0;
        long done = processedRows.get() + failedRows.get();
        return (int) Math.min(99, (done * 100L) / totalRows);
    }

    /** Rows per second (0 if not yet complete or duration is 0). */
    public long getThroughputRowsPerSec() {
        if (durationMs <= 0) return 0;
        return (processedRows.get() * 1000L) / durationMs;
    }

    public long getProcessedCount() { return processedRows.get(); }
    public long getInsertedCount()  { return insertedRows.get();  }
    public long getUpdatedCount()   { return updatedRows.get();   }
    public long getFailedCount()    { return failedRows.get();    }
}
