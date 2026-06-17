# Project Alpha QA 체크리스트

이 문서는 제출 전 직접 눌러볼 항목을 정리한 체크리스트다. 모든 항목을 완벽한 자동 테스트로 대체하기보다, 실제 화면 기준으로 깨지는 부분이 없는지 확인하는 용도다.

## 2026-06-17 QA 실행 결과

아래 결과는 제출 전 기준으로 실제 로컬/AWS 서버와 API를 대상으로 확인한 내용이다. 체크리스트 본문은 최종 제출 전 다시 눌러볼 수 있도록 유지한다.

| 구분 | 확인 내용 | 결과 |
|---|---|---|
| Backend test | `backend`에서 `.\gradlew.bat test` 실행 | 통과 |
| Frontend build | `frontend/project-alpha`에서 `npm run build` 실행 | 통과 |
| Docker | `mysql`, `qdrant` 컨테이너 실행 상태 | 실행 중 |
| Backend | `http://127.0.0.1:8080/api/health` | `200` |
| Frontend | `http://127.0.0.1:5173` | `200` |
| OpenAI key | `backend/.env`에 `OPENAI_API_KEY` 존재 | 확인 |
| Qdrant | collection 목록 | `project_alpha_posts`, `project_alpha_chunks` |
| 게시글 데이터 | `GET /api/posts?page=0&size=3` | published 기준 총 `1303`개. AWS DB 전체 `posts` row는 `1312`개 |
| 카테고리 개수 | 전체 기준 category count | Development 201, Learning 200, Project 202, Daily 300, Review 200, Briefing 200 |
| AWS 배포 | `http://3.37.55.77`, `http://3.37.55.77:8080/actuator/health` | frontend `200`, backend `UP` |
| 임베딩 job | AWS DB `embedding_jobs` 상태 | `COMPLETED 2514`, `FAILED 0` |

### API Smoke Test

| 기능 | 확인 내용 | 결과 |
|---|---|---|
| 인증 | 회원가입, 로그인, `me`, 로그아웃 | 통과 |
| 인증 실패 | 잘못된 비밀번호 로그인 | `401`로 거절 |
| 게시글 | 생성, 수정, 삭제 | 통과 |
| 댓글 | 생성, 삭제 | 통과 |
| 검색 | `keyword=GitHub` | 총 `321`개 |
| 카테고리 필터 | `category=Daily` | 총 `300`개 |
| 태그 검색 | `tag=React` | 총 `139`개 |
| 페이징 | `page=1&size=3` | 총 `435`페이지 |
| RAG 유사글 | `similar-posts` | 후보 `3`개 반환 |
| RAG 초안 | `draft` | 근거 `3`개, 초안 본문 생성 |
| MCP fact check | GitHub 저장소 주장 검증 | `CHECKED`, `supported` |
| Agent 추천 | 놓친 글 추천 | 추천 `5`개, reasoning step `4`개 |

### Browser Smoke Test

| 화면 | 확인 내용 | 결과 |
|---|---|---|
| 로그인 | username/password 기본값이 비어 있음 | 통과 |
| 로그인 | `korean-seed` 계정으로 게시판 진입 | 통과 |
| 메인 | 전체/카테고리별 게시글 개수 표시 | 통과 |
| 메인 | 긴 본문 `펼쳐보기` 버튼 표시 | 통과 |
| 페이징 | `1`, `2`, `435` 형태의 축약 페이지 버튼 | 통과 |
| 글쓰기 모달 | `Write post` 클릭 시 작성 모달 표시 | 통과 |
| RAG 버튼 | `Related posts`, `Draft from sources` 버튼 표시 | 통과 |

### QA 메모

| 항목 | 메모 |
|---|---|
| CSRF | 수동 API 스크립트는 로그인 뒤 mutating request마다 최신 CSRF token을 다시 읽어야 한다. 프론트엔드는 `withCsrf()`에서 처리한다. |
| Vector sync | `/api/ai/vector-store/sync` 같은 운영성 endpoint는 관리자 권한으로 보호된다. 일반/비로그인 요청은 `403`이 맞다. |
| 데모 계정 | `korean-seed / korean-seed-password`로 브라우저 로그인 확인 완료. |

## 실행 환경

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | `docker compose up -d mysql qdrant` | MySQL, Qdrant 컨테이너가 실행된다. |
| [ ] | Backend `.\gradlew.bat bootRun` | `8080` 포트에서 Spring Boot가 실행된다. |
| [ ] | Frontend `npm run dev` | `5173` 포트에서 Vite가 실행된다. |
| [ ] | OpenAI API key 설정 | RAG 유사글 검색과 초안 생성이 실패하지 않는다. |
| [ ] | 관리자 로그인 후 `POST /api/ai/vector-store/sync?limit=2000` | 기존 임베딩이 Qdrant collection에 반영된다. 일반 사용자 호출은 `403`이 맞다. |

