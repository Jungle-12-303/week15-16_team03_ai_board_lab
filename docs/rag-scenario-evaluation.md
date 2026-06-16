# RAG 시나리오 입출력 평가

이 문서는 정량 지표와 별개로 실제 사용자가 넣을 법한 입력에서 어떤 게시글이 추천되고, 초안 생성이 어떤 메시지를 내는지 확인하기 위한 기록이다.

- 생성 시각: 2026-06-16T03:44:55.445Z
- API: `http://localhost:18081`
- Top K: 5
- 초안 생성 포함: 예
- 초안 생성 대상: github-actions-secrets, weather-yongin, weak-source-diary

판정은 보조 기준이다. 실제 판단에서는 각 시나리오의 입력, 추천 제목, 초안 excerpt를 직접 읽는다.

## 요약

| Scenario | Verdict | Relevant in Top5 | Top1 | Reason |
|---|---|---:|---|---|
| GitHub Actions secrets 배포 실패 | pass | 5 | [AB180 Engineering] Github Ops 로 Mono Repo 배포를 더욱 쉽게 | Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다. |
| 7800X3D에서 9800X3D 업그레이드 고민 | pass | 5 | [HW-SCENARIO] CPU 온도와 부스트 클럭 - 업그레이드 전후 체감 | Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다. |
| React useState 폼 상태 관리 | pass | 5 | useState – React | Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다. |
| Spring Redis 세션과 캐시 | pass | 5 | [우아한형제들 기술블로그] 이제 Redis를 멈춰보겠습니다: @CacheEvict 파헤치기 | Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다. |
| 용인 오늘 날씨 브리핑 | partial | 3 | 대구 오늘 날씨는 | 일부 결과는 맞지만 top5 전체 안정성은 부족하다. |
| Airflow 데이터 파이프라인 운영 | pass | 5 | [컬리 기술블로그] 서버리스에서 쿠버네티스로 - Airflow 운영 경험기 - 컬리 기술 블로그 | Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다. |
| 자료가 거의 없는 일상 회고 | partial | 1 | [우아한형제들 기술블로그] [함께 일하기] 온라인 근무와 회고 | 일부 결과는 맞지만 top5 전체 안정성은 부족하다. |

## GitHub Actions secrets 배포 실패

- 기대 의도: GitHub Actions, workflow, secrets, deploy 관련 글이 상위에 와야 한다.
- 기대 주제: GitHub Actions/Secrets/Workflow/Deploy
- 판정: **pass** - Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다.

### 입력

- Category: `Development`
- Title: GitHub Actions 배포가 secrets 때문에 실패함
- Tags: GitHub Actions, CI/CD, Deploy

```text
PR 머지 후 deploy workflow가 실행되지만 AWS_ACCESS_KEY_ID를 읽지 못해서 실패합니다. GitHub Actions secrets 설정과 배포 파이프라인을 정리하고 싶습니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 1562 | 0.916 | Briefing | 4 | [AB180 Engineering] Github Ops 로 Mono Repo 배포를 더욱 쉽게 |
| 2 | 648 | 0.875 | Development | 5 | [Hyperconnect Tech] 모두의 Github Actions (feat. Github Enterprise) 2편 - 공용 CI 머신에서 Secret 관리하기 |
| 3 | 526 | 0.824 | Project | 4 | GitHub Actions 문서 - GitHub 문서 |
| 4 | 646 | 0.82 | Development | 4 | [Hyperconnect Tech] 모두의 Github Actions (feat. Github Enterprise) 1편 - 모두가 쓸 수 있는 패턴 만들기 |
| 5 | 652 | 0.796 | Development | 4 | [Hyperconnect Tech] 모두의 Github Actions (feat. Github Enterprise) 3편 - Build Cache |

### 초안 생성 출력

- Message: 직접 관련성이 높은 유사 게시글 3개를 근거로 초안을 생성했습니다.
- Sources: `[AB180 Engineering] Github Ops 로 Mono Repo 배포를 더욱 쉽게`, `[Hyperconnect Tech] 모두의 Github Actions (feat. Github Enterprise) 2편 - 공용 CI 머신에서 Secret 관리하기`, `GitHub Actions 문서 - GitHub 문서`

```text
PR 머지 후 자동으로 실행되는 deploy workflow에서 AWS_ACCESS_KEY_ID를 읽지 못해 배포가 실패하는 문제가 발생하고 있습니다. 현재 GitHub Actions의 secrets 설정과 배포 파이프라인 구성을 다시 점검하고 정리할 필요가 있어 이 글을 작성합니다. 우선, GitHub Actions에서 사용되는 AWS_ACCESS_KEY_ID와 같은 민감 정보는 반드시 GitHub 리포지토리의 Secrets에 등록되어야 합니다. 코드는 물론 workflow 파일 내에도 직접 노출되어서는 안 되며, secrets에 등록된 값은 실행 시 자동으로 암호화되어 보안이 유지됩니다. 따라서 배포 작업에 필요한 모든 토큰과 크리덴셜 정보를 GitHub Secrets에 올바르게 등록했는지 확인해야 합니다. 다음으로, workflow 파일 내에서 secrets를 참조하는 구문이 정확한지 점검해야 합니다. 예를 들어, AWS_ACCESS_KEY_ID는 `${{ secrets.AWS_ACCESS_KEY_ID }}` 형태로 사용해야 하며, 오타나 잘못된 변수명이 없는지 검토해야 합니다. 또한, workflow가 실행되는 브랜치나 이벤트 조건에 따라 secret...
```

## 7800X3D에서 9800X3D 업그레이드 고민

- 기대 의도: CPU 업그레이드, 온도, 부스트 클럭, 하드웨어 체감 글이 상위에 와야 한다.
- 기대 주제: CPU/7800X3D/9800X3D/업그레이드
- 판정: **pass** - Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다.

### 입력

- Category: `Daily`
- Title: 7800X3D에서 9800X3D로 갈지 고민
- Tags: CPU, Hardware, Upgrade

```text
현재 7800X3D를 사용 중인데 9800X3D로 업그레이드할지, 아니면 다음 세대를 기다릴지 고민하고 있습니다. 온도와 부스트 클럭, 실제 체감 차이가 궁금합니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 1606 | 0.833 | Daily | 3 | [HW-SCENARIO] CPU 온도와 부스트 클럭 - 업그레이드 전후 체감 |
| 2 | 1686 | 0.746 | Daily | 2 | [HW-SCENARIO] CPU 온도와 부스트 클럭 - 다음 교체 우선순위 |
| 3 | 1607 | 0.73 | Daily | 2 | [HW-SCENARIO] GPU 팬 소음과 언더볼팅 - 업그레이드 전후 체감 |
| 4 | 1596 | 0.721 | Daily | 2 | [HW-SCENARIO] CPU 온도와 부스트 클럭 - 구매 전 체크리스트 |
| 5 | 1636 | 0.709 | Daily | 2 | [HW-SCENARIO] CPU 온도와 부스트 클럭 - 발열과 소음 관찰 |

