package com.example.backend.rag;

import com.example.backend.post.Post;
import com.example.backend.post.PostRepository;
import com.example.backend.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PostEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PostEmbeddingService.class);
    private static final int MAX_CHUNK_LENGTH = 700;
    private static final int CHUNK_OVERLAP = 120;
    private static final int RAG_MATCH_LIMIT = 5;

    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final OpenAiChatClient openAiChatClient;
    private final PostEmbeddingRepository postEmbeddingRepository;
    private final PostRepository postRepository;
    private final RagSearchRepository ragSearchRepository;

    public PostEmbeddingService(
        OpenAiEmbeddingClient openAiEmbeddingClient,
        OpenAiChatClient openAiChatClient,
        PostEmbeddingRepository postEmbeddingRepository,
        PostRepository postRepository,
        RagSearchRepository ragSearchRepository
    ) {
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.openAiChatClient = openAiChatClient;
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.postRepository = postRepository;
        this.ragSearchRepository = ragSearchRepository;
    }

    public void syncPostEmbedding(Post post) {
        if (!openAiEmbeddingClient.isConfigured()) {
            log.info("OPENAI_API_KEY가 없어 게시글 {} 임베딩 생성을 건너뜁니다.", post.getId());
            return;
        }

        replacePostEmbeddings(post);
    }

    public RagReindexResponse reindexAllPosts(String requestedBy) {
        ensureConfigured();

        List<Post> posts = postRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<Long> failedPostIds = new ArrayList<>();
        int successCount = 0;

        for (Post post : posts) {
            try {
                replacePostEmbeddings(post);
                successCount++;
            } catch (Exception e) {
                failedPostIds.add(post.getId());
                log.warn("게시글 {} 재임베딩에 실패했습니다.", post.getId(), e);
            }
        }

        return new RagReindexResponse(
            requestedBy,
            openAiEmbeddingClient.getEmbeddingModel(),
            posts.size(),
            successCount,
            failedPostIds.size(),
            failedPostIds
        );
    }

    public void deletePostEmbeddings(Long postId) {
        postEmbeddingRepository.deleteByPostId(postId);
    }

    public RagStatusResponse getRagStatus() {
        return new RagStatusResponse(
            openAiEmbeddingClient.isConfigured() && openAiChatClient.isConfigured(),
            openAiEmbeddingClient.getEmbeddingModel(),
            openAiChatClient.getChatModel(),
            openAiEmbeddingClient.getEmbeddingDimensions(),
            postEmbeddingRepository.countIndexedPosts(),
            postEmbeddingRepository.countAllEmbeddings()
        );
    }

    public RagAskResponse askQuestion(String question, String requestedBy) {
        ensureConfigured();

        String trimmedQuestion = question == null ? "" : question.trim();

        if (trimmedQuestion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 입력해주세요.");
        }

        if (postEmbeddingRepository.countAllEmbeddings() == 0) {
            return new RagAskResponse(
                trimmedQuestion,
                "아직 임베딩된 게시글이 없습니다. 먼저 '기존 글 임베딩 갱신'을 실행해 주세요.",
                openAiChatClient.getChatModel(),
                openAiEmbeddingClient.getEmbeddingModel(),
                0,
                List.of()
            );
        }

        List<RagReferenceResponse> references = findRelevantReferences(trimmedQuestion, requestedBy, RAG_MATCH_LIMIT);

        if (references.isEmpty()) {
            return new RagAskResponse(
                trimmedQuestion,
                "관련 게시글을 찾지 못했습니다. 다른 표현으로 다시 질문해 주세요.",
                openAiChatClient.getChatModel(),
                openAiEmbeddingClient.getEmbeddingModel(),
                0,
                List.of()
            );
        }

        String answer = openAiChatClient.createAnswer(
            buildDeveloperPrompt(),
            buildUserPrompt(trimmedQuestion, references),
            requestedBy
        );

        return new RagAskResponse(
            trimmedQuestion,
            answer,
            openAiChatClient.getChatModel(),
            openAiEmbeddingClient.getEmbeddingModel(),
            references.size(),
            references
        );
    }

    public List<RagReferenceResponse> findRelevantReferences(
        String question,
        String requestedBy,
        int limit
    ) {
        ensureConfigured();

        String trimmedQuestion = question == null ? "" : question.trim();

        if (trimmedQuestion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "질문을 입력해주세요.");
        }

        if (postEmbeddingRepository.countAllEmbeddings() == 0) {
            return List.of();
        }

        List<Double> questionEmbedding = openAiEmbeddingClient.createEmbedding(trimmedQuestion, requestedBy);
        List<RagSearchMatch> matches = ragSearchRepository.searchSimilarPosts(
            questionEmbedding,
            openAiEmbeddingClient.getEmbeddingModel(),
            limit
        );

        if (matches.isEmpty()) {
            return List.of();
        }

        Map<Long, RagSearchMatch> matchByPostId = new HashMap<>();
        List<Long> orderedPostIds = new ArrayList<>();

        for (RagSearchMatch match : matches) {
            matchByPostId.put(match.getPostId(), match);
            orderedPostIds.add(match.getPostId());
        }

        Map<Long, Post> postById = postRepository.findAllById(orderedPostIds).stream()
            .collect(Collectors.toMap(Post::getId, post -> post));

        return orderedPostIds.stream()
            .map(postById::get)
            .filter(post -> post != null)
            .map(post -> {
                RagSearchMatch match = matchByPostId.get(post.getId());
                return new RagReferenceResponse(
                    post.getId(),
                    post.getTitle(),
                    post.getAuthorName(),
                    post.getCreatedAt(),
                    buildPreview(match.getChunkText()),
                    match.getChunkText(),
                    toSimilarityScore(match.getDistance()),
                    post.getTags()
                );
            })
            .sorted(Comparator.comparingDouble(RagReferenceResponse::getSimilarityScore).reversed())
            .toList();
    }

    private void replacePostEmbeddings(Post post) {
        List<String> contentChunks = splitContentIntoChunks(post.getContent());

        if (contentChunks.isEmpty()) {
            contentChunks = List.of(post.getContent());
        }

        postEmbeddingRepository.deleteByPostId(post.getId());

        for (int index = 0; index < contentChunks.size(); index++) {
            String chunkText = buildPostChunkText(post, contentChunks.get(index));
            List<Double> embedding = openAiEmbeddingClient.createEmbedding(chunkText, post.getOwnerLoginId());

            postEmbeddingRepository.saveOrUpdate(
                post.getId(),
                index,
                chunkText,
                openAiEmbeddingClient.getEmbeddingModel(),
                embedding
            );
        }
    }

    private void ensureConfigured() {
        if (!openAiEmbeddingClient.isConfigured() || !openAiChatClient.isConfigured()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "OPENAI_API_KEY가 설정되지 않아 RAG 기능을 실행할 수 없습니다."
            );
        }
    }

    private String buildDeveloperPrompt() {
        return """
            당신은 게시판 검색 도우미입니다.
            반드시 제공된 참고 게시글만 근거로 답변하세요.
            참고 게시글에 없는 내용은 추측해서 쓰지 마세요.
            답변은 한국어로 작성하고, 5문장 이내로 핵심만 간결하게 정리하세요.
            근거가 부족하면 부족하다고 분명히 말하세요.
            """;
    }

    private String buildUserPrompt(String question, List<RagReferenceResponse> references) {
        String referencesText = references.stream()
            .map(reference -> """
                [게시글 #%d]
                제목: %s
                작성자: %s
                작성일: %s
                유사도: %.3f
                태그: %s
                내용 요약: %s
                """.formatted(
                reference.getPostId(),
                reference.getTitle(),
                reference.getAuthorName(),
                reference.getCreatedAt(),
                reference.getSimilarityScore(),
                reference.getTags().stream().map(Tag::getName).collect(Collectors.joining(", ")),
                reference.getMatchedChunkText()
            ))
            .collect(Collectors.joining("\n"));

        return """
            사용자 질문:
            %s

            참고 게시글:
            %s

            위 참고 게시글만 바탕으로 답변해주세요.
            마지막 문장에는 어떤 게시글을 참고했는지 게시글 번호를 적어주세요.
            """.formatted(question, referencesText);
    }

    private double toSimilarityScore(double distance) {
        return Math.max(0.0, 1.0 - distance);
    }

    private List<String> splitContentIntoChunks(String content) {
        String normalized = content == null ? "" : content.trim();

        if (normalized.isBlank()) {
            return List.of();
        }

        if (normalized.length() <= MAX_CHUNK_LENGTH) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + MAX_CHUNK_LENGTH, normalized.length());
            chunks.add(normalized.substring(start, end).trim());

            if (end >= normalized.length()) {
                break;
            }

            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }

        return chunks;
    }

    private String buildPreview(String content) {
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();

        if (normalized.length() <= 220) {
            return normalized;
        }

        return normalized.substring(0, 220) + "...";
    }

    private String buildPostChunkText(Post post, String contentChunk) {
        String tagText = post.getTags().stream()
            .map(Tag::getName)
            .collect(Collectors.joining(", "));

        return """
            제목: %s
            작성자: %s
            태그: %s
            내용:
            %s
            """.formatted(
            post.getTitle(),
            post.getAuthorName(),
            tagText.isBlank() ? "없음" : tagText,
            contentChunk
        );
    }
}
