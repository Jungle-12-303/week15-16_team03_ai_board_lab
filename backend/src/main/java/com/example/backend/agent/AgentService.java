package com.example.backend.agent;

import com.example.backend.mcp.McpWeatherDraftResponse;
import com.example.backend.mcp.McpWeatherDraftService;
import com.example.backend.rag.OpenAiChatClient;
import com.example.backend.rag.PostEmbeddingService;
import com.example.backend.rag.RagReferenceResponse;
import com.example.backend.tag.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final int AGENT_REFERENCE_LIMIT = 5;
    private static final Pattern FORECAST_DAYS_PATTERN = Pattern.compile("([1-3])\\s*일");

    private final PostEmbeddingService postEmbeddingService;
    private final OpenAiChatClient openAiChatClient;
    private final McpWeatherDraftService mcpWeatherDraftService;
    private final ObjectMapper objectMapper;

    public AgentService(
        PostEmbeddingService postEmbeddingService,
        OpenAiChatClient openAiChatClient,
        McpWeatherDraftService mcpWeatherDraftService,
        ObjectMapper objectMapper
    ) {
        this.postEmbeddingService = postEmbeddingService;
        this.openAiChatClient = openAiChatClient;
        this.mcpWeatherDraftService = mcpWeatherDraftService;
        this.objectMapper = objectMapper;
    }

    public AgentDraftResponse createDraft(String userRequest, String requestedBy) {
        String trimmedRequest = userRequest == null ? "" : userRequest.trim();

        if (trimmedRequest.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 요청을 입력해주세요.");
        }

        if (!openAiChatClient.isConfigured()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "OPENAI_API_KEY가 설정되지 않아 Agent 기능을 실행할 수 없습니다."
            );
        }

        List<RagReferenceResponse> ragReferences = postEmbeddingService.findRelevantReferences(
            trimmedRequest,
            requestedBy,
            AGENT_REFERENCE_LIMIT
        );

        McpWeatherDraftResponse mcpResult = tryLoadWeatherDraft(trimmedRequest, requestedBy);

        String rawJson = openAiChatClient.createAnswer(
            buildSystemPrompt(),
            buildUserPrompt(trimmedRequest, ragReferences, mcpResult),
            requestedBy
        );

        AgentDraftResponse response = parseAgentResponse(rawJson);
        response.setReferences(buildReferenceItems(ragReferences));
        response.setToolsUsed(buildToolsUsed(mcpResult));
        response.setTags(normalizeTags(response.getTags(), ragReferences, mcpResult));
        response.setReasoningSummary(buildReasoningSummary(ragReferences, mcpResult, response.getReasoningSummary()));
        return response;
    }

    private String buildSystemPrompt() {
        return """
            당신은 게시판 글 작성을 도와주는 AI 게시글 작성 Agent입니다.

            당신의 역할은 사용자의 짧은 요청을 바탕으로 게시판에 올릴 수 있는 게시글 초안을 생성하는 것입니다.

            당신은 백엔드가 제공한 RAG 검색 결과와 MCP 도구 결과만 참고할 수 있습니다.

            규칙:
            - 한국어로 작성합니다.
            - 게시판에 바로 올릴 수 있는 자연스러운 글을 작성합니다.
            - RAG 검색 결과를 참고하되, 기존 게시글 내용을 그대로 복사하지 않습니다.
            - MCP 도구 결과가 제공된 경우에만 외부 정보를 사용합니다.
            - 참고자료에 없는 내용을 사실처럼 단정하지 않습니다.
            - 근거가 부족하면 일반적인 제안처럼 표현합니다.
            - 태그는 3개에서 5개 정도로 제안합니다.
            - 반드시 JSON 형식으로만 답변합니다.

            출력 형식:
            {
              "title": "게시글 제목",
              "content": "게시글 본문",
              "tags": ["태그1", "태그2", "태그3"],
              "references": [
                {
                  "postId": 1,
                  "title": "참고 게시글 제목"
                }
              ],
              "toolsUsed": ["RAG"],
              "reasoningSummary": "어떤 자료를 참고해 초안을 만들었는지 짧게 설명"
            }
            """;
    }

    private String buildUserPrompt(
        String userRequest,
        List<RagReferenceResponse> ragReferences,
        McpWeatherDraftResponse mcpResult
    ) {
        String ragReferencesText = ragReferences.isEmpty()
            ? "검색 결과 없음"
            : ragReferences.stream()
                .map(reference -> """
                    [게시글 #%d]
                    제목: %s
                    작성자: %s
                    작성일: %s
                    태그: %s
                    내용: %s
                    """.formatted(
                    reference.getPostId(),
                    reference.getTitle(),
                    reference.getAuthorName(),
                    reference.getCreatedAt(),
                    reference.getTags().isEmpty()
                        ? "없음"
                        : reference.getTags().stream().map(Tag::getName).collect(Collectors.joining(", ")),
                    reference.getMatchedChunkText()
                ))
                .collect(Collectors.joining("\n"));

        String mcpResultText = mcpResult == null
            ? "사용하지 않음"
            : """
                제목: %s
                내용: %s
                태그: %s
                참고 요약:
                %s
                """.formatted(
                mcpResult.getTitle(),
                mcpResult.getContent(),
                String.join(", ", mcpResult.getTags()),
                String.join("\n", mcpResult.getSourceSummary())
            );

        return """
            사용자 요청:
            %s

            RAG 검색 결과:
            %s

            MCP 도구 결과:
            %s

            위 정보를 바탕으로 게시판 글 초안을 작성해주세요.

            작성 조건:
            - 사용자 요청과 직접 관련 있는 내용만 사용하세요.
            - RAG 검색 결과가 있으면 참고 게시글 목록에 포함하세요.
            - MCP 도구 결과가 있으면 toolsUsed에 "MCP_WEATHER"를 포함하세요.
            - MCP 도구 결과가 없으면 toolsUsed에는 "RAG"만 포함하세요.
            - 최종 답변은 반드시 JSON 형식으로만 작성하세요.
            """.formatted(userRequest, ragReferencesText, mcpResultText);
    }

    private McpWeatherDraftResponse tryLoadWeatherDraft(String userRequest, String requestedBy) {
        WeatherToolRequest weatherToolRequest = detectWeatherToolRequest(userRequest);

        if (weatherToolRequest == null) {
            return null;
        }

        try {
            return mcpWeatherDraftService.createWeatherDraft(
                weatherToolRequest.city(),
                weatherToolRequest.forecastDays(),
                requestedBy
            );
        } catch (Exception e) {
            log.warn("Agent weather MCP 호출에 실패했습니다. userRequest={}", userRequest, e);
            return null;
        }
    }

    private WeatherToolRequest detectWeatherToolRequest(String userRequest) {
        String normalized = userRequest.toLowerCase(Locale.ROOT);

        if (
            !normalized.contains("날씨") &&
            !normalized.contains("기온") &&
            !normalized.contains("예보") &&
            !normalized.contains("우산") &&
            !normalized.contains("weather")
        ) {
            return null;
        }

        Map<String, String> cityAliases = new LinkedHashMap<>();
        cityAliases.put("서울", "Seoul");
        cityAliases.put("seoul", "Seoul");
        cityAliases.put("부산", "Busan");
        cityAliases.put("busan", "Busan");
        cityAliases.put("인천", "Incheon");
        cityAliases.put("incheon", "Incheon");
        cityAliases.put("대구", "Daegu");
        cityAliases.put("daegu", "Daegu");
        cityAliases.put("대전", "Daejeon");
        cityAliases.put("daejeon", "Daejeon");
        cityAliases.put("광주", "Gwangju");
        cityAliases.put("gwangju", "Gwangju");
        cityAliases.put("울산", "Ulsan");
        cityAliases.put("ulsan", "Ulsan");
        cityAliases.put("제주", "Jeju");
        cityAliases.put("jeju", "Jeju");
        cityAliases.put("도쿄", "Tokyo");
        cityAliases.put("tokyo", "Tokyo");
        cityAliases.put("오사카", "Osaka");
        cityAliases.put("osaka", "Osaka");

        String selectedCity = null;

        for (Map.Entry<String, String> entry : cityAliases.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                selectedCity = entry.getValue();
                break;
            }
        }

        if (selectedCity == null) {
            return null;
        }

        int forecastDays = 2;
        Matcher matcher = FORECAST_DAYS_PATTERN.matcher(userRequest);

        if (matcher.find()) {
            forecastDays = Integer.parseInt(matcher.group(1));
        }

        return new WeatherToolRequest(selectedCity, forecastDays);
    }

    private AgentDraftResponse parseAgentResponse(String rawJson) {
        String normalized = sanitizeJson(rawJson);

        try {
            return objectMapper.readValue(normalized, AgentDraftResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Agent 응답을 JSON으로 해석하지 못했습니다."
            );
        }
    }

    private String sanitizeJson(String rawJson) {
        String trimmed = rawJson == null ? "" : rawJson.trim();

        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json\\s*", "");
            trimmed = trimmed.replaceFirst("^```\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }

        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }

        return trimmed;
    }

    private List<AgentReferenceItem> buildReferenceItems(List<RagReferenceResponse> ragReferences) {
        return ragReferences.stream()
            .map(reference -> new AgentReferenceItem(reference.getPostId(), reference.getTitle()))
            .toList();
    }

    private List<String> buildToolsUsed(McpWeatherDraftResponse mcpResult) {
        List<String> tools = new ArrayList<>();
        tools.add("RAG");

        if (mcpResult != null) {
            tools.add("MCP_WEATHER");
        }

        return tools;
    }

    private List<String> normalizeTags(
        List<String> originalTags,
        List<RagReferenceResponse> ragReferences,
        McpWeatherDraftResponse mcpResult
    ) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();

        if (originalTags != null) {
            for (String tag : originalTags) {
                if (tag != null && !tag.trim().isBlank()) {
                    tags.add(tag.trim());
                }
            }
        }

        if (mcpResult != null) {
            tags.addAll(mcpResult.getTags());
        }

        for (RagReferenceResponse ragReference : ragReferences) {
            for (Tag tag : ragReference.getTags()) {
                tags.add(tag.getName());

                if (tags.size() >= 5) {
                    break;
                }
            }

            if (tags.size() >= 5) {
                break;
            }
        }

        if (tags.size() < 3) {
            tags.add("게시판");
        }

        if (tags.size() < 3) {
            tags.add("정리");
        }

        if (tags.size() < 3) {
            tags.add("공유");
        }

        return tags.stream().limit(5).toList();
    }

    private String buildReasoningSummary(
        List<RagReferenceResponse> ragReferences,
        McpWeatherDraftResponse mcpResult,
        String originalReasoningSummary
    ) {
        if (originalReasoningSummary != null && !originalReasoningSummary.isBlank()) {
            return originalReasoningSummary.trim();
        }

        List<String> parts = new ArrayList<>();

        if (!ragReferences.isEmpty()) {
            parts.add("RAG 검색으로 관련 게시글 " + ragReferences.size() + "건을 참고했습니다.");
        }

        if (mcpResult != null) {
            parts.add("MCP 날씨 도구로 외부 날씨 정보를 조회했습니다.");
        }

        if (parts.isEmpty()) {
            parts.add("입력 요청을 바탕으로 일반적인 초안 형태로 정리했습니다.");
        }

        return String.join(" ", parts);
    }

    private record WeatherToolRequest(String city, int forecastDays) {
    }
}
