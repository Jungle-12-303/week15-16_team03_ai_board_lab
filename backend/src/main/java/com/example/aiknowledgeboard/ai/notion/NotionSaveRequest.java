package com.example.aiknowledgeboard.ai.notion;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record NotionSaveRequest(
        @NotBlank String question,
        @NotBlank String answer,
        List<NotionSourceRequest> sources
) {
}
