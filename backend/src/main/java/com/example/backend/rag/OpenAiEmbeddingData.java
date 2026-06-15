package com.example.backend.rag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiEmbeddingData {

    private List<Double> embedding;
    private Integer index;
    private String object;
}
