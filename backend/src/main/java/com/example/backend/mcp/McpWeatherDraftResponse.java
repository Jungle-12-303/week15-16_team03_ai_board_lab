package com.example.backend.mcp;

import java.util.List;

public class McpWeatherDraftResponse {

    private String requestedBy;
    private String city;
    private Integer forecastDays;
    private String title;
    private String content;
    private List<String> tags;
    private List<String> sourceSummary;

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getForecastDays() {
        return forecastDays;
    }

    public void setForecastDays(Integer forecastDays) {
        this.forecastDays = forecastDays;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getSourceSummary() {
        return sourceSummary;
    }

    public void setSourceSummary(List<String> sourceSummary) {
        this.sourceSummary = sourceSummary;
    }
}
