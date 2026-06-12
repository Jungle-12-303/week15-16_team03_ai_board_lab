package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PostChunkTextSplitterTest {

    @Test
    void splitReturnsEmptyListForBlankContent() {
        PostChunkTextSplitter splitter = new PostChunkTextSplitter(120, 20);

        List<PostChunkTextSplitter.PostTextChunk> chunks = splitter.split("   \n\n   ");

        assertThat(chunks).isEmpty();
    }

    @Test
    void splitKeepsShortParagraphsInOneChunk() {
        PostChunkTextSplitter splitter = new PostChunkTextSplitter(200, 20);

        List<PostChunkTextSplitter.PostTextChunk> chunks = splitter.split("""
                첫 번째 문단입니다.

                두 번째 문단입니다.
                """);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().chunkIndex()).isZero();
        assertThat(chunks.getFirst().text())
                .contains("첫 번째 문단입니다.")
                .contains("두 번째 문단입니다.");
    }

    @Test
    void splitAddsOverlapWhenStartingNextParagraphChunk() {
        PostChunkTextSplitter splitter = new PostChunkTextSplitter(120, 20);
        String firstParagraph = "A".repeat(80);
        String secondParagraph = "B".repeat(80);

        List<PostChunkTextSplitter.PostTextChunk> chunks =
                splitter.split(firstParagraph + "\n\n" + secondParagraph);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(1).text())
                .startsWith(firstParagraph.substring(firstParagraph.length() - 20))
                .contains(secondParagraph);
    }

    @Test
    void splitBreaksParagraphLongerThanMaximumChunkLength() {
        PostChunkTextSplitter splitter = new PostChunkTextSplitter(120, 20);
        String longParagraph = IntStream.range(0, 250)
                .mapToObj((index) -> String.valueOf((char) ('a' + (index % 26))))
                .collect(Collectors.joining());

        List<PostChunkTextSplitter.PostTextChunk> chunks = splitter.split(longParagraph);

        assertThat(chunks).hasSize(3);
        assertThat(chunks)
                .allSatisfy((chunk) -> assertThat(chunk.text()).hasSizeLessThanOrEqualTo(120));
        assertThat(chunks.get(1).text()).startsWith(longParagraph.substring(100, 110));
        assertThat(chunks.get(2).text()).startsWith(longParagraph.substring(200, 210));
    }
}
