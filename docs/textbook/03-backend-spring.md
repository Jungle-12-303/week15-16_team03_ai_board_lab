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

이 설정은 Entity 변경을 빠르게 DB에 반영합니다. 운영 환경에서는 예측하지 못한 스키마 변경을 막기 위해 Flyway나 Liquibase 같은 migration 도구를 사용합니다.

## 인증과 Spring Security

인증 흐름은 다음과 같습니다.

```text
POST /api/auth/login
-> AuthController
-> UserRepository
-> JwtTokenService
-> JWT 반환
```

이후 요청은 다음 흐름을 탑니다.

```text
Cookie: project_alpha_access_token=<JWT>
-> JwtAuthenticationFilter
-> JwtTokenService 검증
-> SecurityContext에 User 저장
-> Controller에서 @AuthenticationPrincipal User 사용
```

JWT는 프론트와 백엔드가 분리된 구조에서 API 요청마다 인증 정보를 전달하는 방식입니다. 현재 구현 범위에는 refresh token과 token rotation이 포함되지 않습니다.

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
- refresh token 전략이 없습니다.
- API rate limit이 없습니다.
- Service가 더 커지면 use case 단위 클래스로 더 쪼갤 수 있습니다.
- OpenAI/GitHub/Weather API 장애 대응은 기본 오류 처리와 fallback 메시지 중심입니다.
