package com.example.productai.batch;

import com.example.productai.dto.HeliumKeywordRow;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class CsvReaderConfig {
    @Bean
    @StepScope
    public FlatFileItemReader<HeliumKeywordRow> heliumReader(@Value("#{jobParameters['inputFile']}") String inputFile) {
        return new FlatFileItemReaderBuilder<HeliumKeywordRow>()
                .name("heliumReader")
                .resource(new FileSystemResource(inputFile))
                .linesToSkip(1)
                .delimited()
                .delimiter(",")
                .quoteCharacter('"')
                .names("keywordPhrase","keywordSales","cerebroIqScore","searchVolume","searchVolumeTrend","h10PpcSuggBid","h10PpcSuggMinBid","h10PpcSuggMaxBid","sponsoredAsins","competingProducts","cpr","organic","titleDensity","smartComplete","amazonRecommended")
                .targetType(HeliumKeywordRow.class)
                .strict(true)
                .build();
    }
}
