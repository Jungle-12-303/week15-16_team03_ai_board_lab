package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

@Service
public class WeatherApiClient {

    private static final String WEATHER_API_BASE_URL = "https://wttr.in";
    private static final String SOURCE_NAME = "wttr.in";
    private static final Map<String, String> LOCATION_ALIASES = Map.ofEntries(
            Map.entry("서울", "Seoul"),
            Map.entry("부산", "Busan"),
            Map.entry("인천", "Incheon"),
            Map.entry("대구", "Daegu"),
            Map.entry("대전", "Daejeon"),
            Map.entry("광주", "Gwangju"),
            Map.entry("울산", "Ulsan"),
            Map.entry("세종", "Sejong"),
            Map.entry("제주", "Jeju"),
            Map.entry("수원", "Suwon"),
            Map.entry("성남", "Seongnam"),
            Map.entry("고양", "Goyang"),
            Map.entry("용인", "Yongin"),
            Map.entry("청주", "Cheongju"),
            Map.entry("천안", "Cheonan"),
            Map.entry("전주", "Jeonju"),
            Map.entry("포항", "Pohang"),
            Map.entry("창원", "Changwon"),
            Map.entry("춘천", "Chuncheon"),
            Map.entry("강릉", "Gangneung"));

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public WeatherApiClient(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public WeatherReport getCurrentForecast(String location) {
        String normalizedLocation = normalizeLocation(location);
        String weatherUrl = UriComponentsBuilder.fromUriString(
                        WEATHER_API_BASE_URL + "/" + UriUtils.encodePathSegment(
                                normalizedLocation,
                                StandardCharsets.UTF_8))
                .queryParam("format", "j1")
                .toUriString();
        String responseBody = restClient.get()
                .uri(weatherUrl)
                .retrieve()
                .body(String.class);
        WttrResponse response = parseResponse(responseBody);

        if (response == null || response.currentCondition() == null
                || response.currentCondition().isEmpty()) {
            throw new IllegalStateException("Weather API response has no current weather.");
        }

        CurrentCondition current = response.currentCondition().getFirst();
        WeatherDay today = response.weather() == null || response.weather().isEmpty()
                ? null
                : response.weather().getFirst();

        return new WeatherReport(
                location == null || location.isBlank() ? normalizedLocation : location.trim(),
                current.observationTime(),
                parseDouble(current.tempC()),
                parseInt(current.humidity()),
                parseDouble(current.precipMm()),
                parseDouble(current.windSpeedKmph()),
                weatherDescription(current.weatherDesc()),
                today == null ? null : parseDouble(today.maxTempC()),
                today == null ? null : parseDouble(today.minTempC()),
                today == null ? null : parseDouble(today.totalSnowCm()),
                SOURCE_NAME);
    }

    private WttrResponse parseResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Weather API response is empty.");
        }

        try {
            return objectMapper.readValue(responseBody, WttrResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Weather API response could not be parsed.", exception);
        }
    }

    private static String normalizeLocation(String location) {
        if (location == null || location.isBlank()) {
            return "Seoul";
        }

        String trimmedLocation = location.trim();
        return LOCATION_ALIASES.getOrDefault(trimmedLocation, trimmedLocation);
    }

    private static String weatherDescription(List<WeatherDescription> descriptions) {
        if (descriptions == null || descriptions.isEmpty()) {
            return "알 수 없음";
        }

        String description = descriptions.getFirst().value();
        if (description == null || description.isBlank()) {
            return "알 수 없음";
        }

        return description;
    }

    private static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value);
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Integer.parseInt(value);
    }

    private record WttrResponse(
            @JsonProperty("current_condition") List<CurrentCondition> currentCondition,
            List<WeatherDay> weather) {
    }

    private record CurrentCondition(
            @JsonProperty("observation_time") String observationTime,
            @JsonProperty("temp_C") String tempC,
            String humidity,
            @JsonProperty("precipMM") String precipMm,
            @JsonProperty("windspeedKmph") String windSpeedKmph,
            List<WeatherDescription> weatherDesc) {
    }

    private record WeatherDescription(String value) {
    }

    private record WeatherDay(
            String date,
            @JsonProperty("maxtempC") String maxTempC,
            @JsonProperty("mintempC") String minTempC,
            @JsonProperty("totalSnow_cm") String totalSnowCm) {
    }

    public record WeatherReport(
            String location,
            String observedAt,
            Double temperatureCelsius,
            Integer humidityPercent,
            Double precipitationMm,
            Double windSpeedKmh,
            String weatherDescription,
            Double maxTemperatureCelsius,
            Double minTemperatureCelsius,
            Double dailySnowCm,
            String source) {

        public String toBriefingText() {
            return """
                    위치: %s
                    기준시각: %s
                    현재 날씨: %s
                    현재 기온: %.1f°C
                    습도: %d%%
                    강수량: %.1fmm
                    풍속: %.1fkm/h
                    오늘 최고/최저: %.1f°C / %.1f°C
                    오늘 적설량: %.1fcm
                    출처: %s
                    """.formatted(
                    location,
                    observedAt,
                    weatherDescription,
                    valueOrZero(temperatureCelsius),
                    humidityPercent == null ? 0 : humidityPercent,
                    valueOrZero(precipitationMm),
                    valueOrZero(windSpeedKmh),
                    valueOrZero(maxTemperatureCelsius),
                    valueOrZero(minTemperatureCelsius),
                    valueOrZero(dailySnowCm),
                    source);
        }

        private static double valueOrZero(Double value) {
            if (value == null) {
                return 0.0;
            }

            return value;
        }
    }
}
