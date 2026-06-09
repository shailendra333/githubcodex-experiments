package com.demo.csvupload.service;

import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.model.ProcessingJob.Mode;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory store for processing job state.
 * Tracks both JPA and JDBC jobs for side-by-side comparison.
 */
@Service
public class JobTrackerService {

    private final Map<String, ProcessingJob> jobs = new ConcurrentHashMap<>();

    public ProcessingJob createJob(String fileName, Mode mode) {
        String jobId = UUID.randomUUID().toString();
        ProcessingJob job = new ProcessingJob(jobId, fileName, mode);
        jobs.put(jobId, job);
        return job;
    }

    public Optional<ProcessingJob> findJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    public Collection<ProcessingJob> allJobs() {
        return jobs.values();
    }

    /** Returns the most recently completed job for a given mode, or empty. */
    public Optional<ProcessingJob> lastCompleted(Mode mode) {
        return jobs.values().stream()
                .filter(j -> j.getMode() == mode
                          && j.getStatus() == ProcessingJob.Status.COMPLETED)
                .max((a, b) -> {
                    if (a.getCompletedAt() == null) return -1;
                    if (b.getCompletedAt() == null) return  1;
                    return a.getCompletedAt().compareTo(b.getCompletedAt());
                });
    }
}
