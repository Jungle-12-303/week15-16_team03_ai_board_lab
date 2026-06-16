package com.jungle_choi.namanmu.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag-search")
public record RagSearchProperties(
        int maxLimit,
        double minVectorRelevanceScore,
        double minHybridRelevanceScore,
        double minRankedResultScore,
        double bm25K1,
        double bm25B,
        double vectorWeightWithQueryTerms,
        double bm25WeightWithQueryTerms,
        double keywordAlignmentWeight,
        double chunkEvidenceWeight,
        double rrfRankConstant,
        int maxQueryTerms,
        int maxGuardTerms,
        QueryMode queryMode,
        FusionMode fusionMode,
        boolean metadataEnabled) {

    public int normalizedMaxLimit() {
        return Math.min(Math.max(maxLimit, 1), 50);
    }

    public int normalizedMaxQueryTerms() {
        return Math.min(Math.max(maxQueryTerms, 1), 64);
    }

    public int normalizedMaxGuardTerms() {
        return Math.min(Math.max(maxGuardTerms, 1), 32);
    }

    public double normalizedChunkEvidenceWeight() {
        return clamp(chunkEvidenceWeight, 0.0, 1.0);
    }

    public double normalizedKeywordAlignmentWeight() {
        return clamp(keywordAlignmentWeight, 0.0, 1.0);
    }

    public double normalizedVectorWeight() {
        return Math.max(vectorWeightWithQueryTerms, 0.0);
    }

    public double normalizedBm25Weight() {
        return Math.max(bm25WeightWithQueryTerms, 0.0);
    }

    public double normalizedBm25K1() {
        return Math.max(bm25K1, 0.01);
    }

    public double normalizedBm25B() {
        return clamp(bm25B, 0.0, 1.0);
    }

    public double normalizedRrfRankConstant() {
        return Math.max(rrfRankConstant, 1.0);
    }

    public QueryMode normalizedQueryMode() {
        return queryMode == null ? QueryMode.CURRENT_TEMPLATE : queryMode;
    }

    public FusionMode normalizedFusionMode() {
        return fusionMode == null ? FusionMode.RRF : fusionMode;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum QueryMode {
        CURRENT_TEMPLATE,
        TITLE_CONTENT
    }

    public enum FusionMode {
        RRF,
        WEIGHTED
    }
}
