# Project Alpha AI 비용 추정

이 문서는 Project Alpha에서 OpenAI API 비용이 발생하는 지점과, 현재 데이터 규모에서 예상되는 비용을 정리한다. 목적은 정확한 청구서를 재현하는 것이 아니라, 제출 문서와 운영 판단에 사용할 수 있는 비용 모델을 남기는 것이다.

가격은 2026-06-17 확인 기준이며, 실제 과금은 OpenAI billing dashboard와 사용 시점의 공식 가격을 기준으로 확인해야 한다.

## 1. 사용 모델과 공식 단가

| 용도 | 모델 | 공식 단가 | Project Alpha 사용 위치 |
|---|---|---:|---|
| 텍스트 생성 | `gpt-4.1-mini` | input `$0.40 / 1M tokens`, cached input `$0.10 / 1M tokens`, output `$1.60 / 1M tokens` | RAG 초안 생성, Agent 추천 요약 |
| 임베딩 | `text-embedding-3-small` | `$0.02 / 1M tokens` | 게시글/청크 임베딩, 작성 중인 글 query 임베딩 |

공식 출처:

- [OpenAI gpt-4.1-mini model pricing](https://developers.openai.com/api/docs/models/gpt-4.1-mini)
- [OpenAI text-embedding-3-small model pricing](https://developers.openai.com/api/docs/models/text-embedding-3-small)
- [OpenAI API pricing](https://openai.com/api/pricing/)

## 2. 비용이 발생하는 기능

| 기능 | OpenAI 호출 | 비용 성격 | 비고 |
|---|---|---|---|
| 게시글 생성/수정 후 임베딩 작업 | `text-embedding-3-small` | 저장 시 비동기 비용 | 게시글 전체 임베딩 1개 + 청크 임베딩 N개 |
| `Related posts` | `text-embedding-3-small` | 버튼 클릭당 query 임베딩 비용 | 검색 자체는 Qdrant/MySQL에서 수행 |
| `Draft from sources` | `text-embedding-3-small` + `gpt-4.1-mini` | query 임베딩 + 초안 생성 비용 | 근거 게시글은 최대 3개, 각 excerpt 900자 제한 |
| `Recommend 5 posts` | `gpt-4.1-mini` | 추천 요약 생성 비용 | 후보 선정은 MySQL 점수화, 마지막 설명만 LLM 사용 |
| MCP fact check | 없음 | OpenAI 토큰 비용 없음 | GitHub/날씨 API를 직접 호출하고 deterministic 비교 수행 |

MCP fact check는 현재 LLM을 호출하지 않는다. 따라서 GitHub/날씨 도구 호출은 OpenAI 토큰 비용이 아니라 외부 API quota와 서버 트래픽 비용으로 본다.

## 3. 현재 AWS 데이터 규모

2026-06-17 AWS 배포 검증 기준이다.

| 항목 | 값 |
|---|---:|
| 전체 게시글 row | 1312 |
| 공개 게시글 | 1303 |
| 삭제 게시글 | 9 |
| 게시글 전체 임베딩 벡터 | 1312 |
| 청크 임베딩 벡터 | 5422 |
| 임베딩 완료 job | 2514 |
| 실패 job | 0 |
| 공개 게시글 제목+본문+카테고리 문자 수 | 약 4,874,951자 |
| 저장된 청크 텍스트 문자 수 | 약 5,464,276자 |

현재 벡터 상태는 `post_embeddings`와 `post_embedding_chunks` 기준으로 게시글 전체 벡터와 청크 벡터가 모두 만들어져 있다. 삭제된 QA 게시글도 임베딩 row는 남아 있으므로, 벡터 수는 공개 게시글 수보다 조금 크다.

## 4. 현재 코퍼스 임베딩 비용 추정

임베딩 비용 공식:

```text
embedding_cost = input_tokens / 1,000,000 * 0.02 USD
```

정확한 토큰 수는 tokenizer와 실제 프롬프트 문자열에 따라 달라진다. 현재 문서는 AWS DB의 문자 수를 기준으로 범위를 잡았다.

| 추정 기준 | 계산 | 예상 비용 |
|---|---:|---:|
| 낙관 추정: 1 token ~= 3 chars | 10.34M chars / 3 = 3.45M tokens | 약 `$0.07` |
| 보수 추정: 1 token ~= 1.5 chars | 10.34M chars / 1.5 = 6.89M tokens | 약 `$0.14` |
| 라벨/태그/프롬프트 구성 오버헤드 포함 | 위 범위에 여유분 반영 | 대략 `$0.08 ~ $0.20` |

결론적으로 현재 1300개 규모의 게시글과 5400개 규모의 청크 임베딩을 한 번 재구축하는 비용은 OpenAI 임베딩 단가 기준으로 매우 작다. 비용 리스크는 임베딩 1회 재구축보다, 같은 데이터를 여러 번 재크롤링/재임베딩하거나 초안 생성을 반복 실행하는 쪽에서 더 커진다.

## 5. 사용자 기능별 1회 비용 예시

아래는 제출 문서와 운영 설명에 사용할 rough estimate다.

| 기능 | 가정 | 예상 비용 |
|---|---|---:|
| 유사글 검색 1회 | query 100~600 tokens 임베딩 | 약 `$0.000002 ~ $0.000012` |
| RAG 초안 생성 1회 | query 임베딩 + chat input 2k~6k tokens + output 0.5k~1.2k tokens | 약 `$0.0016 ~ $0.0044` |
| Agent 추천 1회 | chat input 1k~3k tokens + output 0.3k~0.8k tokens | 약 `$0.0009 ~ $0.0025` |
| 게시글 1개 저장 후 임베딩 | 전체글 1개 + 청크 1~3개, 총 0.6k~2k tokens | 약 `$0.000012 ~ $0.00004` |
| MCP fact check 1회 | OpenAI 호출 없음 | `$0` |

`$100` API credit 기준으로 단순 환산하면, RAG 초안 생성은 대략 수만 회 단위까지 가능하다. 실제 한도는 OpenAI rate limit, 네트워크, 사용자가 동시에 누르는 빈도, 서버 사양이 함께 결정한다.

## 6. 실제 사용자 기준 월 비용 시나리오

실제 서비스 비용은 전체 가입자 수보다 `하루에 몇 명이 AI 기능을 누르는가`와 `그 사용자가 어떤 AI 버튼을 몇 번 누르는가`에 의해 결정된다. 게시글 목록 조회, 상세 조회, 댓글, 일반 검색, 카테고리/태그 필터는 OpenAI를 호출하지 않는다.

월 비용 공식은 다음처럼 잡을 수 있다.

```text
monthly_ai_cost
= DAU * 30
  * (
      related_posts_per_user_day * related_posts_cost
    + rag_draft_per_user_day * rag_draft_cost
    + agent_recommend_per_user_day * agent_cost
    + saved_posts_per_user_day * post_embedding_cost
    )
```

현재 문서의 1회 비용 추정 범위를 그대로 사용하면 다음과 같다.

| 비용 항목 | 1회 추정 비용 |
|---|---:|
| `Related posts` | `$0.000002 ~ $0.000012` |
| `Draft from sources` | `$0.0016 ~ $0.0044` |
| `Recommend 5 posts` | `$0.0009 ~ $0.0025` |
| 게시글 저장 후 임베딩 | `$0.000012 ~ $0.00004` |
| MCP fact check | `$0` |

### 사용자 규모별 월 비용

| 시나리오 | 가정 | 사용자 1명/일 AI 비용 | 월 예상 비용 |
|---|---|---:|---:|
| 개인/데모 사용 | DAU 1명, Related 5회, Draft 3회, Agent 2회, 글 저장 2회 | `$0.0066 ~ $0.0184` | `$0.20 ~ $0.55` |
| 소규모 팀 | DAU 10명, Related 3회, Draft 1회, Agent 1회, 글 저장 1회 | `$0.0025 ~ $0.0070` | `$0.76 ~ $2.09` |
| 교육반/스터디 | DAU 50명, Related 5회, Draft 2회, Agent 1회, 글 저장 2회 | `$0.0041 ~ $0.0114` | `$6.20 ~ $17.16` |
| 작은 커뮤니티 | DAU 200명, Related 8회, Draft 3회, Agent 2회, 글 저장 2회 | `$0.0066 ~ $0.0184` | `$39.84 ~ $110.26` |
| 무거운 사용 | DAU 1000명, Related 10회, Draft 5회, Agent 3회, 글 저장 3회 | `$0.0108 ~ $0.0297` | `$322.68 ~ $892.20` |

### 해석

| 관찰 | 의미 |
|---|---|
| 비용 대부분은 `Draft from sources`에서 발생한다. | 임베딩 검색보다 LLM 초안 생성이 더 비싸다. |
| 게시글 저장 후 임베딩 비용은 매우 작다. | 글을 많이 저장하는 것보다 초안 생성을 반복하는 것이 비용에 더 큰 영향을 준다. |
| MCP fact check는 OpenAI 비용이 없다. | 다만 GitHub/날씨 API quota와 서버 트래픽은 별도로 봐야 한다. |
| `$100` credit은 소규모 팀/교육반 사용에는 충분하다. | DAU 50명 수준의 교육반 시나리오에서도 월 `$6.20 ~ $17.16` 추정이다. |
| DAU 200명 이상에서 비용 관리가 중요해진다. | rate limit, 사용자별 사용량 제한, draft 호출 제한이 필요하다. |

### 실제 운영에서 추가할 비용 관리

현재는 요청 rate limit과 근거량 제한으로 비용을 제어한다. 운영 서비스라면 다음 정책을 추가하는 것이 좋다.

| 정책 | 예시 |
|---|---|
| 사용자별 일일 AI quota | 무료 사용자는 RAG draft 하루 5회, 로그인 사용자 하루 20회 |
| 기능별 quota 분리 | Related posts는 넉넉하게, Draft from sources는 엄격하게 제한 |
| 팀/관리자별 월 budget | 팀별 월 `$10`, `$30`, `$100` 단위로 hard limit |
| usage log 저장 | feature, model, input/output tokens, estimated cost 기록 |
| 캐시 | 같은 제목/본문/태그 조합의 related query embedding 재사용 |
| batch/off-peak 처리 | 대량 임베딩은 즉시 처리하지 않고 batch로 실행 |

결론적으로 Project Alpha의 현재 구조에서 실제 사용 비용을 좌우하는 것은 "게시글 수"보다 "초안 생성 버튼을 얼마나 자주 누르는가"다. 따라서 운영 비용 관리의 핵심은 `Draft from sources` 호출 횟수 제한과 usage logging이다.

## 7. 비용을 낮추기 위해 적용한 설계

| 설계 | 비용 절감 효과 |
|---|---|
| `text-embedding-3-small` 사용 | 임베딩 비용을 낮게 유지 |
| 임베딩 job 비동기 처리 | 글 저장 요청에서 즉시 대량 API 호출을 몰아치지 않음 |
| 게시글 전체 + 청크 임베딩 저장 | 매 검색마다 전체 게시글을 다시 임베딩하지 않음 |
| Qdrant 벡터 저장소 사용 | 벡터 후보 검색은 OpenAI가 아니라 자체 Vector DB에서 수행 |
| RAG 초안 근거 최대 3개 제한 | chat input token 폭증 방지 |
| 근거 본문 excerpt 900자 제한 | 긴 게시글 전체를 프롬프트에 넣지 않음 |
| AI 요청 rate limit | 사용자-facing AI 요청을 기본 1분 20회로 제한 |
| 관리자 endpoint 보호 | Qdrant sync, embedding job 대량 실행 같은 운영 API를 일반 사용자가 호출하지 못하게 함 |

## 8. 요약 문장

Project Alpha는 OpenAI 비용이 큰 모델 추론보다 임베딩과 짧은 초안 생성 중심으로 발생한다. 현재 1300개 게시글과 5400개 청크 벡터를 재구축하는 임베딩 비용은 대략 `$0.08~$0.20` 수준으로 추정되며, 실제 비용 관리는 근거 개수 제한, excerpt 제한, rate limit, 관리자 endpoint 보호로 통제했다.

실제 사용자 기준으로 보면 비용 대부분은 `Draft from sources`에서 발생한다. 소규모 팀이나 교육반 수준에서는 월 수 달러에서 수십 달러 수준으로 추정되지만, DAU 200명 이상에서는 사용자별 quota, 기능별 rate limit, usage log 기반 budget 관리가 필요하다.

## 9. 남은 개선

현재 코드는 OpenAI 응답의 usage를 영구 저장하지 않는다. 운영 서비스로 확장한다면 다음 테이블을 추가하는 것이 좋다.

| 필드 | 설명 |
|---|---|
| `feature` | `rag_similar`, `rag_draft`, `agent_summary`, `embedding_job` |
| `model` | 사용 모델 |
| `input_tokens` | 입력 토큰 |
| `output_tokens` | 출력 토큰 |
| `estimated_cost_usd` | 호출 당시 단가로 계산한 비용 |
| `user_id` | 사용자별 사용량 추적 |
| `created_at` | 호출 시각 |

이렇게 하면 기능별 비용, 사용자별 비용, 일별 비용을 대시보드로 볼 수 있다.
