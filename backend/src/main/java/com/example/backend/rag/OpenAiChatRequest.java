package com.example.backend.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiChatRequest {

    private String model;
    private List<OpenAiChatMessage> messages;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;

    private Double temperature;

    private String user;
}
