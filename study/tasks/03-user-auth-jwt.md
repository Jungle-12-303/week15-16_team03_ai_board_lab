# 03. User/Auth: 회원가입, 로그인, JWT

## 현재 프로젝트 분석

- Entity: `backend/src/main/java/com/example/aiknowledgeboard/user/UserEntity.java`
- Repository: `backend/src/main/java/com/example/aiknowledgeboard/user/UserRepository.java`
- Controller: `backend/src/main/java/com/example/aiknowledgeboard/auth/AuthController.java`
- Service: `backend/src/main/java/com/example/aiknowledgeboard/auth/AuthService.java`
- JWT: `backend/src/main/java/com/example/aiknowledgeboard/config/JwtTokenProvider.java`
- 인증 필터: `backend/src/main/java/com/example/aiknowledgeboard/config/JwtAuthenticationFilter.java`
- 현재 사용자 조회: `backend/src/main/java/com/example/aiknowledgeboard/auth/CurrentUserService.java`

## 학습 목표

- [ ] 회원가입과 로그인이 왜 다른 흐름인지 설명할 수 있다.
- [ ] 비밀번호를 왜 hash로 저장해야 하는지 설명할 수 있다.
- [ ] JWT가 요청마다 사용자를 식별하는 흐름을 설명할 수 있다.
- [ ] `SecurityContextHolder`에 인증 객체가 들어가는 이유를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- Spring Security filter chain: HTTP 요청이 Controller에 도착하기 전 인증을 처리하는 흐름
- `PasswordEncoder`와 BCrypt: 비밀번호를 복호화 불가능한 hash로 저장하는 방식
- JWT 구조: header, payload, signature
- HMAC 서명: 토큰이 서버에서 발급된 것인지 검증하는 방식
- Stateless 인증: 서버 세션 없이 매 요청의 토큰으로 사용자를 확인하는 방식
- `SecurityContextHolder`: 현재 요청의 인증 사용자 정보를 보관하는 Spring Security 저장소
- DTO와 validation: 요청 데이터를 Entity와 분리하고 검증하는 이유
- Transaction: 회원가입처럼 DB 변경이 있는 작업을 하나의 단위로 묶는 개념
- 인증과 인가 차이: 로그인한 사용자인지 확인하는 것과 권한이 있는지 확인하는 것

## 재구현 체크리스트

### 1. `AuthController`에서 회원가입 입구부터 만든다

- [ ] `POST /api/auth/signup` endpoint를 먼저 만든다.
- [ ] 처음에는 `"signup ok"` 같은 임시 문자열을 반환해 요청이 Controller까지 오는지 확인한다.
- [ ] 요청 본문을 받아야 하므로 `@RequestBody`가 필요하다는 지점을 확인한다.
- [ ] `email`, `nickname`, `password`를 한 덩어리로 받기 위해 `SignupRequest`가 필요하다는 것을 확인한다.
- [ ] 잘못된 요청을 Controller 진입 전에 막기 위해 `@Valid`가 필요하다는 것을 확인한다.

### 2. `SignupRequest`를 만들고 Controller에서 Service로 넘긴다

- [ ] `SignupRequest`에 `email`, `nickname`, `password`를 둔다.
- [ ] `@Email`, `@NotBlank`, `@Size`를 붙여 입력 검증을 추가한다.
- [ ] Controller에서 회원가입 규칙을 직접 처리하지 않고 `AuthService.signup(request)`를 호출한다.
- [ ] `AuthController` 필드와 생성자에 `AuthService` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `AuthService`를 넣어주려면 `AuthService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `AuthService`에 `@Service`를 붙여 Spring이 생성자 주입할 수 있게 한다.

### 3. 저장이 필요해지는 순간 생성자에 `UserRepository`를 추가한다

- [v] `AuthService.signup()`에서 사용자를 저장하려고 `userRepository.save(...)`를 호출해본다.
- [v] `AuthService` 필드와 생성자에 `UserRepository` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `UserRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [v] `UserEntity`를 `users` 테이블과 매핑한다.
- [v] `email`, `passwordHash`, `nickname`, `createdAt` 필드를 둔다.
- [v] JPA 기본 생성자를 `protected`로 둔다.
- [v] 회원가입용 생성자는 필요한 값만 받게 한다.
- [v] `UserRepository`가 `JpaRepository<UserEntity, Long>`를 상속하게 한다.
- [v] 회원가입 중복 검사를 위해 `existsByEmail`을 만든다.
- [v] 로그인 사용자 조회를 위해 `findByEmail`을 만든다.
- [v] Spring Data JPA query method가 Entity 필드 이름을 기준으로 쿼리를 만든다는 것을 확인한다.
- [v] 참고: https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html

### 4. `AuthService.signup`에 회원가입 규칙을 하나씩 추가한다

