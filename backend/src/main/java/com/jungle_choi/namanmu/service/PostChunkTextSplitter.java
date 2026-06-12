package com.jungle_choi.namanmu.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PostChunkTextSplitter {

    public static final int DEFAULT_MAX_CHUNK_LENGTH = 1200;
    public static final int DEFAULT_OVERLAP_LENGTH = 180;

    private final int maxChunkLength;
    private final int overlapLength;

    public PostChunkTextSplitter() {
        this(DEFAULT_MAX_CHUNK_LENGTH, DEFAULT_OVERLAP_LENGTH);
    }

    PostChunkTextSplitter(int maxChunkLength, int overlapLength) {
        if (maxChunkLength < 100) {
            throw new IllegalArgumentException("maxChunkLength must be 100 or greater.");
        }

        if (overlapLength < 0 || overlapLength >= maxChunkLength) {
            throw new IllegalArgumentException("overlapLength must be less than maxChunkLength.");
        }

        this.maxChunkLength = maxChunkLength;
        this.overlapLength = overlapLength;
    }

    public List<PostTextChunk> split(String content) {
        String normalizedContent = normalizeContent(content);
        if (normalizedContent.isBlank()) {
            return List.of();
        }

        List<String> paragraphs = splitParagraphs(normalizedContent);
        List<String> chunkTexts = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();
        String previousChunkText = "";

        for (String paragraph : paragraphs) {
            if (paragraph.length() > maxChunkLength) {
                previousChunkText = flushCurrentChunk(chunkTexts, currentChunk, previousChunkText);
                previousChunkText = splitLongParagraph(paragraph, chunkTexts);
                continue;
            }

            if (currentChunk.isEmpty()) {
                appendWithOverlap(currentChunk, previousChunkText, paragraph);
                continue;
            }

            int nextLength = currentChunk.length() + 2 + paragraph.length();
            if (nextLength <= maxChunkLength) {
                currentChunk.append("\n\n").append(paragraph);
                continue;
            }

            previousChunkText = flushCurrentChunk(chunkTexts, currentChunk, previousChunkText);
            appendWithOverlap(currentChunk, previousChunkText, paragraph);
        }

        flushCurrentChunk(chunkTexts, currentChunk, previousChunkText);

        List<PostTextChunk> chunks = new ArrayList<>();
        for (int index = 0; index < chunkTexts.size(); index++) {
            chunks.add(new PostTextChunk(index, chunkTexts.get(index)));
        }

        return chunks;
    }

    private String flushCurrentChunk(
            List<String> chunkTexts,
            StringBuilder currentChunk,
            String previousChunkText) {
        if (currentChunk.isEmpty()) {
            return previousChunkText;
        }

        String chunkText = currentChunk.toString().trim();
        if (!chunkText.isBlank()) {
            chunkTexts.add(chunkText);
            previousChunkText = chunkText;
        }

        currentChunk.setLength(0);
        return previousChunkText;
    }

    private String splitLongParagraph(String paragraph, List<String> chunkTexts) {
        String previousChunkText = "";
        int start = 0;

        while (start < paragraph.length()) {
            int end = Math.min(start + maxChunkLength, paragraph.length());
            String chunkText = paragraph.substring(start, end).trim();

            if (!chunkText.isBlank()) {
                chunkTexts.add(chunkText);
                previousChunkText = chunkText;
            }

            if (end == paragraph.length()) {
                break;
            }

            start = Math.max(end - overlapLength, start + 1);
        }

        return previousChunkText;
    }

    private void appendWithOverlap(
            StringBuilder currentChunk,
            String previousChunkText,
            String paragraph) {
        String overlapText = trailingOverlap(previousChunkText);

        if (!overlapText.isBlank()
                && overlapText.length() + 2 + paragraph.length() <= maxChunkLength) {
            currentChunk.append(overlapText).append("\n\n");
        }

        currentChunk.append(paragraph);
    }

    private String trailingOverlap(String text) {
        if (overlapLength == 0 || text == null || text.isBlank()) {
            return "";
        }

        String normalizedText = text.trim();
        if (normalizedText.length() <= overlapLength) {
            return normalizedText;
        }

        return normalizedText.substring(normalizedText.length() - overlapLength).trim();
    }

    private static List<String> splitParagraphs(String content) {
        return Arrays.stream(content.split("\\n\\s*\\n+"))
                .map(String::trim)
                .filter((paragraph) -> !paragraph.isBlank())
                .toList();
    }

    private static String normalizeContent(String content) {
        if (content == null) {
            return "";
        }

        return content
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .lines()
                .map((line) -> line.replaceAll("\\s+", " ").trim())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("")
                .trim();
    }

    public record PostTextChunk(int chunkIndex, String text) {
    }
}
