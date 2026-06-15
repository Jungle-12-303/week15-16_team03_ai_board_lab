# 04. Post/Tag: 게시글 CRUD, 검색, 태그 정규화

## 현재 프로젝트 분석

- Entity: `backend/src/main/java/com/example/aiknowledgeboard/post/Post.java`
- Repository: `backend/src/main/java/com/example/aiknowledgeboard/post/PostRepository.java`
- Controller: `backend/src/main/java/com/example/aiknowledgeboard/post/PostController.java`
- Service: `backend/src/main/java/com/example/aiknowledgeboard/post/PostService.java`
- Tag Entity: `backend/src/main/java/com/example/aiknowledgeboard/tag/Tag.java`
- Tag Service: `backend/src/main/java/com/example/aiknowledgeboard/tag/TagService.java`
- 현재 특징: 게시글 저장/수정 후 `RagService.indexPost()`를 안전하게 호출한다.

## 학습 목표

- [ ] 게시글 CRUD가 `Controller -> Service -> Repository -> DB`로 흐르는 이유를 설명할 수 있다.
- [ ] 태그 문자열을 정규화한 뒤 중복 없이 저장하는 이유를 설명할 수 있다.
- [ ] 검색 조건이 없을 때, 키워드만 있을 때, 태그만 있을 때, 둘 다 있을 때의 분기 로직을 이해한다.
- [ ] 게시글 저장과 RAG 인덱싱을 왜 느슨하게 연결하는지 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- REST API: `GET`, `POST`, `PUT`, `DELETE`를 리소스 동작에 맞게 쓰는 방식
- Spring Controller: HTTP 요청을 받고 Service를 호출하는 계층
- Service layer: 비즈니스 규칙과 트랜잭션 경계를 담당하는 계층
- Spring Data JPA Repository: 반복적인 CRUD SQL을 인터페이스로 줄이는 방식
- JPA 연관관계: `@ManyToOne`, `@ManyToMany`, `@JoinTable`
- Lazy loading: 필요한 시점에 연관 데이터를 조회하는 JPA 동작
- JPQL `@Query`: 복잡한 검색 조건을 객체 모델 기준으로 작성하는 쿼리
- Pagination: `Pageable`, `Page`, `Sort`
- Java Collection: `Set`과 `LinkedHashSet`의 중복 제거와 순서 보존 차이
- Transaction readOnly: 읽기 작업과 쓰기 작업의 트랜잭션 의도를 구분하는 방법

## 재구현 체크리스트

### 1. `PostController`에서 게시글 API 입구부터 만든다

- [v] `GET /api/posts` endpoint를 먼저 만들고 임시 빈 목록을 반환한다.
- [v] `GET /api/posts/{id}` endpoint를 만들고 임시 상세 응답을 반환한다.
- [v] `POST /api/posts` endpoint를 만들고 로그인 사용자만 작성해야 한다는 요구를 확인한다.
- [v] `PUT /api/posts/{id}`, `DELETE /api/posts/{id}` endpoint를 추가하고 작성자 권한 검사가 필요하다는 지점을 확인한다.
- [v] Controller가 직접 저장/권한/검색을 처리하면 복잡해지므로 `PostService`가 필요하다는 것을 확인한다.
- [v] `PostController` 필드와 생성자에 `PostService` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `PostService`를 넣어주려면 `PostService` Bean이 필요하다는 것을 확인한다.
- [v] 그래서 `PostService`에 `@Service`를 붙인다.

### 2. 요청과 응답 모양이 필요해지는 순간 DTO를 만든다

- [v] `PostRequest`에 `title`, `content`, `tags`를 둔다.
- [v] `title`은 빈 값과 과도한 길이를 막는다.
- [v] `content`는 빈 값을 막는다.
- [v] 목록 화면에 필요한 값만 담는 `PostSummaryResponse`를 만든다.
- [v] 상세 화면에 필요한 값만 담는 `PostDetailResponse`를 만든다.
- [v] 상세 응답에는 댓글 목록과 태그 목록을 함께 담는다.
- [v] Entity를 그대로 API 응답으로 반환하지 않는 이유를 정리한다.
db 내부 구조가 그대로 외부에 노출되기 때문에 dto(data translate object) 객체 형태로 class 또는 record 타입을 통해 반환한다

### 3. 저장이 필요해지는 순간 `Post`와 `PostRepository`를 추가한다

