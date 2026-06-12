# Project Alpha Code Map

이 문서는 처음 프로젝트를 보는 사람이 파일 역할을 빠르게 잡기 위한 지도다.
기능을 고칠 때는 먼저 아래 흐름을 따라 읽고, 같은 책임의 파일 안에서 수정한다.

## 전체 흐름

```mermaid
flowchart LR
    Browser["React 화면"] --> ApiClient["frontend src/api"]
    ApiClient --> Controller["Spring api controller"]
    Controller --> Service["Spring service"]
    Service --> Repository["Spring Data JPA repository"]
    Repository --> MySQL["MySQL tables"]
    Service --> OpenAI["OpenAI API"]
    Service --> ExternalApi["External API through MCP"]
```

## Frontend

| 경로 | 역할 | 먼저 볼 때 |
| --- | --- | --- |
| `frontend/project-alpha/src/App.jsx` | 로그인 여부에 따라 라우팅하고, 게시판 작성/RAG 상태를 묶어 하위 페이지에 전달 | 화면 전체 흐름 |
| `frontend/project-alpha/src/pages/BoardPage.jsx` | 게시글 목록 화면을 조립 | 메인 화면 구조 |
| `frontend/project-alpha/src/pages/PostDetailPage.jsx` | 게시글 상세, 댓글, MCP 팩트체크 화면 | 상세 화면 구조 |
| `frontend/project-alpha/src/pages/LoginPage.jsx` | 로그인 폼 | 인증 화면 |
| `frontend/project-alpha/src/pages/SignupPage.jsx` | 회원가입 폼 | 인증 화면 |
| `frontend/project-alpha/src/hooks/useAuth.js` | 로그인, 회원가입, 로그아웃 상태 관리 | JWT가 프론트에 저장되는 위치 |
| `frontend/project-alpha/src/hooks/usePosts.js` | 게시글/댓글 API 호출 결과를 React 상태로 관리 | CRUD가 화면에 반영되는 방식 |
| `frontend/project-alpha/src/api/postApi.js` | 게시글/댓글 HTTP 요청 함수 | 백엔드 게시판 API 주소 |
| `frontend/project-alpha/src/api/authApi.js` | 로그인/회원가입 HTTP 요청 함수 | 백엔드 인증 API 주소 |
| `frontend/project-alpha/src/api/ragApi.js` | 유사 게시글 검색과 RAG 초안 생성 요청 | RAG 프론트 진입점 |
| `frontend/project-alpha/src/api/mcpApi.js` | MCP 팩트체크 요청 | MCP 프론트 진입점 |
| `frontend/project-alpha/src/components/PostForm.jsx` | 글 작성 폼과 RAG 버튼 | 작성 모달 내부 |
| `frontend/project-alpha/src/components/WeatherFactCheckPanel.jsx` | MCP 팩트체크 결과 표시 | 팩트체크 UI |
| `frontend/project-alpha/src/components/PostCard.jsx` | 목록의 게시글 카드 | 게시글 목록 아이템 |

## Backend

| 경로 | 역할 | 먼저 볼 때 |
| --- | --- | --- |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java` | 회원가입/로그인 API | JWT 발급 흐름 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/PostController.java` | 게시글/댓글 CRUD API | 게시판 API 진입점 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/AiController.java` | 유사 게시글 검색, RAG 초안, 임베딩 작업 API | RAG API 진입점 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/McpController.java` | JSON-RPC MCP endpoint | MCP 프로토콜 진입점 |
| `backend/src/main/java/com/jungle_choi/namanmu/api/PostFactCheckController.java` | 게시글 상세에서 사용하는 MCP 팩트체크 API | 외부 정보 검증 기능 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/post/Post.java` | 게시글 Entity | `posts` 테이블 구조 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/comment/Comment.java` | 댓글 Entity | `comments` 테이블 구조 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/tag/Tag.java` | 태그 Entity | `tags` 테이블 구조 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/embedding/PostEmbedding.java` | 게시글 임베딩 Entity | `post_embeddings` 테이블 구조 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/embedding/EmbeddingJob.java` | 임베딩 작업 Entity | `embedding_jobs` 테이블 구조 |
| `backend/src/main/java/com/jungle_choi/namanmu/domain/read/PostRead.java` | 읽음 기록 Entity | Agent 추천용 상태 데이터 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/EmbeddingJobService.java` | 게시글 임베딩 작업 예약 | 게시글 저장 후 RAG 준비 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/EmbeddingJobProcessor.java` | 임베딩 작업 처리 | OpenAI Embedding 호출 위치 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/SimilarPostSearchService.java` | 코사인 유사도로 비슷한 게시글 검색 | Retrieval 구현 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/RagDraftService.java` | 유사 게시글을 근거로 초안 생성 | RAG 생성 구현 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/McpServerService.java` | MCP 도구 목록/호출 처리 | MCP server 핵심 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/WeatherApiClient.java` | 외부 날씨 API 호출 | MCP 외부 연동 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/WeatherFactCheckService.java` | 날씨 도구 결과로 게시글 팩트체크 | MCP 응용 기능 |
| `backend/src/main/java/com/jungle_choi/namanmu/service/PostReadService.java` | 상세 조회 시 읽음 기록 저장 | Agent 데이터 수집 |

## 기능별 읽기 순서

### 게시글 생성

1. `PostForm.jsx`
2. `App.jsx`의 `handleSubmit`
3. `usePosts.js`의 `createPost`
4. `postApi.js`의 `createPost`
5. `PostController.createPost`
6. `Post.create`
7. `EmbeddingJobService.enqueuePostEmbedding`

### RAG 초안 생성

1. `PostForm.jsx`
2. `App.jsx`의 `handleCreateDraftFromSources`
3. `ragApi.js`의 `createDraftFromSources`
4. `AiController.createDraft`
5. `RagDraftService.createDraft`
6. `SimilarPostSearchService.searchSimilarPosts`
7. `OpenAiTextClient.generateText`

### MCP 날씨 팩트체크

1. `PostDetailPage.jsx`
2. `mcpApi.js`의 `checkWeatherFact`
3. `PostFactCheckController.checkWeatherFact`
4. `WeatherFactCheckService.check`
5. `McpServerService.callWeatherTool`
6. `WeatherApiClient.getCurrentForecast`

### Agent 읽음 기록

1. `PostDetailPage.jsx`
2. `postApi.js`의 `fetchPost`
3. `PostController.getPost`
4. `PostReadService.markRead`
5. `PostReadRepository.findByUserIdAndPostId`

## 현재 정리 필요 지점

- `App.jsx`는 라우팅과 작성/RAG 상태를 함께 들고 있어 점점 길어질 수 있다.
- `PostController.java`는 게시글, 댓글, 태그, 응답 변환 책임이 모여 있어 이후 `PostService`, `CommentController`, Mapper로 나누는 것이 좋다.
- `frontend/project-alpha/src/storage/postStorage.js`는 초기 로컬 저장 학습 단계의 흔적이다. 서버 DB 기준으로 완전히 전환하면 제거하거나 fallback 용도라고 명확히 표시한다.
