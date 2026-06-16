package com.example.backend.mcp;

public class McpWeatherDraftRequest {

    private String city;
    private Integer forecastDays;

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
}
