# Project Alpha Database Schema

이 문서는 Project Alpha 게시판의 기능별 테이블 설계도를 정리한다.
현재 기준은 Spring Boot JPA Entity와 Flyway 초기 migration이다. 초기 스키마는 [V1__create_project_alpha_schema.sql](../backend/src/main/resources/db/migration/V1__create_project_alpha_schema.sql)에 기록했다. 로컬 MySQL 실행 시에는 기본값인 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`도 병행해 개발 편의성을 유지한다. 운영 배포에서는 `validate`와 Flyway migration을 기준으로 스키마 변경 이력을 관리해야 한다.

## 전체 관계도

```mermaid
erDiagram
    users ||--o{ posts : writes
    users ||--o{ comments : writes
    users ||--o{ post_reads : reads
    users ||--o{ refresh_tokens : owns
    posts ||--o{ comments : has
    posts ||--o{ post_tags : has
    posts ||--o| post_embeddings : has
    posts ||--o{ embedding_jobs : queues
    posts ||--o{ post_reads : read_by
    tags ||--o{ post_tags : attached

    users {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
        varchar role
        datetime created_at
        datetime updated_at
    }

    posts {
        bigint id PK
        bigint author_id FK
        varchar category
        varchar title
        text content
        varchar status
        int view_count
        datetime created_at
        datetime updated_at
    }

    comments {
        bigint id PK
        bigint post_id FK
        bigint author_id FK
        text content
        datetime created_at
        datetime updated_at
    }

    tags {
        bigint id PK
        varchar name UK
        datetime created_at
    }

    post_tags {
        bigint id PK
        bigint post_id FK
        bigint tag_id FK
        datetime created_at
    }

    post_embeddings {
        bigint id PK
        bigint post_id FK
        varchar embedding_model
        int dimensions
        longtext embedding_json
        varchar source_hash
        datetime created_at
        datetime updated_at
    }

    embedding_jobs {
        bigint id PK
        bigint post_id FK
        varchar status
        int attempt_count
        varchar error_message
        datetime started_at
        datetime completed_at
        datetime created_at
        datetime updated_at
    }

    post_reads {
        bigint id PK
        bigint user_id FK
        bigint post_id FK
        datetime read_at
        datetime created_at
    }

    refresh_tokens {
        bigint id PK
        bigint user_id FK
        varchar token_hash UK
        datetime expires_at
        datetime revoked_at
        datetime created_at
        datetime updated_at
    }
```

## 기능별 테이블

| 기능 | 테이블 | Entity | 역할 |
| --- | --- | --- | --- |
| 회원가입/로그인 | `users` | `User` | 사용자 계정, 비밀번호 해시, 권한 저장 |
| Refresh token rotation | `refresh_tokens` | `RefreshToken` | refresh token 해시, 만료, 폐기 상태 저장 |
| 게시글 CRUD | `posts` | `Post` | 게시글 제목, 본문, 카테고리, 상태, 조회수 저장 |
| 댓글 | `comments` | `Comment` | 게시글별 댓글과 댓글 작성자 저장 |
| 태그 | `tags` | `Tag` | 태그 이름 저장 |
| 게시글-태그 연결 | `post_tags` | `PostTag` | 게시글과 태그의 다대다 관계 연결 |
| RAG 유사 게시글 | `post_embeddings` | `PostEmbedding` | 게시글별 임베딩 벡터와 원본 해시 저장 |
| RAG 임베딩 작업 큐 | `embedding_jobs` | `EmbeddingJob` | 게시글 임베딩 생성 작업의 상태와 실패 기록 저장 |
| Agent 읽음 기록 | `post_reads` | `PostRead` | 사용자별 게시글 읽음 여부와 마지막 읽은 시간 저장 |

## 테이블 상세

### users

회원가입과 로그인을 위한 사용자 테이블이다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 사용자 식별자 |
| `email` | `VARCHAR(100)` | NOT NULL, UNIQUE | 로그인 이메일 |
| `password_hash` | `VARCHAR(255)` | NOT NULL | 암호화된 비밀번호 |
| `name` | `VARCHAR(30)` | NOT NULL | 화면에 표시할 사용자 이름 |
| `role` | `VARCHAR(20)` | NOT NULL | `USER`, `ADMIN` |
| `created_at` | `DATETIME` | NOT NULL | 가입 시간 |
| `updated_at` | `DATETIME` | NOT NULL | 수정 시간 |

### refresh_tokens

access token 재발급을 위한 refresh token 저장 테이블이다. 브라우저에는 refresh token 원문을 httpOnly cookie로 내려주고, DB에는 SHA-256 해시만 저장한다. 재발급이 일어나면 기존 row는 `revoked_at`으로 폐기하고 새 refresh token row를 만든다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | refresh token 식별자 |
| `user_id` | `BIGINT` | FK, NOT NULL | token 소유 사용자. `users.id` 참조 |
| `token_hash` | `VARCHAR(64)` | NOT NULL, UNIQUE | refresh token 원문의 SHA-256 해시 |
| `expires_at` | `DATETIME` | NOT NULL | refresh token 만료 시각 |
| `revoked_at` | `DATETIME` | NULL | rotation 또는 logout으로 폐기된 시각 |
| `created_at` | `DATETIME` | NOT NULL | 생성 시각 |
| `updated_at` | `DATETIME` | NOT NULL | 수정 시각 |

### posts

게시글 목록, 상세, 검색의 중심 테이블이다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 게시글 식별자 |
| `author_id` | `BIGINT` | FK, NOT NULL | 작성자. `users.id` 참조 |
| `category` | `VARCHAR(30)` | NOT NULL | `Learning`, `Project`, `Daily` 같은 분류 |
| `title` | `VARCHAR(150)` | NOT NULL | 게시글 제목 |
| `content` | `TEXT` | NOT NULL | 게시글 본문 |
| `status` | `VARCHAR(20)` | NOT NULL | `PUBLISHED`, `HIDDEN`, `DELETED` |
| `view_count` | `INT` | NOT NULL | 조회수 |
| `created_at` | `DATETIME` | NOT NULL | 작성 시간 |
| `updated_at` | `DATETIME` | NOT NULL | 수정 시간 |

### comments

댓글 기능을 위한 테이블이다. 댓글은 게시글과 작성자를 모두 참조한다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 댓글 식별자 |
| `post_id` | `BIGINT` | FK, NOT NULL | 댓글이 달린 게시글. `posts.id` 참조 |
| `author_id` | `BIGINT` | FK, NOT NULL | 댓글 작성자. `users.id` 참조 |
| `content` | `TEXT` | NOT NULL | 댓글 내용 |
| `created_at` | `DATETIME` | NOT NULL | 작성 시간 |
| `updated_at` | `DATETIME` | NOT NULL | 수정 시간 |

### tags

태그 이름을 중복 없이 관리하는 테이블이다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 태그 식별자 |
| `name` | `VARCHAR(50)` | NOT NULL, UNIQUE | 태그 이름 |
| `created_at` | `DATETIME` | 생성 시 입력 | 태그 생성 시간 |

### post_tags

게시글과 태그의 다대다 관계를 연결하는 중간 테이블이다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 연결 식별자 |
| `post_id` | `BIGINT` | FK, NOT NULL | 게시글. `posts.id` 참조 |
| `tag_id` | `BIGINT` | FK, NOT NULL | 태그. `tags.id` 참조 |
| `created_at` | `DATETIME` | 생성 시 입력 | 태그 연결 시간 |

`post_id`, `tag_id` 조합에는 unique 제약이 있다. 같은 게시글에 같은 태그가 중복으로 붙지 않게 하기 위해서다.

### post_embeddings

RAG 유사 게시글 검색을 위한 게시글 임베딩 저장 테이블이다.
초기 구현에서는 MySQL에 임베딩을 JSON 문자열로 저장하고, 서버 코드에서 cosine similarity를 계산한다.
데이터가 커지면 Chroma, OpenSearch, pgvector 같은 전용 Vector DB로 교체할 수 있다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 임베딩 식별자 |
| `post_id` | `BIGINT` | FK, UNIQUE, NOT NULL | 임베딩 대상 게시글. `posts.id` 참조 |
| `embedding_model` | `VARCHAR(100)` | NOT NULL | 임베딩 생성에 사용한 모델명 |
| `dimensions` | `INT` | NOT NULL | 임베딩 벡터 차원 수 |
| `embedding_json` | `LONGTEXT` | NOT NULL | 벡터 값을 JSON 배열 문자열로 저장 |
| `source_hash` | `VARCHAR(64)` | NOT NULL | 임베딩 입력 문자열의 SHA-256 해시. 재생성 필요 여부 판단용 |
| `created_at` | `DATETIME` | NOT NULL | 최초 생성 시간 |
| `updated_at` | `DATETIME` | NOT NULL | 마지막 갱신 시간 |

`post_id`는 unique 제약을 둔다. 게시글 하나에는 현재 기준의 최신 임베딩 하나만 연결하기 위해서다.

### embedding_jobs

게시글 저장과 OpenAI Embedding API 호출을 분리하기 위한 작업 큐 테이블이다.
게시글 생성/수정 트랜잭션에서는 이 테이블에 `PENDING` 작업만 예약하고, 실제 외부 API 호출은 별도 수동 실행 API 또는 Scheduler가 처리한다.
현재 구현은 게시글 생성/수정이 완료될 때 `EmbeddingJobService`가 중복 `PENDING` 작업을 확인한 뒤 새 작업을 예약한다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 임베딩 작업 식별자 |
| `post_id` | `BIGINT` | FK, NOT NULL | 임베딩을 생성할 게시글. `posts.id` 참조 |
| `status` | `VARCHAR(20)` | NOT NULL | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` |
| `attempt_count` | `INT` | NOT NULL | 처리 시도 횟수 |
| `error_message` | `VARCHAR(1000)` | NULL | 마지막 실패 사유 |
| `started_at` | `DATETIME` | NULL | 마지막 처리 시작 시간 |
| `completed_at` | `DATETIME` | NULL | 성공 또는 실패로 처리 종료된 시간 |
| `created_at` | `DATETIME` | NOT NULL | 작업 예약 시간 |
| `updated_at` | `DATETIME` | NOT NULL | 작업 상태 갱신 시간 |

이 테이블은 완성된 벡터를 저장하지 않는다. 완성된 결과는 `post_embeddings`에 저장하고, `embedding_jobs`는 처리해야 할 일과 처리 상태만 관리한다.

### post_reads

Agent의 놓친 글 추천 기능을 위한 읽음 기록 테이블이다.
사용자가 게시글 상세 페이지를 열면 `GET /api/posts/{postId}` 요청에서 JWT 사용자 정보를 확인하고 이 테이블에 읽음 기록을 저장한다.

| 컬럼 | 타입 | 제약 | 설명 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | PK, AUTO_INCREMENT | 읽음 기록 식별자 |
| `user_id` | `BIGINT` | FK, NOT NULL | 글을 읽은 사용자. `users.id` 참조 |
| `post_id` | `BIGINT` | FK, NOT NULL | 읽은 게시글. `posts.id` 참조 |
| `read_at` | `DATETIME` | NOT NULL | 마지막으로 읽은 시간 |
| `created_at` | `DATETIME` | NOT NULL | 최초 읽음 기록 생성 시간 |

`user_id`, `post_id` 조합에는 unique 제약을 둔다. 같은 사용자가 같은 글을 여러 번 읽으면 새 행을 만들지 않고 `read_at`만 갱신한다.

## 현재 API 연결 상태

| API | 현재 상태 |
| --- | --- |
| `POST /api/auth/signup` | `users` 테이블에 사용자 저장, 비밀번호는 해시로 저장 |
| `POST /api/auth/login` | `users` 조회 후 access token cookie와 refresh token cookie 발급 |
| `POST /api/auth/refresh` | `refresh_tokens`의 해시를 검증하고 기존 refresh token을 폐기한 뒤 새 token 발급 |
| `POST /api/auth/logout` | 현재 refresh token을 폐기하고 auth cookie 삭제 |
| `POST /api/auth/logout-all` | 현재 사용자의 활성 refresh token을 모두 폐기하고 auth cookie 삭제 |
| `GET /api/posts` | `posts`, `post_tags`, `comments`를 조회해 목록 응답 생성 |
| `GET /api/posts/{postId}` | 게시글 상세 조회 후 `post_reads`에 읽음 기록 저장 |
| `POST /api/posts` | `posts` 저장, `post_tags` 갱신, `embedding_jobs` 예약 |
| `PATCH /api/posts/{postId}` | 작성자 검증 후 게시글 수정, 태그 갱신, 임베딩 재생성 예약 |
| `DELETE /api/posts/{postId}` | 작성자 검증 후 게시글을 `DELETED` 상태로 변경 |
| `POST /api/posts/{postId}/comments` | `comments` 테이블에 댓글 저장 |
| `DELETE /api/posts/{postId}/comments/{commentId}` | 댓글 작성자 검증 후 댓글 삭제 |
| `POST /api/ai/similar-posts` | 작성 중인 글을 임베딩하고 `post_embeddings`에서 유사 게시글 검색 |
| `POST /api/ai/draft` | 유사 게시글을 근거로 RAG 초안 생성 |
| `POST /api/ai/embedding-jobs/process` | `embedding_jobs`의 대기 작업을 처리해 `post_embeddings` 갱신 |
| `POST /api/posts/{postId}/fact-check/weather` | MCP 날씨 도구로 외부 정보를 조회하고 게시글 팩트체크 |

게시글 생성/수정은 게시글 저장과 임베딩 생성을 한 요청에서 모두 처리하지 않는다.
게시글 트랜잭션에서는 `embedding_jobs`에 작업만 예약하고, 실제 OpenAI Embedding API 호출은 별도 처리 API가 담당한다.

## 남은 확장 후보

AI 기능과 개인화 기능 중 일부는 아직 테이블로 만들지 않았다. 구현 시점에 아래 후보를 추가 검토한다.

| 기능 | 후보 테이블 | 목적 |
| --- | --- | --- |
| AI 초안 생성 기록 | `ai_draft_logs` | 입력 초안, 참조 게시글, 생성 결과, 모델명 저장 |
| MCP 날씨 브리핑 | `weather_briefing_logs` | 지역, 날씨 원본 데이터, 생성된 브리핑 저장 |
| 놓친 글 추천 Agent | `user_interests` | 사용자 선호 태그 저장 |

## 유지보수 규칙

- 기능 하나를 추가할 때 먼저 어떤 테이블이 필요한지 이 문서에 기록한다.
- Entity 필드명을 바꾸면 DB 컬럼명도 함께 바뀔 수 있으므로 API와 프론트 응답을 같이 확인한다.
- 로컬 개발에서는 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`를 병행하지만, 배포 단계에서는 `validate`와 Flyway migration 파일을 기준으로 스키마를 변경한다.
- 비밀번호, DB 주소, API key는 `.env` 또는 배포 환경변수로 관리하고 Git에 커밋하지 않는다.
