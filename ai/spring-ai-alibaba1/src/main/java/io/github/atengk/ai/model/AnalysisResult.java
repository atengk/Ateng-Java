package io.github.atengk.ai.model;

/**
 * 问题分析结果
 *
 * @author Ateng
 * @since 2026-04-24
 */
public class AnalysisResult {

    private String summary;

    private String riskLevel;

    private String suggestion;

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

}
