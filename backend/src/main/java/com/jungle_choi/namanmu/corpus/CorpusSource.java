package com.jungle_choi.namanmu.corpus;

import java.util.List;

public record CorpusSource(
        String category,
        List<String> tags,
        String url) {
}
