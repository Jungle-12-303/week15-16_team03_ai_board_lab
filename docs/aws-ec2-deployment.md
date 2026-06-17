# Project Alpha AWS EC2 배포 가이드

이 문서는 Project Alpha를 AWS에 처음 올려보기 위한 최소 배포 절차다. 목표는 운영급 완성형 인프라가 아니라, 발표와 학습을 위해 React, Spring Boot, MySQL, Qdrant가 EC2 한 대에서 함께 동작하는 상태를 만드는 것이다.

현재 로컬 DB에는 게시글 1309개, 전체글 임베딩 1309개, 청크 임베딩 5419개가 들어 있다. 압축한 MySQL dump는 약 54MB라서 EC2로 옮기는 데 부담이 크지 않다. 데이터 이전 절차는 [AWS 데이터 이전 가이드](aws-data-migration.md)에 따로 정리했다.

## 배포 방식 선택

| 방식 | 장점 | 단점 | 이번 선택 |
|---|---|---|---|
| EC2 + Docker Compose | 구조가 단순하고 로컬 환경과 가장 비슷하다. | DB와 Vector DB도 한 서버에 있어 운영 안정성은 낮다. | 선택 |
| EC2 + RDS + Qdrant container | DB 백업/운영성이 좋아진다. | RDS 보안그룹, 비용, 마이그레이션 관리가 추가된다. | 이후 개선 |
| S3/CloudFront + EC2 API | 프론트 배포가 더 정석적이다. | CORS, 쿠키, HTTPS 설정이 더 복잡해진다. | 이후 개선 |
| ECS/Fargate | 컨테이너 운영에 적합하다. | 학습/발표 직전에는 설정량이 크다. | 보류 |

## AWS에서 먼저 할 일

### 1. 비용 경보

AWS Billing Alarm을 먼저 만든다. 과제용 크레딧이 있어도 EC2, EBS, 데이터 전송, CloudWatch alarm 비용이 누적될 수 있다.

권장 기준:

| 경보 | 금액 |
|---|---:|
| 1차 | 10 USD |
| 2차 | 30 USD |
| 3차 | 70 USD |

### 2. EC2 생성

| 항목 | 권장값 |
|---|---|
| Region | `ap-northeast-2` Seoul |
| AMI | Amazon Linux 2023 |
| Instance type | `t3.medium` 이상 권장 |
| Storage | 30GB gp3 이상 |
| Key pair | 새 키 생성 후 안전하게 보관 |

`t3.small`도 시도는 가능하지만, MySQL, Qdrant, Spring Boot, 프론트 컨테이너가 함께 떠야 하므로 메모리 여유가 작다.

### 3. Security Group

| Type | Port | Source | 이유 |
|---|---:|---|---|
| SSH | 22 | 내 IP만 | 서버 접속 |
| HTTP | 80 | `0.0.0.0/0` | 프론트 접속 |
| Custom TCP | 8080 | `0.0.0.0/0` | 백엔드 API 접속 |

이번 최소 배포에서는 프론트가 `http://EC2_PUBLIC_IP`에서 뜨고, API는 `http://EC2_PUBLIC_IP:8080`으로 호출한다. MySQL `3306`, Qdrant `6333`은 외부에 열지 않는다.

## EC2 접속 후 설치

```bash
sudo yum update -y
sudo yum install -y git docker
sudo service docker start
sudo usermod -aG docker ec2-user
```

권한 반영을 위해 SSH를 한 번 종료했다가 다시 접속한다.

Docker Compose plugin 확인:

```bash
docker compose version
```

## 프로젝트 받기

```bash
git clone https://github.com/Jungle-12-303/week15-16_team03_ai_board_lab.git
cd week15-16_team03_ai_board_lab
git checkout HYUNJIN
```

## 환경변수 설정

```bash
cp .env.aws.example .env.aws
nano .env.aws
```

반드시 바꿀 값:

| 변수 | 설명 |
|---|---|
| `MYSQL_PASSWORD` | MySQL 앱 계정 비밀번호 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 비밀번호 |
| `SPRING_DATASOURCE_PASSWORD` | `MYSQL_PASSWORD`와 같은 값 |
| `APP_JWT_SECRET` | 32자 이상 랜덤 문자열 |
| `APP_SECURITY_CORS_ALLOWED_ORIGINS` | `http://EC2_PUBLIC_IP` |
| `FRONTEND_API_BASE_URL` | `http://EC2_PUBLIC_IP:8080` |
| `OPENAI_API_KEY` | OpenAI API key |
| `APP_ADMIN_USERNAMES` | 운영성 API를 실행할 관리자 username |

HTTP IP 배포에서는 cookie secure를 `false`로 둔다.

```properties
APP_SECURITY_PRODUCTION_MODE=false
APP_SECURITY_COOKIE_SECURE=false
APP_SECURITY_COOKIE_SAME_SITE=Lax
```

도메인과 HTTPS를 붙인 뒤에는 아래처럼 바꾼다.

```properties
APP_SECURITY_PRODUCTION_MODE=true
APP_SECURITY_COOKIE_SECURE=true
APP_SECURITY_CORS_ALLOWED_ORIGINS=https://your-domain.example
FRONTEND_API_BASE_URL=https://your-api-domain.example
```

