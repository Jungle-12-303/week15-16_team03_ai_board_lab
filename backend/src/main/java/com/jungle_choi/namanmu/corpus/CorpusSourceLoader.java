package com.jungle_choi.namanmu.corpus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CorpusSourceLoader {

    public List<CorpusSource> load(String sourceFile) {
        Path sourcePath = Path.of(sourceFile);

        if (!Files.exists(sourcePath)) {
            throw new IllegalStateException("Corpus source file was not found: " + sourcePath);
        }

        try {
            return Files.readAllLines(sourcePath, StandardCharsets.UTF_8)
                    .stream()
                    .map(String::trim)
                    .filter((line) -> !line.isBlank())
                    .filter((line) -> !line.startsWith("#"))
                    .map(this::parseLine)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Corpus source file could not be read: " + sourcePath, exception);
        }
    }

    private CorpusSource parseLine(String line) {
        String[] columns = line.split("\t");

        if (columns.length < 3) {
            throw new IllegalArgumentException(
                    "Corpus source line must have category, tags, and url columns: " + line);
        }

        return new CorpusSource(
                columns[0].trim(),
                parseTags(columns[1]),
                columns[2].trim());
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }

        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter((tag) -> !tag.isBlank())
                .toList();
    }
}
