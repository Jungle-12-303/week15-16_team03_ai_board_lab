# study/AGENTS.md

이 폴더는 `/Users/juhoseok/Documents/게시판 만드릭` 프로젝트를 단순히 실행하는 곳이 아니라, 코드를 자기 코드로 다시 만들 수 있을 때까지 구조와 이유를 학습하는 공간이다.

AI는 이 폴더에서 답변하거나 파일을 만들 때 반드시 아래 원칙을 따른다.

## 1. 학습 목표

최종 목표는 코드를 복사해서 쓰는 것이 아니라, 다음 질문에 스스로 답할 수 있는 상태가 되는 것이다.

- 요청이 들어오면 왜 `Controller -> Service -> Repository -> DB` 순서로 흐르는가?
- 왜 게시글 CRUD와 RAG 인덱싱을 한 기능처럼 보이게 하되 장애 범위는 분리했는가?
- 왜 RAG는 JPA 대신 `JdbcTemplate`과 pgvector SQL을 직접 사용하는가?
- 왜 Agent는 완전 자율 실행기가 아니라 최대 3개의 제한된 도구 실행기로 설계되었는가?
- 왜 OpenAI API 키가 없어도 fallback 로직이 있어야 과제 데모가 안정적인가?
- 왜 각 함수 단위 테스트를 먼저 작성하면 전체 프로젝트를 다시 만들 수 있는가?

작업을 시작할 때는 아래 형태로 성공 기준을 정한다.

```txt
1. 학습할 함수 선택 -> 검증: 해당 함수의 정상/경계/실패 케이스 테스트 작성
2. 테스트 실행 -> 검증: 실패 원인을 설명할 수 있음
3. 필요한 최소 수정 -> 검증: 테스트 통과와 실행 흐름 설명 가능
```

## 2. 답변 형식

코드 생성, 코드 수정, 디버깅, 테스트 작성 요청에는 반드시 아래 형식을 사용한다.

### [1] 전체 구조 설명 (Top-down)

- 프로그램 또는 기능의 전체 흐름
- 실행 순서
- 각 주요 구성 요소의 역할

### [2] 설계 이유 (WHY 중심)

- 왜 이 아키텍처를 선택했는지
- 다른 방법과 비교해서 왜 이 방식이 현재 과제에 적절한지
- 어떤 복잡도를 줄이고 어떤 위험을 감수했는지

### [3] 핵심 코드 단위 설명

각 함수 또는 중요한 코드 블록마다 다음을 설명한다.

- 역할
- 필요한 이유
- 해결하는 문제
- 내부 실행 흐름
- 테스트해야 할 정상/경계/실패 케이스

### [4] 중요한 포인트 정리

- 핵심 개념
- 헷갈리기 쉬운 부분
- 실수하기 쉬운 부분
- 다음에 혼자 다시 짤 때 기억할 기준

## 4. 현재 프로젝트 전체 구조

이 프로젝트는 React + Spring Boot + PostgreSQL + pgvector 기반의 AI 지식 게시판 MVP이다.

```txt
사용자
 -> React UI
 -> Axios API Client
 -> Spring Boot Controller
 -> Service
 -> Repository 또는 JdbcTemplate
 -> PostgreSQL + pgvector
```

AI 기능은 게시판 기능 안에 섞지 않고 `ai` 패키지 아래로 분리되어 있다.

```txt
backend/src/main/java/com/example/aiknowledgeboard
├── auth      : 회원가입, 로그인, 현재 사용자 조회
├── config    : JWT, Security, CORS
├── user      : 사용자 엔티티와 저장소
├── post      : 게시글 CRUD, 검색, 태그 연결, RAG 인덱싱 트리거
├── comment   : 댓글 생성, 조회, 삭제
├── tag       : 태그 정규화, 중복 제거, 생성
├── common    : 공통 응답, 페이지 응답, 예외 처리
└── ai
    ├── common : OpenAI 호출, fallback, AI 로그
    ├── rag    : 임베딩 저장, 유사 게시글 검색, 요약
    ├── mcp    : JSON-RPC 스타일 GitHub API 도구 호출
    └── agent  : 작성 보조 도구 선택, 실행, memory/log 저장
```

프론트엔드는 MVP 속도를 위해 `frontend/src/App.jsx`에 주요 화면과 상태가 모여 있다.

```txt
App
 -> AuthPanel
 -> PostList
 -> PostDetail
 -> PostEditor
 -> AiResult
 -> TagList
```

API 호출은 `frontend/src/api/client.js`에 모여 있다. 이 구조는 화면 컴포넌트가 Axios 세부 설정을 몰라도 되게 만든다.

## 5. 기능별 실행 흐름

### 5.1 회원가입/로그인

```txt
AuthController
 -> AuthService
 -> UserRepository
 -> PasswordEncoder
 -> JwtTokenProvider
 -> AuthResponse
```

