# 05. Comment: 댓글 작성/삭제

## 현재 프로젝트 분석

- Entity: `backend/src/main/java/com/example/aiknowledgeboard/comment/Comment.java`
- Repository: `backend/src/main/java/com/example/aiknowledgeboard/comment/CommentRepository.java`
- Controller: `backend/src/main/java/com/example/aiknowledgeboard/comment/CommentController.java`
- Service: `backend/src/main/java/com/example/aiknowledgeboard/comment/CommentService.java`
- 요청/응답: `CommentRequest`, `CommentResponse`
- 현재 댓글 목록은 게시글 상세 응답 안에 포함된다.

## 학습 목표

- [ ] 댓글이 독립 리소스이면서도 게시글에 종속되는 이유를 설명할 수 있다.
- [ ] 댓글 작성 전에 게시글 존재 여부를 확인해야 하는 이유를 설명할 수 있다.
- [ ] 댓글 삭제 권한을 작성자 기준으로 검사하는 흐름을 이해한다.
- [ ] 게시글 삭제 시 댓글을 DB cascade로 정리하는 이유를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- REST nested resource: `/posts/{postId}/comments`처럼 부모 리소스 아래 자식 리소스를 표현하는 방식
- JPA `@ManyToOne`: 여러 댓글이 하나의 게시글과 하나의 작성자를 참조하는 관계
- Foreign key cascade: 부모 데이터 삭제 시 자식 데이터를 함께 정리하는 DB 규칙
- Access control: 현재 사용자와 리소스 작성자를 비교해 권한을 판단하는 방식
- Validation: 빈 댓글이나 너무 긴 댓글을 Controller 진입 시점에 막는 방식
- Entity와 Response DTO 분리: DB 내부 구조를 API 응답과 분리하는 이유
- Repository query method: `findByPostIdOrderByCreatedAtAsc`처럼 메서드 이름으로 쿼리를 만드는 방식
- HTTP status: 생성 성공 `201`, 삭제 성공 `204`, 권한 없음 `403`, 없음 `404`

## 재구현 체크리스트

### 1. `CommentController`에서 댓글 API 입구부터 만든다

- [v] `POST /api/posts/{postId}/comments` endpoint를 먼저 만든다.
- [v] 처음에는 임시 응답으로 요청이 들어오는지 확인한다.
- [v] 댓글 본문을 받기 위해 `CommentRequest`가 필요하다는 것을 확인한다.
- [v] `DELETE /api/comments/{commentId}` endpoint를 추가한다.
- [v] 작성 성공은 `201 Created`, 삭제 성공은 `204 No Content`가 적절한 이유를 확인한다.

### 2. 요청과 응답 모양이 필요해지는 순간 DTO를 만든다

- [v] `CommentRequest`에는 `content`만 둔다.
- [v] `content`는 `@NotBlank`, `@Size(max = 1000)`로 검증한다.
- [v] Controller 요청 DTO에 `@Valid`를 붙인다.
- [v]`CommentResponse`에는 `id`, `authorId`, `authorNickname`, `content`, `createdAt`을 담는다.
- [v] Entity를 그대로 반환하지 않고 Response DTO로 변환한다.

### 3. Controller가 복잡해지는 순간 `CommentService`로 위임한다

- [v] `CommentController`에서 `CommentService.create(postId, request)`를 호출한다.
- [v] `CommentController`에서 `CommentService.delete(commentId)`를 호출한다.
- [v]  `CommentController` 필드와 생성자에 `CommentService` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `CommentService`를 넣어주려면 `CommentService` Bean이 필요하다는 것을 확인한다.
- [v] 그래서 `CommentService`에 `@Service`를 붙인다.
- [v] 댓글 작성 시 현재 로그인 사용자가 필요하다는 것을 확인한다.
- [v] 댓글 작성 시 `postId`로 게시글을 먼저 찾아야 한다는 것을 확인한다.
- [v] 댓글 삭제 시 현재 로그인 사용자와 댓글 작성자 비교가 필요하다는 것을 확인한다.

