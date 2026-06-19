package com.example.aiknowledgeboard.ai.notion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotionClient {
    private static final Logger log = LoggerFactory.getLogger(NotionClient.class);
    private static final String TITLE_PROPERTY_NAME = "Question";
    private static final int RICH_TEXT_LIMIT = 1900;

    private final String apiKey;
    private final String dataSourceId;
    private final String notionVersion;
    private final RestClient restClient;

    public NotionClient(
            @Value("${notion.api-key}") String apiKey,
            @Value("${notion.data-source-id}") String dataSourceId,
            @Value("${notion.version}") String notionVersion
    ) {
        this.apiKey = apiKey;
        this.dataSourceId = dataSourceId;
        this.notionVersion = hasText(notionVersion) ? notionVersion : "2026-03-11";
        this.restClient = RestClient.builder().baseUrl("https://api.notion.com/v1").build();
    }

    public NotionSaveResponse saveRagAnswer(NotionSaveRequest request) {
        if (!hasText(apiKey) || !hasText(dataSourceId)) {
            log.warn("Notion save skipped because required settings are missing. apiKeyConfigured={}, dataSourceIdConfigured={}",
                    hasText(apiKey),
                    hasText(dataSourceId));
            throw new NotionClientException("Notion 저장 실패");
        }

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/pages")
                    .headers(headers -> {
                        headers.setBearerAuth(apiKey);
                        headers.set("Notion-Version", notionVersion);
                        headers.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .body(buildCreatePageBody(request))
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("id") == null || response.get("url") == null) {
                log.warn("Notion API returned an incomplete create page response.");
                throw new NotionClientException("Notion 저장 실패");
            }

            return new NotionSaveResponse(
                    response.get("id").toString(),
                    response.get("url").toString(),
                    true
            );
        } catch (RestClientResponseException ex) {
            log.warn("Notion API request failed. status={}, body={}",
                    ex.getStatusCode().value(),
                    trim(ex.getResponseBodyAsString()));
            throw new NotionClientException("Notion 저장 실패", ex);
        } catch (RestClientException ex) {
            log.warn("Notion API request failed before receiving a response: {}", ex.getMessage());
            throw new NotionClientException("Notion 저장 실패", ex);
        }
    }

    private Map<String, Object> buildCreatePageBody(NotionSaveRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("parent", Map.of("data_source_id", dataSourceId));
        body.put("properties", Map.of(
                TITLE_PROPERTY_NAME,
                Map.of("title", List.of(richText(request.question())))
        ));
        body.put("children", buildChildren(request));
        return body;
    }

    private List<Map<String, Object>> buildChildren(NotionSaveRequest request) {
        List<Map<String, Object>> children = new ArrayList<>();
        children.add(headingBlock("답변"));
        addParagraphBlocks(children, request.answer());

        if (request.sources() != null && !request.sources().isEmpty()) {
            children.add(headingBlock("참고 게시글"));
            for (NotionSourceRequest source : request.sources()) {
                children.add(bulletedListItemBlock(formatSource(source)));
            }
        }

        return children;
    }

    private void addParagraphBlocks(List<Map<String, Object>> children, String text) {
        for (String chunk : splitText(text)) {
            children.add(paragraphBlock(chunk));
        }
    }

    private Map<String, Object> headingBlock(String text) {
        return block("heading_2", Map.of("rich_text", List.of(richText(text))));
    }

    private Map<String, Object> paragraphBlock(String text) {
        return block("paragraph", Map.of("rich_text", List.of(richText(text))));
    }

    private Map<String, Object> bulletedListItemBlock(String text) {
        return block("bulleted_list_item", Map.of("rich_text", List.of(richText(text))));
    }

    private Map<String, Object> block(String type, Map<String, Object> payload) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("object", "block");
        block.put("type", type);
        block.put(type, payload);
        return block;
    }

    private Map<String, Object> richText(String content) {
        return Map.of(
                "type", "text",
                "text", Map.of("content", content == null ? "" : content)
        );
    }

    private String formatSource(NotionSourceRequest source) {
        String title = hasText(source.title()) ? source.title() : "제목 없음";
        String link = hasText(source.link()) ? " | 링크: " + source.link() : "";
        String preview = hasText(source.contentPreview()) ? " | 미리보기: " + source.contentPreview() : "";
        return title + link + preview;
    }

    private List<String> splitText(String text) {
        String value = text == null ? "" : text;
        if (value.length() <= RICH_TEXT_LIMIT) {
            return List.of(value);
        }

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < value.length(); start += RICH_TEXT_LIMIT) {
            chunks.add(value.substring(start, Math.min(start + RICH_TEXT_LIMIT, value.length())));
        }
        return chunks;
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
