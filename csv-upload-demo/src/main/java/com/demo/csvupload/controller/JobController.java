package com.demo.csvupload.controller;

import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.model.ProcessingJob.Mode;
import com.demo.csvupload.repository.CustomerRepository;
import com.demo.csvupload.service.JobTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobs")
public class JobController {

    private final JobTrackerService  jobTrackerService;
    private final CustomerRepository customerRepository;

    @GetMapping("/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        return jobTrackerService.findJob(jobId)
                .map(job -> ResponseEntity.ok(buildStatusMap(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Map<String, Object>> getAllJobs() {
        return jobTrackerService.allJobs().stream()
                .map(this::buildStatusMap)
                .sorted((a, b) -> b.get("jobId").toString()
                        .compareTo(a.get("jobId").toString()))
                .toList();
    }

    @GetMapping("/db-stats")
    public Map<String, Long> dbStats() {
        return Map.of("totalCustomers", customerRepository.countAllCustomers());
    }

    /**
     * Returns last completed JPA job vs last completed JDBC job side-by-side
     * for the benchmark comparison page.
     */
    @GetMapping("/benchmark")
    public Map<String, Object> benchmark() {
        Map<String, Object> result = new LinkedHashMap<>();
        jobTrackerService.lastCompleted(Mode.JPA)
                .ifPresent(j -> result.put("jpa", buildStatusMap(j)));
        jobTrackerService.lastCompleted(Mode.JDBC)
                .ifPresent(j -> result.put("jdbc", buildStatusMap(j)));
        result.put("totalCustomers", customerRepository.countAllCustomers());
        return result;
    }

    // -----------------------------------------------------------------------

    private Map<String, Object> buildStatusMap(ProcessingJob job) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("jobId",              job.getJobId());
        m.put("fileName",           job.getFileName());
        m.put("mode",               job.getMode().name());
        m.put("status",             job.getStatus().name());
        m.put("totalRows",          job.getTotalRows());
        m.put("processedRows",      job.getProcessedCount());
        m.put("insertedRows",       job.getInsertedCount());
        m.put("updatedRows",        job.getUpdatedCount());
        m.put("failedRows",         job.getFailedCount());
        m.put("progressPct",        job.getProgressPercent());
        m.put("durationMs",         job.getDurationMs());
        m.put("throughputRowsSec",  job.getThroughputRowsPerSec());
        m.put("peakHeapMb",         job.getPeakHeapMb());
        m.put("errors",             job.getErrors());
        m.put("fatalError",         job.getFatalError() != null ? job.getFatalError() : "");
        m.put("startedAt",          job.getStartedAt() != null ? job.getStartedAt().toString() : "");
        return m;
    }
}
