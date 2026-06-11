package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.tag.Tag;
import com.jungle_choi.namanmu.domain.tag.TagRepository;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DevSeedService {

    private static final String SEED_AUTHOR_NAME = "alpha-seed";
    private static final String SEED_AUTHOR_EMAIL = User.accountEmail(SEED_AUTHOR_NAME);

    private static final List<Angle> ANGLES = List.of(
            new Angle(
                    "초기 설계",
                    "처음부터 완성형으로 만들기보다 작은 단위로 쪼개어 검증하는 방식이 필요했다.",
                    List.of("Planning", "Architecture")),
            new Angle(
                    "구현 기록",
                    "실제 코드를 붙이면서 막힌 지점과 해결 기준을 남겼다.",
                    List.of("Implementation", "Debugging")),
            new Angle(
                    "운영 체크",
                    "로컬에서 동작한 기능이 서버와 DB까지 이어지는지 확인하는 절차를 정리했다.",
                    List.of("Ops", "Checklist")),
            new Angle(
                    "회고",
                    "이번 작업에서 배운 점, 한계, 다음에 개선할 아이디어를 짧게 정리했다.",
                    List.of("Review", "Retrospective")));

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final PostTagRepository postTagRepository;
    private final EmbeddingJobService embeddingJobService;

    public DevSeedService(
            UserRepository userRepository,
            PostRepository postRepository,
            TagRepository tagRepository,
            PostTagRepository postTagRepository,
            EmbeddingJobService embeddingJobService) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.postTagRepository = postTagRepository;
        this.embeddingJobService = embeddingJobService;
    }

    @Transactional
    public SeedResult seedPosts() {
        long existingSeedPostCount = postRepository.countByAuthor_Email(SEED_AUTHOR_EMAIL);
        if (existingSeedPostCount > 0) {
            return new SeedResult(0, 0, true, "Seed posts already exist.");
        }

        User author = userRepository.findByEmail(SEED_AUTHOR_EMAIL)
                .orElseGet(() -> userRepository.save(
                        User.createRegisteredUser(SEED_AUTHOR_NAME, "seed-data-user")));
        List<SeedPostDraft> drafts = createSeedPostDrafts();
        int createdPosts = 0;

        for (SeedPostDraft draft : drafts) {
            Post post = Post.create(
                    author,
                    draft.category(),
                    draft.title(),
                    draft.content());
            Post savedPost = postRepository.save(post);
            saveTags(savedPost, draft.tags());
            embeddingJobService.enqueuePostEmbedding(savedPost);
            createdPosts++;
        }

        return new SeedResult(
                createdPosts,
                createdPosts,
                false,
                "Seed posts were created.");
    }

    private void saveTags(Post post, List<String> tagNames) {
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
            postTagRepository.save(PostTag.create(post, tag));
        }
    }

    private static List<SeedPostDraft> createSeedPostDrafts() {
        List<SeedPostDraft> drafts = new ArrayList<>();

        for (CategoryPlan categoryPlan : categoryPlans()) {
            for (Topic topic : categoryPlan.topics()) {
                for (Angle angle : ANGLES) {
                    drafts.add(createDraft(categoryPlan, topic, angle));
                }
            }
        }

        return drafts;
    }

    private static SeedPostDraft createDraft(
            CategoryPlan categoryPlan,
            Topic topic,
            Angle angle) {
        String title = "%s - %s".formatted(topic.title(), angle.title());
        String content = """
                %s

                핵심 주제는 %s이다. %s

                %s

                다음 액션은 관련 코드를 작게 검증하고, 결과를 게시글과 태그로 다시 남기는 것이다.
                """.formatted(
                categoryPlan.intro(),
                topic.title(),
                topic.description(),
                angle.content()).trim();
        List<String> tags = mergeTags(categoryPlan.defaultTags(), topic.tags(), angle.tags());

        return new SeedPostDraft(categoryPlan.category(), title, content, tags);
    }

    private static List<String> mergeTags(
            List<String> defaultTags,
            List<String> topicTags,
            List<String> angleTags) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.addAll(defaultTags);
        tags.addAll(topicTags);
        tags.addAll(angleTags);

        return List.copyOf(tags);
    }

    private static List<CategoryPlan> categoryPlans() {
        return List.of(
                new CategoryPlan(
                        "Development",
                        "개발 로그는 구현 선택과 코드 변경 이유를 중심으로 남긴다.",
                        List.of("Development", "Code"),
                        List.of(
                                new Topic(
                                        "Spring Boot JWT 인증 흐름",
                                        "필터에서 토큰을 읽고 SecurityContext에 사용자 정보를 넣는 흐름을 점검했다.",
                                        List.of("Spring Boot", "JWT", "Security")),
                                new Topic(
                                        "React 작성 모달 상태 관리",
                                        "제목, 본문, 태그 입력값이 어떤 컴포넌트를 거쳐 전달되는지 확인했다.",
                                        List.of("React", "State", "Form")),
                                new Topic(
                                        "MySQL 게시글 검색과 페이징",
                                        "검색어, 카테고리, 페이지 번호가 서버 쿼리로 바뀌는 과정을 정리했다.",
                                        List.of("MySQL", "Paging", "Search")),
                                new Topic(
                                        "Docker 로컬 개발 환경",
                                        "DB 컨테이너와 Spring Boot 서버, Vite 서버를 함께 띄우는 절차를 다뤘다.",
                                        List.of("Docker", "Local", "MySQL")),
                                new Topic(
                                        "공통 예외 처리와 입력 검증",
                                        "Validation과 예외 응답을 통해 API 실패 상황을 일관되게 보여주는 방법을 정리했다.",
                                        List.of("Validation", "API", "Error")))),
                new CategoryPlan(
                        "Learning",
                        "학습 노트는 새 개념을 내 말로 다시 설명하는 데 집중한다.",
                        List.of("Learning", "Study"),
                        List.of(
                                new Topic(
                                        "RAG 기본 흐름",
                                        "문서를 검색한 뒤 검색 결과를 LLM 입력으로 넣는 구조를 학습했다.",
                                        List.of("RAG", "Retrieval", "LLM")),
                                new Topic(
                                        "Embedding 벡터 의미",
                                        "텍스트를 숫자 배열로 바꾸고 의미가 가까운 문장을 비교하는 방식을 정리했다.",
                                        List.of("Embedding", "Vector", "OpenAI")),
                                new Topic(
                                        "BM25와 벡터 검색 차이",
                                        "키워드 기반 검색과 의미 기반 검색이 서로 다른 문제를 해결한다는 점을 비교했다.",
                                        List.of("BM25", "Vector Search", "Search")),
                                new Topic(
                                        "Cosine similarity 계산",
                                        "두 벡터의 방향이 얼마나 비슷한지 점수화하는 수식을 코드와 함께 살펴봤다.",
                                        List.of("Cosine Similarity", "Math", "Retrieval")),
                                new Topic(
                                        "Agent 추론 루프",
                                        "도구 선택, 실행, 상태 업데이트, 종료 조건이 반복되는 구조를 정리했다.",
                                        List.of("Agent", "Loop", "LangGraph")))),
                new CategoryPlan(
                        "Project",
                        "프로젝트 기록은 기능, 일정, 의사결정을 나중에 다시 볼 수 있게 남긴다.",
                        List.of("Project", "Alpha"),
                        List.of(
                                new Topic(
                                        "Project Alpha 로드맵",
                                        "게시판 기능에서 AI 기능으로 넘어가는 순서를 단계별로 재정리했다.",
                                        List.of("Roadmap", "Planning", "Alpha")),
                                new Topic(
                                        "GitHub Projects 운영",
                                        "Todo, In Progress, Done 상태가 실제 개발 흐름과 맞는지 확인했다.",
                                        List.of("GitHub Projects", "Issue", "Workflow")),
                                new Topic(
                                        "AWS 배포 준비",
                                        "VPC, EC2, RDS, S3를 어디에 쓸지 과제 범위 안에서 검토했다.",
                                        List.of("AWS", "EC2", "RDS")),
                                new Topic(
                                        "README 문서 구조",
                                        "아키텍처, 데모, 회고가 빠지지 않도록 제출 문서 목차를 잡았다.",
                                        List.of("README", "Docs", "Architecture")),
                                new Topic(
                                        "발표 데모 시나리오",
                                        "로그인, 게시글 작성, RAG 추천, 날씨 브리핑 순서로 시연 흐름을 구성했다.",
                                        List.of("Demo", "Presentation", "Scenario")))),
                new CategoryPlan(
                        "Daily",
                        "일상 로그는 작업 리듬과 컨디션, 작은 발견을 가볍게 기록한다.",
                        List.of("Daily", "Log"),
                        List.of(
                                new Topic(
                                        "오전 개발 루틴",
                                        "서버 상태 확인, 오늘 할 일 점검, 첫 번째 작은 커밋까지의 루틴을 적었다.",
                                        List.of("Routine", "Morning", "Focus")),
                                new Topic(
                                        "디버깅 메모",
                                        "에러 메시지를 그대로 옮기고 원인 후보를 하나씩 줄이는 과정을 남겼다.",
                                        List.of("Debugging", "Error", "Memo")),
                                new Topic(
                                        "커피챗 이후 아이디어",
                                        "팀 프로젝트와 개인 과제 아이디어가 섞이지 않도록 분리해 정리했다.",
                                        List.of("Coffee Chat", "Idea", "Team")),
                                new Topic(
                                        "집중 시간 관리",
                                        "긴 개발 시간 동안 언제 쉬고 언제 구현에 들어갈지 기준을 세웠다.",
                                        List.of("Focus", "Time", "Energy")),
                                new Topic(
                                        "작은 회고 메모",
                                        "오늘 이해한 것과 아직 흐릿한 것을 분리해서 다음 학습 주제로 남겼다.",
                                        List.of("Retrospective", "WIL", "Memo")))),
                new CategoryPlan(
                        "Review",
                        "리뷰 글은 결과물을 비판적으로 보고 개선 포인트를 찾는 데 집중한다.",
                        List.of("Review", "Feedback"),
                        List.of(
                                new Topic(
                                        "코드 리뷰 기준",
                                        "동작 여부뿐 아니라 책임 분리, 테스트 가능성, 예외 처리를 함께 봤다.",
                                        List.of("Code Review", "Quality", "Test")),
                                new Topic(
                                        "UI 밀도와 사용성 리뷰",
                                        "메인 피드에서 글 작성 영역을 어떻게 숨기고 꺼낼지 비교했다.",
                                        List.of("UI", "UX", "Design")),
                                new Topic(
                                        "API 설계 리뷰",
                                        "엔드포인트 이름, 인증 필요 여부, 요청 응답 구조가 자연스러운지 검토했다.",
                                        List.of("API", "REST", "Design")),
                                new Topic(
                                        "DB 스키마 리뷰",
                                        "게시글, 댓글, 태그, 임베딩 작업 테이블 사이 관계를 다시 점검했다.",
                                        List.of("Database", "Schema", "JPA")),
                                new Topic(
                                        "보안 설정 리뷰",
                                        "공개 API와 인증 API가 어디서 갈리는지 SecurityConfig 기준으로 확인했다.",
                                        List.of("Security", "JWT", "Config")))),
                new CategoryPlan(
                        "Briefing",
                        "브리핑 글은 외부 정보나 운영 상태를 짧고 명확하게 요약한다.",
                        List.of("Briefing", "Summary"),
                        List.of(
                                new Topic(
                                        "오늘의 날씨 브리핑",
                                        "지역 날씨 데이터를 게시글 초안으로 바꾸는 흐름을 상상해봤다.",
                                        List.of("Weather", "MCP", "Briefing")),
                                new Topic(
                                        "AWS 비용 알림 점검",
                                        "크레딧이 있어도 예산 알림과 리소스 정리 기준이 필요하다는 점을 정리했다.",
                                        List.of("AWS", "Billing", "CloudWatch")),
                                new Topic(
                                        "OpenAI 사용량 확인",
                                        "임베딩과 생성 요청을 나눠 보고 비용이 생기는 지점을 표시했다.",
                                        List.of("OpenAI", "Cost", "Token")),
                                new Topic(
                                        "스프린트 진행 브리핑",
                                        "오늘 완료한 기능과 막힌 부분, 다음 액션을 한 화면에 모았다.",
                                        List.of("Sprint", "Status", "Planning")),
                                new Topic(
                                        "릴리즈 노트 초안",
                                        "기능 추가, 버그 수정, 남은 한계를 짧은 목록으로 정리했다.",
                                        List.of("Release Note", "Changelog", "Docs")))));
    }

    private record CategoryPlan(
            String category,
            String intro,
            List<String> defaultTags,
            List<Topic> topics) {
    }

    private record Topic(String title, String description, List<String> tags) {
    }

    private record Angle(String title, String content, List<String> tags) {
    }

    private record SeedPostDraft(
            String category,
            String title,
            String content,
            List<String> tags) {
    }

    public record SeedResult(
            int createdPosts,
            int createdEmbeddingJobs,
            boolean skipped,
            String message) {
    }
}
