# Board Backend

Java 21, Spring Boot 3.x, Gradle, PostgreSQL을 사용하는 게시판 백엔드 학습용 프로젝트입니다.

오늘 목표는 API 기능 구현이 아니라 다음 3가지를 확인하는 것입니다.

1. Spring Boot 프로젝트가 실행된다.
2. Spring Boot가 Docker PostgreSQL에 연결된다.
3. JPA Entity 기준으로 `users`, `posts`, `comments` 테이블이 생성된다.

아직 구현하지 않는 기능:

- 회원가입
- 로그인
- 게시글 CRUD API
- 댓글 CRUD API
- Spring Security
- JWT
- 프론트엔드

## 사용 기술

- Java 21
- Spring Boot 3.x
- Gradle
- Spring Web
- Spring Data JPA
- PostgreSQL Driver
- Validation
- Lombok
- Docker Compose
- pgvector PostgreSQL 이미지

## PostgreSQL 연결 정보

이 프로젝트는 Docker PostgreSQL 사용을 기준으로 설정되어 있습니다.

```yaml
database: board
username: board_user
password: board_pass
host port: 5433
container port: 5432
```

Spring Boot JDBC URL:

```text
jdbc:postgresql://localhost:5433/board
```

## 포트 설명

Spring Boot는 내 PC 기준으로 `localhost:5433`에 접속합니다.

Docker 컨테이너 내부 PostgreSQL은 `5432` 포트에서 실행됩니다.

`docker-compose.yml`의 아래 설정은 내 PC의 `5433` 포트를 컨테이너의 `5432` 포트로 연결한다는 뜻입니다.

```yaml
ports:
  - "5433:5432"
```

이렇게 설정한 이유는 내 PC에 PostgreSQL이 이미 설치되어 있고 `5432` 포트를 사용 중일 수 있기 때문입니다.

## Docker PostgreSQL 실행 방식

프로젝트 루트인 `C:\fullstack01`에서 실행합니다.

```powershell
docker compose up -d
```

컨테이너 실행 확인:

```powershell
docker ps
```

목록에 `board-postgres`가 보이면 PostgreSQL 컨테이너가 실행 중입니다.

DB 접속:

```powershell
docker exec -it board-postgres psql -U board_user -d board
```

테이블 목록 확인:

```sql
\dt
```

테이블 구조 확인:

```sql
\d users
\d posts
\d comments
```

컨테이너 중지:

```powershell
docker compose down
```

데이터까지 모두 삭제하려면 볼륨도 함께 삭제합니다.

```powershell
docker compose down -v
```

`docker-compose.yml`은 `pgvector/pgvector:pg16` 이미지를 사용합니다.
나중에 RAG 학습을 할 때 PostgreSQL에 vector 확장을 켤 수 있습니다.

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

오늘은 vector 기능을 사용하지 않아도 됩니다.

## 로컬 PostgreSQL 방식

현재 프로젝트는 Docker PostgreSQL 기준입니다.

로컬에 직접 설치한 PostgreSQL을 사용하려면 `application.yml`의 포트를 로컬 PostgreSQL 포트에 맞게 바꿔야 합니다.
대부분 로컬 PostgreSQL은 `5432`를 사용합니다.

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/board
```

하지만 지금은 포트 충돌을 피하기 위해 Docker 방식인 `localhost:5433`을 사용합니다.

## IntelliJ에서 실행하는 방법

1. IntelliJ IDEA에서 `C:\fullstack01` 폴더를 엽니다.
2. Gradle 프로젝트로 인식되면 Gradle 동기화를 실행합니다.
3. `File > Project Structure > Project SDK`가 Java 21인지 확인합니다.
4. `File > Settings > Build, Execution, Deployment > Build Tools > Gradle`에서 Gradle JVM도 Java 21인지 확인합니다.
5. Docker PostgreSQL을 먼저 실행합니다.
6. `src/main/java/com/example/board/BoardApplication.java` 파일을 엽니다.
7. `main` 메서드 옆 실행 버튼을 누릅니다.
8. 브라우저에서 `http://localhost:8080/health`를 확인합니다.

