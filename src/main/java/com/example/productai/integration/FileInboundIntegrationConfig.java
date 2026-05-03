package com.example.productai.integration;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.file.dsl.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Configuration
public class FileInboundIntegrationConfig {

    @Bean
    public IntegrationFlow fileInboundFlow(
            JobLauncher jobLauncher,
            Job heliumKeywordAnalysisJob,
            @Value("${app.input-dir}") String inputDir,
            @Value("${app.processing-dir}") String processingDir,
            @Value("${app.output-dir}") String outputDir,
            @Value("${app.error-dir}") String errorDir,
            @Value("${app.archive-dir}") String archiveDir,
            @Value("${app.poller-delay-ms}") long pollerDelayMs
    ) {
        return IntegrationFlow
                .from(
                        Files.inboundAdapter(new File(inputDir))
                                .patternFilter("*.csv")
                                .preventDuplicates(true),
                        e -> e.poller(p -> p.fixedDelay(pollerDelayMs))
                )
                .handle(File.class, (file, headers) -> {
                    createFolders(inputDir, processingDir, outputDir, errorDir, archiveDir);

                    Path sourcePath = file.toPath();
                    Path processingPath = Path.of(processingDir, file.getName());

                    try {
                        java.nio.file.Files.move(
                                sourcePath,
                                processingPath,
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    File movedFile = processingPath.toFile();
                    String outputFileName = movedFile.getName().replaceFirst("\\.csv$", "") + "_result.csv";
                    String outputFile = Path.of(outputDir, outputFileName).toString();

                    try {
                        jobLauncher.run(
                                heliumKeywordAnalysisJob,
                                new JobParametersBuilder()
                                        .addString("inputFile", movedFile.getAbsolutePath())
                                        .addString("outputFile", outputFile)
                                        .addLong("timestamp", System.currentTimeMillis())
                                        .toJobParameters()
                        );

                        java.nio.file.Files.move(
                                processingPath,
                                Path.of(archiveDir, movedFile.getName()),
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    } catch (Exception e) {
                        try {
                            moveToError(processingPath, errorDir, movedFile.getName());
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        try {
                            throw e;
                        } catch (JobExecutionAlreadyRunningException ex) {
                            throw new RuntimeException(ex);
                        } catch (JobRestartException ex) {
                            throw new RuntimeException(ex);
                        } catch (JobInstanceAlreadyCompleteException ex) {
                            throw new RuntimeException(ex);
                        } catch (JobParametersInvalidException ex) {
                            throw new RuntimeException(ex);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    return null;
                })
                .get();
    }

    private void createFolders(String... paths) {
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
    }

    private void moveToError(Path sourcePath, String errorDir, String fileName) throws IOException {
        java.nio.file.Files.move(
                sourcePath,
                Path.of(errorDir, fileName),
                StandardCopyOption.REPLACE_EXISTING
        );
    }
}