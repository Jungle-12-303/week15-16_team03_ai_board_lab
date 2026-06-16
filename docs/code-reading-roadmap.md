# Code Reading Roadmap

이 문서는 Project Alpha 코드를 처음 보는 사람이 어떤 순서로 읽으면 되는지 정리한 학습용 로드맵입니다.
목표는 모든 파일을 한 번에 이해하는 것이 아니라, 기능 하나가 화면에서 DB와 AI 기능까지 어떻게 이어지는지 따라가는 것입니다.

더 자세한 교재형 설명은 `docs/textbook/README.md`부터 읽으면 됩니다. 이 문서는 코드 읽기 순서에 집중하고, 교재형 문서는 기술 선택, 대안, 적용 범위를 함께 설명합니다.

## 먼저 잡아야 할 큰 그림

Project Alpha는 크게 네 덩어리입니다.

| 덩어리 | 위치 | 하는 일 |
| --- | --- | --- |
| React frontend | `frontend/project-alpha/src` | 화면, 입력 폼, 버튼, API 호출 상태 관리 |
| Spring Boot backend | `backend/src/main/java/com/jungle_choi/namanmu` | REST API, 인증, 게시판 로직, AI 기능 실행 |
| MySQL database | Docker container | 사용자, 게시글, 댓글, 태그, 임베딩, 읽음 기록 저장 |
| External AI/API | OpenAI, GitHub, Weather API | 초안 생성, 임베딩, 외부 데이터 팩트체크 |

흐름은 대부분 아래 형태입니다.

```text
React component
-> React hook
-> frontend api function
-> Spring Controller
-> Spring Service
-> Repository
-> MySQL
```

AI 기능은 중간에 OpenAI 또는 MCP 외부 도구 호출이 추가됩니다.

## 0단계: 실행 구조부터 확인

처음에는 코드를 읽기 전에 서버가 어떻게 나뉘는지부터 봅니다.

| 파일 | 확인할 것 |
| --- | --- |
| `docker-compose.yml` | MySQL 컨테이너 이름, DB 이름, 포트 |
| `backend/build.gradle` | Spring Boot, JPA, Security, MySQL Driver 의존성 |
| `backend/src/main/resources/application.properties` | DB 접속, JWT, OpenAI, 임베딩 워커 설정 |
| `frontend/project-alpha/package.json` | React/Vite 실행 명령어와 의존성 |

읽고 나서 설명할 수 있어야 하는 것:

- 왜 서버가 프론트, 백엔드, DB 세 개로 나뉘는가
- `npm run dev`, `gradlew bootRun`, `docker compose up`이 각각 무엇을 띄우는가
- 백엔드가 DB와 OpenAI key를 어디서 읽는가

## 1단계: React 화면 진입점

먼저 화면 전체 구조를 잡습니다.

| 파일 | 역할 |
| --- | --- |
| `frontend/project-alpha/src/main.jsx` | React 앱을 브라우저 DOM에 붙이는 시작점 |
| `frontend/project-alpha/src/App.jsx` | 로그인 상태에 따라 페이지와 기능 hook을 연결 |
| `frontend/project-alpha/src/pages/LoginPage.jsx` | 로그인 화면 |
| `frontend/project-alpha/src/pages/SignupPage.jsx` | 회원가입 화면 |
| `frontend/project-alpha/src/pages/BoardPage.jsx` | 게시판 메인 화면 |
| `frontend/project-alpha/src/pages/PostDetailPage.jsx` | 게시글 상세, 댓글, MCP 팩트체크 화면 |

핵심 질문:

- 로그인하지 않았을 때 왜 게시판이 아니라 로그인 화면이 나오는가
- `BoardPage`는 직접 API를 호출하는가, 아니면 hook에서 받은 값을 렌더링하는가
- 상세 페이지는 URL의 `postId`를 어디서 얻는가

## 2단계: React hook으로 상태 관리 이해

React에서 기능 단위로 상태를 분리한 부분입니다.

