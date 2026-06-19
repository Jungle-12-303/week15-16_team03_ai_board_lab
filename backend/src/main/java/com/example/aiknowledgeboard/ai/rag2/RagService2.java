package com.example.aiknowledgeboard.ai.rag2;

import com.example.aiknowledgeboard.ai.rag.RagResponse;
import com.example.aiknowledgeboard.ai.rag.SimilarPostResponse;
import com.example.aiknowledgeboard.post.Post;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RagService2 {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final TextSplitter textSplitter;
    private final int topK;
    private final double similarityThreshold;

    public RagService2(VectorStore vectorStore, ChatClient.Builder chatClientBuilder,
                       @Value("${RAG_TOP_K:6}") int topK,
                       @Value("${RAG_SIMILARITY_THRESHOLD:0.4}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.textSplitter = createTokenTextSplitter();
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public void loadIndex(Post post) {
        delete(post);
        index(post);
    }

    private void index(Post post) {
        Document document = toDocument(post);
        List<Document> chunks = textSplitter.split(document);
        vectorStore.add(chunks);
    }

    public int reindexAll(List<Post> posts) {
        deleteAllPostIndexes();

        int indexedCount = 0;
        for (Post post : posts) {
            if (post.getId() == null) {
                continue;
            }

            index(post);
            indexedCount++;
        }
        return indexedCount;
    }

    private TextSplitter createTokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMinChunkSizeChars(200)
                .build();
    }

    private Document toDocument(Post post) {
        String sourceText = "제목: %s\n내용: %s"
                .formatted(post.getTitle(), post.getContent());

        return new Document(
                sourceText,
                Map.of(
                        "type", "post",
                        "postId", post.getId(),
                        "title", post.getTitle(),
                        "content", post.getContent(),
                        "authorNickname", post.getAuthor().getNickname()
                )
        );
    }

    public String requestQuery(String question) {
        return answer(question, null).summary();
    }

    public RagResponse answer(String query, Long excludePostId) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .filterExpression(postSearchFilter(excludePostId))
                        .build()
        );

        List<SimilarPostResponse> sources = toSources(documents);
        String summary = summarize(query, sources);
        return new RagResponse(summary, sources);
    }

    private String summarize(String query, List<SimilarPostResponse> sources) {
        if (sources.isEmpty()) {
            return "관련 게시글을 찾지 못했습니다. 먼저 관련 게시글을 작성하거나 기존 게시글을 수정해 인덱싱한 뒤 다시 질문해 주세요.";
        }

        StringBuilder sourceBuilder = new StringBuilder();
        for (SimilarPostResponse source : sources) {
            sourceBuilder.append("- 제목: ").append(source.title()).append("\n")
                    .append("  내용: ").append(source.contentPreview()).append("\n")
                    .append("  링크: ").append(source.link()).append("\n");
        }

        return chatClient.prompt()
                .system("너는 게시판 지식 베이스 Q&A 봇이다. 반드시 검색된 게시글만 근거로 사용하고, 한국어로 짧고 구체적으로 답한다.")
                .user("""
                        사용자 질문:
                        %s

                        검색된 게시글:
                        %s

                        위 게시글을 근거로 답변해라. 관련 게시글 제목과 링크도 함께 언급해라.
                        """.formatted(query, sourceBuilder))
                .call()
                .content();
    }

    private List<SimilarPostResponse> toSources(List<Document> documents) {
        Map<Long, SimilarPostResponse> sourcesByPostId = new LinkedHashMap<>();
        for (Document document : documents) {
            Long postId = metadataLong(document, "postId");
            if (postId == null || sourcesByPostId.containsKey(postId)) {
                continue;
            }

            SimilarPostResponse source = new SimilarPostResponse(
                    postId,
                    metadataText(document, "title", "제목 없음"),
                    preview(document.getText()),
                    metadataText(document, "authorNickname", "알 수 없음"),
                    document.getScore() == null ? 0.0d : document.getScore(),
                    "/posts/" + postId
            );
            sourcesByPostId.put(postId, source);
        }
        return new ArrayList<>(sourcesByPostId.values());
    }

    private Filter.Expression postSearchFilter(Long excludePostId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op postTypeFilter = builder.eq("type", "post");
        if (excludePostId == null) {
            return postTypeFilter.build();
        }
        return builder.and(postTypeFilter, builder.ne("postId", excludePostId)).build();
    }

    private Long metadataLong(Document document, String key) {
        Object value = document.getMetadata().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String metadataText(Document document, String key, String fallback) {
        Object value = document.getMetadata().get(key);
        if (value == null) {
            return fallback;
        }
        String text = value.toString();
        return text.isBlank() ? fallback : text;
    }

    private String preview(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 240) {
            return normalized;
        }
        return normalized.substring(0, 240);
    }

    public void delete(Post post) {
        if (post.getId() == null) {
            return;
        }

        Filter.Expression filter = getFilter(post);
        vectorStore.delete(filter);
    }

    private void deleteAllPostIndexes() {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq("type", "post")
                .build();
        vectorStore.delete(filter);
    }

    private Filter.Expression getFilter(Post post) {
        return new FilterExpressionBuilder()
                .eq("postId", post.getId())
                .build();
    }
}
