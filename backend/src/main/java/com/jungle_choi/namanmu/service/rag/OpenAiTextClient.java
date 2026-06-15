package com.jungle_choi.namanmu.service.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jungle_choi.namanmu.config.OpenAiProperties;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OpenAiTextClient {

    private final RestClient openAiRestClient;
    private final OpenAiProperties openAiProperties;

    public OpenAiTextClient(
            RestClient openAiRestClient,
            OpenAiProperties openAiProperties) {
        this.openAiRestClient = openAiRestClient;
        this.openAiProperties = openAiProperties;
    }

    public TextGenerationResult generateText(String instructions, String input) {
        if (!openAiProperties.hasApiKey()) {
            throw new IllegalStateException("OPENAI_API_KEY is required to generate text.");
        }

        ResponsesResponse response = openAiRestClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, openAiProperties.authorizationHeader())
                .body(new ResponsesRequest(
                        openAiProperties.chatModel(),
                        instructions,
                        input,
                        false,
                        null,
                        1200))
                .retrieve()
                .body(ResponsesResponse.class);

        return new TextGenerationResult(extractText(response));
    }

    public TextGenerationResult generateStructuredJson(
            String instructions,
            String input,
            StructuredJsonSchema structuredJsonSchema) {
        if (!openAiProperties.hasApiKey()) {
            throw new IllegalStateException("OPENAI_API_KEY is required to generate text.");
        }

        ResponsesResponse response = openAiRestClient.post()
                .uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, openAiProperties.authorizationHeader())
                .body(new ResponsesRequest(
                        openAiProperties.chatModel(),
                        instructions,
                        input,
                        false,
                        TextConfiguration.from(structuredJsonSchema),
                        1200))
                .retrieve()
                .body(ResponsesResponse.class);

        return new TextGenerationResult(extractText(response));
    }

    private static String extractText(ResponsesResponse response) {
        if (response == null) {
            throw new IllegalStateException("OpenAI text response is empty.");
        }

        if (response.outputText() != null && !response.outputText().isBlank()) {
            return response.outputText().trim();
        }

        if (response.output() == null) {
            throw new IllegalStateException("OpenAI text response has no output.");
        }

        String generatedText = response.output()
                .stream()
                .filter((output) -> output.content() != null)
                .flatMap((output) -> output.content().stream())
                .filter((content) -> content.text() != null && !content.text().isBlank())
                .map(ResponseContent::text)
                .reduce("", (left, right) -> left + right)
                .trim();

        if (generatedText.isBlank()) {
            throw new IllegalStateException("OpenAI text response has no generated text.");
        }

        return generatedText;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record ResponsesRequest(
            String model,
            String instructions,
            String input,
            Boolean store,
            TextConfiguration text,
            @JsonProperty("max_output_tokens") int maxOutputTokens) {
    }

    private record TextConfiguration(TextFormat format) {

        private static TextConfiguration from(StructuredJsonSchema structuredJsonSchema) {
            return new TextConfiguration(new TextFormat(
                    "json_schema",
                    structuredJsonSchema.name(),
                    structuredJsonSchema.description(),
                    true,
                    structuredJsonSchema.schema()));
        }
    }

    private record TextFormat(
            String type,
            String name,
            String description,
            Boolean strict,
            Map<String, Object> schema) {
    }

    private record ResponsesResponse(
            @JsonProperty("output_text") String outputText,
            List<ResponseOutput> output) {
    }

    private record ResponseOutput(List<ResponseContent> content) {
    }

    private record ResponseContent(String text) {
    }

    public record TextGenerationResult(String text) {
    }

    public record StructuredJsonSchema(
            String name,
            String description,
            Map<String, Object> schema) {
    }
}
