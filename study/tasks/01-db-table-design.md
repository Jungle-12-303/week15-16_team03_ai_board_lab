# 01. DB 테이블 설계: users, posts, comments, tags, post_tags

## 현재 프로젝트 분석

- 기준 파일: `backend/src/main/resources/db/migration/V1__init.sql`
- 현재 핵심 테이블: `users`, `posts`, `comments`, `tags`, `post_tags`
- AI 확장 테이블: `post_embeddings`, `ai_logs`, `agent_memory`, `mcp_tool_logs`
- 기본 게시판 관계: 사용자 1명은 여러 게시글을 쓰고, 게시글 1개는 여러 댓글과 여러 태그를 가진다.

## 학습 목표

- [ ] 게시판 핵심 도메인을 테이블 관계로 설명할 수 있다.
- [ ] `1:N` 관계와 `N:M` 관계가 왜 다른 테이블 구조를 요구하는지 설명할 수 있다.
- [ ] Flyway migration 파일 하나로 초기 DB 구조를 재현할 수 있다.
- [ ] `post_tags` 같은 조인 테이블이 왜 필요한지 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- 관계형 데이터베이스 기본 개념: table, row, column, schema
- 기본키와 외래키: `PRIMARY KEY`, `FOREIGN KEY`
- 관계 설계: `1:1`, `1:N`, `N:M`
- 정규화: 중복 데이터를 줄이고 변경 지점을 줄이는 방법
- 조인 테이블: `post_tags`처럼 `N:M` 관계를 풀어내는 테이블
- 제약 조건: `NOT NULL`, `UNIQUE`, `ON DELETE CASCADE`
- 인덱스: 검색과 정렬을 빠르게 만들기 위한 DB 자료구조
- Flyway migration: DB 구조 변경을 버전으로 관리하는 방식
- pgvector 기본 개념: PostgreSQL에서 벡터 데이터를 저장하고 검색하는 확장

## 재구현 체크리스트

### 1. 도메인 관계 먼저 그리기

- [ ] `users -> posts` 관계를 `1:N`으로 정의한다.
- [ ] `posts -> comments` 관계를 `1:N`으로 정의한다.
- [ ] `posts <-> tags` 관계를 `N:M`으로 정의한다.
- [ ] `post_tags`가 게시글과 태그 사이의 연결만 담당하도록 설계한다.
- [ ] 댓글은 게시글이 삭제되면 함께 삭제되어야 하는지 결정한다.

### 2. users 테이블 설계

- [ ] `id`를 `BIGSERIAL PRIMARY KEY`로 둔다.
- [ ] `email`은 로그인 식별자이므로 `UNIQUE NOT NULL`로 둔다.
- [ ] 평문 비밀번호가 아니라 `password_hash` 컬럼만 저장한다.
- [ ] `nickname`은 화면 표시용 이름으로 분리한다.
- [ ] 가입 시점을 추적하기 위해 `created_at`을 둔다.

### 3. posts 테이블 설계

- [ ] `user_id`를 `users(id)` 외래키로 연결한다.
- [ ] `title`은 검색과 목록 표시를 고려해 길이 제한을 둔다.
- [ ] `content`는 긴 본문 저장을 위해 `TEXT`로 둔다.
- [ ] `created_at`, `updated_at`을 모두 둔다.
- [ ] 게시글 목록 정렬을 위해 `created_at DESC` 인덱스를 고려한다.

### 4. comments 테이블 설계

- [ ] `post_id`를 `posts(id)` 외래키로 연결한다.
- [ ] `user_id`를 `users(id)` 외래키로 연결한다.
- [ ] 게시글 삭제 시 댓글이 남지 않도록 `ON DELETE CASCADE`를 적용한다.
- [ ] 댓글 정렬을 위해 `created_at`을 둔다.

### 5. tags, post_tags 설계

- [ ] `tags.name`은 중복 방지를 위해 `UNIQUE`로 둔다.
- [ ] 태그명 검색을 위해 `idx_tags_name` 인덱스를 둔다.
- [ ] `post_tags`의 기본키를 `(post_id, tag_id)` 복합키로 둔다.
- [ ] 게시글 삭제 시 연결 데이터가 남지 않도록 `post_tags.post_id`에 `ON DELETE CASCADE`를 둔다.
- [ ] 태그 삭제 시 연결 데이터가 남지 않도록 `post_tags.tag_id`에 `ON DELETE CASCADE`를 둔다.

## 검증 체크리스트

- [ ] `docker compose up -d db`로 PostgreSQL 컨테이너를 실행한다.
- [ ] `CREATE EXTENSION IF NOT EXISTS vector;`가 pgvector 이미지에서 성공하는지 확인한다.
- [ ] Flyway가 `V1__init.sql`을 실행해 모든 테이블을 생성하는지 확인한다.
- [ ] `users.email`에 같은 값을 두 번 넣으면 실패하는지 확인한다.
- [ ] 게시글 삭제 시 `comments`, `post_tags`, `post_embeddings`가 함께 정리되는지 확인한다.

## WHY 정리 질문

- [ ] 왜 게시글 본문에 태그 문자열을 그냥 저장하지 않고 `tags`, `post_tags`로 분리했는가?
- [ ] 왜 `password` 컬럼이 아니라 `password_hash` 컬럼이어야 하는가?
- [ ] 왜 `post_embeddings`는 `posts` 테이블 안에 넣지 않고 별도 테이블로 분리하는가?
- [ ] 왜 DB 스키마를 JPA 자동 생성 대신 Flyway로 관리하는가?