- [v] `PostService`에서 `postRepository.save(...)`, `postRepository.findById(...)`를 호출해본다.
- [v] `PostService` 필드와 생성자에 `PostRepository` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `PostRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [v] `Post`를 `posts` 테이블과 매핑한다.
- [v] `author`를 `UserEntity`와 `ManyToOne`으로 연결한다.
- [v] `title`, `content` 필드를 둔다.
- [v] `createdAt`, `updatedAt`을 `@PrePersist`, `@PreUpdate`로 관리한다.
- [v] 수정 전용 메서드 `update(title, content, tags)`를 만든다.
- [v] `PostRepository`가 `JpaRepository<Post, Long>`를 상속하게 한다.
- [v] 작성 시 현재 로그인 사용자를 작성자로 넣기 위해 `PostService` 생성자에 `CurrentUserService`를 추가한다.
- [ ] 게시글 상세 조회에서 없으면 `EntityNotFoundException`을 던진다.

### 4. 태그 입력을 처리해야 하는 순간 `Tag`와 `TagService`를 추가한다

- [v] `PostService`에서 태그 문자열을 `Tag` 집합으로 바꾸려다 `TagService`가 필요하다는 것을 확인한다.
- [v] `PostService` 필드와 생성자에 `TagService` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `TagService`를 넣어주려면 `TagService` Bean이 필요하다는 것을 확인한다.
- [v] 그래서 `TagService`에 `@Service`를 붙인다.
- [v] `TagService`에서 기존 태그 조회/저장을 하려다 `TagRepository`가 필요하다는 것을 확인한다.
- [v] `TagService` 필드와 생성자에 `TagRepository` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `TagRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [v] `Tag`를 태그 테이블과 매핑한다.
- [v] `Post.tags`를 `ManyToMany`와 `post_tags` 조인 테이블로 연결한다.
- [v] 입력 태그 목록이 `null`이면 빈 집합으로 처리한다.
- [v] 각 태그를 `trim`, `#` 제거, 소문자 변환한다.
- [v] 빈 태그는 버린다.
- [v] `LinkedHashSet`으로 입력 순서를 유지하면서 중복을 제거한다.
- [v] 최대 5개까지만 저장한다.
- [v] 기존 태그는 재사용하고 없는 태그만 새로 저장한다.
- [v] `TagRepository`가 `JpaRepository<Tag, Long>`를 상속하게 한다.

### 5. `PostService`에 CRUD 규칙을 하나씩 추가한다

- [v] 작성, 수정, 삭제처럼 DB 변경이 있는 메서드에 `@Transactional`을 붙인다.
- [v] 목록/상세 조회처럼 읽기만 하는 메서드에는 `@Transactional(readOnly = true)`를 붙인다.
- [v] 수정/삭제 시 작성자 본인인지 확인한다.
- [v] 권한이 없으면 `AccessDeniedException`을 던진다.
- [v] 수정 시 Entity의 `update()` 메서드를 사용해 변경 규칙을 한 곳에 둔다.
- [] 작성/수정 후 RAG 인덱싱이 필요해지면 `PostService` 생성자에 `RagService`를 추가한다.
- [ ] 이때 Spring이 `RagService`를 넣어주려면 `RagService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `RagService`에 `@Service`가 필요하다는 연결을 09번 RAG 문서와 함께 확인한다.
- [ ] RAG 인덱싱 실패가 게시글 저장 실패로 번지지 않게 예외를 격리한다.

### 6. 목록 조회가 필요해지는 순간 검색과 페이징을 추가한다

- [v] 목록 조회에서 page와 size의 범위를 제한한다.
- [v] 키워드와 태그 파라미터를 공백 제거 후 소문자로 정규화한다.
- [v] 조건이 없으면 `findAll(pageable)`을 사용한다.
- [ ] 키워드만 있으면 제목/본문 `like` 검색을 한다.
- [ ] 태그만 있으면 태그명으로 검색한다.
- [ ] 키워드와 태그가 모두 있으면 두 조건을 함께 적용한다.
- [ ] 태그 join 때문에 중복 게시글이 생기지 않도록 `distinct`를 사용한다.

## 검증 체크리스트

- [ ] 로그인 사용자가 게시글을 작성할 수 있는지 확인한다.
- [ ] 비로그인 사용자가 게시글 작성 시 거부되는지 확인한다.
- [ ] 작성자가 아닌 사용자가 수정/삭제하면 403 응답이 오는지 확인한다.
- [ ] `Spring`, ` spring `, `#spring`이 같은 태그로 저장되는지 확인한다.
- [ ] 태그를 5개 넘게 입력하면 5개까지만 저장되는지 확인한다.
- [ ] 키워드 검색, 태그 검색, 키워드+태그 검색이 각각 동작하는지 확인한다.

## WHY 정리 질문

- [ ] 왜 게시글 수정 로직을 Entity의 `update()` 메서드로 모았는가?
- [ ] 왜 태그 정규화에 `Set`이 아니라 `LinkedHashSet`을 사용했는가?
- [ ] 왜 검색 조건 분기를 Service에서 결정하고 Repository는 쿼리만 담당하는가?
- [ ] 왜 AI 인덱싱 실패가 게시글 CRUD를 실패시키면 안 되는가?
