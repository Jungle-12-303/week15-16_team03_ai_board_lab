package com.example.backend.rag;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OpenAiChatChoice {

    private Integer index;
    private OpenAiChatMessage message;
    private String finish_reason;
}
