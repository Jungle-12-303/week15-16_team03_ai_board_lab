# AI 지식 게시판

React, Spring Boot, PostgreSQL, pgvector를 사용한 2주 개인 과제용 MVP 게시판입니다. 기본 게시판 기능에 RAG, JSON-RPC 스타일 MCP, 작성 보조 Agent를 최소 범위로 붙였습니다.

## 1. 프로젝트 소개

이 프로젝트의 목표는 “복잡한 AI 서비스”가 아니라 제출 가능한 게시판에 AI 응용 기술의 핵심 흐름을 연결하는 것입니다.

- 기본 게시판: 회원가입/로그인, 게시글 CRUD, 댓글, 태그, 페이징, 검색
- RAG: 게시글 임베딩 저장, 유사 글 검색, 요약, 출처 링크
- MCP: JSON-RPC 요청으로 GitHub Public API 호출
- Agent: 글 초안을 보고 태그 추천, 유사 글 검색, MCP 호출 중 필요한 도구 실행
- DevOps: Docker Compose, Nginx, EC2 배포 문서, 로그/장애 기록

## 2. 기술 스택

| 영역 | 사용 기술 | 선택 이유 |
|---|---|---|
| Frontend | React + Vite + Axios | 빠르게 화면을 만들고 API 연동을 확인하기 쉽습니다. |
| Backend | Spring Boot + Security + JPA | 계층형 게시판 API와 JWT 인증을 설명하기 좋습니다. |
| DB | PostgreSQL + pgvector | 일반 데이터와 벡터 검색 데이터를 한 DB에서 관리합니다. |
| AI | OpenAI API + 로컬 fallback | API 키가 있으면 실제 LLM, 없으면 데모 가능한 로컬 로직을 사용합니다. |
| MCP | JSON-RPC 스타일 + GitHub Public API | 최소 구현으로 외부 도구 호출 흐름을 보여줄 수 있습니다. |
| DevOps | Docker Compose + Nginx + EC2 | 개인 과제에서 현실적으로 배포 경험을 남기기 좋습니다. |

## 3. 전체 아키텍처

```txt
[React Frontend]
  - 로그인/회원가입
  - 게시글/댓글/검색
  - AI 작성 도우미 패널
        |
        v
[Spring Boot Backend]
  auth -> user
  post -> comment -> tag
  ai.rag -> post_embeddings
  ai.mcp -> GitHub API
  ai.agent -> RAG/MCP/tag tools
        |
        v
[PostgreSQL + pgvector]
  users, posts, comments, tags, post_tags
  post_embeddings, ai_logs, agent_memory, mcp_tool_logs
```

## 4. DB 설계

| 테이블 | 역할 |
|---|---|
| users | 로그인 사용자 저장 |
| posts | 게시글 본문 저장 |
| comments | 게시글 댓글 저장 |
| tags | 태그 이름 저장 |
| post_tags | 게시글과 태그의 N:M 연결 |
| post_embeddings | RAG 검색용 게시글 벡터 저장 |
| ai_logs | RAG/Agent 실행 기록 |
| agent_memory | Agent의 최소 상태 기록 |
| mcp_tool_logs | MCP 도구 호출 성공/실패 기록 |

`post_embeddings`를 별도 테이블로 둔 이유는 게시글 CRUD와 AI 검색 데이터를 분리하기 위해서입니다. 이렇게 하면 게시글 저장 기능은 단순하게 유지하고, RAG 검색만 별도로 개선할 수 있습니다.

## 5. REST API

Swagger UI로 실행 중인 API 명세를 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| GET | `/api/posts?page=&keyword=&tag=` | 게시글 목록/검색 |
| POST | `/api/posts` | 게시글 작성 |
| GET | `/api/posts/{id}` | 게시글 상세 |
| PUT | `/api/posts/{id}` | 게시글 수정 |
| DELETE | `/api/posts/{id}` | 게시글 삭제 |
| POST | `/api/posts/{id}/comments` | 댓글 작성 |
| DELETE | `/api/comments/{id}` | 댓글 삭제 |
| POST | `/api/ai/rag/similar` | 유사 게시글 검색과 요약 |
| POST | `/api/ai/mcp/call` | JSON-RPC 스타일 MCP 호출 |
| POST | `/api/ai/agent/write-helper` | 작성 보조 Agent 실행 |

## 6. AI 기능 설명

### RAG

게시글 작성/수정 시 `title + content`를 임베딩하고 `post_embeddings`에 저장합니다. 질문이나 초안이 들어오면 같은 방식으로 임베딩을 만든 뒤 pgvector의 cosine distance로 유사 글 Top 3을 찾습니다.

```txt
게시글 저장
 -> title + content
 -> OpenAI Embedding API 또는 로컬 fallback embedding
 -> post_embeddings 저장

유사 글 요청
 -> 사용자 query embedding
 -> pgvector 유사도 검색
 -> LLM 요약 또는 로컬 fallback 요약
 -> 출처 링크 반환
```

OpenAI API 키가 없을 때도 로컬 해시 기반 임베딩을 사용합니다. 이 fallback은 실제 품질보다는 제출 데모 안정성을 위한 장치입니다.

### MCP

