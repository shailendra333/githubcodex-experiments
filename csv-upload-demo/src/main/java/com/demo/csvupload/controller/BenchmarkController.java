package com.demo.csvupload.controller;

import com.demo.csvupload.model.ProcessingJob.Mode;
import com.demo.csvupload.service.JobTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class BenchmarkController {

    private final JobTrackerService jobTrackerService;

    @GetMapping("/benchmark")
    public String benchmarkPage(Model model) {
        jobTrackerService.lastCompleted(Mode.JPA)
                .ifPresent(j -> model.addAttribute("jpaJob", j));
        jobTrackerService.lastCompleted(Mode.JDBC)
                .ifPresent(j -> model.addAttribute("jdbcJob", j));
        return "benchmark";
    }
}

