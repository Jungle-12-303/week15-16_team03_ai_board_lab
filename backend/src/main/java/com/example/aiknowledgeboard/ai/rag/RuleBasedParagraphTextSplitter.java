package com.example.aiknowledgeboard.ai.rag;

import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RuleBasedParagraphTextSplitter extends TextSplitter {

    private static final int MIN_CHARS = 300;
    private static final int TARGET_CHARS = 1200;
    private static final int MAX_CHARS = 1800;

    @Override
    protected List<String> splitText(String text) {
        String normalized = normalize(text);

        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> paragraphs = splitParagraphs(normalized);

        if (paragraphs.size() <= 1) {
            return splitLongText(normalized);
        }

        return mergeParagraphs(paragraphs);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private List<String> splitParagraphs(String text) {
        String[] parts = text.split("(?:\\n[ \\t]*){2,}");
        List<String> paragraphs = new ArrayList<>();

        for (String part : parts) {
            String paragraph = part.trim();

            if (!paragraph.isBlank()) {
                paragraphs.add(paragraph);
            }
        }

        return paragraphs;
    }

    private List<String> splitLongText(String text) {
        List<String> chunks = new ArrayList<>();
        int current = 0;

        while (current < text.length()) {
            int remaining = text.length() - current;

            if (remaining <= TARGET_CHARS) {
                addChunk(chunks, text.substring(current));
                break;
            }

            int targetEnd = current + TARGET_CHARS;
            int end = findBestSplitPoint(text, current, targetEnd);

            addChunk(chunks, text.substring(current, end));
            current = end;
        }

        return chunks;
    }

    private List<String> mergeParagraphs(List<String> paragraphs) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.length() > MAX_CHARS) {
                addChunk(chunks, current.toString());
                chunks.addAll(splitLongText(paragraph));
                current.setLength(0);
                continue;
            }

            if (current.isEmpty()) {
                current.append(paragraph);
                continue;
            }

            if (isTopicBoundary(paragraph)) {
                addChunk(chunks, current.toString());
                current = new StringBuilder(paragraph);
                continue;
            }

            if (canMerge(current, paragraph)) {
                current.append("\n\n").append(paragraph);
                continue;
            }

            addChunk(chunks, current.toString());
            current = new StringBuilder(paragraph);
        }

        addChunk(chunks, current.toString());

        return chunks;
    }

    private void addChunk(List<String> chunks, String chunk) {
        if (chunk == null) {
            return;
        }

        String trimmed = chunk.trim();

        if (!trimmed.isBlank()) {
            chunks.add(trimmed);
        }
    }

    private int findBestSplitPoint(String text, int start, int targetEnd) {
        int safeEnd = Math.min(targetEnd, text.length());
        int sentenceEnd = findLastSentenceEnd(text, start, safeEnd);

        if (sentenceEnd > start + MIN_CHARS) {
            return sentenceEnd + 1;
        }

        return safeEnd;
    }

    private int findLastSentenceEnd(String text, int start, int end) {
        int last = -1;

        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '.' || c == '?' || c == '!' || c == '\n'
                    || c == '。' || c == '？' || c == '！') {
                last = i;
            }
        }

        return last;
    }

    private boolean canMerge(StringBuilder current, String nextParagraph) {
        int mergedLength = current.length() + 2 + nextParagraph.length();

        if (mergedLength <= TARGET_CHARS) {
            return true;
        }

        return current.length() < MIN_CHARS && mergedLength <= MAX_CHARS;
    }

    private boolean isTopicBoundary(String paragraph) {
        String text = paragraph.trim();

        if (text.isBlank()) {
            return false;
        }

        return isQuestionTitle(text)
                || isNumberedTitle(text)
                || isMarkdownTitle(text);
    }

    private boolean isQuestionTitle(String text) {
        return text.endsWith("?") || text.endsWith("？");
    }

    private boolean isNumberedTitle(String text) {
        return text.matches("^\\d+[.)]\\s+.*");
    }

    private boolean isMarkdownTitle(String text) {
        return text.matches("^#{1,6}\\s+.*");
    }
}
