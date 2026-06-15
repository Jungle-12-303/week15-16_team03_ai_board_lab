# API 명세서

## 1. 공통 규칙

### Base URL

프론트엔드 개발 환경에서는 프록시 또는 Nginx 기준으로 아래처럼 호출하는 것을 권장한다.

```txt
/api
```

백엔드 서버를 직접 호출할 때는 로컬 기준으로 아래 주소를 사용할 수 있다.

```txt
http://localhost:8080/api
```

### Swagger/OpenAPI 문서

백엔드를 실행한 뒤 아래 주소에서 Swagger UI 기반 API 명세서를 확인할 수 있다.

```txt
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 원문은 아래 주소에서 확인할 수 있다.

```txt
http://localhost:8080/v3/api-docs
```

### Content-Type

요청 body가 있는 API는 JSON을 사용한다.

```http
Content-Type: application/json
```

### 인증 헤더

로그인 후 받은 JWT는 인가가 필요한 요청에 아래 형식으로 보낸다.

```http
Authorization: Bearer <token>
```

예:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature
```

`Bearer ` 뒤의 문자열이 실제 JWT이며, JWT는 `header.payload.signature` 형식이다.

### 인증 필요 여부

| 구분 | 인증 필요 |
| --- | --- |
| `POST /api/auth/signup` | 없음 |
| `POST /api/auth/login` | 없음 |
| `GET /api/posts` | 없음 |
| `GET /api/posts/{id}` | 없음 |
| 그 외 API | 필요 |

### 공통 에러 응답

에러 응답은 공통적으로 아래 구조를 사용한다.

```json
{
  "code": "BAD_REQUEST",
  "message": "이미 가입된 이메일입니다.",
  "timestamp": "2026-06-12T10:15:30.123Z"
}
```

| HTTP Status | code | 주요 상황 |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | DTO validation 실패 |
| 400 | `BAD_REQUEST` | 중복 이메일, 잘못된 로그인 정보, 잘못된 요청 |
| 403 | `FORBIDDEN` | 작성자 본인이 아닌 수정/삭제 요청 |
| 404 | `NOT_FOUND` | 게시글/댓글/사용자 없음 |
| 500 | `INTERNAL_ERROR` | 예상하지 못한 서버 오류 |

인증이 필요한 API에서 JWT가 없거나 잘못된 경우는 Controller에 도착하기 전에 Spring Security가 먼저 응답할 수 있다. 프론트에서는 `401` 또는 `403`을 모두 로그인 필요 상태로 처리한다.

### 날짜 형식

`createdAt`, `updatedAt`, `timestamp`는 ISO-8601 문자열로 내려온다.

```json
"2026-06-12T10:15:30.123Z"
```

---

## 2. Auth API

### 2.1 회원가입

```http
POST /api/auth/signup
```

인증 필요: 없음

#### Request Body

```json
{
  "email": "user@example.com",
  "nickname": "tester",
  "password": "password123"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `email` | 필수, 이메일 형식 |
| `nickname` | 필수, 2자 이상 40자 이하 |
| `password` | 필수, 6자 이상 80자 이하 |

#### Response 200

회원가입 성공 시 바로 JWT를 발급한다.

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiZXhwIjoxNzYwMDAwMDAwfQ.signature",
  "userId": 1,
  "email": "user@example.com",
  "nickname": "tester"
}
```

#### Error

| Status | 상황 |
| --- | --- |
| 400 | 이미 가입된 이메일 |
| 400 | validation 실패 |

---

### 2.2 로그인

```http
POST /api/auth/login
```

인증 필요: 없음

#### Request Body

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `email` | 필수, 이메일 형식 |
| `password` | 필수 |