- [v] email을 `trim().toLowerCase()`로 정규화한다.
- [v] `userRepository.existsByEmail(email)`로 중복 이메일을 확인한다.
- [v] 중복 이메일이면 `IllegalArgumentException`을 던진다.
- [v] 비밀번호를 그대로 저장하면 안 된다는 문제를 확인한다.
- [v] `AuthService` 필드와 생성자에 `PasswordEncoder` 타입을 먼저 추가한다.
- [v] 이때 Spring이 `PasswordEncoder`를 넣어주려면 `PasswordEncoder` Bean이 필요하다는 것을 확인한다.
- [v] 그래서 `SecurityConfig`에 `PasswordEncoder` `@Bean`을 만들고 `BCryptPasswordEncoder`를 반환한다.
- [v] `passwordEncoder.encode(request.password())` 결과만 `passwordHash`로 저장한다.
- [v] 회원가입처럼 DB 변경이 있는 메서드에 `@Transactional`을 붙인다.

### 5. 응답이 필요해지는 순간 `AuthResponse`와 JWT 발급을 추가한다

- [v] 회원가입 성공 후 문자열 대신 응답 DTO가 필요하다는 것을 확인한다.
- [v] `AuthResponse`에 `token`, `userId`, `email`, `nickname`을 담는다.
- [v] `UserEntity`를 그대로 반환하면 `passwordHash`가 노출될 수 있음을 확인한다.
- [ ] `toResponse(user)` 메서드로 `UserEntity -> AuthResponse` 변환을 분리한다.
- [ ] `toResponse(user)`에서 토큰을 만들기 위해 `AuthService` 생성자에 `JwtTokenProvider`를 추가한다.
- [ ] 이때 Spring이 `JwtTokenProvider`를 넣어주려면 `JwtTokenProvider` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `JwtTokenProvider`에 `@Component`를 붙여 Bean으로 등록한다.
- [ ] JWT header에 `alg=HS256`, `typ=JWT`를 넣는다.
- [ ] payload에 `sub=userId`, `email`, `exp`를 넣는다.
- [ ] header와 payload를 Base64 URL-safe 방식으로 인코딩한다.
- [ ] HMAC-SHA256으로 서명한다.
- [ ] 회원가입 성공 시 `jwtTokenProvider.createToken(user.getId(), user.getEmail())`로 토큰을 발급한다.

### 6. 같은 구조로 로그인 흐름을 추가한다

- [ ] `POST /api/auth/login` endpoint를 만든다.
- [ ] `LoginRequest`에 `email`, `password`를 둔다.
- [ ] `@Email`, `@NotBlank`로 로그인 요청을 검증한다.
- [ ] `AuthService.login(request)`를 호출한다.
- [ ] `userRepository.findByEmail(...)`로 사용자를 찾는다.
- [ ] 사용자가 없으면 이메일 존재 여부가 드러나지 않는 실패 메시지를 반환한다.
- [ ] `passwordEncoder.matches(request.password(), user.getPasswordHash())`로 비밀번호를 검증한다.
- [ ] 로그인 성공 시에도 `toResponse(user)`로 JWT와 사용자 정보를 반환한다.
- [ ] 로그인 조회 메서드에는 `@Transactional(readOnly = true)`를 붙인다.

### 7. 보호 API가 필요해지는 순간 인증 필터를 추가한다

- [ ] JWT 토큰 검증 메서드에서 조각 개수, 서명, 만료 시간을 순서대로 확인한다.
- [ ] 서명 비교는 constant-time 방식으로 비교한다.
- [ ] `Authorization` 헤더가 `Bearer `로 시작하는지 확인한다.
- [ ] JWT에서 userId를 파싱한다.
- [ ] userId로 DB 사용자를 조회한다.
- [ ] 사용자가 있으면 `UsernamePasswordAuthenticationToken`을 만든다.
- [ ] 인증 객체를 `SecurityContextHolder`에 저장한다.
- [ ] 토큰이 잘못되면 인증 컨텍스트를 비우고 다음 필터로 넘긴다.

## 검증 체크리스트

- [ ] 회원가입 성공 시 DB에 `password_hash`가 평문이 아닌 값으로 저장되는지 확인한다.
- [ ] 같은 이메일로 회원가입하면 400 응답이 오는지 확인한다.
- [ ] 잘못된 비밀번호로 로그인하면 400 응답이 오는지 확인한다.
- [ ] 로그인 성공 응답에 JWT와 사용자 정보가 함께 오는지 확인한다.
- [ ] JWT 없이 쓰기 API를 호출하면 거부되는지 확인한다.
- [ ] JWT를 붙이면 쓰기 API가 통과하는지 확인한다.

## WHY 정리 질문

- [ ] 왜 로그인 실패 메시지는 이메일 존재 여부를 노출하지 않게 만드는가?
- [ ] 왜 JWT payload에 비밀번호나 민감한 정보를 넣으면 안 되는가?
- [ ] 왜 Controller가 직접 비밀번호 검증을 하지 않고 Service에 맡기는가?
- [ ] 왜 `CurrentUserService`가 있으면 게시글/댓글 서비스 코드가 단순해지는가?