왜 이 구조인가:

- Controller는 HTTP 요청/응답만 담당해야 한다.
- Service는 이메일 정규화, 중복 검사, 비밀번호 검증, 토큰 발급 같은 인증 규칙을 담당한다.
- JWT 발급을 별도 클래스로 빼면 인증 정책을 테스트하기 쉽다.

우선 테스트할 함수:

- `AuthService.signup`
- `AuthService.login`
- `JwtTokenProvider.createToken`
- `JwtTokenProvider.parseUserId`

### 5.2 게시글 목록/작성/수정/삭제

```txt
PostController
 -> PostService
 -> CurrentUserService
 -> TagService
 -> PostRepository
 -> RagService.indexPost
```

왜 이 구조인가:

- 게시글 저장과 태그 저장은 같은 사용자 행동에 속하므로 `PostService`에서 묶는다.
- RAG 인덱싱은 게시글 작성 후 부가적으로 실행한다.
- `indexPostSafely`가 예외를 삼키는 이유는 AI/벡터 DB 실패가 기본 게시판 저장을 막으면 안 되기 때문이다.

우선 테스트할 함수:

- `PostService.list`
- `PostService.create`
- `PostService.update`
- `PostService.delete`
- `TagService.normalize`
- `TagService.getOrCreateTags`

### 5.3 댓글

```txt
CommentController
 -> CommentService
 -> PostRepository
 -> CommentRepository
```

왜 이 구조인가:

- 댓글은 반드시 게시글에 속하므로 생성 전에 게시글 존재 여부를 확인한다.
- 삭제는 작성자만 가능하므로 `CurrentUserService`와 소유자 검증이 필요하다.

우선 테스트할 함수:

- `CommentService.create`
- `CommentService.findByPost`
- `CommentService.delete`

### 5.4 RAG

```txt
게시글 작성/수정
 -> RagService.indexPost
 -> OpenAiClient.createEmbedding
 -> OpenAiClient.toVectorLiteral
 -> post_embeddings upsert

유사 글 검색
 -> RagService.findSimilarPosts
 -> pgvector cosine distance SQL
 -> RagService.summarize
 -> RagResponse
```

왜 이 구조인가:

- `post_embeddings`를 `posts`와 분리하면 게시글 CRUD와 AI 검색 데이터를 따로 관리할 수 있다.
- pgvector의 `<=>` 연산자는 JPA 메서드 이름으로 표현하기 어렵기 때문에 `JdbcTemplate`이 더 명확하다.
- OpenAI 키가 없을 때 fallback embedding/chat을 사용하면 데모와 테스트가 외부 API에 의존하지 않는다.

우선 테스트할 함수:

- `OpenAiClient.createEmbedding`
- `OpenAiClient.toVectorLiteral`
- `RagService.indexPost`
- `RagService.findSimilarPosts`
- `RagService.findSimilarAndSummarize`

### 5.5 MCP

```txt
McpController
 -> McpService.call
 -> JSON-RPC 검증
 -> github.getUser 또는 github.getRepo
 -> McpToolLog 저장
 -> JsonRpcResponse
```

왜 이 구조인가:

- 과제의 MCP 요구사항을 최소 범위에서 보여주기 위해 JSON-RPC 스타일 요청/응답을 사용한다.
- 외부 도구 호출은 실패할 수 있으므로 성공/실패 로그를 별도로 남긴다.
- 지원 method를 switch로 제한해 예측 가능한 도구 실행만 허용한다.

우선 테스트할 함수:

- `McpService.call`
- `McpService.saveLog`
- `JsonRpcResponse.success`
- `JsonRpcResponse.error`

### 5.6 Agent

```txt
AgentController
 -> AgentService.help
 -> chooseTools
 -> recommend_tags
 -> search_similar_posts
 -> call_mcp_external_api 또는 summarize_draft
 -> buildFinalMessage
 -> agent_memory 저장
 -> ai_logs 저장
```

왜 이 구조인가:

- Agent를 완전 자율 시스템으로 만들면 디버깅과 발표가 어려워진다.
- 이 프로젝트의 Agent는 "도구 선택기 + 결과 조합기"로 제한해서 동작을 설명하기 쉽게 만든다.
- 최대 3회 실행 제한은 무한 반복과 외부 API 남용을 막는다.

우선 테스트할 함수:

- `AgentService.help`
- `chooseTools`
- `recommendTags`
- `extractGithubUsername`
- `buildFinalMessage`

주의:

- 현재 `chooseTools`, `recommendTags`, `extractGithubUsername`, `buildFinalMessage`는 private이다.
- 처음부터 private 메서드를 억지로 테스트하지 않는다.
- public `help` 테스트로 동작을 검증하거나, 중복 테스트가 많아질 때만 별도 컴포넌트로 분리한다.

