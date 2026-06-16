# Project Alpha 계정 모음집

이 문서는 로컬 MySQL `project_alpha.users` 테이블을 기준으로 만든 테스트/데모 계정 목록이다. 기준 시점은 `2026-06-17` 로컬 DB다.

비밀번호 원문은 DB에 저장되지 않는다. `users.password_hash`에는 BCrypt hash 또는 로컬 개발용 placeholder만 저장되므로, 이 문서에는 비밀번호 해시를 기록하지 않는다.

## 바로 로그인 가능한 데모 계정

| Username | Password | 용도 |
|---|---|---|
| `korean-seed` | `korean-seed-password` | 발표/QA/RAG 평가용 기본 계정 |

`korean-seed` 비밀번호는 seed/import/evaluation script와 데모 문서에서 사용하는 값이다. 다른 계정은 원문 비밀번호를 DB에서 복구할 수 없다.

## 권한 요약

| Role | Count | 비고 |
|---|---:|---|
| `USER` | 23 | 현재 모든 계정이 일반 사용자 |
| `ADMIN` | 0 | 관리자 계정 없음 |

`codex-sync-admin`은 이름만 admin이고 현재 DB role은 `USER`다. 운영성 API를 테스트하려면 `.env`의 `APP_ADMIN_USERNAMES`에 원하는 username을 넣고 백엔드를 재시작해야 한다.

## 전체 계정 목록

| id | username | role | created_at | posts | comments | read_count | 용도 추정 |
|---:|---|---|---|---:|---:|---:|---|
| 1 | `cedis` | USER | 2026-06-08 21:29:55 | 0 | 0 | 0 | 초반 수동/개발 계정 |
| 2 | `authcheck001456` | USER | 2026-06-09 00:14:56 | 0 | 0 | 0 | 인증 테스트 |
| 3 | `jwtcheck003121` | USER | 2026-06-09 00:31:22 | 0 | 0 | 0 | JWT 테스트 |
| 4 | `jwtwriter003148` | USER | 2026-06-09 00:31:48 | 0 | 0 | 0 | JWT 작성 테스트 |
| 5 | `owner003215` | USER | 2026-06-09 00:32:15 | 0 | 0 | 0 | 작성자 권한 테스트 |
| 6 | `other003215` | USER | 2026-06-09 00:32:15 | 0 | 0 | 0 | 다른 사용자 권한 테스트 |
| 7 | `최현진` | USER | 2026-06-09 21:18:44 | 0 | 0 | 12 | 수동 생성 계정 추정 |
| 12 | `external-seed` | USER | 2026-06-11 11:34:17 | 0 | 0 | 0 | 외부 seed 테스트 |
| 13 | `korean-seed` | USER | 2026-06-11 11:39:44 | 1193 | 0 | 7 | 공식 데모/seed/RAG 평가 계정 |
| 14 | `crawler` | USER | 2026-06-12 14:24:18 | 9 | 0 | 0 | 크롤링 글 작성자 |
| 15 | `embedding-runner` | USER | 2026-06-12 14:31:09 | 0 | 0 | 0 | 임베딩 작업용 |
| 16 | `rag-checker` | USER | 2026-06-13 03:28:27 | 0 | 0 | 0 | RAG 확인용 |
| 17 | `hardware-manager` | USER | 2026-06-13 13:57:09 | 100 | 0 | 50 | 하드웨어 추천 시나리오용 |
| 18 | `rag_eval` | USER | 2026-06-13 15:49:16 | 0 | 0 | 0 | RAG 평가용 |
| 19 | `666666` | USER | 2026-06-13 22:46:27 | 0 | 0 | 0 | 수동 생성 계정 추정 |
| 20 | `practice1` | USER | 2026-06-15 11:09:25 | 2 | 0 | 2 | MCP/fact check 연습 |
| 21 | `practice2` | USER | 2026-06-15 11:09:25 | 0 | 0 | 0 | MCP/fact check 연습 |
| 22 | `practice3` | USER | 2026-06-15 11:09:26 | 0 | 0 | 0 | MCP/fact check 연습 |
| 23 | `qa_user_20260615144413` | USER | 2026-06-15 22:44:13 | 2 | 1 | 1 | QA 테스트 |
| 24 | `qa_user_20260615144649` | USER | 2026-06-15 22:46:49 | 2 | 0 | 1 | QA 테스트 |
| 25 | `codex-sync-admin` | USER | 2026-06-16 01:52:37 | 0 | 0 | 0 | 운영성 API 권한 테스트용 이름 |
| 26 | `qa-20260617002804` | USER | 2026-06-17 00:28:04 | 0 | 0 | 0 | QA smoke test |
| 27 | `qa-20260617002833` | USER | 2026-06-17 00:28:33 | 1 | 0 | 5 | QA smoke test |

## 현재 refresh token이 남아 있는 계정

| username | refresh token count | 비고 |
|---|---:|---|
| `korean-seed` | 1 | 브라우저 데모 로그인 흔적 |
| `qa-20260617002804` | 1 | QA smoke test 흔적 |
| `qa-20260617002833` | 1 | QA smoke test 흔적 |

## 계정 분류

| 분류 | 계정 |
|---|---|
| 발표 데모 | `korean-seed` |
| 데이터 seed/import | `korean-seed`, `crawler`, `external-seed` |
| RAG/embedding/evaluation | `embedding-runner`, `rag-checker`, `rag_eval`, `korean-seed` |
| MCP/fact check 연습 | `practice1`, `practice2`, `practice3` |
| Agent 추천 시나리오 | `hardware-manager`, `korean-seed`, `최현진` |
| 인증/JWT 테스트 | `authcheck001456`, `jwtcheck003121`, `jwtwriter003148`, `owner003215`, `other003215` |
| QA smoke test | `qa_user_20260615144413`, `qa_user_20260615144649`, `qa-20260617002804`, `qa-20260617002833` |
| 수동 생성 추정 | `cedis`, `최현진`, `666666` |

## 다시 조회하는 SQL

```sql
SELECT
  u.id,
  u.name,
  u.email,
  u.role,
  u.created_at,
  COUNT(DISTINCT p.id) AS post_count,
  COUNT(DISTINCT c.id) AS comment_count,
  COUNT(DISTINCT pr.id) AS read_count
FROM users u
LEFT JOIN posts p ON p.author_id = u.id
LEFT JOIN comments c ON c.author_id = u.id
LEFT JOIN post_reads pr ON pr.user_id = u.id
GROUP BY u.id, u.name, u.email, u.role, u.created_at
ORDER BY u.id;
```
