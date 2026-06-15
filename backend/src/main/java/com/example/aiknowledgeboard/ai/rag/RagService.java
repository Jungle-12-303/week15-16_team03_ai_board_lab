package com.example.aiknowledgeboard.ai.rag;

import com.example.aiknowledgeboard.ai.common.AiLogService;
import com.example.aiknowledgeboard.ai.common.OpenAiClient;
import com.example.aiknowledgeboard.post.Post;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RagService {
    private final JdbcTemplate jdbcTemplate;
    private final OpenAiClient openAiClient;
    private final AiLogService aiLogService;

    public RagService(JdbcTemplate jdbcTemplate, OpenAiClient openAiClient, AiLogService aiLogService) {
        this.jdbcTemplate = jdbcTemplate;
        this.openAiClient = openAiClient;
        this.aiLogService = aiLogService;
    }

    @Transactional
    public void indexPost(Post post) {
        String sourceText = post.getTitle() + "\n" + post.getContent();
        String vectorLiteral = openAiClient.toVectorLiteral(openAiClient.createEmbedding(sourceText));
        try {
            jdbcTemplate.update("""
                            insert into post_embeddings (post_id, embedding, source_text, created_at, updated_at)
                            values (?, cast(? as vector), ?, now(), now())
                            on conflict (post_id)
                            do update set embedding = excluded.embedding,
                                          source_text = excluded.source_text,
                                          updated_at = now()
                            """,
                    post.getId(),
                    vectorLiteral,
                    sourceText
            );
            aiLogService.log("RAG_INDEX", sourceText, "postId=" + post.getId(), true, null);
        } catch (Exception ex) {
            aiLogService.log("RAG_INDEX", sourceText, null, false, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public RagResponse findSimilarAndSummarize(String query, Long excludePostId) {
        List<SimilarPostResponse> sources = findSimilarPosts(query, excludePostId, 3);
        String summary = summarize(query, sources);
        aiLogService.log("RAG_QUERY", query, summary, true, null);
        return new RagResponse(summary, sources);
    }

    @Transactional(readOnly = true)
    public List<SimilarPostResponse> findSimilarPosts(String query, Long excludePostId, int limit) {
        String vectorLiteral = openAiClient.toVectorLiteral(openAiClient.createEmbedding(query));
        try {
            return jdbcTemplate.query("""
                            with q as (select cast(? as vector) as embedding)
                            select p.id,
                                   p.title,
                                   left(p.content, 240) as content_preview,
                                   u.nickname as author_nickname,
                                   (1 - (pe.embedding <=> q.embedding)) as score
                            from post_embeddings pe
                            join q on true
                            join posts p on p.id = pe.post_id
                            join users u on u.id = p.user_id
                            where (? is null or p.id <> ?)
                            order by pe.embedding <=> q.embedding
                            limit ?
                            """,
                    (rs, rowNum) -> new SimilarPostResponse(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getString("content_preview"),
                            rs.getString("author_nickname"),
                            rs.getDouble("score"),
                            "/posts/" + rs.getLong("id")
                    ),
                    vectorLiteral,
                    excludePostId,
                    excludePostId,
                    Math.max(1, Math.min(limit, 10))
            );
        } catch (Exception ex) {
            aiLogService.log("RAG_QUERY", query, null, false, ex.getMessage());
            return List.of();
        }
    }

    private String summarize(String query, List<SimilarPostResponse> sources) {
        if (sources.isEmpty()) {
            return "유사한 게시글을 찾지 못했습니다. 먼저 관련 게시글을 몇 개 작성한 뒤 다시 시도해 보세요.";
        }
        StringBuilder sourceBuilder = new StringBuilder();
        for (SimilarPostResponse source : sources) {
            sourceBuilder.append("- 제목: ").append(source.title()).append("\n")
                    .append("  내용 일부: ").append(source.contentPreview()).append("\n")
                    .append("  링크: ").append(source.link()).append("\n");
        }
        String fallback = "OpenAI API 키가 없어 로컬 요약을 사용했습니다. 참고할 글은 "
                + sources.stream().map(SimilarPostResponse::title).toList()
                + " 입니다. 출처 링크를 확인해 중복 내용을 줄이고 글의 관점을 보완하세요.";
        return openAiClient.chat(
                "너는 게시판 작성 보조 AI다. 반드시 한국어로 짧고 구체적으로 답한다.",
                """
                        아래 사용자 입력과 유사 게시글 목록을 참고해서
                        1. 참고할 만한 핵심 내용
                        2. 중복되는 주장
                        3. 출처 게시글 링크
                        를 짧게 정리해라.

                        사용자 입력:
                        %s

                        유사 게시글:
                        %s
                        """.formatted(query, sourceBuilder),
                fallback
        );
    }
}
