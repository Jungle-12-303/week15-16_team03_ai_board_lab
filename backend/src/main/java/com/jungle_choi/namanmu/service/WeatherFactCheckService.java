package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class WeatherFactCheckService {

    private static final String WEATHER_TOOL_NAME = "weather.current_forecast";
    private static final List<Pattern> WEATHER_PATTERNS = List.of(
            Pattern.compile("날씨|기온|온도|습도|풍속|바람|강수|강수량|우산|폭염|한파|흐림|맑음|더위|추위"),
            Pattern.compile("비\\s*(가|는|와|오|올|내리|내릴|왔|옵|예보|소식|확률)"),
            Pattern.compile("눈\\s*(이|은|가|오|올|내리|내릴|왔|옵|예보|소식)"),
            Pattern.compile("(덥|더운|더워|춥|추운|추워)")
    );
    private static final List<String> LOCATION_CANDIDATES = List.of(
            "서울", "부산", "인천", "대구", "대전", "광주", "울산", "세종", "제주",
            "수원", "성남", "고양", "용인", "청주", "천안", "전주", "포항", "창원", "춘천", "강릉");
    private static final String FACT_CHECK_INSTRUCTIONS = """
            You are a fact checker for Project Alpha.

            You will receive a Korean board post and weather facts retrieved through an MCP tool.
            Check only weather-related claims in the post.
            Do not judge claims that are not covered by the external facts.
            Do not merely summarize the external facts. Compare the post's actual wording with
            the retrieved facts.
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

        Optional<String> location = extractLocation(joinedText);
        if (location.isEmpty()) {
            return new WeatherFactCheckResult(
                    "LOCATION_REQUIRED",
                    "날씨 관련 표현은 있지만 조회할 지역 정보가 없어 MCP 날씨 도구를 호출하지 않았습니다.",
                    WEATHER_TOOL_NAME,
                    "",
                    "",
                    "",
                    "",
                    "");
        }

        McpServerService.McpToolCallResult toolCallResult =
                mcpServerService.callWeatherTool(location.get());
        WeatherApiClient.WeatherReport weatherReport = objectMapper.convertValue(
                toolCallResult.structuredContent(),
                WeatherApiClient.WeatherReport.class);
        String promptInput = buildPromptInput(category, title, content, tags, weatherReport);
        OpenAiTextClient.TextGenerationResult textGenerationResult =
                openAiTextClient.generateText(FACT_CHECK_INSTRUCTIONS, promptInput);

        return new WeatherFactCheckResult(
                "CHECKED",
                "게시글의 날씨 관련 표현과 MCP 외부 날씨 정보를 비교했습니다.",
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
                Compare the weather-related wording in the post with the weather facts.
                Focus on whether the post's actual weather claim is supported, contradicted,
                or too vague to judge from the fetched data.
                Return this structure:
                검증 대상: quote or summarize the exact weather-related claim from the post
                판정: supported / contradicted / uncertain / too vague
                비교: explain how the post claim differs from or matches the fetched weather facts
                확인한 외부정보: only the key weather facts used for the comparison
                수정 제안: safer wording if needed. If no change is needed, say 유지 가능
                """.formatted(
                normalize(category),
                normalize(title),
                formatTags(tags),
                normalize(content),
                weatherReport.toBriefingText());
    }

    static boolean hasWeatherIntent(String text, List<String> tags) {
        String searchableText = (text + " " + formatTags(tags)).toLowerCase(Locale.ROOT);

        return WEATHER_PATTERNS.stream()
                .anyMatch((pattern) -> pattern.matcher(searchableText).find());
    }

    static Optional<String> extractLocation(String text) {
        return LOCATION_CANDIDATES.stream()
                .filter(text::contains)
                .findFirst();
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