| 파일 | 역할 |
| --- | --- |
| `frontend/project-alpha/src/hooks/useAuth.js` | 로그인, 회원가입, 로그아웃, 세션 복원 |
| `frontend/project-alpha/src/hooks/usePosts.js` | 게시글 목록, 생성, 수정, 삭제, 댓글 상태 |
| `frontend/project-alpha/src/hooks/usePostComposer.js` | 글 작성/수정 모달의 입력값 상태 |
| `frontend/project-alpha/src/hooks/useRagDraft.js` | RAG 유사글 검색, 초안 생성, 로딩/에러 상태 |
| `frontend/project-alpha/src/hooks/useAgentRecommendations.js` | Agent 추천 요청과 결과 상태 |

핵심 질문:

- `useState`로 관리하는 값은 무엇인가
- API 요청 전후로 loading, error, result가 어떻게 바뀌는가
- 화면 컴포넌트가 복잡해지지 않도록 hook이 어떤 책임을 가져갔는가

면접식으로 말하면:

> 반복되는 상태 관리와 API 호출 흐름을 custom hook으로 분리했습니다. 예를 들어 `usePosts`는 게시글 목록과 CRUD 요청을 담당하고, `useRagDraft`는 RAG 검색/초안 생성의 loading, error, result 상태를 관리합니다. 그래서 컴포넌트는 화면 조립에 집중하고, 데이터 흐름은 hook에서 추적할 수 있게 했습니다.

## 3단계: Frontend API 함수 확인

React가 백엔드와 만나는 지점입니다.

| 파일 | 역할 |
| --- | --- |
| `frontend/project-alpha/src/api/auth/authApi.js` | 회원가입, 로그인 요청 |
| `frontend/project-alpha/src/api/posts/postApi.js` | 게시글, 댓글 API 요청 |
| `frontend/project-alpha/src/api/ai/ragApi.js` | 유사 게시글 검색, RAG 초안 생성 요청 |
| `frontend/project-alpha/src/api/ai/mcpApi.js` | MCP fact check 요청 |
| `frontend/project-alpha/src/api/ai/agentApi.js` | Agent 추천 요청 |

핵심 질문:

- JWT는 httpOnly cookie로 내려가고, 변경 요청에는 CSRF header가 들어가는가
- `fetch` 결과가 실패했을 때 어디서 `throw new Error`를 하는가
- 프론트에서 쓰기 좋게 응답을 normalize하는 코드는 어디에 있는가

## 4단계: Spring Controller로 API 입구 보기

백엔드에서는 Controller가 HTTP 요청을 받습니다.

