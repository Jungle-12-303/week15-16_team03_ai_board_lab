package com.example.backend.rag;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiEmbeddingRequest {

    private String input;
    private String model;
    private Integer dimensions;

    @JsonProperty("encoding_format")
    private String encodingFormat;

    private String user;
}
