# 06. Common: 에러 응답, 페이지 응답

## 현재 프로젝트 분석

- 에러 응답: `backend/src/main/java/com/example/aiknowledgeboard/common/ErrorResponse.java`
- 예외 처리: `backend/src/main/java/com/example/aiknowledgeboard/common/GlobalExceptionHandler.java`
- 페이지 응답: `backend/src/main/java/com/example/aiknowledgeboard/common/PageResponse.java`
- 현재 프론트엔드는 `error.response.data.message`를 우선 읽는다.

## 학습 목표

- [ ] API 에러 응답 형식을 통일해야 하는 이유를 설명할 수 있다.
- [ ] Validation, Bad Request, Not Found, Forbidden, Internal Error를 구분할 수 있다.
- [ ] Spring `Page<T>`를 프론트엔드 친화적인 응답으로 바꾸는 이유를 설명할 수 있다.
- [ ] 공통 응답 코드가 각 도메인 코드의 중복을 줄이는 방식을 이해한다.

## 공부할 때 참고해야 할 개념

- `@RestControllerAdvice`: 여러 Controller의 예외를 한 곳에서 처리하는 Spring 기능
- `@ExceptionHandler`: 특정 예외 타입을 HTTP 응답으로 바꾸는 메서드
- `ResponseEntity`: HTTP status와 body를 함께 제어하는 객체
- HTTP status code: 400, 403, 404, 500의 의미
- Bean Validation error: `MethodArgumentNotValidException`이 발생하는 흐름
- Java record: 단순 응답 DTO를 간결하게 표현하는 문법
- Spring Data `Page<T>`: 페이징 결과와 메타데이터를 함께 담는 객체
- API response contract: 프론트엔드와 백엔드가 약속하는 응답 형식
- 예외 추상화: 도메인별 예외를 공통 에러 응답으로 바꾸는 이유

## 재구현 체크리스트

### 1. 실제 API 실패를 먼저 만들어 공통 에러의 필요성을 확인한다

- [ ] 회원가입 중복 이메일처럼 `IllegalArgumentException`이 발생하는 흐름을 확인한다.
- [ ] 없는 게시글 조회처럼 `EntityNotFoundException`이 발생하는 흐름을 확인한다.
- [ ] 남의 게시글 수정처럼 `AccessDeniedException`이 발생하는 흐름을 확인한다.
- [ ] 잘못된 DTO 요청처럼 `MethodArgumentNotValidException`이 발생하는 흐름을 확인한다.
- [ ] Controller마다 `try/catch`를 쓰면 중복이 커진다는 문제를 확인한다.

### 2. `GlobalExceptionHandler`를 만들고 예외를 한 곳에서 받는다

- [ ] `@RestControllerAdvice`를 붙인다.
- [ ] `IllegalArgumentException`을 400으로 처리한다.
- [ ] `EntityNotFoundException`을 404로 처리한다.
- [ ] `AccessDeniedException`을 403으로 처리한다.
- [ ] `MethodArgumentNotValidException`을 400으로 처리한다.
- [ ] 예상하지 못한 `Exception`은 500으로 처리하되 일반 메시지만 반환한다.
- [ ] Controller에서 반복하던 `try/catch`를 제거한다.

### 3. 에러 body가 필요해지는 순간 `ErrorResponse`를 만든다

- [ ] `code`와 `message` 필드를 가진 record를 만든다.
- [ ] 정적 팩토리 메서드 `of(code, message)`를 만든다.
- [ ] 프론트엔드가 표시할 수 있는 사람이 읽는 메시지를 담는다.
- [ ] 내부 예외 stack trace는 응답에 노출하지 않는다.
- [ ] validation field error들을 `field: message` 형태로 모아준다.
- [ ] 에러 `code`와 사용자 표시용 `message`를 분리한다.

### 4. 목록 API를 만들다가 페이징 응답의 필요성을 확인한다

- [ ] `PostService.list()`가 Spring Data `Page<T>`를 반환하는 흐름을 확인한다.
- [ ] 프론트엔드에는 `content`, `page`, `totalPages` 같은 값만 필요하다는 것을 확인한다.
- [ ] `PageResponse`에 `content`, `page`, `size`, `totalElements`, `totalPages` 필드를 둔다.
- [ ] Spring Data `Page<T>`를 받는 `from()` 메서드를 만든다.
- [ ] Entity Page가 아니라 Response DTO Page를 변환 대상으로 삼는다.
- [ ] 프론트엔드 pagination이 필요한 값만 포함한다.

### 5. 도메인 코드에 공통 규칙을 적용한다

- [ ] `PostService.list()`에서 `PageResponse.from()`을 사용한다.
- [ ] 각 Service에서 없는 리소스는 `EntityNotFoundException`으로 통일한다.
- [ ] 권한 문제는 `AccessDeniedException`으로 통일한다.
- [ ] 잘못된 입력이나 중복은 `IllegalArgumentException`으로 통일한다.
- [ ] 프론트엔드 `getErrorMessage()`가 백엔드 `message`를 읽는 흐름을 확인한다.

## 검증 체크리스트

- [ ] 잘못된 회원가입 이메일 요청이 `VALIDATION_ERROR`로 오는지 확인한다.
- [ ] 없는 게시글 조회가 `NOT_FOUND`로 오는지 확인한다.
- [ ] 남의 게시글 수정이 `FORBIDDEN`으로 오는지 확인한다.
- [ ] 중복 회원가입이 `BAD_REQUEST`로 오는지 확인한다.
- [ ] 게시글 목록 응답에 `content`, `page`, `totalPages`가 포함되는지 확인한다.
- [ ] 프론트엔드 `getErrorMessage()`가 백엔드 메시지를 표시하는지 확인한다.

## WHY 정리 질문

- [ ] 왜 각 Controller마다 try/catch를 쓰지 않고 전역 예외 처리로 모았는가?
- [ ] 왜 서버 내부 오류의 상세 내용을 사용자에게 그대로 보여주면 안 되는가?
- [ ] 왜 Spring Data `Page` 객체를 그대로 반환하지 않고 `PageResponse`로 감싸는가?
- [ ] 왜 에러 `code`와 사용자 표시용 `message`를 분리하는가?