## React useState 폼 상태 관리

- 기대 의도: React, hook, useState, 프론트엔드 상태 관리 글이 상위에 와야 한다.
- 기대 주제: React/useState/hook/state
- 판정: **pass** - Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다.

### 입력

- Category: `Learning`
- Title: React에서 useState로 글쓰기 폼 상태 관리하기
- Tags: React, useState, Form

```text
제목, 내용, 태그 input을 useState로 관리하고 submit 시 API 요청을 보내는 흐름을 공부하고 있습니다. 컴포넌트 분리와 custom hook도 같이 보고 싶습니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 522 | 0.894 | Development | 4 | useState – React |
| 2 | 718 | 0.794 | Learning | 4 | [우아한형제들 기술블로그] Frontend |
| 3 | 803 | 0.793 | Learning | 3 | [우아한형제들 기술블로그] 코드와 함께 살펴보는 프론트엔드 단위 테스트 – Part 2. 실전 편 |
| 4 | 521 | 0.792 | Development | 4 | 빠르게 시작하기 – React |
| 5 | 806 | 0.757 | Learning | 2 | [우아한형제들 기술블로그] 프론트엔드 개발자들의 즐거운 상상 🎈 – 쓰면글림체 사이드 프로젝트 이야기 |

## Spring Redis 세션과 캐시

- 기대 의도: Redis, Spring Session, cache, 분산 락 관련 글이 상위에 와야 한다.
- 기대 주제: Redis/Spring Session/Cache
- 판정: **pass** - Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다.

### 입력

- Category: `Development`
- Title: Spring Boot에서 Redis 세션과 캐시를 안정적으로 쓰기
- Tags: Spring Boot, Redis, Cache

```text
Spring Session을 Redis에 저장하고 API 캐시도 Redis로 관리하려고 합니다. connection 증가, TTL, 캐시 삭제, 분산 락 이슈를 정리하고 싶습니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 674 | 0.794 | Development | 3 | [우아한형제들 기술블로그] 이제 Redis를 멈춰보겠습니다: @CacheEvict 파헤치기 |
| 2 | 695 | 0.793 | Development | 5 | [Hyperconnect Tech] Spring Session + Custom Session Repository 기반 세션 저장소의 메모리 누수 해결 |
| 3 | 552 | 0.791 | Development | 4 | [Hyperconnect Tech] Spring Session 으로의 마이그레이션 작업기 |
| 4 | 595 | 0.79 | Development | 4 | [우아한형제들 기술블로그] Redis New Connection 증가 이슈 돌아보기 |
| 5 | 580 | 0.764 | Development | 4 | [우아한형제들 기술블로그] Spring Cache(@Cacheable) + Spring Data Redis 사용 시 record 직렬화 오류 원인과 해결 |

## 용인 오늘 날씨 브리핑

- 기대 의도: 날씨라는 대주제는 맞아야 하고, 지역까지 맞으면 더 좋다.
- 기대 주제: 날씨 + 용인
- 판정: **partial** - 일부 결과는 맞지만 top5 전체 안정성은 부족하다.

### 입력

