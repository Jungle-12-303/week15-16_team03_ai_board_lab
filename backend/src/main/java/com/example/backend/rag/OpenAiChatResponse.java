package com.example.backend.rag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiChatResponse {

    private String id;
    private String model;
    private List<OpenAiChatChoice> choices;
}
