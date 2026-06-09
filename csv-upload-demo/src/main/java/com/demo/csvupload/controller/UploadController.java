package com.demo.csvupload.controller;

import com.demo.csvupload.model.ProcessingJob;
import com.demo.csvupload.model.ProcessingJob.Mode;
import com.demo.csvupload.service.CsvProcessingService;
import com.demo.csvupload.service.JdbcCsvProcessingService;
import com.demo.csvupload.service.JobTrackerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UploadController {

    private final JobTrackerService       jobTrackerService;
    private final CsvProcessingService    csvProcessingService;       // JPA engine
    private final JdbcCsvProcessingService jdbcCsvProcessingService;  // JDBC engine

    @GetMapping("/")
    public String uploadForm(Model model) {
        model.addAttribute("totalJobs", jobTrackerService.allJobs().size());
        return "upload";
    }

    /**
     * Accepts the CSV file and a {@code mode} parameter ("JPA" or "JDBC")
     * that selects which processing engine to use.
     */
    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file")             MultipartFile file,
                               @RequestParam(value = "mode",
                                             defaultValue = "JPA") String       modeParam,
                               RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a CSV file.");
            return "redirect:/";
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".csv")) {
            redirectAttributes.addFlashAttribute("error", "Only .csv files are accepted.");
            return "redirect:/";
        }

        Mode mode = "JDBC".equalsIgnoreCase(modeParam) ? Mode.JDBC : Mode.JPA;
        ProcessingJob job = jobTrackerService.createJob(originalName, mode);

        // Rough row estimate for the progress bar
        if (file.getSize() > 0) job.setTotalRows(Math.max(1, file.getSize() / 60));

        log.info("Upload '{}' ({} bytes) → mode={} jobId={}",
                originalName, file.getSize(), mode, job.getJobId());

        try {
            if (mode == Mode.JDBC) {
                jdbcCsvProcessingService.processAsync(file.getInputStream(), job);
            } else {
                csvProcessingService.processAsync(file.getInputStream(), job);
            }
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to read uploaded file: " + e.getMessage());
            return "redirect:/";
        }

        return "redirect:/progress/" + job.getJobId();
    }

    @GetMapping("/progress/{jobId}")
    public String progressPage(@PathVariable String jobId, Model model) {
        jobTrackerService.findJob(jobId).ifPresentOrElse(
                job -> model.addAttribute("job", job),
                () -> model.addAttribute("error", "Job not found: " + jobId)
        );
        model.addAttribute("jobId", jobId);
        return "result";
    }
}