MCP는 과제용 최소 구현으로 JSON-RPC 스타일 요청/응답을 사용합니다.

요청 예시:

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

응답 예시:

```json
{
  "jsonrpc": "2.0",
  "result": {
    "login": "openai",
    "publicRepos": 100,
    "profileUrl": "https://github.com/openai"
  },
  "id": "req-1"
}
```

지원 도구:

- `github.getUser`
- `github.getRepo`

### Agent

Agent는 게시글을 직접 수정하지 않고 추천만 반환합니다.

도구:

- `recommend_tags`: 초안 기반 태그 추천
- `search_similar_posts`: RAG 유사 글 검색
- `call_mcp_external_api`: GitHub 사용자 정보 조회
- `summarize_draft`: 초안 개선 요약

제한:

- 최대 3회 도구 실행
- 반복 실패 시 중단
- 실패해도 fallback 메시지 반환
- `agent_memory`, `ai_logs`에 실행 기록 저장

## 7. 로컬 실행 방법

### PostgreSQL 준비

가장 쉬운 방법은 Docker Compose로 전체 실행하는 것입니다. 백엔드만 직접 실행하려면 PostgreSQL과 pgvector가 필요합니다.

### 백엔드

```bash
cd backend
./gradlew bootJar -x test
./gradlew bootRun
```

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

브라우저에서 `http://localhost:5173`에 접속합니다.

## 8. Docker Compose 실행

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
docker compose logs -f backend
docker compose down
```

접속 주소:

- Nginx/Frontend: `http://localhost`
- Backend API: `http://localhost/api`

## 9. AWS EC2 배포 가이드

1. Ubuntu EC2 생성
2. 보안 그룹 인바운드 허용: `22`, `80`, 필요 시 `443`
3. Docker와 Docker Compose 설치
4. 저장소 clone
5. `.env` 생성 후 비밀값 입력
6. 실행

```bash
docker compose up -d --build
docker compose logs -f
```

Nginx는 `/api`를 백엔드로 보내고, 나머지 경로를 React 정적 파일로 보냅니다.

## 10. DevOps 9단계 기록

| 단계 | 적용 위치 | 성공 기준 |
|---|---|---|
| Linux | EC2 접속, 파일 권한, 로그 확인 | `ssh`, `ls`, `chmod`, `ps` 사용 |
| Network | 포트 80, 8080, Docker network | 브라우저 접속 성공 |
| Docker | backend/frontend Dockerfile | 이미지 빌드 성공 |
| Compose | `docker-compose.yml` | db/backend/frontend/nginx 실행 |
| EC2 | Ubuntu 서버 | 외부 IP 접속 성공 |
| Nginx | `infra/nginx/default.conf` | `/api` 프록시 성공 |
| HTTPS | Certbot 적용 가능 | 도메인 HTTPS 접속 |
| GitHub Actions | `.github/workflows/ci.yml` | push/PR 빌드 성공 |
| Logs | `docs/troubleshooting.md` | 장애 원인과 해결 기록 |

## 11. 데모 스크린샷

스크린샷은 `docs/screenshots/`에 저장합니다.

권장 캡처:

- 로그인/회원가입
- 게시글 목록
- 게시글 상세와 댓글
- 게시글 작성 화면
- RAG 유사 글 결과
- MCP GitHub 결과
- Agent 작성 보조 결과
- Docker Compose 실행 화면

## 12. 회고

이 프로젝트는 AI 기능의 품질보다 구조 이해를 우선했습니다. RAG는 게시글 기반 벡터 검색과 출처 표시를 보여주는 데 집중했고, MCP는 JSON-RPC 스타일 도구 호출을 최소화했습니다. Agent도 자율 시스템이 아니라 작성 보조 도구 선택기로 제한했습니다.

## 13. 한계와 개선점

한계:

- OpenAI 키가 없으면 로컬 fallback이 동작하지만 실제 LLM 품질은 아닙니다.
- Agent의 도구 선택은 복잡한 추론보다 안전한 규칙 기반에 가깝습니다.
- HTTPS와 자동 배포는 환경에 따라 추가 설정이 필요합니다.

개선점:

- 실제 function calling schema 적용
- RAG 검색 품질 평가
- 게시글 이미지 업로드
- 테스트 코드 보강
- HTTPS와 GitHub Actions 배포 자동화 완성

## 14. 발표용 1분 설명

AI 지식 게시판은 일반 게시판 기능에 RAG, MCP, Agent를 최소 단위로 결합한 개인 과제 프로젝트입니다. 사용자가 글을 작성하면 기존 게시글을 벡터 검색해서 유사 글을 추천하고, LLM이 핵심 내용을 요약하며 출처 링크를 함께 보여줍니다. MCP 기능은 JSON-RPC 방식의 작은 도구 서버로 구현했고, GitHub 공개 API를 호출해 외부 정보를 가져옵니다. Agent는 글 초안을 보고 태그 추천, 유사 글 검색, 외부 API 호출 중 필요한 도구를 선택하도록 만들었습니다. 전체 서비스는 React, Spring Boot, PostgreSQL, pgvector로 구성했고 Docker Compose와 AWS EC2 배포까지 진행할 수 있게 구성했습니다.
