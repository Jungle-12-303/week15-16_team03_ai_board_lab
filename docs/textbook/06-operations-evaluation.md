# 6장. 운영과 평가

프로젝트는 기능 구현뿐 아니라 실행 방법, 테스트, 평가, 보안, 비용 구조까지 설명할 수 있어야 합니다.

## 로컬 실행

로컬 개발은 세 프로세스를 띄웁니다.

```powershell
docker compose up -d mysql
```

```powershell
cd backend
.\gradlew.bat bootRun
```

```powershell
cd frontend/project-alpha
npm run dev
```

개발 중에는 프론트 개발 서버와 백엔드 API 서버를 따로 띄웁니다. 배포 시에는 React 정적 빌드 파일을 CDN/Nginx/S3 등에 올리거나 백엔드와 분리 배포할 수 있습니다.

## 환경 변수

민감한 값은 Git에 올리지 않습니다.

| 값 | 설명 |
| --- | --- |
| `OPENAI_API_KEY` | OpenAI API 호출 |
| `GITHUB_TOKEN` | GitHub rate limit 완화 |
| `APP_JWT_SECRET` | JWT 서명 |
| `SPRING_DATASOURCE_*` | DB 접속 정보 |

`.env.example`은 필요한 변수 목록을 알려주는 템플릿이고, 실제 `.env`는 커밋하지 않습니다.

## 테스트

백엔드:

```powershell
cd backend
.\gradlew.bat test
```

프론트:

```powershell
cd frontend/project-alpha
npm run build
```

테스트는 리팩터링 후 주요 규칙이 유지되는지 확인하는 안전망입니다.

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| 로컬 DB 실행 | [docker-compose.yml](../../docker-compose.yml) |
| 백엔드 빌드 설정 | [build.gradle](../../backend/build.gradle), [settings.gradle](../../backend/settings.gradle) |
| 백엔드 환경 설정 | [application.properties](../../backend/src/main/resources/application.properties), [test application.properties](../../backend/src/test/resources/application.properties) |
| 프론트 빌드 설정 | [package.json](../../frontend/project-alpha/package.json), [vite.config.js](../../frontend/project-alpha/vite.config.js) |
| 백엔드 테스트 | [backend/src/test/java](../../backend/src/test/java) |
| RAG 검색 평가 | [evaluate-rag-retrieval.mjs](../../scripts/evaluate-rag-retrieval.mjs), [RagEvaluationController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/RagEvaluationController.java), [RagEvaluationService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/RagEvaluationService.java) |
| RAGAS 평가 | [eval/ragas/README.md](../../eval/ragas/README.md), [run_project_alpha_ragas.py](../../eval/ragas/run_project_alpha_ragas.py), [cases.json](../../eval/ragas/cases.json) |
| 아키텍처/DB 문서 | [project-alpha-architecture.png](../project-alpha-architecture.png), [database-schema.md](../database-schema.md), [code-map.md](../code-map.md) |

## RAG 평가를 왜 따로 하나

AI 기능은 "응답이 나왔다"만으로 평가할 수 없습니다. 관련 없는 글을 근거로 삼거나, 근거에 없는 내용을 생성할 수 있습니다.

그래서 두 관점으로 봅니다.

| 관점 | 질문 |
| --- | --- |
| Retrieval | 관련 글을 제대로 찾아왔는가 |
| Generation | 찾은 글을 근거로 충실하게 생성했는가 |

`scripts/evaluate-rag-retrieval.mjs`는 검색 품질을 보고, `eval/ragas`는 생성 결과를 RAGAS 지표로 봅니다.

## AWS 관점

현재 로컬 구조를 AWS에 올린다면 대략 이렇게 나눌 수 있습니다.

| 로컬 구성 | AWS 후보 |
| --- | --- |
| React build | S3 + CloudFront 또는 Amplify |
| Spring Boot | EC2, ECS, Elastic Beanstalk |
| MySQL container | RDS MySQL |
| 환경 변수 | Parameter Store, Secrets Manager |
| 로그 | CloudWatch Logs |
| 파일 저장 | S3 |

RAG를 AWS 서비스 중심으로 바꾸면 Bedrock Knowledge Bases, OpenSearch, Aurora PostgreSQL + pgvector 같은 선택지도 있습니다. 이 프로젝트는 핵심 RAG 흐름을 코드로 추적할 수 있도록 직접 구현한 구조를 유지합니다.

## 비용과 보안

주의할 비용:

- OpenAI embedding/chat 호출
- GitHub API 자체는 무료지만 token 관리 필요
- AWS 배포 시 EC2/RDS/S3/CloudWatch 비용

주의할 보안:

- API key 커밋 금지
- JWT secret 커밋 금지
- CORS 허용 origin 제한
- 회원 비밀번호 hash 저장
- 관리자 기능이 생기면 권한 분리 필요

## 현재 범위 밖의 운영 기능

다음 항목은 현재 구현 범위에 포함되지 않습니다.

- DB migration 도구 없음
- refresh token 없음
- Vector DB 없음
- 검색 index 없음
- 관리자 페이지 없음
- 모니터링/알림 없음
- API rate limit 없음
- RAG 평가 케이스 수 부족

## 다음 개선 우선순위

1. Flyway로 DB migration 도입
2. refresh token 또는 session 전략 보강
3. Vector DB 또는 pgvector 도입 검토
4. RAG 평가 케이스 확장
5. MCP 도구 추가
6. AWS 배포 자동화
7. README 데모 스크린샷과 발표 자료 정리
