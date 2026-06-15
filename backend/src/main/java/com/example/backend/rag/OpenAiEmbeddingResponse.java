package com.example.backend.rag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiEmbeddingResponse {

    private List<OpenAiEmbeddingData> data;
    private String model;
    private String object;
    private OpenAiEmbeddingUsage usage;
}