#### Response 200

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiZXhwIjoxNzYwMDAwMDAwfQ.signature",
  "userId": 1,
  "email": "user@example.com",
  "nickname": "tester"
}
```

#### Error

| Status | 상황 |
| --- | --- |
| 400 | 이메일 또는 비밀번호가 올바르지 않음 |
| 400 | validation 실패 |

---

## 3. Post API

### 3.1 게시글 목록 조회

```http
GET /api/posts
```

인증 필요: 없음

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
| --- | --- | --- | --- | --- |
| `page` | number | 아니오 | `0` | 0부터 시작하는 페이지 번호 |
| `size` | number | 아니오 | `10` | 페이지 크기. 서버에서 1 이상 50 이하로 제한 |
| `keyword` | string | 아니오 | 없음 | 제목/본문 검색어 |
| `tag` | string | 아니오 | 없음 | 태그 이름 |

#### Request Example

```http
GET /api/posts?page=0&size=10&keyword=spring&tag=backend
```

#### Response 200

```json
{
  "content": [
    {
      "id": 1,
      "title": "Spring Boot 정리",
      "contentPreview": "Spring Boot 게시판 구현 내용...",
      "authorId": 1,
      "authorNickname": "tester",
      "tags": ["spring", "backend"],
      "commentCount": 2,
      "createdAt": "2026-06-12T10:15:30.123Z",
      "updatedAt": "2026-06-12T10:20:30.123Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 3.2 게시글 상세 조회

```http
GET /api/posts/{id}
```

인증 필요: 없음

#### Path Parameters

| 이름 | 타입 | 설명 |
| --- | --- | --- |
| `id` | number | 게시글 id |

#### Response 200

```json
{
  "id": 1,
  "title": "Spring Boot 정리",
  "content": "본문 전체 내용입니다.",
  "authorId": 1,
  "authorNickname": "tester",
  "tags": ["spring", "backend"],
  "comments": [
    {
      "id": 10,
      "postId": 1,
      "authorId": 2,
      "authorNickname": "commenter",
      "content": "좋은 글입니다.",
      "createdAt": "2026-06-12T10:30:30.123Z"
    }
  ],
  "createdAt": "2026-06-12T10:15:30.123Z",
  "updatedAt": "2026-06-12T10:20:30.123Z"
}
```

#### Error

| Status | 상황 |
| --- | --- |
| 404 | 게시글 없음 |

---

### 3.3 게시글 작성

```http
POST /api/posts
```

인증 필요: 필요

#### Request Headers

```http
Authorization: Bearer <token>
Content-Type: application/json
```

#### Request Body

```json
{
  "title": "Spring Boot 정리",
  "content": "본문 전체 내용입니다.",
  "tags": ["Spring", "#backend", " spring "]
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `title` | 필수, 200자 이하 |
| `content` | 필수 |
| `tags` | 선택 |

#### 처리 규칙

- 태그는 `trim`, `#` 제거, 소문자 변환 후 저장된다.
- 태그는 입력 순서를 유지하면서 중복 제거된다.
- 태그는 최대 5개까지만 저장된다.

#### Response 201

`PostDetailResponse`를 반환한다. 형식은 게시글 상세 조회 응답과 같다.

---

### 3.4 게시글 수정

```http
PUT /api/posts/{id}
```

인증 필요: 필요

#### Request Body

```json
{
  "title": "수정된 제목",
  "content": "수정된 본문입니다.",
  "tags": ["spring", "jpa"]
}
```

#### Response 200

`PostDetailResponse`를 반환한다.

#### Error

| Status | 상황 |
| --- | --- |
| 403 | 게시글 작성자가 아님 |
| 404 | 게시글 없음 |

---

### 3.5 게시글 삭제

```http
DELETE /api/posts/{id}
```

인증 필요: 필요

#### Response 204

응답 body 없음.

#### Error

| Status | 상황 |
| --- | --- |
| 403 | 게시글 작성자가 아님 |
| 404 | 게시글 없음 |

---

## 4. Comment API

### 4.1 댓글 작성

```http
POST /api/posts/{postId}/comments
```

인증 필요: 필요

#### Request Body

```json
{
  "content": "댓글 내용입니다."
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `content` | 필수, 1000자 이하 |

#### Response 201

```json
{
  "id": 10,
  "postId": 1,
  "authorId": 2,
  "authorNickname": "commenter",
  "content": "댓글 내용입니다.",
  "createdAt": "2026-06-12T10:30:30.123Z"
}
```

#### Error

| Status | 상황 |
| --- | --- |
| 404 | 게시글 없음 |
| 400 | validation 실패 |

---

### 4.2 댓글 삭제

```http
DELETE /api/comments/{commentId}
```

인증 필요: 필요

#### Response 204

응답 body 없음.

#### Error

| Status | 상황 |
| --- | --- |
| 403 | 댓글 작성자가 아님 |
| 404 | 댓글 없음 |

---

## 5. RAG API

### 5.1 유사 글 검색 및 요약

```http
POST /api/ai/rag/similar
```

인증 필요: 필요

#### Request Body

```json
{
  "query": "Spring Boot에서 JWT 인증 흐름을 정리한 글",
  "excludePostId": 1
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `query` | 필수 |
| `excludePostId` | 선택. 현재 상세 글을 검색 결과에서 제외할 때 사용 |

#### Response 200

```json
{
  "summary": "관련 글 요약입니다.",
  "sources": [
    {
      "id": 2,
      "title": "JWT 인증 흐름",
      "contentPreview": "JWT 필터와 SecurityContextHolder 설명...",
      "authorNickname": "tester",
      "score": 0.82,
      "link": "/posts/2"
    }
  ]
}
```

---

## 6. MCP API

### 6.1 JSON-RPC 도구 호출

```http
POST /api/ai/mcp/call
```

인증 필요: 필요

#### Request Body

`params`는 도구별로 달라지는 값이므로 object로 보낸다.

```json
{
  "jsonrpc": "2.0",
  "method": "github.getUser",
  "params": {
    "username": "openai"
  },
  "id": "req-1"
}
```

#### 지원 method

| method | params | 설명 |
| --- | --- | --- |
| `github.getUser` | `{ "username": "openai" }` | GitHub 사용자 정보 조회 |
| `github.getRepo` | `{ "owner": "openai", "repo": "openai-cookbook" }` | GitHub 저장소 정보 조회 |

#### Success Response 200

```json
{
  "jsonrpc": "2.0",
  "result": {
    "login": "openai",
    "name": "OpenAI",
    "publicRepos": 100,
    "followers": 100000,
    "profileUrl": "https://github.com/openai"
  },
  "id": "req-1"
}
```

#### JSON-RPC Error Response 200

MCP 내부 에러는 HTTP 200 안의 JSON-RPC error로 반환된다.

```json
{
  "jsonrpc": "2.0",
  "error": {
    "code": -32000,
    "message": "지원하지 않는 MCP method 입니다: unknown.method"
  },
  "id": "req-1"
}
```

---

## 7. Agent API

### 7.1 글쓰기 도우미 실행

```http
POST /api/ai/agent/write-helper
```

인증 필요: 필요

#### Request Body

```json
{
  "draft": "Spring Boot JWT 인증 흐름을 정리하고 싶다.",
  "intention": "태그 추천과 유사 글 검색",
  "sessionId": "optional-session-id"
}
```

#### Validation

| 필드 | 규칙 |
| --- | --- |
| `draft` | 필수 |
| `intention` | 선택 |
| `sessionId` | 선택. 없으면 서버가 새 세션 id를 생성해 내부 처리 |

#### Response 200

```json
{
  "finalMessage": "초안에 맞는 태그와 참고 글을 찾았습니다.",
  "recommendedTags": ["spring", "jwt", "security"],
  "similarPosts": [
    {
      "id": 2,
      "title": "JWT 인증 흐름",
      "contentPreview": "JWT 필터와 SecurityContextHolder 설명...",
      "authorNickname": "tester",
      "score": 0.82,
      "link": "/posts/2"
    }
  ],
  "mcpResult": null,
  "steps": [
    {
      "tool": "recommend_tags",
      "status": "SUCCESS",
      "message": "초안에서 태그를 추천했습니다."
    },
    {
      "tool": "search_similar_posts",
      "status": "SUCCESS",
      "message": "기존 게시글에서 유사 글을 검색했습니다."
    }
  ],
  "fallback": false
}
```

---

## 8. 프론트 구현 메모

### 로그인 상태 저장

로그인/회원가입 성공 응답의 `token`, `userId`, `email`, `nickname`을 프론트 상태와 저장소에 보관한다.

```js
localStorage.setItem("token", response.token);
localStorage.setItem("user", JSON.stringify({
  id: response.userId,
  email: response.email,
  nickname: response.nickname
}));
```

### 인증 요청 처리

인증이 필요한 API에는 매 요청마다 JWT를 붙인다.

```js
headers: {
  Authorization: `Bearer ${token}`
}
```

### 권한 UI 처리

수정/삭제 버튼은 프론트에서 먼저 숨길 수 있다.

```js
post.authorId === user.id
```

단, 최종 권한 검사는 백엔드가 한다. 프론트 버튼 숨김은 UX 목적이고 보안 목적이 아니다.

### 204 응답 처리

`DELETE /api/posts/{id}`, `DELETE /api/comments/{commentId}`는 body 없이 `204 No Content`를 반환한다. 프론트에서는 `response.data`를 기대하지 않는 방식으로 처리한다.

### 에러 메시지 표시

백엔드 에러 응답의 `message`를 우선 표시한다.

```js
const message = error.response?.data?.message ?? "요청 처리 중 오류가 발생했습니다.";
```
