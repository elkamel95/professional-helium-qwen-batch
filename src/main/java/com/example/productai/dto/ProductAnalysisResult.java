package com.example.productai.dto;

public class ProductAnalysisResult {
    private String keyword;
    private Integer localScore;
    private Integer aiScore;
    private String decision;
    private String explanation;
    private String status;
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Integer getLocalScore() { return localScore; }
    public void setLocalScore(Integer localScore) { this.localScore = localScore; }
    public Integer getAiScore() { return aiScore; }
    public void setAiScore(Integer aiScore) { this.aiScore = aiScore; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
