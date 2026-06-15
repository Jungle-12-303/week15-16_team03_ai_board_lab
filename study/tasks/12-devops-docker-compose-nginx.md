# 12. DevOps: Dockerfile, docker-compose, Nginx, troubleshooting 문서

## 현재 프로젝트 분석

- Compose: `docker-compose.yml`
- Backend Dockerfile: `backend/Dockerfile`
- Frontend Dockerfile: `frontend/Dockerfile`
- Nginx: `infra/nginx/default.conf`
- 배포 스크립트: `infra/scripts/deploy-ec2.sh`
- CI: `.github/workflows/ci.yml`
- 문서: `docs/트러블슈팅.md`, `README.md`

## 학습 목표

- [ ] 프론트엔드, 백엔드, DB, Nginx가 Docker Compose에서 어떻게 연결되는지 설명할 수 있다.
- [ ] multi-stage Dockerfile을 쓰는 이유를 설명할 수 있다.
- [ ] Nginx가 `/api` 요청과 정적 화면 요청을 나누는 흐름을 이해한다.
- [ ] EC2 배포와 로컬 실행에서 확인해야 할 로그/네트워크 포인트를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- Docker image와 container: 실행 가능한 패키지와 실제 실행 인스턴스의 차이
- Multi-stage build: 빌드 도구가 포함된 이미지와 실행 이미지를 분리하는 Dockerfile 패턴
- Docker Compose service: DB, backend, frontend, nginx를 하나의 로컬 환경으로 묶는 방식
- Compose network: 서비스 이름으로 컨테이너끼리 통신하는 구조
- Volume: DB 데이터를 컨테이너 재생성 후에도 유지하는 저장소
- Healthcheck: DB가 준비된 뒤 backend를 시작하게 만드는 확인 절차
- Environment variable: 비밀값과 실행 환경별 설정을 코드 밖으로 분리하는 방식
- Nginx reverse proxy: `/api`는 backend, `/`는 frontend로 요청을 나누는 앞단 서버
- CI/CD: GitHub Actions로 빌드 성공 여부를 자동 검증하는 흐름
- EC2 배포: 클라우드 VM에서 Docker Compose로 서비스를 실행하는 기본 운영 방식
- Troubleshooting: 장애 증상, 원인, 해결, 예방을 기록하는 운영 문서화 방법

## 재구현 체크리스트

### 1. `docker compose up` 실행 목표부터 잡는다

- [ ] 로컬에서 전체 서비스를 한 명령으로 띄우는 목표를 `docker compose up -d --build`로 정한다.
- [ ] `docker-compose.yml`에 `db`, `backend`, `frontend`, `nginx` 서비스 이름만 먼저 잡는다.
- [ ] `db` 서비스는 `pgvector/pgvector:pg16` 이미지를 사용한다.
- [ ] DB 이름, 사용자, 비밀번호를 환경변수 fallback으로 설정한다.
- [ ] DB 데이터는 named volume에 저장한다.
- [ ] `pg_isready` healthcheck를 둔다.
- [ ] backend가 DB 준비 전 시작하면 실패할 수 있다는 문제를 확인한다.

### 2. backend 서비스를 띄우려는 순간 Backend Dockerfile을 만든다

- [ ] build stage에서 `eclipse-temurin:21-jdk`를 사용한다.
- [ ] Gradle wrapper와 빌드 파일을 먼저 복사한다.
- [ ] `gradlew`에 실행 권한을 준다.
- [ ] `src`를 복사한 뒤 `./gradlew bootJar -x test`를 실행한다.
- [ ] runtime stage에서 `eclipse-temurin:21-jre`를 사용한다.
- [ ] build stage의 jar를 `app.jar`로 복사한다.
- [ ] 8080 포트를 expose한다.
- [ ] `java -jar app.jar`로 실행한다.
- [ ] compose에서 backend가 db healthcheck 성공 후 시작하게 한다.
- [ ] backend 환경변수에 datasource, JWT, OpenAI, GitHub, CORS 값을 전달한다.

### 3. frontend 서비스를 띄우려는 순간 Frontend Dockerfile을 만든다