## 인증

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | 회원가입 | 새 사용자를 생성하고 로그인 페이지로 이동한다. |
| [ ] | 로그인 | access/refresh cookie가 발급되고 게시판 메인으로 이동한다. |
| [ ] | 새로고침 | 로그인 상태가 유지된다. |
| [ ] | 로그아웃 | 토큰이 삭제되고 로그인 화면으로 이동한다. |
| [ ] | 잘못된 비밀번호 | 오류 메시지가 표시된다. |

## 게시판 기본 기능

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | 게시글 목록 조회 | DB의 게시글이 최신순으로 표시된다. |
| [ ] | 게시글 상세 이동 | 선택한 게시글 상세 화면으로 이동한다. |
| [ ] | 게시글 생성 | 작성한 제목/본문/태그/카테고리가 목록에 반영된다. |
| [ ] | 게시글 수정 | 제목, 본문, 카테고리, 태그가 수정된다. |
| [ ] | 게시글 삭제 | 삭제 후 목록에서 사라진다. |
| [ ] | 댓글 생성 | 상세 화면 댓글 목록에 추가된다. |
| [ ] | 댓글 삭제 | 댓글 목록에서 사라진다. |
| [ ] | 긴 본문 접기 | 5줄 이상 글은 접히고 펼쳐보기로 확인 가능하다. |

## 검색, 필터, 페이징

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | 키워드 검색 | 제목/본문/댓글 기준으로 검색 결과가 바뀐다. |
| [ ] | 카테고리 필터 | 선택한 카테고리 글만 표시된다. |
| [ ] | 태그 검색 | 태그 입력값에 맞는 글이 표시된다. |
| [ ] | Reset | 검색어, 태그, 카테고리가 초기화된다. |
| [ ] | 페이지 이동 | `1`, `현재-1`, `현재`, `현재+1`, `마지막` 형태로 이동한다. |
| [ ] | 카테고리별 개수 | 현재 페이지가 아니라 전체 게시글 기준 개수가 표시된다. |

## RAG

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | 작성 모달에서 제목/본문 입력 | `Related posts`, `Draft from sources` 버튼이 활성화된다. |
| [ ] | 유사 게시글 검색 | 작성 중인 내용과 관련된 게시글이 상위에 표시된다. |
| [ ] | 유사 게시글 클릭 | 추천된 게시글의 내용을 확인할 수 있다. |
| [ ] | 초안 생성 | 유사 게시글을 근거로 초안이 생성된다. |
| [ ] | 근거 부족 케이스 | 억지 생성 대신 근거 부족 메시지 또는 약한 결과가 표시된다. |
| [ ] | Qdrant 미동기화 케이스 | 검색 품질이 이상하면 sync API로 복구 가능하다. |

## MCP

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | GitHub URL이 있는 게시글에서 fact check | GitHub repository summary 도구가 선택된다. |
| [ ] | 지원되는 주장 | `Supported` 판정과 비교 내용이 표시된다. |
| [ ] | 모순되는 주장 | `Contradicted` 또는 수정 제안이 표시된다. |
| [ ] | 근거 부족 주장 | `Insufficient` 또는 `Not supported`가 표시된다. |
| [ ] | 외부 데이터 표시 | 사용한 tool, repository, URL, source가 표시된다. |

## Agent

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | 추천 버튼 클릭 | 놓친 글 5개가 표시된다. |
| [ ] | 읽은 글 제외 | 최근 읽은 글은 추천에서 제외된다. |
| [ ] | 태그/카테고리 기반 추천 | 읽음 기록과 가까운 태그/카테고리 글이 우선된다. |
| [ ] | reasoning steps | observe, infer, retrieve, rank 단계가 응답에 포함된다. |
| [ ] | 읽음 기록 50개 제한 | 사용자별 읽음 기록이 과도하게 늘지 않는다. |

## 문서/제출물

| 체크 | 항목 | 기대 결과 |
|---|---|---|
| [ ] | README | 프로젝트 개요, 기능, 아키텍처, 실행 방법, 한계가 포함된다. |
| [ ] | RAG 보고서 | 평가셋, 지표, online/offline 차이, RAGAS 해석이 포함된다. |
| [ ] | 데모 시나리오 | 발표 순서와 스크린샷이 포함된다. |
| [ ] | 코드 학습 문서 | 초보자가 코드 흐름을 따라갈 수 있다. |
| [ ] | 데모 흐름 | 핵심 기능 3개인 RAG, MCP, Agent를 순서대로 확인할 수 있다. |

## 최종 명령

```powershell
cd C:\Users\cedis\week15_project\backend
.\gradlew.bat test

cd C:\Users\cedis\week15_project\frontend\project-alpha
npm run build
```