## Java 21 확인 방법

PowerShell에서 아래 명령어를 실행합니다.

```powershell
java -version
javac -version
```

`21`이 보이면 Java 21을 사용 중입니다.
`22` 또는 다른 버전이 보이면 IntelliJ의 Project SDK와 Gradle JVM을 Java 21로 맞춰주세요.

## 실행 순서

아래 순서대로 진행하면 됩니다.

1. Docker PostgreSQL 실행

```powershell
docker compose up -d
```

2. `board-postgres` 컨테이너 실행 확인

```powershell
docker ps
```

3. Docker PostgreSQL DB 접속

```powershell
docker exec -it board-postgres psql -U board_user -d board
```

4. IntelliJ에서 Spring Boot 실행

```text
BoardApplication.java 실행
```

5. 브라우저에서 서버 확인

```text
http://localhost:8080/health
```

정상 응답:

```text
ok
```

6. psql에서 테이블 생성 확인

```sql
\dt
```

Spring Boot가 정상 실행되고 DB 연결에 성공하면 `users`, `posts`, `comments` 테이블이 보여야 합니다.

## 각 파일의 역할

| 파일 | 역할 |
| --- | --- |
| `settings.gradle` | Gradle 프로젝트 이름을 정합니다. |
| `build.gradle` | Java 21, Spring Boot, JPA, PostgreSQL, Validation, Lombok 의존성을 설정합니다. |
| `src/main/resources/application.yml` | 서버 포트와 Docker PostgreSQL 연결 정보를 설정합니다. |
| `docker-compose.yml` | Docker로 PostgreSQL + pgvector DB를 실행합니다. |
| `BoardApplication.java` | Spring Boot 애플리케이션 시작점입니다. JPA Auditing도 켭니다. |
| `BaseTimeEntity.java` | `createdAt`, `updatedAt` 공통 시간을 자동으로 기록합니다. |
| `User.java` | 사용자 테이블인 `users` Entity입니다. |
| `Post.java` | 게시글 테이블인 `posts` Entity입니다. |
| `Comment.java` | 댓글 테이블인 `comments` Entity입니다. |
| `HealthCheckController.java` | 서버 실행 확인용 `/health` API를 제공합니다. |

## Entity 관계 설계 원칙

현재 Entity에는 `ManyToOne`만 만들었습니다.

- User 1명은 Post 여러 개를 작성할 수 있습니다.
- User 1명은 Comment 여러 개를 작성할 수 있습니다.
- Post 1개에는 Comment 여러 개가 달릴 수 있습니다.

지금은 `User.posts`, `User.comments`, `Post.comments` 같은 `OneToMany` 필드는 만들지 않았습니다.

이유는 초반 학습 단계에서 양방향 관계까지 넣으면 연관관계 관리, 순환 참조, JSON 응답 문제까지 같이 배워야 하기 때문입니다.
오늘 목표는 테이블 생성과 기본 관계 확인이므로 `ManyToOne`만 사용합니다.

## 오늘 반드시 이해해야 하는 질문 5개

1. Spring Boot는 왜 `localhost:5433`으로 접속하는가?
2. Docker 컨테이너 내부 PostgreSQL은 왜 `5432`를 사용하는가?
3. `ddl-auto: update`를 켜면 Spring Boot 실행 시 어떤 일이 일어나는가?
4. `@Entity` 클래스가 왜 DB 테이블로 연결되는가?
5. `Post`와 `Comment`가 `User`를 `ManyToOne`으로 참조하는 이유는 무엇인가?

## 커밋 기준

### commit 1

```text
chore: initialize Spring Boot project with PostgreSQL configuration
```

포함 내용:

- `settings.gradle`
- `build.gradle`
- `application.yml`
- `docker-compose.yml`
- `README.md` 기본 실행 가이드
- `HealthCheckController`

### commit 2

```text
feat: add initial board domain entities
```

포함 내용:

- `BaseTimeEntity`
- `User Entity`
- `Post Entity`
- `Comment Entity`
- JPA Auditing 설정