## 실행

```bash
docker compose --env-file .env.aws -f docker-compose.aws.yml up -d --build
```

상태 확인:

```bash
docker compose --env-file .env.aws -f docker-compose.aws.yml ps
docker compose --env-file .env.aws -f docker-compose.aws.yml logs -f backend
```

접속:

| 대상 | URL |
|---|---|
| Frontend | `http://EC2_PUBLIC_IP` |
| Backend health | `http://EC2_PUBLIC_IP:8080/actuator/health` |
| Posts API | `http://EC2_PUBLIC_IP:8080/api/posts?page=0&size=3` |

## 초기 데이터와 임베딩

데이터가 비어 있으면 회원가입 후 직접 게시글을 만들 수 있다. 하지만 현재 프로젝트처럼 로컬에 이미 RAG 평가용 게시글과 임베딩이 만들어져 있다면, AWS에서 다시 크롤링하거나 OpenAI Embedding API를 다시 호출하는 것보다 MySQL dump를 가져가는 편이 낫다.

권장 순서:

1. 로컬 MySQL에서 gzip dump 생성
2. dump 파일을 EC2로 복사
3. AWS MySQL 컨테이너에 import
4. backend/frontend 실행
5. MySQL에 저장된 임베딩을 Qdrant에 재동기화

상세 명령은 [AWS 데이터 이전 가이드](aws-data-migration.md)를 따른다.

기존 seed/import 흐름을 AWS에서도 실행하려면 backend 컨테이너에 명령을 넣는 방식으로 진행한다.

예시:

```bash
docker compose --env-file .env.aws -f docker-compose.aws.yml run --rm --no-deps backend \
  java -jar /app/app.jar --app.corpus-import.enabled=true --app.corpus-import.max-items=20
```

이 방식은 별도 one-off backend 컨테이너를 한 번 띄워 import만 실행하고 종료한다. 대량 import는 OpenAI embedding 비용과 EC2 메모리 사용량이 함께 늘어나므로, 로컬에서 만든 DB dump를 가져오거나 짧게 나눠 실행하는 편이 안전하다.

Qdrant는 MySQL과 별도의 vector index다. MySQL dump를 import한 뒤 Qdrant가 비어 있으면 관리자 권한으로 아래 API를 호출해 재동기화한다.

```bash
POST /api/ai/vector-store/sync?limit=5000
```

이 endpoint는 `ADMIN` 권한이 필요하다. `APP_ADMIN_USERNAMES`에 이미 존재하는 계정을 지정한 뒤 로그인 cookie와 CSRF token을 포함해서 호출한다.

## 배포 후 검증 체크리스트

| 확인 | 명령 또는 URL | 기대 결과 |
|---|---|---|
| 컨테이너 상태 | `docker compose --env-file .env.aws -f docker-compose.aws.yml ps` | `mysql`, `qdrant`, `backend`, `frontend` 실행 |
| Backend health | `http://EC2_PUBLIC_IP:8080/actuator/health` | `UP` |
| Posts API | `http://EC2_PUBLIC_IP:8080/api/posts?page=0&size=3` | 게시글 목록 반환 |
| MySQL row count | `SELECT COUNT(*) FROM posts;` | 로컬 dump 기준 1309개 |
| Qdrant post points | `/collections/project_alpha_posts` | 약 1309개 이상 |
| Qdrant chunk points | `/collections/project_alpha_chunks` | 약 5419개 이상 |
| RAG 유사글 | 프론트 작성 모달 `Related posts` | 관련 게시글 후보 반환 |

## 종료와 삭제

발표 후 비용 방지를 위해 EC2를 중지하거나 삭제한다.

컨테이너만 내리기:

```bash
docker compose --env-file .env.aws -f docker-compose.aws.yml down
```

볼륨까지 삭제하기:

```bash
docker compose --env-file .env.aws -f docker-compose.aws.yml down -v
```

EC2를 삭제하지 않으면 EBS 비용이 계속 발생할 수 있다.

## 이번 배포의 한계

| 한계 | 이유 | 개선 |
|---|---|---|
| DB가 EC2 내부 volume | 서버 삭제 시 데이터 관리가 어렵다. | RDS MySQL 또는 dump 백업 |
| Qdrant가 EC2 내부 volume | vector index 운영 안정성이 낮다. | Qdrant Cloud, ECS, EBS backup |
| HTTP IP 접속 | cookie secure를 true로 둘 수 없다. | Route 53, ACM, HTTPS |
| API 8080 공개 | 최소 배포라 단순하게 열었다. | Nginx reverse proxy 또는 ALB |
| 단일 서버 | 장애 시 전체 서비스 중단 | multi-AZ, managed DB |

## 참고한 공식 문서

- [Amazon EC2 security groups](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/ec2-security-groups.html): EC2 인스턴스의 inbound/outbound traffic을 제어하는 가상 방화벽이다.
- [Installing Docker on Amazon Linux 2023](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-docker.html): Amazon Linux 2023에서는 Docker를 `sudo yum install -y docker`로 설치하고 서비스를 시작할 수 있다.
- [CloudWatch billing alarm](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/monitor_estimated_charges_with_cloudwatch.html): 예상 비용이 기준을 넘을 때 알림을 보내도록 설정할 수 있다.
