package com.example.backend.rag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiChatMessage {

    private String role;
    private String content;

    public OpenAiChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
