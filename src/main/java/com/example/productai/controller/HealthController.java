package com.example.productai.controller;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/status")
public class HealthController {

    @Autowired
    private JobExplorer jobExplorer;

    @GetMapping
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("application", "professional-helium-qwen-batch");
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());

        // Batch job info
        try {
            List<String> jobNames = jobExplorer.getJobNames();
            Map<String, Object> batchInfo = new HashMap<>();

            for (String jobName : jobNames) {
                Set<JobExecution> executions = jobExplorer.findRunningJobExecutions(jobName);
                int runningCount = executions.size();

                JobExecution lastExecution = jobExplorer
                        .getJobInstances(jobName, 0, 1)
                        .stream()
                        .findFirst()
                        .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream().findFirst())
                        .orElse(null);

                Map<String, Object> jobInfo = new HashMap<>();
                jobInfo.put("runningExecutions", runningCount);

                if (lastExecution != null) {
                    BatchStatus batchStatus = lastExecution.getStatus();
                    jobInfo.put("lastStatus", batchStatus.name());
                    jobInfo.put("lastStartTime", lastExecution.getStartTime() != null ? lastExecution.getStartTime().toString() : "N/A");
                    jobInfo.put("lastEndTime", lastExecution.getEndTime() != null ? lastExecution.getEndTime().toString() : "N/A");
                    jobInfo.put("exitCode", lastExecution.getExitStatus().getExitCode());
                } else {
                    jobInfo.put("lastStatus", "NO_EXECUTION");
                }

                batchInfo.put(jobName, jobInfo);
            }

            status.put("jobs", batchInfo);
            status.put("totalJobs", jobNames.size());

        } catch (Exception e) {
            status.put("batchError", e.getMessage());
        }

        return status;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is running");
        return response;
    }
}
