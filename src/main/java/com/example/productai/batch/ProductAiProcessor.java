package com.example.productai.batch;

import com.example.productai.dto.HeliumKeywordRow;
import com.example.productai.dto.ProductAnalysisResult;
import com.example.productai.service.QwenClient;
import com.example.productai.util.JsonUtils;
import com.example.productai.util.NumberUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ProductAiProcessor implements ItemProcessor<HeliumKeywordRow, ProductAnalysisResult> {
    private final QwenClient qwenClient;
    private final int localThresholdScore;

    public ProductAiProcessor(QwenClient qwenClient, @Value("${app.local-threshold-score}") int localThresholdScore) {
        this.qwenClient = qwenClient;
        this.localThresholdScore = localThresholdScore;
    }

    @Override
    public ProductAnalysisResult process(HeliumKeywordRow item) throws Exception {
        Integer localScore = calculateLocalScore(item);
        if (shouldRejectBeforeAi(item, localScore)) {
            ProductAnalysisResult result = new ProductAnalysisResult();
            result.setKeyword(item.getKeywordPhrase());
            result.setLocalScore(localScore);
            result.setAiScore(0);
            result.setDecision("REJECT");
            result.setExplanation("Rejected by local keyword rules");
            result.setStatus("LOCAL_REJECT");
            return result;
        }
        String rawJson = qwenClient.analyzeKeyword(item, localScore);
        JsonNode node = JsonUtils.readTree(rawJson);
        ProductAnalysisResult result = new ProductAnalysisResult();
        result.setKeyword(item.getKeywordPhrase());
        result.setLocalScore(localScore);
        result.setAiScore(node.path("score").asInt(0));
        result.setDecision(node.path("decision").asText("REJECT"));
        result.setExplanation(node.path("explanation").asText("No explanation"));
        result.setStatus("AI_ANALYZED");
        return result;
    }

    private boolean shouldRejectBeforeAi(HeliumKeywordRow item, Integer localScore) {
        Integer searchVolume = NumberUtils.toInteger(item.getSearchVolume());
        Integer competingProducts = NumberUtils.toInteger(item.getCompetingProducts());
        Integer titleDensity = NumberUtils.toInteger(item.getTitleDensity());
        Integer cpr = NumberUtils.toInteger(item.getCpr());
        if (searchVolume == null || searchVolume < 3000) return true;
        if (competingProducts != null && competingProducts > 10000) return true;
        if (titleDensity != null && titleDensity > 80) return true;
        if (cpr != null && cpr > 40) return true;
        return localScore < localThresholdScore;
    }

    private Integer calculateLocalScore(HeliumKeywordRow item) {
        int score = 0;
        Integer searchVolume = NumberUtils.toInteger(item.getSearchVolume());
        Integer competingProducts = NumberUtils.toInteger(item.getCompetingProducts());
        Integer titleDensity = NumberUtils.toInteger(item.getTitleDensity());
        Integer cpr = NumberUtils.toInteger(item.getCpr());
        Integer keywordSales = NumberUtils.toInteger(item.getKeywordSales());
        Double iq = NumberUtils.toDouble(item.getCerebroIqScore());
        boolean amazonRecommended = NumberUtils.toBooleanFlag(item.getAmazonRecommended());
        if (searchVolume != null) {
            if (searchVolume >= 20000) score += 30; else if (searchVolume >= 10000) score += 20; else if (searchVolume >= 3000) score += 10;
        }
        if (competingProducts != null) {
            if (competingProducts <= 2000) score += 25; else if (competingProducts <= 5000) score += 15; else if (competingProducts <= 10000) score += 5;
        }
        if (titleDensity != null) {
            if (titleDensity <= 10) score += 20; else if (titleDensity <= 30) score += 10; else if (titleDensity <= 80) score += 5;
        }
        if (cpr != null) {
            if (cpr <= 10) score += 15; else if (cpr <= 20) score += 10; else if (cpr <= 40) score += 5;
        }
        if (keywordSales != null) {
            if (keywordSales >= 500) score += 5; else if (keywordSales >= 100) score += 3;
        }
        if (iq != null) {
            if (iq >= 10000) score += 10; else if (iq >= 5000) score += 5;
        }
        if (amazonRecommended) score += 5;
        return Math.min(score, 100);
    }
}
