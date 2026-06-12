package com.jungle_choi.namanmu.api.dto;

import java.util.List;

public record PostPageResponse(
        List<PostResponse> posts,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<CategoryCountResponse> categoryCounts) {
}
