package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.service.EmbeddingJobProcessor;
import com.jungle_choi.namanmu.service.OpenAiEmbeddingClient;
import com.jungle_choi.namanmu.service.PostEmbeddingTextBuilder;
import com.jungle_choi.namanmu.service.RagDraftService;
import com.jungle_choi.namanmu.service.SimilarPostSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class AiController {

    private final PostEmbeddingTextBuilder postEmbeddingTextBuilder;
    private final OpenAiEmbeddingClient openAiEmbeddingClient;
    private final SimilarPostSearchService similarPostSearchService;
    private final EmbeddingJobProcessor embeddingJobProcessor;
    private final RagDraftService ragDraftService;

    public AiController(
            PostEmbeddingTextBuilder postEmbeddingTextBuilder,
            OpenAiEmbeddingClient openAiEmbeddingClient,
            SimilarPostSearchService similarPostSearchService,
            EmbeddingJobProcessor embeddingJobProcessor,
            RagDraftService ragDraftService) {
        this.postEmbeddingTextBuilder = postEmbeddingTextBuilder;
        this.openAiEmbeddingClient = openAiEmbeddingClient;
        this.similarPostSearchService = similarPostSearchService;
        this.embeddingJobProcessor = embeddingJobProcessor;
        this.ragDraftService = ragDraftService;
    }

    @PostMapping("/similar-posts")
    public SimilarPostsResponse findSimilarPosts(
            @Valid @RequestBody SimilarPostsRequest request) {
        String sourceText = postEmbeddingTextBuilder.buildQuery(
                request.category(),
                request.title(),
                request.content(),
                request.tags());
        OpenAiEmbeddingClient.EmbeddingResult embeddingResult =
                openAiEmbeddingClient.createEmbedding(sourceText);

        List<SimilarPostResponse> posts = similarPostSearchService.searchSimilarPosts(
                        embeddingResult.embedding(),
                        request.excludedPostId(),
                        request.limit(),
                        request.title(),
                        request.content(),
                        request.tags())
                .stream()
                .map(SimilarPostResponse::from)
                .toList();

        return new SimilarPostsResponse(posts);
    }

    @PostMapping("/draft")
    public RagDraftService.RagDraftResult createDraft(
            @Valid @RequestBody RagDraftRequest request) {
        return ragDraftService.createDraft(
                request.category(),
                request.title(),
                request.content(),
                request.tags(),
                request.excludedPostId(),
                request.limit());
    }

    @PostMapping("/embedding-jobs/process-one")
    public EmbeddingJobProcessor.ProcessEmbeddingJobResult processOneEmbeddingJob() {
        return embeddingJobProcessor.processOnePendingJob();
    }

    @PostMapping("/embedding-jobs/process")
    public EmbeddingJobProcessor.ProcessEmbeddingJobsResult processEmbeddingJobs(
            @RequestParam(defaultValue = "5") int limit) {
        return embeddingJobProcessor.processPendingJobs(limit);
    }

    @GetMapping("/embedding-jobs/status")
    public EmbeddingJobProcessor.EmbeddingJobStatusSummary getEmbeddingJobStatus() {
        return embeddingJobProcessor.summarizeStatus();
    }

    public record SimilarPostsRequest(
            Long excludedPostId,
            @NotBlank(message = "category is required.")
            @Size(max = 30, message = "category must be 30 characters or fewer.")
            String category,
            @NotBlank(message = "title is required.")
            @Size(max = 120, message = "title must be 120 characters or fewer.")
            String title,
            @NotBlank(message = "content is required.")
            @Size(max = 5000, message = "content must be 5000 characters or fewer.")
            String content,
            List<@Size(max = 30, message = "tag must be 30 characters or fewer.") String> tags,
            int limit) {
    }

    public record SimilarPostsResponse(List<SimilarPostResponse> posts) {
    }

    public record RagDraftRequest(
            Long excludedPostId,
            @NotBlank(message = "category is required.")
            @Size(max = 30, message = "category must be 30 characters or fewer.")
            String category,
            @NotBlank(message = "title is required.")
            @Size(max = 120, message = "title must be 120 characters or fewer.")
            String title,
            @NotBlank(message = "content is required.")
            @Size(max = 5000, message = "content must be 5000 characters or fewer.")
            String content,
            List<@Size(max = 30, message = "tag must be 30 characters or fewer.") String> tags,
            int limit) {
    }

    public record SimilarPostResponse(
            Long postId,
            String title,
            String category,
            String content,
            double score) {

        static SimilarPostResponse from(SimilarPostSearchService.SimilarPostResult result) {
            return new SimilarPostResponse(
                    result.postId(),
                    result.title(),
                    result.category(),
                    result.content(),
                    result.score());
        }
    }
}
