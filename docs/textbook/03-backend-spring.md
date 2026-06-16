# 3장. Spring Boot 백엔드

백엔드는 인증, DB 저장, 검색, AI 기능 실행을 담당합니다. Project Alpha는 Controller, Service, Repository, Entity 계층으로 책임을 나눕니다.

## 계층 구조

```text
Controller
-> Service
-> Repository
-> Entity / MySQL
```

| 계층 | 역할 |
| --- | --- |
| Controller | HTTP 요청을 받고 응답 모양을 만든다 |
| Service | 실제 비즈니스 규칙을 처리한다 |
| Repository | DB 조회와 저장을 담당한다 |
| Entity | DB 테이블을 Java 객체로 표현한다 |
| DTO/Response | 프론트로 내려줄 JSON 모양을 정의한다 |

Controller가 DB를 직접 만지지 않는 이유는 책임을 분리하기 위해서입니다. 예를 들어 게시글 생성은 단순히 `posts`만 저장하지 않고, 태그 저장과 임베딩 작업 예약도 함께 처리합니다. 이런 규칙은 Controller보다 Service에 있는 것이 자연스럽습니다.

## 서비스 패키지 분리

`service` 아래를 기능별로 나누었습니다.

| 패키지 | 책임 |
| --- | --- |
| `service/post` | 게시글, 댓글, 태그, 읽음 기록 |
| `service/rag` | 임베딩, 검색, RAG 초안, 평가 |
| `service/mcp` | MCP server, 외부 API 도구, 팩트체크 |
| `service/agent` | 놓친 글 추천 Agent |

기능별 패키지는 파일을 찾는 기준을 제공합니다. 게시판 기본 기능과 AI 응용 기능이 같은 폴더에 섞이지 않아 변경 범위를 좁히기 쉽습니다.

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| 백엔드 시작점 | [NamanmuApplication.java](../../backend/src/main/java/com/jungle_choi/namanmu/NamanmuApplication.java) |
| 인증 API | [AuthController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java), [SecurityConfig.java](../../backend/src/main/java/com/jungle_choi/namanmu/config/SecurityConfig.java) |
| JWT 생성/검증 | [JwtTokenService.java](../../backend/src/main/java/com/jungle_choi/namanmu/security/JwtTokenService.java), [JwtAuthenticationFilter.java](../../backend/src/main/java/com/jungle_choi/namanmu/security/JwtAuthenticationFilter.java) |
| Refresh token rotation | [RefreshTokenService.java](../../backend/src/main/java/com/jungle_choi/namanmu/security/RefreshTokenService.java), [RefreshToken.java](../../backend/src/main/java/com/jungle_choi/namanmu/domain/auth/RefreshToken.java) |
| 로그인 실패 제한 | [LoginAttemptService.java](../../backend/src/main/java/com/jungle_choi/namanmu/security/LoginAttemptService.java), [AuthController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/AuthController.java) |
| 게시글 API | [PostController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/PostController.java), [CommentController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/CommentController.java) |
| 게시글 비즈니스 규칙 | [PostService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/PostService.java), [CommentService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/CommentService.java), [PostReadService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/post/PostReadService.java) |
| Entity와 Repository | [Post.java](../../backend/src/main/java/com/jungle_choi/namanmu/domain/post/Post.java), [PostRepository.java](../../backend/src/main/java/com/jungle_choi/namanmu/domain/post/PostRepository.java), [User.java](../../backend/src/main/java/com/jungle_choi/namanmu/domain/user/User.java) |
| 응답 변환 | [PostResponseMapper.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/mapper/PostResponseMapper.java), [PostResponse.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/dto/PostResponse.java) |
| 예외 처리 | [ApiExceptionHandler.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/ApiExceptionHandler.java) |

## JPA와 Entity

JPA는 Java 객체를 DB 테이블과 연결해줍니다.

예를 들어 `Post` entity는 `posts` 테이블과 대응합니다. `PostRepository`는 `posts` 테이블을 조회하고 저장하는 입구입니다.

현재 로컬 설정은 다음과 같습니다.

```properties
spring.jpa.hibernate.ddl-auto=update
```

이 설정은 Entity 변경을 빠르게 DB에 반영합니다. 동시에 초기 스키마는 Flyway migration 파일로 기록했습니다.

```text
backend/src/main/resources/db/migration/V1__create_project_alpha_schema.sql
```

운영 환경에서는 `ddl-auto=validate`로 바꾸고 Flyway migration을 기준으로 DB 변경 이력을 관리합니다.

## 인증과 Spring Security

인증 흐름은 다음과 같습니다.

```text
POST /api/auth/login
-> AuthController
-> LoginAttemptService
-> UserRepository
-> JwtTokenService
-> httpOnly access token cookie와 refresh token cookie 발급
```

이후 요청은 다음 흐름을 탑니다.

```text
Cookie: project_alpha_access_token=<JWT>
-> JwtAuthenticationFilter
-> JwtTokenService 검증
-> SecurityContext에 User 저장
-> Controller에서 @AuthenticationPrincipal User 사용
```

JWT는 프론트와 백엔드가 분리된 구조에서 API 요청마다 인증 정보를 전달하는 방식입니다. access token은 JWT로 짧게 검증하고, refresh token은 DB에 해시로 저장해 재발급 때마다 회전시킵니다.

쿠키 인증은 브라우저가 자동으로 인증 정보를 붙인다는 장점이 있지만, 그만큼 CSRF 방어가 필요합니다. Project Alpha는 Spring Security의 `CookieCsrfTokenRepository`를 사용하고, React는 `/api/auth/csrf`로 받은 token을 변경 요청 header에 넣습니다.

로그인 실패는 `LoginAttemptService`가 사용자 계정 기준으로 기록합니다. 기본값은 10분 안에 5회 실패하면 5분 동안 로그인을 막는 방식입니다. 현재 구현은 단일 서버 인메모리 방식이므로, 서버를 여러 대로 늘리면 Redis 같은 공유 저장소로 옮겨야 합니다.

프론트의 일반 API 요청은 `authFetch`를 거칩니다. 서버가 access token 만료로 401을 반환하면 프론트는 `/api/auth/refresh`를 한 번 호출하고, 성공하면 원래 API 요청을 한 번 재시도합니다. 이 재시도 횟수를 제한해 refresh 실패가 무한 루프로 이어지지 않게 했습니다.

## 트랜잭션

게시글 생성처럼 여러 DB 작업이 함께 일어나야 하는 기능에는 `@Transactional`을 사용합니다.

예:

```text
PostService.createPost
-> posts 저장
-> tags / post_tags 갱신
-> embedding_jobs 예약
```

중간에 실패하면 전체 작업이 함께 실패해야 데이터가 어긋나지 않습니다.

## 예외 처리

`ApiExceptionHandler`는 validation 실패나 business error를 HTTP 응답으로 바꿉니다. 이렇게 하면 Service는 예외를 던지고, Controller 바깥의 공통 처리기가 응답 형식을 맞출 수 있습니다.

## 백엔드의 적용 범위와 확장 지점

- DB migration 도구가 아직 없습니다.
- access token blocklist나 강제 전체 로그아웃 기능은 아직 없습니다.
- API rate limit이 없습니다.
- Service가 더 커지면 use case 단위 클래스로 더 쪼갤 수 있습니다.
- OpenAI/GitHub/Weather API 장애 대응은 기본 오류 처리와 fallback 메시지 중심입니다.
