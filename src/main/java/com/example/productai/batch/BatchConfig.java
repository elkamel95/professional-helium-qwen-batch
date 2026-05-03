package com.example.productai.batch;

import com.example.productai.dto.HeliumKeywordRow;
import com.example.productai.dto.ProductAnalysisResult;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {
    @Bean
    public Step analyzeHeliumKeywordsStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          FlatFileItemReader<HeliumKeywordRow> heliumReader,
                                          ProductAiProcessor processor,
                                          ProductResultWriter writer,
                                          @Value("${app.chunk-size}") int chunkSize) {
        return new StepBuilder("analyzeHeliumKeywordsStep", jobRepository)
                .<HeliumKeywordRow, ProductAnalysisResult>chunk(chunkSize, transactionManager)
                .reader(heliumReader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skipLimit(1000)
                .skip(Exception.class)
                .build();
    }
    @Bean
    public Job heliumKeywordAnalysisJob(JobRepository jobRepository, Step analyzeHeliumKeywordsStep) {
        return new JobBuilder("heliumKeywordAnalysisJob", jobRepository)
                .start(analyzeHeliumKeywordsStep)
                .build();
    }
}
