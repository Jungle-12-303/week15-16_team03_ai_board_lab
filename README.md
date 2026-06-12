# Project Alpha

React와 Spring Boot로 구현하는 AI 게시판 프로젝트입니다.
기본 게시판 기능을 먼저 서버와 DB에 연결하고, 그 위에 RAG, MCP, Agent 기능을 단계적으로 붙이는 것을 목표로 합니다.

## 기술 스택

| 영역 | 선택 |
| --- | --- |
| Frontend | React, Vite, React Router |
| Backend | Spring Boot 4, Java |
| Database | MySQL 8.4, Spring Data JPA |
| Auth | Spring Security, JWT |
| LLM | OpenAI Chat API |
| Embedding | OpenAI Embedding API |
| RAG 저장소 | MySQL `post_embeddings` 테이블 |
| MCP | Spring Boot 내부 JSON-RPC MCP 서버 |
| Agent 기반 데이터 | `post_reads` 읽음 기록 |

## 주요 구현 기능

### 게시판

- 회원가입, 로그인, 로그아웃
- JWT 기반 인증 요청
- 게시글 목록, 상세, 생성, 수정, 삭제
- 댓글 생성, 삭제
- 태그 저장, 태그 검색
- 카테고리 필터, 키워드 검색, 페이지네이션

### RAG

- 게시글 생성/수정 시 임베딩 작업을 `embedding_jobs`에 예약
- 임베딩 처리 API로 게시글 내용을 벡터화
- 작성 중인 글을 임베딩하여 유사 게시글 검색
- 검색된 유사 게시글을 근거로 초안 생성

### MCP

- `/api/mcp` JSON-RPC 엔드포인트 제공
- `weather.current_forecast` 도구 구현
- 게시글 상세 화면에서 MCP 날씨 도구를 호출해 날씨 관련 문장 팩트체크

### Agent

- 게시글 상세 조회 시 사용자별 읽음 기록 저장
- 이후 "선호 태그 기반 놓친 글 추천" 기능의 상태 데이터로 사용 예정

## 실행 방법

### 1. MySQL 실행

```bash
docker compose up -d mysql
```

### 2. Backend 실행

```bash
cd backend
./gradlew bootRun
```

Windows PowerShell에서는 아래 명령을 사용할 수 있습니다.

```powershell
cd backend
.\gradlew.bat bootRun
```

### 3. Frontend 실행

```bash
cd frontend/project-alpha
npm install
npm run dev
```

프론트 주소는 `http://127.0.0.1:5173/` 입니다.

## 환경 변수

Backend는 `backend/.env` 파일을 선택적으로 읽습니다.

```properties
OPENAI_API_KEY=...
APP_JWT_SECRET=...
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/project_alpha?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=alpha
SPRING_DATASOURCE_PASSWORD=alpha_password
```

API key와 DB 비밀번호는 Git에 커밋하지 않습니다.

## 코드 읽는 순서

처음 보는 사람이 흐름을 따라가기 쉬운 순서입니다.

1. [Frontend entry](frontend/project-alpha/src/App.jsx)
2. [Board page](frontend/project-alpha/src/pages/BoardPage.jsx)
3. [Post API client](frontend/project-alpha/src/api/postApi.js)
4. [Post controller](backend/src/main/java/com/jungle_choi/namanmu/api/PostController.java)
5. [Post entity](backend/src/main/java/com/jungle_choi/namanmu/domain/post/Post.java)
6. [Auth controller](backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java)
7. [RAG API client](frontend/project-alpha/src/api/ragApi.js)
8. [AI controller](backend/src/main/java/com/jungle_choi/namanmu/api/AiController.java)
9. [RAG draft service](backend/src/main/java/com/jungle_choi/namanmu/service/RagDraftService.java)
10. [MCP fact check service](backend/src/main/java/com/jungle_choi/namanmu/service/WeatherFactCheckService.java)

더 자세한 파일별 역할은 [Code Map](docs/code-map.md)을 참고합니다.

## 문서

- [Code Map](docs/code-map.md)
- [Database Schema](docs/database-schema.md)
- [AWS Workshop Application Plan](docs/aws-workshop-application-plan.md)