### 4. 저장이 필요해지는 순간 `Comment`와 `CommentRepository`를 추가한다

- [v] `CommentService`에서 댓글 저장/삭제를 하려다 `CommentRepository`가 필요하다는 것을 확인한다.
- [v] `CommentService` 필드와 생성자에 `CommentRepository` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `CommentRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [v] `Comment`를 `comments` 테이블과 매핑한다.
- [v] `post`를 `Post`와 `ManyToOne`으로 연결한다.
- [v] `author`를 `UserEntity`와 `ManyToOne`으로 연결한다.
- [v] `content`는 `TEXT`로 매핑한다.
- [v] `createdAt`은 `@PrePersist`에서 설정한다.
- [v] 댓글 수정 기능은 요구사항에 없으므로 추가하지 않는다.
- [v] `CommentRepository`가 `JpaRepository<Comment, Long>`를 상속하게 한다.

### 5. `CommentService`에 작성/삭제 규칙을 하나씩 추가한다

- [v] 댓글 작성 시 현재 로그인 사용자를 조회한다.
- [v] 현재 로그인 사용자 조회가 필요해지면 `CommentService` 생성자에 `CurrentUserService`를 추가한다.
- [v] `postId`로 게시글을 먼저 찾는다.
- [v] 게시글 조회가 필요해지면 `CommentService` 생성자에 `PostRepository`를 추가한다.
- [v] 게시글이 없으면 `EntityNotFoundException`을 던진다.
- [v] 댓글 본문은 저장 전에 `trim()`한다.
- [v] 댓글 삭제 시 현재 로그인 사용자를 조회한다.
- [v] 댓글이 없으면 `EntityNotFoundException`을 던진다.
- [v] 댓글 작성자가 아니면 `AccessDeniedException`을 던진다.
- [v] 작성자라면 댓글을 삭제한다.
- [v] 작성/삭제처럼 DB 변경이 있는 메서드에 `@Transactional`을 붙인다.
- [v] `CurrentUserService`와 `PostRepository`도 생성자에 들어왔으므로 둘 다 Spring이 주입 가능한 Bean이어야 함을 확인한다.

### 6. 게시글 상세 화면에서 댓글이 필요해지는 순간 조회 메서드를 추가한다

- [v] 게시글 상세에 사용할 `findByPostIdOrderByCreatedAtAsc`를 만든다.
- [v] 목록 카드에 댓글 수를 보여줄 `countByPostId`를 만든다.
- [v] 게시글 상세 응답에서 댓글 목록을 `CommentResponse`로 변환한다.
- [v] 게시글 삭제 시 댓글이 DB cascade로 함께 정리되는지 확인한다.

## 검증 체크리스트

- [v] 로그인 사용자가 게시글 상세에서 댓글을 작성할 수 있는지 확인한다.
- [v] 존재하지 않는 게시글에 댓글 작성 시 404 응답이 오는지 확인한다.
- [v] 빈 댓글 작성 시 validation 에러가 오는지 확인한다.
- [v] 댓글 작성자만 삭제 버튼이 의미 있게 동작하는지 확인한다.
- [v] 다른 사용자가 댓글 삭제를 시도하면 403 응답이 오는지 확인한다.
- [v] 게시글 삭제 후 해당 댓글들이 DB에 남지 않는지 확인한다.

## WHY 정리 질문

- [v] 왜 댓글 작성 API는 `/api/posts/{postId}/comments`이고 삭제 API는 `/api/comments/{commentId}`인가?
- [v] 왜 댓글 목록 조회 API를 따로 만들지 않고 게시글 상세 응답에 포함했는가?
- [v] 왜 댓글 삭제 권한은 게시글 작성자가 아니라 댓글 작성자 기준인가?

