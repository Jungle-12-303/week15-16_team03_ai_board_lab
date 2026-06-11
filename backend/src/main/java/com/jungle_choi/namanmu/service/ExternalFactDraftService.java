package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ExternalFactDraftService {

    private static final String WEATHER_TOOL_NAME = "weather.current_forecast";
    private static final String DEFAULT_LOCATION = "서울";
    private static final List<String> WEATHER_KEYWORDS = List.of(
            "날씨", "기온", "온도", "비", "눈", "우산", "습도", "바람", "폭염", "한파", "흐림", "맑음");
    private static final List<String> LOCATION_CANDIDATES = List.of(
            "서울", "부산", "인천", "대구", "대전", "광주", "울산", "세종", "제주",
            "수원", "성남", "고양", "용인", "청주", "천안", "전주", "포항", "창원", "춘천", "강릉");
    private static final String EXTERNAL_FACT_INSTRUCTIONS = """
            You are a writing assistant for Project Alpha.

            The user is writing a Korean board post. You will receive the user's current draft
            and external facts retrieved through an MCP tool.

            Write only the updated post body in Korean.
            Keep the user's intent and tone.
            Use the external facts only when they are relevant.
            Include the data source and observed time naturally when useful.
            Do not invent facts that are not present in the external facts.
            """;

    private final McpServerService mcpServerService;
    private final OpenAiTextClient openAiTextClient;
    private final ObjectMapper objectMapper;

    public ExternalFactDraftService(
            McpServerService mcpServerService,
            OpenAiTextClient openAiTextClient,
            ObjectMapper objectMapper) {
        this.mcpServerService = mcpServerService;
        this.openAiTextClient = openAiTextClient;
        this.objectMapper = objectMapper;
    }

    public ExternalFactDraftResult createDraft(
            String category,
            String title,
            String content,
            List<String> tags) {
        String joinedText = String.join(" ", normalize(category), normalize(title), normalize(content));

        if (!hasWeatherIntent(joinedText, tags)) {
            return new ExternalFactDraftResult(
                    "",
                    "현재 작성글에서 호출할 수 있는 외부 데이터 의도를 찾지 못했습니다. 지금은 날씨 관련 문맥을 지원합니다.",
                    "",
                    List.of());
        }

        String location = extractLocation(joinedText);
        McpServerService.McpToolCallResult toolCallResult =
                mcpServerService.callWeatherTool(location);
        WeatherApiClient.WeatherReport weatherReport = objectMapper.convertValue(
                toolCallResult.structuredContent(),
                WeatherApiClient.WeatherReport.class);
        String promptInput = buildPromptInput(category, title, content, tags, weatherReport);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateText(EXTERNAL_FACT_INSTRUCTIONS, promptInput);

        return new ExternalFactDraftResult(
                textGenerationResult.text(),
                "MCP weather tool로 외부 데이터를 조회해 초안을 보강했습니다.",
                WEATHER_TOOL_NAME,
                List.of(new ExternalFactSource(
                        WEATHER_TOOL_NAME,
                        weatherReport.source(),
                        weatherReport.location(),
                        weatherReport.observedAt())));
    }

    private static String buildPromptInput(
            String category,
            String title,
            String content,
            List<String> tags,
            WeatherApiClient.WeatherReport weatherReport) {
        return """
                User draft:
                Category: %s
                Title: %s
                Tags: %s
                Current content:
                %s

                External facts from MCP tool:
                %s

                Task:
                Rewrite the current content into a stronger board post body using the external facts.
                If the current content is short, expand it into a useful first draft.
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
                .map(ExternalFactDraftService::normalize)
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

    public record ExternalFactDraftResult(
            String draft,
            String message,
            String toolName,
            List<ExternalFactSource> sources) {
    }

    public record ExternalFactSource(
            String toolName,
            String source,
            String location,
            String observedAt) {
    }
}