## 6. DB와 자료구조 이해 포인트

### 6.1 `Set<Tag>`와 `LinkedHashSet`

태그는 중복이 없어야 하고, 사용자가 입력한 순서도 어느 정도 유지되어야 한다. 그래서 `TagService.normalize`와 `Post.tags`는 `LinkedHashSet`을 사용한다.

테스트 포인트:

- `#React`, `react`, ` React `가 하나의 `react`로 정규화되는가?
- 빈 문자열과 null은 제거되는가?
- 5개를 넘으면 잘리는가?
- 입력 순서가 유지되는가?

### 6.2 `PageResponse<T>`

프론트엔드는 페이지 번호, 전체 페이지 수, 콘텐츠 목록이 필요하다. Spring의 `Page`를 그대로 노출하지 않고 `PageResponse`로 감싸면 API 응답 구조를 프론트에 맞게 고정할 수 있다.

테스트 포인트:

- content가 그대로 매핑되는가?
- page, size, totalElements, totalPages가 올바른가?

### 6.3 `post_embeddings`

게시글 본문과 벡터를 분리 저장한다. 게시글 기능은 관계형 데이터로 단순하게 유지하고, 벡터 검색만 별도 테이블에서 처리하기 위해서다.

테스트 포인트:

- 같은 post_id로 다시 index하면 insert가 아니라 update 되는가?
- source_text가 `title + "\n" + content` 형태인가?
- embedding vector literal이 pgvector 형식 `[0.1,0.2,...]`인가?

### 6.4 `ai_logs`, `agent_memory`, `mcp_tool_logs`

AI 기능은 결과 품질보다 실행 흐름을 설명하는 것이 중요하다. 그래서 실행 기록을 남겨 장애 원인과 데모 흐름을 추적한다.

테스트 포인트:

- 성공 시 success/status가 기록되는가?
- 실패 시 errorMessage가 잘리는가?
- `REQUIRES_NEW` 로그 트랜잭션이 본 작업 실패와 분리되는가?

## 7. 함수 단위 테스트 학습 계획

테스트는 무작정 많이 쓰지 않는다. 각 함수마다 아래 순서로 작성한다.

```txt
1. 이 함수가 받는 입력을 정리한다.
2. 이 함수가 반드시 지켜야 할 규칙을 한 문장으로 쓴다.
3. 정상 케이스를 테스트한다.
4. 경계 케이스를 테스트한다.
5. 실패 케이스를 테스트한다.
6. 테스트 이름만 보고도 요구사항이 읽히게 만든다.
```

테스트 이름 규칙:

```txt
함수명_상황_기대결과
```

예시:

```txt
normalize_중복태그와해시가들어오면_소문자고유태그만반환한다
login_비밀번호가틀리면_예외를던진다
parseUserId_서명이변조되면_토큰검증에실패한다
findSimilarPosts_DB조회가실패하면_빈목록을반환한다
```

## 8. 추천 학습 순서

### 1단계: 순수 로직

먼저 DB, Security, 외부 API가 없어도 이해 가능한 함수를 테스트한다.

- `TagService.normalize`
- `OpenAiClient.toVectorLiteral`
- `OpenAiClient.createEmbedding`의 fallback 결과
- `AiLogService.trim`은 private이므로 public `log`에서 간접 검증
- 프론트 `mergeTags`
- 프론트 `getErrorMessage`

왜 먼저 하는가:

- 입력과 출력이 명확하다.
- 테스트 실패 원인을 빠르게 이해할 수 있다.
- 프로젝트 자신감을 만들기 좋다.

### 2단계: 인증과 권한

- `AuthService.signup`
- `AuthService.login`
- `JwtTokenProvider.createToken`
- `JwtTokenProvider.parseUserId`
- `CurrentUserService.getCurrentUser`

왜 두 번째인가:

- 게시글/댓글 작성은 로그인 사용자에 의존한다.
- 권한 검증을 이해해야 `PostService.update/delete`, `CommentService.delete`가 이해된다.

### 3단계: 게시판 도메인

- `PostService.list`
- `PostService.create`
- `PostService.update`
- `PostService.delete`
- `CommentService.create`
- `CommentService.delete`

왜 세 번째인가:

- 게시판의 핵심 기능이다.
- Controller, Service, Repository가 어떻게 역할을 나누는지 가장 잘 보인다.

### 4단계: AI 최소 기능

- `RagService.indexPost`
- `RagService.findSimilarPosts`
- `McpService.call`
- `AgentService.help`

왜 네 번째인가:

- 외부 API, DB SQL, fallback, 로그 저장이 섞여 있어 난도가 높다.
- 앞 단계에서 서비스 테스트 감각을 만든 뒤 접근해야 이해가 빠르다.

### 5단계: 통합 흐름