- [ ] build stage에서 `node:22-alpine`을 사용한다.
- [ ] `package*.json`을 먼저 복사한다.
- [ ] `npm ci`로 lockfile 기반 설치를 한다.
- [ ] 소스 복사 후 `npm run build`를 실행한다.
- [ ] runtime stage에서 `nginx:1.27-alpine`을 사용한다.
- [ ] 빌드된 `dist`를 Nginx html 디렉터리로 복사한다.
- [ ] 80 포트를 expose한다.
- [ ] compose에서 `frontend`가 frontend Dockerfile로 빌드되게 한다.

### 4. 브라우저 진입점이 필요해지는 순간 Nginx reverse proxy를 구성한다

- [ ] compose에서 `nginx`가 backend/frontend에 의존하게 한다.
- [ ] host 80 포트를 nginx에 연결한다.
- [ ] `frontend_upstream`을 `frontend:80`으로 둔다.
- [ ] `backend_upstream`을 `backend:8080`으로 둔다.
- [ ] `/api/` 요청은 backend로 proxy한다.
- [ ] 나머지 `/` 요청은 frontend로 proxy한다.
- [ ] `Host`, `X-Real-IP`, `X-Forwarded-*` 헤더를 넘긴다.
- [ ] 큰 요청을 막기 위해 `client_max_body_size`를 정한다.

### 5. 로컬 실행이 깨지는 순간 troubleshooting 문서를 작성한다

- [ ] DB 연결 실패 시 datasource URL, 계정, 컨테이너 healthcheck를 확인하는 절차를 적는다.
- [ ] Flyway 실패 시 migration 파일과 기존 DB 상태를 확인하는 절차를 적는다.
- [ ] 401/403 실패 시 JWT, Authorization header, Security 설정을 확인하는 절차를 적는다.
- [ ] Nginx 502 실패 시 backend 컨테이너 상태와 upstream 이름을 확인하는 절차를 적는다.
- [ ] 프론트 API 연결 실패 시 `baseURL`, Nginx `/api` proxy, CORS 설정을 확인하는 절차를 적는다.
- [ ] OpenAI/GitHub API 실패 시 fallback 동작과 로그 위치를 확인하는 절차를 적는다.
- [ ] 증상, 원인, 해결, 예방을 같은 형식으로 기록한다.

### 6. 로컬 흐름이 잡힌 뒤 EC2 배포 흐름을 정리한다

- [ ] Ubuntu EC2 인스턴스를 만든다.
- [ ] 보안 그룹에서 22, 80 포트를 연다.
- [ ] Docker와 Docker Compose plugin을 설치한다.
- [ ] 프로젝트를 clone한다.
- [ ] `.env.example`을 참고해 `.env`를 만든다.
- [ ] `docker compose up -d --build`를 실행한다.
- [ ] `docker compose ps`로 서비스 상태를 확인한다.
- [ ] `docker compose logs -f backend`로 백엔드 로그를 확인한다.

### 7. 배포 전에 자동 검증이 필요해지는 순간 CI를 구성한다

- [ ] GitHub Actions에서 Java 21을 설정한다.
- [ ] backend에서 `./gradlew bootJar -x test`를 실행한다.
- [ ] Node 22를 설정한다.
- [ ] npm cache 경로를 `frontend/package-lock.json` 기준으로 잡는다.
- [ ] frontend에서 `npm ci`, `npm run build`를 실행한다.
- [ ] 실패 로그를 보고 로컬에서 같은 명령으로 재현한다.

## 검증 체크리스트

- [ ] `docker compose up -d --build`가 성공하는지 확인한다.
- [ ] `docker compose ps`에서 db/backend/frontend/nginx가 실행 중인지 확인한다.
- [ ] `http://localhost`에서 React 화면이 열리는지 확인한다.
- [ ] `http://localhost/api/posts`가 Nginx를 통해 backend로 전달되는지 확인한다.
- [ ] backend 로그에서 Flyway migration 성공 여부를 확인한다.
- [ ] frontend build가 로컬과 CI에서 모두 성공하는지 확인한다.
- [ ] 문제를 하나 재현하고 `docs/트러블슈팅.md`에 원인/해결/예방을 기록한다.

## WHY 정리 질문

- [ ] 왜 프론트엔드와 백엔드를 하나의 컨테이너에 넣지 않고 분리하는가?
- [ ] 왜 backend는 DB healthcheck 이후 시작해야 하는가?
- [ ] 왜 프론트엔드 컨테이너에도 Nginx가 있고, 앞단에도 Nginx reverse proxy가 있는가?
- [ ] 왜 트러블슈팅 문서는 기능 구현만큼 중요한 제출 자료가 되는가?