| 파일 | 역할 |
| --- | --- |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java` | 회원가입/로그인 API |
| `backend/src/main/java/com/jungle_choi/namanmu/api/PostController.java` | 게시글 CRUD, 검색, 상세 조회 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/CommentController.java` | 댓글 생성/삭제 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AiController.java` | RAG 검색, RAG 초안, 임베딩 작업 처리 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/PostFactCheckController.java` | 게시글 상세의 MCP fact check API |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AgentController.java` | 놓친 글 추천 Agent API |
| `backend/src/main/java/com/jungle_choi/namanmu/api/McpController.java` | JSON-RPC MCP endpoint |
| `backend/src/main/java/com/jungle_choi/namanmu/api/ApiExceptionHandler.java` | 예외를 HTTP 응답으로 변환 |

핵심 질문:

- `@RestController`, `@GetMapping`, `@PostMapping`은 어떤 역할인가
- Controller는 직접 DB를 만지는가, Service에 위임하는가
- 로그인 사용자는 `@AuthenticationPrincipal` 또는 security context에서 어떻게 들어오는가

## 4.5단계: Service 패키지 구조 보기

Controller 다음에는 바로 `service`를 보되, 이제 기능별 하위 패키지로 나누어 읽습니다.

| 패키지 | 먼저 볼 파일 | 책임 |
| --- | --- | --- |
| `service/post` | `PostService.java` | 게시글 CRUD, 댓글, 태그, 읽음 기록 |
| `service/rag` | `SimilarPostSearchService.java`, `RagDraftService.java` | 임베딩, retrieval, RAG 초안 생성 |
| `service/mcp` | `McpServerService.java`, `McpFactCheckService.java` | MCP 도구 호출과 팩트체크 |
| `service/agent` | `AgentRecommendationService.java` | 읽음 기록 기반 놓친 글 추천 |

핵심 질문:

- 게시판 기본 기능과 AI 응용 기능이 같은 Service 폴더에 섞이지 않도록 어떻게 분리했는가
- Controller가 어떤 하위 패키지의 Service를 호출하는가
- 기능을 추가한다면 어느 패키지에 넣어야 자연스러운가

## 5단계: Entity와 Repository로 DB 구조 이해

JPA Entity는 테이블 설계의 코드 버전입니다.

| 기능 | Entity | Repository |
| --- | --- | --- |
| 사용자 | `domain/user/User.java` | `UserRepository.java` |
| 게시글 | `domain/post/Post.java` | `PostRepository.java` |
| 댓글 | `domain/comment/Comment.java` | `CommentRepository.java` |
| 태그 | `domain/tag/Tag.java` | `TagRepository.java` |
| 게시글-태그 연결 | `domain/post/PostTag.java` | `PostTagRepository.java` |
| 게시글 임베딩 | `domain/embedding/PostEmbedding.java` | `PostEmbeddingRepository.java` |
| 게시글 청크 임베딩 | `domain/embedding/PostEmbeddingChunk.java` | `PostEmbeddingChunkRepository.java` |
| 임베딩 작업 | `domain/embedding/EmbeddingJob.java` | `EmbeddingJobRepository.java` |
| 읽음 기록 | `domain/read/PostRead.java` | `PostReadRepository.java` |

핵심 질문:

- Entity 하나가 테이블 하나와 어떻게 대응되는가
- `@ManyToOne`, `@OneToMany`는 어떤 관계를 표현하는가
- Repository method 이름만으로 쿼리가 만들어지는 예시는 무엇인가

더 자세한 테이블 설명은 `docs/database-schema.md`를 봅니다.

## 6단계: 게시판 CRUD 흐름 따라가기

가장 기본이 되는 흐름입니다. 이걸 이해하면 나머지 AI 기능도 붙일 위치가 보입니다.

### 게시글 목록 조회

```text
BoardPage.jsx
-> usePosts.js
-> postApi.js / fetchPosts
-> PostController.getPosts
-> PostService.getPosts
-> PostRepository
-> PostResponseMapper
```

봐야 할 것:

- 검색어, 카테고리, 태그, 페이지 번호가 어떻게 API query parameter가 되는가
- 서버가 `posts`, `totalPages`, `categoryCounts`를 어떻게 내려주는가
- 프론트의 페이지네이션이 서버 응답에 맞춰 어떻게 바뀌는가

### 게시글 생성

```text
PostForm.jsx
-> usePostComposer.js
-> App.jsx handleSubmitPost
-> usePosts.createPost
-> postApi.createPost
-> PostController.createPost
-> PostService.createPost
-> PostTagService
-> EmbeddingJobService
```

봐야 할 것:

- 게시글 저장과 태그 저장이 어떻게 분리되는가
- 글을 저장한 뒤 왜 바로 임베딩하지 않고 `embedding_jobs`에 작업을 예약하는가

### 게시글 수정/삭제

```text
PostDetailPage.jsx 또는 BoardPage.jsx
-> usePosts.updatePost / deletePost
-> PostController
-> PostService
```

봐야 할 것:

- 작성자만 수정/삭제할 수 있게 검증하는 위치
- 삭제가 실제 row 삭제인지, `DELETED` 상태 변경인지

## 7단계: 인증/JWT 흐름

인증은 프론트와 백엔드가 같이 봐야 합니다.

| 파일 | 역할 |
| --- | --- |
| `frontend/project-alpha/src/hooks/useAuth.js` | 로그인, 로그아웃, `/api/auth/me` 기반 사용자 복원 |
| `frontend/project-alpha/src/api/http/config.js` | 쿠키 인증 요청에 공통 `credentials: 'include'`, CSRF header, 401 refresh 재시도 적용 |
| `frontend/project-alpha/src/api/*.js` | cookie 인증과 CSRF header를 붙여 API 호출 |
| `backend/src/main/java/com/jungle_choi/namanmu/config/SecurityConfig.java` | 인증/인가 설정 |
| `backend/src/main/java/com/jungle_choi/namanmu/security/JwtTokenService.java` | JWT 생성/검증 |
| `backend/src/main/java/com/jungle_choi/namanmu/security/JwtAuthenticationFilter.java` | access token cookie에서 JWT 읽기 |
| `backend/src/main/java/com/jungle_choi/namanmu/security/LoginAttemptService.java` | 로그인 실패 횟수와 잠금 상태 관리 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java` | 회원가입/로그인 API |

핵심 질문:

- 로그인 성공 후 token은 어디에 저장되는가
- 새로고침 후에도 로그인 상태가 유지되는 이유는 무엇인가
- 백엔드는 어떤 filter에서 JWT를 읽고 인증 객체를 만드는가
- 비밀번호를 반복해서 틀리면 어느 단계에서 요청을 막는가
- access token이 만료된 일반 API 요청은 어떻게 복구되는가

## 8단계: RAG 구현 흐름

RAG는 retrieval과 generation을 나눠서 읽어야 합니다.

### 8-1. 임베딩 준비

```text
PostService.createPost / updatePost
-> EmbeddingJobService.enqueuePostEmbedding
-> embedding_jobs
-> EmbeddingJobScheduler
-> EmbeddingJobProcessor
-> PostChunkTextSplitter
-> PostEmbeddingTextBuilder
-> OpenAiEmbeddingClient
-> post_embeddings / post_embedding_chunks
```

핵심 질문:

- 게시글을 왜 청크로 나누는가
- `embedding_jobs`는 왜 필요한가
- 완성된 임베딩 결과와 작업 상태가 왜 다른 테이블인가

### 8-2. 유사 게시글 검색

```text
PostForm.jsx Related posts 버튼
-> useRagDraft.findRelatedPosts
-> ragApi.findSimilarPosts
-> AiController.findSimilarPosts
-> SimilarPostSearchService.searchSimilarPosts
```

핵심 질문:

- Vector similarity는 무엇과 무엇의 cosine similarity를 계산하는가
- BM25는 어떤 단어 매칭 신호를 보완하는가
- 카테고리, 태그, matched terms는 왜 metadata signal로 쓰는가

### 8-3. RAG 초안 생성

```text
PostForm.jsx Draft from sources 버튼
-> useRagDraft.createDraftFromSources
-> ragApi.createDraftFromSources
-> AiController.createDraft
-> RagDraftService.createDraft
-> OpenAiTextClient.generateText
```

핵심 질문:

- 검색된 글을 모두 프롬프트에 넣지 않는 이유는 무엇인가
- `RagDraftService`가 어떤 기준으로 초안 근거 source를 줄이는가
- LLM에게 "근거 없는 사실을 만들지 말라"고 지시하는 위치는 어디인가

## 9단계: MCP 구현 흐름

MCP는 "LLM/서비스가 외부 도구를 호출할 수 있게 하는 통로"로 이해하면 됩니다.

### JSON-RPC MCP endpoint

```text
McpController
-> McpServerService
-> WeatherApiClient / GitHubApiClient
```

봐야 할 것:

- tool list를 어떻게 정의하는가
- tool call 요청과 응답이 어떤 JSON 구조인가
- 외부 API key나 token은 어디서 읽는가

### 게시글 상세의 fact check

```text
PostDetailPage.jsx
-> mcpApi.checkFact
-> PostFactCheckController.checkFact
-> McpFactCheckService
-> GitHubFactCheckService 또는 WeatherFactCheckService
-> McpServerService
-> 외부 API
```

핵심 질문:

- 사용자가 쓴 글에서 GitHub 저장소나 날씨 주장을 어떻게 찾는가
- 외부 데이터와 게시글 내용을 비교한 결과는 어떤 필드로 내려오는가
- 왜 MCP 기능을 "글 생성"이 아니라 "이미 쓴 글의 검증"으로 분리했는가

## 10단계: Agent 추천 흐름

Agent는 단순 랜덤 추천이 아니라 상태를 보고 추천합니다.

```text
PostDetailPage.jsx에서 글 상세 조회
-> PostController.getPost
-> PostReadService.markRead
-> post_reads 저장

BoardPage.jsx Recommend 5 posts 버튼
-> useAgentRecommendations
-> agentApi.fetchMissedPosts
-> AgentController
-> AgentRecommendationService
-> post_reads, tags, posts 조회
```

핵심 질문:

- 사용자가 읽은 글은 언제 저장되는가
- 최근 읽은 글을 몇 개까지 추천 근거로 쓰는가
- 이미 읽은 글이 추천에서 빠지는 위치는 어디인가
- 추천 결과와 함께 reasoning step을 내려주는 이유는 무엇인가

## 11단계: 테스트와 평가 코드 읽기

기능을 고칠 때는 테스트와 평가 도구를 같이 봐야 합니다.

| 위치 | 역할 |
| --- | --- |
| `backend/src/test/java/...` | Spring service 단위 테스트 |
| `scripts/evaluate-rag-retrieval.mjs` | RAG retrieval 개선 전후 점수 비교 |
| `eval/ragas/run_project_alpha_ragas.py` | RAGAS 기반 생성 결과 평가 |
| `eval/ragas/cases.json` | RAGAS 평가 케이스 |

핵심 질문:

- 단위 테스트는 어떤 비즈니스 규칙을 보호하는가
- retrieval 평가는 어떤 기준으로 hit를 판단하는가
- RAGAS는 retrieval과 generation 중 어느 부분을 평가하는가

## 기능 수정할 때의 안전한 순서

새 기능이나 버그 수정을 할 때는 아래 순서가 가장 덜 헷갈립니다.

1. 화면에서 어떤 버튼/입력이 문제인지 찾기
2. 해당 컴포넌트 확인
3. 연결된 custom hook 확인
4. `src/api`의 HTTP 요청 함수 확인
5. Spring Controller endpoint 확인
6. Service에서 실제 규칙 확인
7. Repository/Entity에서 DB 구조 확인
8. 필요한 테스트 추가 또는 기존 테스트 실행
9. 브라우저에서 직접 시나리오 확인

## 자주 헷갈리는 개념 정리

| 개념 | 이 프로젝트에서의 의미 |
| --- | --- |
| Component | 화면 조각. 예: `PostCard`, `PostForm`, `FactCheckPanel` |
| Hook | 화면과 API 사이의 상태 관리 묶음. 예: `usePosts`, `useRagDraft` |
| API client | 프론트에서 백엔드로 `fetch` 요청을 보내는 함수 |
| Controller | 백엔드 HTTP 요청 입구 |
| Service | 실제 비즈니스 규칙이 있는 곳 |
| Repository | DB 조회/저장 입구 |
| Entity | DB 테이블을 Java 객체로 표현한 것 |
| DTO/Response | 프론트로 내려줄 JSON 모양 |
| Embedding | 텍스트 의미를 숫자 벡터로 바꾼 것 |
| Retrieval | 입력과 관련 있는 기존 글을 찾아오는 과정 |
| Reranking | 검색된 후보를 더 좋은 순서로 다시 정렬하는 과정 |
| MCP Tool | 외부 시스템을 호출하는 기능 단위 |
| Agent State | 추천 판단에 쓰는 사용자별 기록 |

## 발표 전에 설명할 수 있어야 하는 문장

- "React에서는 화면과 상태 관리를 분리하기 위해 custom hook을 만들었습니다."
- "Spring Boot에서는 Controller, Service, Repository 계층으로 나눠서 API 입구, 비즈니스 로직, DB 접근 책임을 분리했습니다."
- "RAG는 게시글을 임베딩해 두고, 사용자가 작성 중인 글과 유사한 글을 검색한 뒤, 직접 관련성이 높은 글만 초안 생성 근거로 사용합니다."
- "MCP는 외부 API를 도구처럼 호출하기 위한 통로로 구현했고, 현재는 GitHub와 날씨 데이터를 가져와 게시글 주장을 검증합니다."
- "Agent는 최근 읽은 글을 상태로 저장하고, 사용자가 아직 읽지 않은 글 중 관심사와 가까운 글 5개를 추천합니다."
