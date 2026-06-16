# 1장. 시스템 전체 구조

Project Alpha는 게시판 화면, API 서버, 데이터베이스, 외부 AI/API 호출로 나뉩니다.

```text
Browser
-> React / Vite
-> Spring Boot REST API
-> MySQL
-> OpenAI / GitHub / Weather API
```

## 왜 실행 프로세스가 나뉘는가

로컬 개발에서는 보통 세 개가 동시에 실행됩니다.

| 실행 대상 | 역할 | 대표 명령 |
| --- | --- | --- |
| MySQL | 데이터를 영구 저장 | `docker compose up -d mysql` |
| Spring Boot | 인증, 게시판 API, AI 기능 실행 | `.\gradlew.bat bootRun` |
| Vite | React 개발 서버 | `npm run dev` |

React는 브라우저 화면을 담당하고, Spring Boot는 인증, DB 트랜잭션, 외부 API 호출을 담당합니다. MySQL은 사용자, 게시글, 임베딩, 읽음 기록을 영구 저장합니다.

## 요청 흐름

예를 들어 사용자가 게시글을 작성하면 흐름은 이렇게 이어집니다.

```text
PostForm.jsx
-> usePostComposer.js
-> usePosts.js
-> postApi.js
-> PostController
-> PostService
-> PostRepository / PostTagService / EmbeddingJobService
-> MySQL
```

중요한 점은 React가 DB에 직접 접근하지 않는다는 것입니다. React는 HTTP 요청만 보내고, DB를 다루는 책임은 Spring Boot가 가집니다.

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| React 앱 시작점 | [main.jsx](../../frontend/project-alpha/src/main.jsx), [App.jsx](../../frontend/project-alpha/src/App.jsx) |
| 게시판 메인 화면 | [BoardPage.jsx](../../frontend/project-alpha/src/pages/BoardPage.jsx) |
| 글 작성 모달 | [ComposerModal.jsx](../../frontend/project-alpha/src/components/ComposerModal.jsx), [PostForm.jsx](../../frontend/project-alpha/src/components/PostForm.jsx) |
| 프론트 게시글 상태 | [usePosts.js](../../frontend/project-alpha/src/hooks/usePosts.js), [usePostComposer.js](../../frontend/project-alpha/src/hooks/usePostComposer.js) |
| 프론트 API 요청 | [postApi.js](../../frontend/project-alpha/src/api/postApi.js) |
| 백엔드 게시글 API | [PostController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/PostController.java) |
| 게시글 저장 규칙 | [PostService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/PostService.java), [PostTagService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/PostTagService.java) |
| 임베딩 작업 예약 | [EmbeddingJobService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/EmbeddingJobService.java) |
| 로컬 실행 구성 | [docker-compose.yml](../../docker-compose.yml), [application.properties](../../backend/src/main/resources/application.properties) |

## 기술 선택과 적용 범위

| 영역 | 선택한 방식 | 맡은 역할 | 적용 범위 |
| --- | --- | --- | --- |
| Frontend | React + Vite | 화면 구성, 라우팅, 사용자 입력 상태 | 서버 상태 캐싱 전용 도구는 쓰지 않음 |
| Backend | Spring Boot 3.5 + Java 25 | REST API, 인증, 트랜잭션, 외부 API 호출 | 단일 애플리케이션 구조 |
| Database | MySQL 8.4 | 게시판 원본 데이터와 작업 상태 저장 | Flyway migration으로 초기 스키마 관리 |
| Auth | Spring Security + JWT | 로그인 상태 검증과 API 보호 | httpOnly cookie, CSRF, refresh token rotation 적용 |
| AI | OpenAI API | 임베딩 생성과 초안 생성 | 비용, rate limit, 외부 장애 영향 |
| Vector DB | Qdrant | 게시글/청크 벡터 후보 검색 | MySQL 원본 조회와 함께 사용 |
| MCP | Spring 내부 JSON-RPC endpoint | `tools/list`, `tools/call` 처리와 외부 API 호출 | 별도 MCP 프로세스 배포는 없음 |

## 이 구조의 학습 포인트

이 구조에서 읽어야 할 핵심은 웹 서비스 기본 흐름과 AI 기능이 붙는 위치입니다.

- 게시판 기본 기능으로 웹 서비스의 뼈대를 배웁니다.
- RAG로 LLM이 내부 데이터를 사용하는 방식을 배웁니다.
- MCP로 외부 API를 도구처럼 호출하는 방식을 배웁니다.
- Agent로 사용자 상태를 기반으로 추천하는 흐름을 배웁니다.

AWS Bedrock, OpenSearch, Pinecone, Redis, message queue는 배포 규모와 운영 요구가 커질 때 검토할 수 있습니다. 이 프로젝트는 게시판 API, RAG 검색, MCP 도구 호출, Agent 추천 흐름을 코드 안에서 직접 따라갈 수 있도록 Spring Boot와 MySQL 중심으로 구성했습니다.