- 회원가입 후 로그인
- 게시글 작성
- 게시글 목록 조회
- 댓글 작성
- RAG 유사 글 검색
- MCP GitHub 사용자 조회
- Agent 작성 보조 실행
- Docker Compose 실행

왜 마지막인가:

- 통합 테스트는 실패 원인이 넓다.
- 함수 단위 이해 없이 통합부터 보면 어디서 깨졌는지 찾기 어렵다.

## 9. 테스트 작성 기준

### 9.1 Backend

현재 백엔드는 JUnit 5와 Spring Boot Test를 사용한다. `spring-boot-starter-test` 안에 AssertJ와 Mockito가 포함되어 있으므로, 추가 의존성 없이 시작한다.

권장 방식:

- 순수 로직: 일반 JUnit 테스트
- Service: Mockito로 Repository/의존 서비스 mock
- Repository/SQL: 필요할 때만 통합 테스트
- Controller: 필요할 때만 `MockMvc`

처음부터 모든 테스트를 Spring Context로 띄우지 않는다. 함수 단위 학습에는 빠른 단위 테스트가 더 적합하다.

### 9.2 Frontend

현재 프론트엔드에는 테스트 도구가 없다. 따라서 처음부터 Vitest를 추가하지 않는다.

우선순위:

- `mergeTags`, `getErrorMessage`처럼 순수 함수부터 이해한다.
- 실제 테스트 도구 추가는 프론트 테스트가 명확히 필요해질 때만 한다.
- 컴포넌트 분리는 학습 목적이 분명할 때만 한다.

### 9.3 외부 API

OpenAI와 GitHub는 네트워크 상태와 키에 따라 결과가 달라진다. 테스트에서는 실제 API를 직접 호출하지 않는 것을 기본으로 한다.

- OpenAI: fallback 경로를 우선 검증한다.
- GitHub: `McpService.call`에서 `RestClient` 호출 부분은 mock 또는 별도 wrapper가 필요할 때만 분리한다.
- Agent: 도구 선택과 결과 조합을 중심으로 검증한다.

## 10. 혼자 다시 구현하는 순서

프로젝트를 처음부터 다시 만든다고 가정하면 아래 순서로 구현한다.

```txt
1. DB 테이블 설계: users, posts, comments, tags, post_tags
2. Spring Boot 기본 설정: JPA, Security, Validation, Flyway
3. User/Auth: 회원가입, 로그인, JWT
4. Post/Tag: 게시글 CRUD, 검색, 태그 정규화
5. Comment: 댓글 작성/삭제
6. Common: 에러 응답, 페이지 응답
7. Frontend API client: Axios baseURL, token interceptor
8. Frontend UI: list/detail/edit/auth 상태 흐름
9. RAG: post_embeddings, fallback embedding, 유사 글 검색
10. MCP: JSON-RPC request/response, GitHub 도구 호출
11. Agent: 도구 선택, 실행 제한, memory/log 저장
12. DevOps: Dockerfile, docker-compose, Nginx, troubleshooting 문서
```

이 순서를 따르는 이유:

- 인증 없는 게시글은 소유자 검증을 설명하기 어렵다.
- 게시글 없는 RAG는 검색할 데이터가 없다.
- MCP 없는 Agent는 외부 도구 호출 예시가 약하다.
- Docker는 코드가 동작한 뒤 전체 실행 환경을 고정하는 단계다.

## 11. AI에게 요청할 때 권장 프롬프트

### 함수 학습

```txt
TagService.normalize를 내가 직접 테스트할 수 있게,
정상/경계/실패 케이스를 왜 필요한지 중심으로 설명해줘.
아직 코드는 작성하지 말고 테스트 케이스 목록만 줘.
```

### 테스트 작성

```txt
AuthService.login 테스트를 JUnit으로 작성해줘.
단, 각 테스트가 어떤 요구사항을 검증하는지 WHY 중심으로 설명해줘.
```

### 디버깅

```txt
이 테스트가 왜 실패하는지 먼저 원인과 실행 흐름을 설명하고,
그 다음 최소 수정 코드를 제안해줘.
```

### 재구현 연습

```txt
PostService.create를 내가 처음부터 다시 짠다고 생각하고,
필요한 의존성, 실행 순서, 테스트 케이스를 먼저 설계해줘.
```

## 12. 절대 금지

- 코드만 제공하고 설명을 생략하지 않는다.
- "이 함수는 ~한다" 수준의 얕은 설명으로 끝내지 않는다.
- WHY 없이 WHAT만 설명하지 않는다.
- 테스트 없이 큰 구조를 리팩터링하지 않는다.
- 외부 API 호출 결과에 의존하는 불안정한 테스트를 기본값으로 만들지 않는다.
- MVP 범위를 넘어서는 기능을 임의로 추가하지 않는다.

