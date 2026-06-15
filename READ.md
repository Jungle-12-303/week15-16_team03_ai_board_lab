# AI 지식 게시판 READ

React, Spring Boot, PostgreSQL, pgvector를 사용해 만든 2주 개인 과제용 AI 게시판 프로젝트입니다.
일반 게시판 기능 위에 RAG, MCP, Agent 기능을 최소 단위로 붙여서 “AI 기능이 실제 서비스 흐름에 어떻게 연결되는지”를 보여주는 것이 목표입니다.

## 문서 작성 기준

이 문서는 [[Project] 좋은 README 파일을 만들려면 어떻게?](https://kimhyeongi.tistory.com/69)의 README 작성 흐름을 참고했습니다.

참고 글에서 강조하는 핵심은 README가 프로젝트를 처음 보는 사람에게 다음 질문에 답해야 한다는 점입니다.

- 이 프로젝트는 무엇인가?
- 왜 만들었는가?
- 어떤 문제를 해결하는가?
- 어떻게 설치하고 실행하는가?
- 어떻게 사용하는가?
- 어떤 기술을 왜 사용했는가?

그래서 이 `READ.md`는 단순 기능 목록보다 “처음 보는 사람이 프로젝트를 실행하고 구조를 이해하는 순서”를 우선해서 작성했습니다.

## 목차

1. 프로젝트 개요
2. 핵심 기능
3. 기술 스택
4. 전체 아키텍처
5. 프로젝트 구조
6. 실행 방법
7. 사용 방법
8. API 요약
9. AI 기능 설계
10. 데이터베이스 설계
11. 테스트와 빌드
12. 배포 흐름
13. 한계와 개선 방향
14. 참고 자료

## 1. 프로젝트 개요

AI 지식 게시판은 게시글 작성, 검색, 댓글, 태그 같은 기본 게시판 기능에 AI 보조 기능을 결합한 웹 애플리케이션입니다.

이 프로젝트의 핵심 목적은 거대한 AI 서비스를 만드는 것이 아니라, 작은 게시판 서비스 안에서 RAG, MCP, Agent가 각각 어떤 역할을 하는지 이해할 수 있게 만드는 것입니다.

주요 사용자 흐름은 다음과 같습니다.

```txt
회원가입/로그인
 -> 게시글 작성
 -> 태그 저장
 -> 게시글 임베딩 저장
 -> 유사 게시글 검색
 -> AI 요약 또는 작성 보조 결과 확인
 -> 필요 시 MCP로 GitHub 정보 조회
```

## 2. 핵심 기능

| 구분 | 기능 | 설명 |
|---|---|---|
| 인증 | 회원가입, 로그인 | JWT를 사용해 API 요청 사용자를 식별합니다. |
| 게시판 | 게시글 CRUD | 게시글 작성, 조회, 수정, 삭제를 제공합니다. |
| 댓글 | 댓글 작성, 삭제 | 게시글 상세 화면에서 댓글을 관리합니다. |
| 태그 | 태그 저장, 필터링 | 게시글을 주제별로 분류하고 태그 검색에 사용합니다. |
| 검색 | 키워드/태그 검색 | 게시글 목록에서 제목, 본문, 태그 기준으로 찾습니다. |
| RAG | 유사 게시글 검색과 요약 | 게시글 임베딩을 저장하고 비슷한 글을 찾아 요약합니다. |
| MCP | JSON-RPC 스타일 GitHub API 호출 | 외부 도구 호출 방식을 단순한 JSON-RPC 형태로 구현했습니다. |
| Agent | 글 작성 보조 | 초안을 보고 태그 추천, 유사 글 검색, 외부 API 조회를 수행합니다. |
| DevOps | Docker Compose, Nginx, EC2 문서 | 로컬 및 서버 실행 흐름을 컨테이너 기반으로 정리했습니다. |

## 3. 기술 스택

| 영역 | 기술 | 선택 이유 |
|---|---|---|
| Frontend | React, Vite, Axios, lucide-react | 빠르게 화면을 구성하고 API 연동 상태를 확인하기 좋습니다. |
| Backend | Spring Boot 3.5, Java 21, Spring Security, JPA | 계층형 API 구조, 인증, DB 접근을 안정적으로 구현하기 좋습니다. |
| Database | PostgreSQL, pgvector | 일반 게시판 데이터와 벡터 검색 데이터를 같은 DB에서 관리할 수 있습니다. |
| Migration | Flyway | DB 스키마를 코드처럼 버전 관리하기 위해 사용했습니다. |
| AI | OpenAI API, local fallback | API 키가 있으면 실제 모델을 사용하고, 없으면 데모가 깨지지 않게 fallback을 사용합니다. |
| MCP | JSON-RPC 스타일 요청/응답, GitHub Public API | 과제 범위 안에서 외부 도구 호출 구조를 설명하기 쉽습니다. |
| DevOps | Docker Compose, Nginx, GitHub Actions | 로컬 실행, 프록시, 빌드 검증 흐름을 최소 구성으로 제공합니다. |

## 4. 전체 아키텍처

```txt
[Browser]
  |
  v
[React + Vite Frontend]
  - 로그인/회원가입 UI
  - 게시글 목록/상세/작성 UI
  - RAG, MCP, Agent 실행 UI
  |
  v
[Spring Boot Backend]
  - auth: 회원가입, 로그인, JWT 인증
  - post/comment/tag: 게시판 핵심 기능
  - ai.rag: 임베딩 저장, 유사 글 검색, 요약
  - ai.mcp: JSON-RPC 요청 처리, GitHub API 호출
  - ai.agent: 도구 선택, 태그 추천, RAG/MCP 연결
  |
  v
[PostgreSQL + pgvector]
  - users, posts, comments, tags, post_tags
  - post_embeddings
  - ai_logs, agent_memory, mcp_tool_logs
```

Docker 환경에서는 Nginx가 가장 앞에서 요청을 받습니다.

```txt
http://localhost
  |
  v
[Nginx]
  |-- /api/* -> backend:8080
  `-- /*     -> frontend:80
```

## 5. 프로젝트 구조

```txt
.
├── backend
│   ├── src/main/java/com/example/aiknowledgeboard
│   │   ├── auth
│   │   ├── post
│   │   ├── comment
│   │   ├── tag
│   │   ├── ai/rag
│   │   ├── ai/mcp
│   │   └── ai/agent
│   ├── src/main/resources/db/migration
│   ├── build.gradle.kts
│   └── Dockerfile
├── frontend
│   ├── src/App.jsx
│   ├── src/api/client.js
│   ├── package.json
│   └── Dockerfile
├── infra
│   ├── nginx/default.conf
│   └── scripts/deploy-ec2.sh
├── docs
│   ├── architecture.md
│   ├── troubleshooting.md
│   └── screenshots
├── docker-compose.yml
├── README.md
└── READ.md
```

이 구조는 기능별 책임을 분리하기 위한 구조입니다.
프론트엔드는 사용자 화면과 API 호출만 담당하고, 백엔드는 인증/게시판/AI 로직을 담당하며, DB 마이그레이션과 배포 설정은 별도 디렉터리에 둡니다.

## 6. 실행 방법

### 6.1 Docker Compose로 전체 실행

가장 간단한 실행 방법입니다.

```bash
cp .env.example .env
docker compose up -d --build
docker compose ps
```

접속 주소는 다음과 같습니다.

- Frontend + Nginx: `http://localhost`
- Backend API: `http://localhost/api`

로그 확인:

```bash
docker compose logs -f backend
docker compose logs -f nginx
docker compose logs -f db
```

종료:

```bash
docker compose down
```

DB 볼륨까지 삭제해야 할 때:

```bash
docker compose down -v
```

### 6.2 개발 모드로 실행

백엔드와 프론트엔드를 따로 실행할 수 있습니다.

백엔드:

```bash
cd backend
./gradlew bootRun
```

프론트엔드:

```bash
cd frontend
npm install
npm run dev
```

개발 모드 접속 주소:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`

Vite 개발 서버는 `/api` 요청을 `http://localhost:8080`으로 프록시합니다.

### 6.3 데이터베이스 접속 방법

현재 데이터베이스는 외부 호스팅 DB가 아니라 Docker Compose의 `db` 컨테이너에서 실행됩니다.

```txt
내 Mac 또는 EC2
 -> Docker Compose
 -> ai-board-db 컨테이너
 -> PostgreSQL + pgvector
```

백엔드 컨테이너는 Docker 내부 네트워크에서 다음 주소로 DB에 접속합니다.

```txt
jdbc:postgresql://db:5432/ai_board
```

여기서 `db`는 인터넷 주소가 아니라 `docker-compose.yml`에 정의된 서비스 이름입니다.
Docker Compose는 같은 네트워크에 있는 컨테이너끼리 서비스 이름으로 찾을 수 있게 해줍니다.

DB 접속 정보는 다음과 같습니다.

| 항목 | 값 |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `ai_board` |
| Username | `ai_board` |
| Password | `ai_board` |

가장 간단한 접속 방법은 PostgreSQL 컨테이너 안에서 `psql`을 실행하는 것입니다.

```bash
docker compose exec db psql -U ai_board -d ai_board
```

접속 후 자주 쓰는 명령은 다음과 같습니다.

```sql
\dt
select * from users;
select * from posts;
select * from tags;
\q
```

Mac에 `psql`이 설치되어 있다면 컨테이너 밖에서도 접속할 수 있습니다.

```bash
psql -h localhost -p 5432 -U ai_board -d ai_board
```

DBeaver, DataGrip, TablePlus 같은 GUI 도구를 사용할 때도 같은 접속 정보를 입력하면 됩니다.

```txt
Host: localhost
Port: 5432
Database: ai_board
User: ai_board
Password: ai_board
JDBC URL: jdbc:postgresql://localhost:5432/ai_board
```

컨테이너 밖에서 `localhost:5432`로 접속할 수 있는 이유는 `docker-compose.yml`에서 DB 포트를 다음처럼 열어 두었기 때문입니다.

```yaml
ports:
  - "5432:5432"
```

DB 데이터는 Docker volume인 `postgres_data`에 저장됩니다.
그래서 `docker compose down`만 실행하면 데이터는 유지되지만, `docker compose down -v`를 실행하면 volume까지 삭제되어 DB 데이터도 지워질 수 있습니다.

## 7. 사용 방법

1. 회원가입 후 로그인합니다.
2. `글쓰기` 버튼으로 제목, 본문, 태그를 입력합니다.
3. 게시글이 저장되면 백엔드가 게시글 내용을 임베딩하고 `post_embeddings`에 저장합니다.
4. 게시글 목록에서 키워드 또는 태그로 검색합니다.
5. 게시글 상세 화면에서 댓글을 작성하거나 삭제합니다.
6. 작성 화면 또는 상세 화면에서 RAG 유사 글 검색을 실행합니다.
7. 작성 화면에서 Agent를 실행하면 태그 추천과 유사 글 검색 결과를 받을 수 있습니다.
8. MCP 실행은 GitHub 사용자 정보를 JSON-RPC 스타일로 조회합니다.

## 8. API 요약

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/auth/signup` | 회원가입 |
| POST | `/api/auth/login` | 로그인 |
| GET | `/api/posts?page=&size=&keyword=&tag=` | 게시글 목록, 검색, 페이징 |
| POST | `/api/posts` | 게시글 작성 |
| GET | `/api/posts/{id}` | 게시글 상세 조회 |
| PUT | `/api/posts/{id}` | 게시글 수정 |
| DELETE | `/api/posts/{id}` | 게시글 삭제 |
| POST | `/api/posts/{postId}/comments` | 댓글 작성 |
| DELETE | `/api/comments/{commentId}` | 댓글 삭제 |
| POST | `/api/ai/rag/similar` | 유사 게시글 검색과 요약 |
| POST | `/api/ai/mcp/call` | JSON-RPC 스타일 MCP 호출 |
| POST | `/api/ai/agent/write-helper` | 작성 보조 Agent 실행 |

## 9. AI 기능 설계

### 9.1 RAG

RAG는 게시글을 저장할 때 임베딩을 만들고, 사용자의 질문이나 초안과 비슷한 게시글을 찾는 기능입니다.

```txt
게시글 작성/수정
 -> title + content 합치기
 -> OpenAI embedding 또는 local fallback embedding 생성
 -> post_embeddings에 upsert

유사 글 검색
 -> query embedding 생성
 -> pgvector cosine distance 검색
 -> Top 3 게시글 반환
 -> OpenAI 요약 또는 fallback 요약 생성
```

RAG 검색에는 JPA 대신 SQL을 직접 사용합니다.
이유는 pgvector의 벡터 거리 연산자인 `<=>`를 SQL로 쓰는 편이 더 명확하고, JPA 엔티티로 억지 추상화하는 것보다 과제 목적에 맞기 때문입니다.

### 9.2 MCP

MCP는 JSON-RPC 스타일 요청을 받아 GitHub Public API를 호출합니다.

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

지원 method:

- `github.getUser`
- `github.getRepo`

MCP 호출 결과와 실패 정보는 `mcp_tool_logs`에 기록합니다.

### 9.3 Agent

Agent는 사용자의 초안을 보고 필요한 도구를 선택합니다.

실행 가능한 도구는 다음과 같습니다.

| 도구 | 역할 |
|---|---|
| `recommend_tags` | 초안에서 키워드를 뽑아 태그를 추천합니다. |
| `search_similar_posts` | RAG를 사용해 유사 게시글을 검색합니다. |
| `call_mcp_external_api` | GitHub 관련 내용이 있으면 MCP로 GitHub 정보를 조회합니다. |
| `summarize_draft` | 초안 개선 방향을 요약합니다. |

Agent는 최대 3회까지만 도구를 실행합니다.
이 제한을 둔 이유는 개인 과제 범위에서 무한 반복이나 과도한 외부 호출을 막고, 실행 흐름을 쉽게 추적하기 위해서입니다.

## 10. 데이터베이스 설계

| 테이블 | 역할 |
|---|---|
| `users` | 사용자 이메일, 비밀번호 해시, 닉네임 저장 |
| `posts` | 게시글 제목, 본문, 작성자 저장 |
| `comments` | 게시글 댓글 저장 |
| `tags` | 태그 이름 저장 |
| `post_tags` | 게시글과 태그의 N:M 관계 저장 |
| `post_embeddings` | RAG 검색용 게시글 임베딩 저장 |
| `ai_logs` | RAG와 Agent 실행 로그 저장 |
| `agent_memory` | Agent 세션별 최소 기억 저장 |
| `mcp_tool_logs` | MCP 도구 호출 로그 저장 |

`post_embeddings`를 `posts`와 분리한 이유는 게시판 본문 데이터와 AI 검색 데이터를 분리하기 위해서입니다.
이렇게 하면 게시글 CRUD가 실패하지 않도록 유지하면서, 임베딩 저장이나 검색 기능만 따로 개선할 수 있습니다.

## 11. 테스트와 빌드

백엔드 빌드:

```bash
cd backend
./gradlew bootJar -x test
```

백엔드 테스트:

```bash
cd backend
./gradlew test
```

프론트엔드 빌드:

```bash
cd frontend
npm ci
npm run build
```

GitHub Actions는 다음을 확인합니다.

- Java 21 환경에서 백엔드 `bootJar` 빌드
- Node 22 환경에서 프론트엔드 `npm run build`

## 12. 배포 흐름

EC2 배포의 기본 흐름은 다음과 같습니다.

```txt
Ubuntu EC2 생성
 -> 보안 그룹에서 22, 80 포트 허용
 -> Docker와 Docker Compose 설치
 -> 저장소 clone
 -> .env 작성
 -> docker compose up -d --build
 -> Nginx를 통해 http://서버IP 접속
```

배포 스크립트는 `infra/scripts/deploy-ec2.sh`에 있습니다.
이 스크립트는 저장소를 pull한 뒤 Docker 이미지를 빌드하고 컨테이너를 다시 실행합니다.

```bash
APP_DIR=$HOME/ai-knowledge-board ./infra/scripts/deploy-ec2.sh
```

## 13. 한계와 개선 방향

현재 한계:

- OpenAI API 키가 없으면 fallback 임베딩과 fallback 요약을 사용하므로 실제 AI 품질은 제한적입니다.
- Agent는 완전한 자율 에이전트가 아니라 규칙 기반 도구 선택기에 가깝습니다.
- MCP는 과제용 최소 구현이므로 GitHub 사용자/저장소 조회만 지원합니다.
- 테스트 코드는 기본 빌드 검증 수준이며, API 통합 테스트는 더 보강할 수 있습니다.
- HTTPS와 자동 배포는 환경에 따라 추가 설정이 필요합니다.

개선 방향:

- RAG 검색 품질 평가 데이터 추가
- Agent 도구 선택 로직 고도화
- MCP 도구 종류 확장
- 게시글 이미지 업로드 추가
- API 통합 테스트와 프론트엔드 테스트 추가
- HTTPS와 배포 자동화 완성

## 14. 참고 자료

- [[Project] 좋은 README 파일을 만들려면 어떻게?](https://kimhyeongi.tistory.com/69)
- [프로젝트 아키텍처 문서](docs/architecture.md)
- [트러블슈팅 문서](docs/troubleshooting.md)
- [기존 README](README.md)
