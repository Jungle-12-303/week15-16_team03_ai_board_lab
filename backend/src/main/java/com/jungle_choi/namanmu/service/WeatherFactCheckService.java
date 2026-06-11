package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class WeatherFactCheckService {

    private static final String WEATHER_TOOL_NAME = "weather.current_forecast";
    private static final String DEFAULT_LOCATION = "서울";
    private static final List<String> WEATHER_KEYWORDS = List.of(
            "날씨", "기온", "온도", "비", "눈", "우산", "습도", "바람", "폭염", "한파", "흐림", "맑음");
    private static final List<String> LOCATION_CANDIDATES = List.of(
            "서울", "부산", "인천", "대구", "대전", "광주", "울산", "세종", "제주",
            "수원", "성남", "고양", "용인", "청주", "천안", "전주", "포항", "창원", "춘천", "강릉");
    private static final String FACT_CHECK_INSTRUCTIONS = """
            You are a fact checker for Project Alpha.

            You will receive a Korean board post and weather facts retrieved through an MCP tool.
            Check only weather-related claims in the post.
            Do not judge claims that are not covered by the external facts.
            Write in Korean.
            Keep the result concise and practical.
            Include a clear judgement and a safer wording suggestion when needed.
            """;

    private final McpServerService mcpServerService;
    private final OpenAiTextClient openAiTextClient;
    private final ObjectMapper objectMapper;

    public WeatherFactCheckService(
            McpServerService mcpServerService,
            OpenAiTextClient openAiTextClient,
            ObjectMapper objectMapper) {
        this.mcpServerService = mcpServerService;
        this.openAiTextClient = openAiTextClient;
        this.objectMapper = objectMapper;
    }

    public WeatherFactCheckResult check(
            String category,
            String title,
            String content,
            List<String> tags) {
        String joinedText = String.join(" ", normalize(category), normalize(title), normalize(content));

        if (!hasWeatherIntent(joinedText, tags)) {
            return new WeatherFactCheckResult(
                    "NOT_SUPPORTED",
                    "이 게시글에서 날씨 관련 팩트체크 대상을 찾지 못했습니다.",
                    WEATHER_TOOL_NAME,
                    "",
                    "",
                    "",
                    "",
                    "");
        }

        String location = extractLocation(joinedText);
        McpServerService.McpToolCallResult toolCallResult =
                mcpServerService.callWeatherTool(location);
        WeatherApiClient.WeatherReport weatherReport = objectMapper.convertValue(
                toolCallResult.structuredContent(),
                WeatherApiClient.WeatherReport.class);
        String promptInput = buildPromptInput(category, title, content, tags, weatherReport);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateText(FACT_CHECK_INSTRUCTIONS, promptInput);

        return new WeatherFactCheckResult(
                "CHECKED",
                "MCP weather tool로 외부 날씨 정보를 조회해 게시글을 팩트체크했습니다.",
                WEATHER_TOOL_NAME,
                weatherReport.location(),
                weatherReport.source(),
                weatherReport.observedAt(),
                weatherReport.toBriefingText(),
                textGenerationResult.text());
    }

    private static String buildPromptInput(
            String category,
            String title,
            String content,
            List<String> tags,
            WeatherApiClient.WeatherReport weatherReport) {
        return """
                Board post:
                Category: %s
                Title: %s
                Tags: %s
                Content:
                %s

                Weather facts from MCP tool:
                %s

                Task:
                Compare the post with the weather facts.
                Return this structure:
                판정: weather-related claim status
                확인한 외부정보: key weather facts
                근거: why the post is accurate, uncertain, or needs caution
                수정 제안: safer wording if needed
                """.formatted(
                normalize(category),
                normalize(title),
                formatTags(tags),
                normalize(content),
                weatherReport.toBriefingText());
    }

    private static boolean hasWeatherIntent(String text, List<String> tags) {
        String searchableText = (text + " " + formatTags(tags)).toLowerCase(Locale.ROOT);

        return WEATHER_KEYWORDS.stream()
                .map((keyword) -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(searchableText::contains);
    }

    private static String extractLocation(String text) {
        return LOCATION_CANDIDATES.stream()
                .filter(text::contains)
                .findFirst()
                .orElse(DEFAULT_LOCATION);
    }

    private static String formatTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "None";
        }

        String joinedTags = String.join(", ", tags.stream()
                .map(WeatherFactCheckService::normalize)
                .filter((tag) -> !tag.isBlank())
                .toList());

        if (joinedTags.isBlank()) {
            return "None";
        }

        return joinedTags;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.trim();
    }

    public record WeatherFactCheckResult(
            String status,
            String message,
            String toolName,
            String location,
            String source,
            String observedAt,
            String externalFact,
            String judgement) {
    }
}