- Category: `Briefing`
- Title: 용인 오늘 날씨 브리핑
- Tags: Weather, Briefing, 용인

```text
오늘 용인 날씨를 바탕으로 외출 전 참고할 짧은 게시글을 쓰고 싶습니다. 비가 오는지, 체감온도와 옷차림은 어떤지 정리하고 싶습니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 520 | 0.875 | Learning | 1 | 대구 오늘 날씨는 |
| 2 | 1551 | 0.715 | Briefing | 1 | [컬리 기술블로그] 딜리버리 프로덕트 개발팀의 개발문화 - 로그 & 알람편 - 컬리 기술 블로그 |
| 3 | 511 | 0.706 | Briefing | 0 | [Dev Sisters Tech] 데브시스터즈 엔지니어링 데이 - Infra/SRE 돌아보기 |
| 4 | 510 | 0.703 | Briefing | 0 | [Dev Sisters Tech] 데브시스터즈 엔지니어링 데이 - Data 돌아보기 |
| 5 | 1484 | 0.697 | Briefing | 2 | [AB180 Engineering] 동일한 비용으로 리포트 요청 10배 처리하기 |

### 초안 생성 출력

- Message: 초안 생성에 사용할 만큼 직접적인 유사 게시글이 없습니다. Related posts에서 후보를 먼저 확인해 주세요.
- Sources: None

```text
(초안 없음)
```

## Airflow 데이터 파이프라인 운영

- 기대 의도: Airflow, ELT, 로그 파이프라인, 데이터 플랫폼 관련 글이 상위에 와야 한다.
- 기대 주제: Airflow/ELT/Data pipeline
- 판정: **pass** - Top1이 기대 주제와 강하게 맞고 top5 중 관련 후보가 5개다.

### 입력

- Category: `Project`
- Title: Airflow로 로그 데이터 파이프라인 운영하기
- Tags: Airflow, Data Pipeline, ELT

```text
Airflow DAG로 로그 수집과 ELT 파이프라인을 운영하고 있습니다. 실패 재시도, 스케줄링, 데이터 품질 체크, 분석 플랫폼 연동 사례를 찾고 싶습니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 1595 | 0.84 | Development | 4 | [컬리 기술블로그] 서버리스에서 쿠버네티스로 - Airflow 운영 경험기 - 컬리 기술 블로그 |
| 2 | 1463 | 0.832 | Briefing | 4 | [쏘카 기술블로그] 쏘카 데이터 그룹 - Airflow와 함께한 데이터 환경 구축기(feat. Airflow on Kubernetes) |
| 3 | 1285 | 0.819 | Review | 4 | [뱅크샐러드 기술블로그] 데이터 분석가가 직접 정의, 배포, 관리하는 뱅크샐러드 데이터 파이프라인 |
| 4 | 1577 | 0.815 | Briefing | 4 | [쏘카 기술블로그] 전사 구성원들이 사용하는 배치 데이터 플랫폼 만들기 - Airflow Advanced |
| 5 | 1232 | 0.784 | Daily | 4 | [쏘카 기술블로그] 로그 파이프라인 개선기 - 기존 파이프라인 문제 정의 및 해결 방안 적용 |

## 자료가 거의 없는 일상 회고

- 기대 의도: 회고/일상뿐 아니라 운동/수면/컨디션까지 직접 맞는 근거가 있는지 확인한다.
- 기대 주제: 회고/일상/운동/컨디션
- 판정: **partial** - 일부 결과는 맞지만 top5 전체 안정성은 부족하다.

### 입력

- Category: `Daily`
- Title: 퇴근 후 운동 루틴을 다시 잡아보려는 회고
- Tags: Daily, Review, Health

```text
요즘 밤에 늦게 자고 운동을 빼먹어서 컨디션이 무너졌습니다. 다음 주에는 퇴근 후 30분만이라도 가볍게 운동하고 수면 시간을 고정해보려고 합니다.
```

### 유사 게시글 출력

| Rank | Post ID | Score | Category | Matched Groups | Title |
|---:|---:|---:|---|---:|---|
| 1 | 918 | 0.773 | Project | 1 | [우아한형제들 기술블로그] [함께 일하기] 온라인 근무와 회고 |
| 2 | 1135 | 0.764 | Daily | 1 | [우아한형제들 기술블로그] 우아한테크캠프 인턴들의 8월의 마지막 회고 |
| 3 | 924 | 0.697 | Project | 2 | [우아한형제들 기술블로그] 우테코에서 찾은 나만의 효과적인 공부법 |
| 4 | 1070 | 0.691 | Daily | 1 | [우아한형제들 기술블로그] 우아한테크캠프 – 8월의 일기 |
| 5 | 1134 | 0.682 | Daily | 1 | [우아한형제들 기술블로그] 2020 우아한테크캠프 3기 8월의 일기 |

### 초안 생성 출력

- Message: 초안 생성에 사용할 만큼 직접적인 유사 게시글이 없습니다. Related posts에서 후보를 먼저 확인해 주세요.
- Sources: None

```text
(초안 없음)
```

