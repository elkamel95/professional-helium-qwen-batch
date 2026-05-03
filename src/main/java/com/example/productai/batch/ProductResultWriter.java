package com.example.productai.batch;

import com.example.productai.dto.ProductAnalysisResult;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Component
@StepScope
public class ProductResultWriter implements ItemWriter<ProductAnalysisResult> {

    private final String outputFile;

    public ProductResultWriter(@Value("#{jobParameters['outputFile']}") String outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void write(Chunk<? extends ProductAnalysisResult> chunk) throws IOException {
        File file = new File(outputFile);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        boolean exists = file.exists();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            if (!exists) {
                writer.write("keyword,localScore,aiScore,decision,status,explanation");
                writer.newLine();
            }

            for (ProductAnalysisResult item : chunk.getItems()) {
                if(!item.getDecision().contains("REJECT")) {
                    writer.write(
                            csv(item.getKeyword()) + "," +
                                    value(item.getLocalScore()) + "," +
                                    value(item.getAiScore()) + "," +
                                    csv(item.getDecision()) + "," +
                                    csv(item.getStatus()) + "," +
                                    csv(item.getExplanation())
                    );
                    writer.newLine();
                }
            }
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String value(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }
}