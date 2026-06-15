# 02. Spring Boot 기본 설정: JPA, Security, Validation, Flyway

## 현재 프로젝트 분석

- 기준 파일: `backend/build.gradle.kts`
- 설정 파일: `backend/src/main/resources/application.properties`
- 보안 설정: `backend/src/main/java/com/example/aiknowledgeboard/config/SecurityConfig.java`
- 마이그레이션: `backend/src/main/resources/db/migration/V1__init.sql`
- 현재 Spring Boot 버전은 `3.5.0`, Java 버전은 `21`이다.

## 학습 목표

- [ ] Spring Boot 프로젝트에서 의존성이 기능 단위로 어떤 역할을 하는지 설명할 수 있다.
- [ ] JPA, Security, Validation, Flyway가 왜 기본 설정 단계에 먼저 필요한지 설명할 수 있다.
- [ ] `application.properties`의 환경변수 fallback 구조를 이해한다.
- [ ] 로컬 실행과 Docker 실행에서 DB 접속 설정이 어떻게 달라지는지 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- Spring Boot starter: 필요한 Spring 설정과 라이브러리를 묶어 주는 의존성
https://docs.spring.io/spring-boot/reference/using/build-systems.html?utm_source=chatgpt.com 
(gradle과 maven 빌드를 권장, sprin-boot-start* 에대한 설명) 
게시판 만드릭/docs/공식문서_주제별_한국어_학습본/01_Spring_Boot_Starter_한국어_학습본.pdf
- IoC와 DI: 객체 생성과 의존성 주입을 Spring Container가 담당하는 구조
https://docs.spring.io/spring-framework/reference/core/beans/introduction.html
(Bean에 등록함으로 스프링이 직접 객체를 생성하고 의존성을 관리하도록 만듬) 
https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html
(어노테이션으로 생성자 주입, 관리하는 방식이 아닌 예전의 방식 xml로 생성자 주입의 방식 )
- Spring MVC: `Controller -> Service -> Repository`로 요청을 처리하는 웹 구조
(DispatcherServlet) https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet.html

- JPA와 Hibernate: Java 객체와 DB 테이블을 매핑하는 ORM 기술
flyway를 통해 db의 creaet부분을 hibernate가 entity를 보고 테이블을 관리하지 못하게함 

- Spring Security: 인증과 인가를 필터 체인으로 처리하는 보안 프레임워크
https://www.youtube.com/watch?v=y0PXQgrkb90&list=PLJkjrxxiBSFCKD9TRKDYn7IE96K2u3C3U&index=1
- Bean 등록: `@Configuration`, `@Bean`, `@Component`, `@Service`
- Bean Validation: `@NotBlank`, `@Email`, `@Size` 같은 요청 검증 방식
- Flyway: SQL migration으로 DB 스키마 변경 이력을 관리하는 도구
- 환경 설정: `application.properties`, 환경변수, 기본값 placeholder

## 재구현 체크리스트

### 1. Gradle 프로젝트 기본 구조 만들기

- [v] `settings.gradle.kts`에서 프로젝트 이름을 정한다.
- [v] `build.gradle.kts`에 Spring Boot 플러그인을 추가한다.
- [v] Java toolchain을 21로 고정한다.
- [v] `mavenCentral()` 저장소를 설정한다.

### 2. 핵심 의존성 추가

- [v] `spring-boot-starter-web`을 추가해 REST API를 만들 수 있게 한다.
- [v] `spring-boot-starter-data-jpa`를 추가해 Entity와 Repository를 사용할 수 있게 한다.
- [v] `spring-boot-starter-security`를 추가해 인증/인가 흐름을 구성한다.
- [v] `spring-boot-starter-validation`을 추가해 요청 DTO 검증을 가능하게 한다.
- [v] `flyway-core`, `flyway-database-postgresql`을 추가해 DB 변경 이력을 코드로 관리한다.
- [v] `postgresql` 드라이버를 runtime 의존성으로 추가한다.
- [v] 테스트용으로 `spring-boot-starter-test`, `spring-security-test`를 추가한다.

### 3. application.properties 구성

- [v] `server.port`를 환경변수로 바꿀 수 있게 설정한다.
- [v] `spring.datasource.url`, `username`, `password`에 로컬 기본값과 환경변수 값을 함께 둔다.
- [v] `spring.jpa.hibernate.ddl-auto=none`으로 자동 스키마 생성을 끈다.
- [v] `spring.jpa.open-in-view=false`로 트랜잭션 범위를 명확히 한다.
- [v] `spring.flyway.enabled=true`로 migration 실행을 켠다.
- [] JWT, CORS, OpenAI, GitHub 설정값을 환경변수 기반으로 분리한다.

### 4. Security 기본 뼈대 만들기

- [v] `SecurityConfig` 클래스를 만든다.
- [v] CSRF를 비활성화하는 이유를 정리한다.
- [v] 세션을 사용하지 않도록 `SessionCreationPolicy.STATELESS`를 설정한다.
- [v] `/api/auth/**`는 인증 없이 접근 가능하게 둔다.
- [v] `GET /api/posts/**`는 목록/상세 조회를 위해 공개한다.
- [v] 나머지 쓰기 API는 인증이 필요하도록 설정한다.
- [v] CORS 허용 origin을 환경변수로 관리한다.

## 검증 체크리스트

- [ ] `cd backend && ./gradlew test`를 실행해 기본 테스트가 통과하는지 확인한다.
- [ ] `cd backend && ./gradlew bootJar -x test`로 빌드가 되는지 확인한다.
- [ ] DB가 켜진 상태에서 backend를 실행하면 Flyway가 migration을 적용하는지 확인한다.
- [ ] 인증 없이 `GET /api/posts`는 접근되고, `POST /api/posts`는 막히는지 확인한다.

## WHY 정리 질문

- [ ] 왜 게시판 API 프로젝트에서 `Controller`, `Service`, `Repository` 계층을 나누는가?
- [ ] 왜 JPA 자동 생성 대신 Flyway를 먼저 설정하는가?
- [ ] 왜 JWT 프로젝트에서는 Spring Security 세션을 `STATELESS`로 두는가?
- [ ] 왜 설정값을 코드에 박지 않고 환경변수 fallback 구조로 만드는가?
