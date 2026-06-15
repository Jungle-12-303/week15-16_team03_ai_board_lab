package com.example.aiknowledgeboard.ai.agent;

import com.example.aiknowledgeboard.ai.common.AiLogService;
import com.example.aiknowledgeboard.ai.common.OpenAiClient;
import com.example.aiknowledgeboard.ai.mcp.JsonRpcRequest;
import com.example.aiknowledgeboard.ai.mcp.JsonRpcResponse;
import com.example.aiknowledgeboard.ai.mcp.McpService;
import com.example.aiknowledgeboard.ai.rag.RagService;
import com.example.aiknowledgeboard.ai.rag.SimilarPostResponse;
import com.example.aiknowledgeboard.auth.CurrentUserService;
import com.example.aiknowledgeboard.user.UserEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {
    private static final int MAX_ITERATIONS = 3;
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile("github\\.com/([A-Za-z0-9-]+)");

    private final RagService ragService;
    private final McpService mcpService;
    private final OpenAiClient openAiClient;
    private final CurrentUserService currentUserService;
    private final AgentMemoryRepository memoryRepository;
    private final AiLogService aiLogService;

    public AgentService(
            RagService ragService,
            McpService mcpService,
            OpenAiClient openAiClient,
            CurrentUserService currentUserService,
            AgentMemoryRepository memoryRepository,
            AiLogService aiLogService
    ) {
        this.ragService = ragService;
        this.mcpService = mcpService;
        this.openAiClient = openAiClient;
        this.currentUserService = currentUserService;
        this.memoryRepository = memoryRepository;
        this.aiLogService = aiLogService;
    }

    @Transactional
    public AgentResponse help(AgentRequest request) {
        UserEntity user = currentUserService.getCurrentUser();
        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.sessionId();

        List<AgentStepResponse> steps = new ArrayList<>();
        List<String> recommendedTags = List.of();
        List<SimilarPostResponse> similarPosts = List.of();
        Object mcpResult = null;
        boolean fallback = false;

        List<String> tools = chooseTools(request.draft(), request.intention());
        int repeatedFailures = 0;
        String lastFailedTool = null;

        for (int i = 0; i < Math.min(MAX_ITERATIONS, tools.size()); i++) {
            String tool = tools.get(i);
            try {
                switch (tool) {
                    case "recommend_tags" -> {
                        recommendedTags = recommendTags(request.draft());
                        steps.add(new AgentStepResponse(tool, "SUCCESS", "초안에서 태그를 추천했습니다."));
                    }
                    case "search_similar_posts" -> {
                        similarPosts = ragService.findSimilarPosts(request.draft(), null, 3);
                        steps.add(new AgentStepResponse(tool, "SUCCESS", "기존 게시글에서 유사 글을 검색했습니다."));
                    }
                    case "call_mcp_external_api" -> {
                        String username = extractGithubUsername(request.draft());
                        JsonRpcResponse response = mcpService.call(new JsonRpcRequest(
                                "2.0",
                                "github.getUser",
                                Map.of("username", username),
                                "agent-" + sessionId
                        ));
                        mcpResult = response.error() == null ? response.result() : response.error();
                        steps.add(new AgentStepResponse(tool, response.error() == null ? "SUCCESS" : "FAIL", "GitHub 사용자 정보를 조회했습니다: " + username));
                    }
                    case "summarize_draft" -> steps.add(new AgentStepResponse(tool, "SUCCESS", "초안 개선 요약을 생성했습니다."));
                    default -> steps.add(new AgentStepResponse(tool, "SKIP", "알 수 없는 도구라 건너뛰었습니다."));
                }
                repeatedFailures = 0;
            } catch (Exception ex) {
                fallback = true;
                repeatedFailures = tool.equals(lastFailedTool) ? repeatedFailures + 1 : 1;
                lastFailedTool = tool;
                steps.add(new AgentStepResponse(tool, "FAIL", ex.getMessage()));
                if (repeatedFailures >= 2) {
                    break;
                }
            }
        }

        String finalMessage = buildFinalMessage(request.draft(), recommendedTags, similarPosts, mcpResult);
        memoryRepository.save(new AgentMemory(user.getId(), sessionId, "last_recommended_tags", recommendedTags.toString()));
        memoryRepository.save(new AgentMemory(user.getId(), sessionId, "last_agent_summary", finalMessage));
        aiLogService.log("AGENT", request.draft(), finalMessage, !fallback, fallback ? "일부 도구 실행 실패" : null);
        return new AgentResponse(finalMessage, recommendedTags, similarPosts, mcpResult, steps, fallback);
    }

    private List<String> chooseTools(String draft, String intention) {
        LinkedHashSet<String> tools = new LinkedHashSet<>();
        tools.add("recommend_tags");
        tools.add("search_similar_posts");

        String text = ((draft == null ? "" : draft) + " " + (intention == null ? "" : intention)).toLowerCase(Locale.ROOT);
        if (text.contains("github") || text.contains("깃허브")) {
            tools.add("call_mcp_external_api");
        } else {
            tools.add("summarize_draft");
        }
        return new ArrayList<>(tools);
    }

    private List<String> recommendTags(String draft) {
        String text = draft.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Map<String, String> keywordToTag = new LinkedHashMap<>();
        keywordToTag.put("spring", "spring");
        keywordToTag.put("react", "react");
        keywordToTag.put("postgres", "postgresql");
        keywordToTag.put("docker", "docker");
        keywordToTag.put("aws", "aws");
        keywordToTag.put("rag", "rag");
        keywordToTag.put("mcp", "mcp");
        keywordToTag.put("agent", "agent");
        keywordToTag.put("jwt", "auth");
        keywordToTag.put("인증", "auth");
        keywordToTag.put("배포", "devops");
        keywordToTag.put("검색", "search");
        keywordToTag.put("게시판", "board");

        for (Map.Entry<String, String> entry : keywordToTag.entrySet()) {
            if (text.contains(entry.getKey())) {
                tags.add(entry.getValue());
            }
            if (tags.size() >= 5) {
                break;
            }
        }
        for (String token : text.split("[^a-z0-9가-힣]+")) {
            if (token.length() >= 3 && tags.size() < 5) {
                tags.add(token);
            }
        }
        if (tags.isEmpty()) {
            tags.addAll(List.of("note", "draft", "knowledge"));
        }
        return new ArrayList<>(tags).subList(0, Math.min(5, tags.size()));
    }

    private String extractGithubUsername(String draft) {
        Matcher matcher = GITHUB_URL_PATTERN.matcher(draft);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "openai";
    }

    private String buildFinalMessage(String draft, List<String> tags, List<SimilarPostResponse> similarPosts, Object mcpResult) {
        String fallback = """
                추천 태그: %s
                유사 게시글 수: %d
                GitHub 조회 결과: %s
                이 결과를 참고해서 제목은 구체적으로, 본문은 문제-시도-결과 순서로 정리해 보세요.
                """.formatted(tags, similarPosts.size(), mcpResult == null ? "없음" : mcpResult);
        return openAiClient.chat(
                "너는 게시글 작성 보조 Agent다. 도구 결과를 바탕으로 한국어로 짧고 실용적인 조언을 한다.",
                """
                        초안:
                        %s

                        추천 태그: %s
                        유사 게시글: %s
                        MCP 결과: %s

                        사용자가 바로 글을 고칠 수 있게 개선 방향을 3문장 이내로 제안해라.
                        """.formatted(draft, tags, similarPosts, mcpResult),
                fallback
        );
    }
}
