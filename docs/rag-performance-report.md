# Project Alpha RAG 검색 성능 보고

이 문서는 Project Alpha의 RAG 유사 게시글 검색 기능을 개선하면서 적용한 검색 조합과 성능 지표를 정리한 문서다.

목표는 단순히 점수를 높이는 것이 아니라, 글 작성 중 사용자가 참고할 만한 게시글을 상위에 안정적으로 추천하고, 그 추천 결과를 AI 초안 생성의 근거로 사용하는 것이다.

전체 측정 로그와 반복 실행값은 별도 문서인 [rag-measurement-inventory.md](rag-measurement-inventory.md)에 정리했다.

정량 점수와 실제 출력 품질을 함께 확인하기 위해, 대표 입력과 실제 추천 제목/초안 일부를 보는 [rag-scenario-evaluation.md](rag-scenario-evaluation.md)도 별도로 둔다.

## 0. 핵심 결론

Project Alpha의 RAG 검색은 `Qdrant Vector DB + MySQL 원본 조회 + BM25 + RRF + 한국어 형태소 분석 + 청크 근거 보강` 구조로 정리했다. 최종 운영 설정은 온라인 API 경로에서 `Precision@5 = 0.8667`, `MRR@5 = 1.0000`, `NDCG@5 = 0.9076`, `Hit@5 = 1.0000`을 기록했다.

| 질문 | 결론 |
|---|---|
| 어떤 RAG 기능인가 | 사용자가 글을 작성할 때 기존 게시글 중 유사한 글을 찾고, 그 글을 근거로 AI 초안을 생성한다. |
| 최종 검색 조합은 무엇인가 | `TITLE_CONTENT` query, Qdrant 후보 검색, BM25/RRF 재정렬, query term `12/6`, chunk evidence `3%`, metadata enabled |
| 왜 vector-only를 쓰지 않았나 | 의미 검색은 고유명사, 기술명, 짧은 쿼리에서 흔들렸다. `GitHub Actions`, `Redis`, `CPU` 같은 단어 신호를 BM25로 보강했다. |
| 왜 BM25만 쓰지 않았나 | 단어가 직접 겹치지 않아도 의미적으로 가까운 글을 찾기 위해 embedding/vector search가 필요했다. |
| 왜 Qdrant를 썼나 | MySQL은 원본 데이터를 관리하고, Qdrant는 임베딩 벡터 후보 검색을 담당하게 역할을 분리했다. |
| 왜 오프라인/온라인 실험을 나눴나 | 오프라인은 많은 조합을 빠르게 줄이는 실험이고, 온라인은 실제 Spring API, OpenAI, Qdrant, MySQL 경로를 모두 태우는 채택 검증이다. |
| 최종 판단 기준은 무엇인가 | 최종 채택은 온라인 후보 검증과 실제 시나리오 입출력 평가를 기준으로 한다. |

## 0-1. 읽는 순서

초보자는 0장, 1장, 3장, 5장, 8장, 9장, 10장을 먼저 읽으면 전체 구조를 잡을 수 있다. 실험 근거를 확인하려면 6장과 [rag-measurement-inventory.md](rag-measurement-inventory.md)를 보면 된다.

| 독자 | 먼저 볼 부분 | 이유 |
|---|---|---|
| 처음 RAG를 보는 사람 | 0장, 8장, 10장 | 기능 목적, 전체 흐름, 용어를 먼저 잡는다. |
| 구현 코드를 읽는 사람 | 1장, 3장, 8장, 9장 | 어떤 구성요소가 어떤 역할을 하는지 확인한다. |
| 실험 결과를 검토하는 사람 | 2장, 5장, 6장 | 평가 방식, 지표 해석, 단계별 점수 변화를 확인한다. |
| 발표 자료를 만드는 사람 | 0장, 3장, 9장, 11장 | 핵심 결론, 최종 점수, 재현 방법, 팀 공유 요약을 사용한다. |

## 1. 평가 대상

| 항목 | 내용 |
|---|---|
| 평가 기능 | RAG 기반 유사 게시글 검색 |
| 사용 위치 | 글 작성 모달의 `Related posts`, `Draft from sources` |
| 검색 대상 | 게시글 전체 임베딩 + 게시글 청크 임베딩 |
| 최종 검색 구조 | Qdrant 후보 검색 + MySQL 원본 조회 + BM25/RRF/키워드/청크 근거 재정렬 |
| 임베딩 모델 | `text-embedding-3-small` |
| 게시글 벡터 수 | 1307 |
| 청크 벡터 수 | 5417 |
| 평가 케이스 수 | 최신 온라인 후보 검증 6개, 기존 Spring 내부 평가 5개 |
| 측정 기준 | `Precision@5`, `Recall@5`, `MRR@5`, `NDCG@5`, `Hit@5`, 시나리오별 실제 입출력 |

### 평가셋 구성

평가셋은 한 종류만 사용하지 않았다. 검색 품질, 시나리오 품질, 생성 품질은 보는 지점이 다르기 때문이다.

| 평가셋 | 케이스 수 | 라벨/판정 방식 | 사용 목적 | 주요 출처 |
|---|---:|---|---|---|
| 최신 온라인 검색 평가 | 6 | 평가 케이스별 관련 게시글 id 목록 | 최종 검색 조합 채택 | `scripts/evaluate-rag-retrieval.mjs`, `backend/build/rag-current-retrieval-evaluation.json` |
| 온라인 후보 검증 | 6 | 같은 6개 평가 케이스로 후보 설정별 비교 | 설정 조합 선택 | `scripts/evaluate-rag-online-candidates.mjs`, `backend/build/rag-online-candidate-evaluation.json` |
| 기존 Spring 내부 평가 | 5 | Spring 내부 평가 서비스의 관련도 기준 | 과거 기준선과 참고값 보관 | `backend/build/rag-evaluation-k5.json` |
| 시나리오 입출력 평가 | 7 | 실제 입력, 추천 제목, 초안 일부를 사람이 판정 | 숫자 지표가 놓치는 UX 확인 | `docs/rag-scenario-evaluation.md` |
| RAGAS 생성 평가 | 5 | `faithfulness`, `answer_relevancy`, `context_precision`, `context_recall` | 생성 답변의 근거성 점검 | `eval/ragas/output/ragas-scores-*.csv` |

### 지표 우선순위

이 기능은 검색 엔진 전체가 아니라 글 작성 화면에서 참고할 상위 3~5개 게시글을 보여주는 기능이다. 그래서 `관련 글 전체를 얼마나 많이 회수했는가`보다 `상위에 바로 쓸 만한 글이 있는가`를 더 중요하게 본다.

| 우선순위 | 지표 | 이 프로젝트에서의 의미 | 판단 기준 |
|---:|---|---|---|
| 1 | `MRR@5` | 첫 번째 관련 글이 몇 번째에 나오는가 | 작성 화면에서는 첫 추천의 품질이 가장 중요하다. |
| 2 | `Hit@5` | 상위 5개 안에 관련 글이 하나라도 있는가 | 사용자가 참고할 출발점을 얻는지 확인한다. |
| 3 | `NDCG@5` | 관련 글이 상위에 잘 배치됐는가 | 같은 top5라도 관련 글이 위에 몰릴수록 좋다. |
| 4 | `Precision@5` | 상위 5개 중 관련 글 비율 | 추천 목록의 노이즈를 본다. |
| 5 | `Recall@5` | 전체 관련 글 중 top5에 들어온 비율 | 관련 글이 많은 주제에서는 구조적으로 낮게 나온다. |

## 2. 평가 방식 구분

이 보고서에서는 `오프라인`, `온라인`, `과거 온라인 기록`, `시나리오 입출력 평가`를 구분해서 사용한다. 네 가지는 모두 RAG 품질을 보는 방법이지만 목적과 신뢰도가 다르다.

| 구분 | 의미 | 사용 목적 | 최종 채택 근거 여부 |
|---|---|---|---|
| 오프라인 실험 | 서버를 새로 띄우지 않고 DB/저장된 임베딩/스크립트 계산으로 후보를 비교한 실험 | 많은 조합을 빠르게 걸러내는 후보 선별 | 채택 전 온라인 검증 필요 |
| 온라인 실험 | 실제 Spring API를 띄우고 `/api/ai/similar-posts`를 호출해 OpenAI, Qdrant, MySQL, 서버 랭킹 경로를 모두 태운 실험 | 현재 서비스 경로에서 후보를 검증 | 최종 채택의 주 근거 |
| 과거 온라인 기록 | 과거 코드 상태에서 실제 API로 측정했지만, 현재 구조와 검색 경로가 달라진 기록 | 개선 흐름과 의사결정 맥락 설명 | 현재 후보와 직접 비교하지 않는다 |
| 시나리오 입출력 평가 | 실제 사용자가 넣을 법한 입력을 넣고 추천 제목과 초안 일부를 사람이 읽어보는 평가 | 정량 점수가 놓치는 UX 문제 확인 | 온라인 점수의 보조 근거 |

이렇게 나누는 이유는 오프라인 점수가 높은 후보가 실제 서비스 결과와 일치하지 않는 경우가 있기 때문이다. 오프라인 실험은 빠르고 저렴하지만 Qdrant 후보 제한, metadata 완화, API 입력 정규화, 서버 내부 랭킹의 상호작용을 동일하게 재현하지 않는다. 따라서 이 프로젝트에서는 다음 순서로 판단했다.

```text
오프라인 실험
-> 온라인 검증 대상 압축
-> 실제 Spring API 온라인 실험
-> 시나리오 입출력 확인
-> 최종 채택/폐기
```

현재 최종 채택의 직접 기준은 `온라인 후보 검증`과 `최종 설정 재측정`이다.

이 문서 안에서 각 실험은 다음처럼 분류한다.

| 문서 위치 | 평가 종류 | 설명 |
|---|---|---|
| 3장 최종 측정 결과 | 온라인 실험 | 채택 설정을 실제 API 경로로 재측정한 최종값 |
| 4장 기존 Spring 내부 5케이스 참고 결과 | 과거/참고 온라인 기록 | 예전 평가셋으로 남아 있는 참고값 |
| 6장 주요 실험 흐름 | 혼합 기록 | 과거 온라인 기록, 현재 구조 확인, 최종 온라인 검증이 함께 들어간 흐름표 |
| 6장 청크 크기 12종 오프라인 전수 실험 | 오프라인 실험 | 청크 크기를 바꿨을 때 텍스트 근거 검색이 유리한지 보는 후보 선별 |
| 6장 검색 파라미터 오프라인 Ablation | 오프라인 실험 | 여러 검색 파라미터 후보를 빠르게 줄이기 위한 계산 실험 |
| 6장 현재 온라인 기준점 | 온라인 실험 | 현재 구조에서 후보 실험 전 기준선을 잡은 값 |
| 6장 실제 온라인 후보 검증 | 온라인 실험 | 최종 채택 후보를 실제 API 경로로 비교한 값 |
| 별도 문서 `rag-scenario-evaluation.md` | 시나리오 입출력 평가 | 숫자 지표가 포착하지 못하는 실제 추천 제목과 초안 품질 확인 |

## 3. 최종 측정 결과

현재 채택한 운영 설정은 온라인 후보 검증에서 가장 높은 점수를 기록한 `Combined tuned` 조합이다. 이 실험은 실제 Spring Boot 서버를 후보 설정별로 띄우고, `/api/ai/similar-posts`를 호출해 Qdrant, MySQL, OpenAI 쿼리 임베딩 경로를 모두 태운 결과다.

후보 비교 결과 파일: `backend/build/rag-online-candidate-evaluation.json`

최종 설정 재측정 파일: `backend/build/rag-current-retrieval-evaluation.json`

| Metric | Score |
|---|---:|
| Precision@5 | 0.8667 |
| Recall@5 | 0.4957 |
| MRR@5 | 1.0000 |
| NDCG@5 | 0.9076 |
| Hit@5 | 1.0000 |

채택한 설정은 다음과 같다.

| 설정 | 값 |
|---|---|
| query mode | `TITLE_CONTENT` |
| chunk evidence weight | `0.03` |
| BM25 k1 / b | `2.0 / 0.25` |
| query terms / guard terms | `12 / 6` |
| fusion mode | `RRF` 유지 |
| metadata | enabled 유지 |

최종 설정을 기본값으로 반영한 뒤 같은 평가 스크립트를 다시 실행했을 때도 같은 평균값이 나왔다. 따라서 위 점수는 현재 애플리케이션 기본 설정에서 재현되는 값이다.

## 4. 기존 Spring 내부 5케이스 참고 결과

아래 표는 `backend/build/rag-evaluation-k5.json`에 남아 있는 기존 Spring 내부 평가 결과다. 최신 최종값은 3장의 온라인 후보 검증 결과를 기준으로 본다.

| Case | Relevant Total | Retrieved | Precision@5 | Recall@5 | MRR@5 | NDCG@5 | Hit@5 | Top1 |
|---|---:|---:|---:|---:|---:|---:|---:|---|
| `hardware-cpu-upgrade` | 1 | 5 | 0.20 | 1.00 | 1.00 | 1.00 | 1.00 | `[HW-SCENARIO] CPU 온도와 부스트 클럭 - 업그레이드 전후 체감` |
| `hardware-gpu-noise` | 28 | 5 | 1.00 | 0.18 | 1.00 | 1.00 | 1.00 | `[HW-SCENARIO] GPU 팬 소음과 언더볼팅 - 발열과 소음 관찰` |
| `github-actions-deploy` | 49 | 5 | 1.00 | 0.10 | 1.00 | 1.00 | 1.00 | `[AB180 Engineering] Github Ops 로 Mono Repo 배포를 더욱 쉽게` |
| `react-state-hooks` | 73 | 5 | 0.80 | 0.05 | 1.00 | 0.87 | 1.00 | `useState - React` |
| `weather-briefing` | 3 | 5 | 0.20 | 0.33 | 1.00 | 0.47 | 1.00 | `대구 오늘 날씨는` |

## 5. 결과 해석

### 강점

- `MRR@5 = 1.0`, `Hit@5 = 1.0`이다.
- 모든 평가 케이스에서 첫 번째 추천 결과가 관련 글이었다.
- 글 작성 UX 기준으로는 "맨 위 추천 글이 쓸만한가"가 중요하므로, top1 품질은 현재 꽤 안정적이다.
- 최신 6케이스 재측정에서는 `RAG/LLM 서비스 설계`, `Redis 캐시와 세션` 케이스가 `Precision@5 = 1.0`으로 top5 전체가 관련 글이었다.

### 낮은 지표와 해석

- `Recall@5 = 0.4957`로 Precision, MRR, NDCG보다 낮다.
- 다만 `RAG/LLM 서비스 설계`는 관련 글이 11개, `Kubernetes/EKS 운영`은 16개, `React 프론트엔드 개발`과 `Airflow 데이터 파이프라인`은 각각 9개다.
- `Recall@5`는 정답 후보 전체 중 top5 안에 들어온 비율이므로, 관련 글이 많은 케이스에서는 구조적으로 낮게 나온다.
- 따라서 이 기능에서는 `Recall@5` 단독보다 `MRR@5`, `NDCG@5`, `Hit@5`를 함께 봐야 한다.

### 현재 판단

현재 최종 조합은 "상위 추천의 품질" 기준을 충족한다. "관련 글 전체를 넓게 회수하는 능력"은 개선 대상이다.

서비스 UX가 `유사글 3~5개 추천`이라면 현재 구조는 실사용 기준을 만족한다. 반대로 `관련 글 전체 탐색`이나 `지식 검색 엔진`에 가까운 기능으로 확장하려면 Recall 개선이 필요하다.

## 6. 단계별 측정 기록

아래 표는 `scripts/evaluate-rag-retrieval.mjs`와 최종 평가 JSON에 남아 있는 단계별 측정 결과다.

이 표는 서로 다른 시점의 실험 기록을 함께 보여준다. 과거 6케이스 로그, 현재 구조 재검사, 온라인 후보 검증, 기존 5케이스 Spring 내부 평가가 섞여 있으므로 같은 실험군끼리 비교한다. 현재 최종 성능은 3장의 `rag-current-retrieval-evaluation.json` 결과를 기준으로 본다.

또한 모든 행이 최종 구조에 누적 적용된 것은 아니다. 최종 구조로 이어진 흐름과 폐기한 분기 실험을 분리해서 본다.

### 주요 실험 흐름

| 단계 | 검색 구조 | 상태 | 평가 기준 | Hit@5 | Recall@5 | Precision@5 | MRR@5 | NDCG@5 |
|---|---|---|---|---:|---:|---:|---:|---:|
| 최초 기준 | Vector 검색 중심 | 기준선 | 6 cases | 0.8333 | 0.4200 | 0.7000 | 0.8333 | 0.7370 |
| 알고리즘 개선판 | Metadata Filter + BM25 + Vector + RRF | 채택 흐름 | 6 cases | 1.0000 | 0.5320 | 0.9333 | 1.0000 | 0.9538 |
| 청크 일부 적용 버그 상태 | 청크 후보만 보게 되어 후보 누락 | 버그/복구 | 6 cases | 0.3333 | 0.0289 | 0.0667 | 0.3333 | 0.1216 |
| 전량 청크 단독 검색 | 전체 글 대신 청크 중심 검색 | 폐기 | 6 cases | 1.0000 | 0.4646 | 0.8000 | 0.8889 | 0.8154 |
| 청크 + 전체글 결합 5% | Post-level + Chunk evidence boost 5% | 조정 중 | 6 cases | 1.0000 | 0.5169 | 0.9000 | 1.0000 | 0.9294 |
| 청크 + 전체글 결합 1% | Post-level primary + Chunk evidence boost 1% | 채택 흐름 | 6 cases | 1.0000 | 0.5320 | 0.9333 | 1.0000 | 0.9538 |
| 한국어 조사 정규화 | 조사 정규화 + 제목 중심 guard 개선 | 채택 흐름 | 6 cases | 1.0000 | 0.5070 | 0.8667 | 0.9167 | 0.8729 |
| 짧은 쿼리 최종 튜닝 | `깃허브`, `용인 날씨` 예시를 고려한 soft signal | 과거 온라인 기록 | 6 cases | 1.0000 | 0.5070 | 0.8667 | 1.0000 | 0.9365 |
| Lucene Nori + Qdrant 재검사 | Nori 형태소 분석기와 Qdrant가 포함된 현재 파이프라인 | 현재 구조 확인 | 6 cases | 1.0000 | 0.4621 | 0.8000 | 1.0000 | 0.8599 |
| 온라인 후보 검증 최종 채택 | `TITLE_CONTENT` + Chunk 3% + BM25 `2.0/0.25` + query `12/6` + RRF | 최종 채택 | 6 cases | 1.0000 | 0.4957 | 0.8667 | 1.0000 | 0.9076 |
| 기존 Spring 내부 평가 | 현재 코드 기준 5케이스 평가 | 참고 | 5 cases | 1.0000 | 0.3337 | 0.6400 | 1.0000 | 0.8676 |

`채택 흐름`은 해당 시점의 점수를 그대로 최종 고정했다는 뜻이 아니라, 이후 구조의 기반으로 유지한 방향이라는 뜻이다. `과거 온라인 기록`은 당시 실행 중이던 서버/API로 측정한 값이다. 이후 Nori 형태소 분석기, Qdrant, 검색 설정 파라미터화가 들어가면서 현재 구조와 검색 경로가 달라졌으므로 현재 후보 검증 표와 분리해서 읽는다.

특히 `한국어 조사 정규화`는 직전 `청크 + 전체글 결합 1%`보다 점수가 내려갔지만 채택 흐름으로 남겼다. 이유는 다음과 같다.

| 변경 | 점수상 손해 | 그래도 유지한 이유 |
|---|---|---|
| 한국어 조사 정규화 | `NDCG@5 0.9538 -> 0.8729`, `Precision@5 0.9333 -> 0.8667` | 한국어 서비스에서는 `으로`, `했습니다`, `정리하고`, `찾고` 같은 표현이 검색 핵심어로 남으면 실제 입력에서 오탐이 늘어난다. |
| 제목 중심 guard 개선 | `MRR@5 1.0000 -> 0.9167` | 단순 점수보다 제목/핵심어가 사용자의 의도와 맞는지가 중요했다. 높은 점수인데 제목상 어긋나는 결과를 줄이기 위한 기반 변경이었다. |
| 정량 점수보다 실제 출력 확인으로 전환 | 일부 평균 지표 하락 | `깃허브`, `용인 날씨`처럼 짧고 실제적인 입력에서 반환 제목을 직접 확인하는 방향으로 평가 기준을 바꿨다. |

즉 이 구간의 선택 기준은 "6케이스 평균 점수를 가장 높게 유지"가 아니라, "한국어 게시판에서 실제 사용자가 넣을 표현을 더 안정적으로 처리"하는 것이었다. 이후 `짧은 쿼리 최종 튜닝`에서 `MRR@5`는 다시 1.0000으로 회복됐다. 이 값은 과거 코드 상태의 성능 기록이며, 현재 최종 채택안과 같은 후보군에서 나온 경쟁 결과가 아니다.

### 폐기한 분기 실험

| 실험 | 적용 내용 | 평가 기준 | Hit@5 | Recall@5 | Precision@5 | MRR@5 | NDCG@5 | 최종 반영 여부 |
|---|---|---|---:|---:|---:|---:|---:|---|
| 과도한 score cutoff | 최종 score 0.99 미만 제거 | 6 cases | 0.5000 | 0.0701 | 0.1000 | 0.5000 | 0.5000 | 폐기 |

`0.99 cutoff`는 후속 단계에 누적 적용된 구조가 아니다. 한 번 적용해보고 평가가 급락해서 폐기한 실험이다. 현재 검색 코드는 `0.99` 기준을 사용하지 않고, 더 완화된 최소 랭킹 기준과 keyword/metadata/chunk signal을 함께 사용한다.

이전 단계별 실험의 결론은 다음과 같다.

- 초기 vector-only보다 BM25, metadata, RRF를 섞은 알고리즘 개선판에서 상위 추천 품질이 크게 좋아졌다.
- 청크를 단독 검색기로 쓰면 글 전체의 주제 맥락이 약해져 성능이 떨어졌다.
- 그래서 최종 방향은 게시글 전체 임베딩을 주 검색기로 쓰고, 청크는 근거 보강용으로 약하게 반영하는 방식이 되었다.
- `0.99 cutoff`처럼 점수만 강하게 자르는 방식은 오히려 성능을 크게 떨어뜨렸다.
- 같은 과거 6케이스 실험군 안에서는 `청크 + 전체글 결합 1%`와 짧은 쿼리 soft guard 계열이 좋은 균형을 보였다.
- Lucene Nori는 위 표의 `Lucene Nori + Qdrant 재검사` 단계에서 현재 파이프라인에 포함된 상태로 확인했다.

### 청크 크기 기준선

현재 청킹은 퍼센트 기준이 아니라 문자 수와 문단 기준이다. 구현 위치는 `PostChunkTextSplitter`이며 기본값은 `maxChunkLength = 1200`, `overlapLength = 180`이다.

청크 크기 후보를 정량적으로 비교하기 위해 현재 DB의 게시글 본문 길이를 먼저 확인했다.

| 항목 | 값 |
|---|---:|
| PUBLISHED 게시글 수 | 1303 |
| 본문 평균 길이 | 3687.3자 |
| 본문 최소 길이 | 179자 |
| 본문 최대 길이 | 12058자 |
| p25 | 2759자 |
| p50 | 4793자 |
| p75 | 4794자 |
| p90 | 4794자 |
| p95 | 4794자 |

카테고리별 평균 본문 길이는 다음과 같다.

| 카테고리 | 게시글 수 | 평균 본문 길이 | 최소 | 최대 |
|---|---:|---:|---:|---:|
| Daily | 300 | 2707.3자 | 232자 | 4794자 |
| Project | 202 | 3941.4자 | 179자 | 4794자 |
| Development | 201 | 4238.9자 | 611자 | 12058자 |
| Briefing | 200 | 3771.3자 | 317자 | 5676자 |
| Learning | 200 | 3968.4자 | 243자 | 5724자 |
| Review | 200 | 3980.8자 | 322자 | 4794자 |

현재 데이터는 3000~4999자 구간에 게시글이 많이 몰려 있다. 따라서 `1200자`는 한 문장 기준으로는 길지만, 현재 게시글 데이터 기준으로는 게시글 하나를 평균 약 4개 청크로 나누는 값이다.

| 본문 길이 구간 | 게시글 수 |
|---|---:|
| 300자 미만 | 103 |
| 300~599자 | 83 |
| 600~899자 | 41 |
| 900~1199자 | 29 |
| 1200~1799자 | 34 |
| 1800~2999자 | 46 |
| 3000~4999자 | 963 |
| 5000자 이상 | 4 |

청크 크기 후보별 예상 청크 수는 다음과 같다. 이 표는 길이 기반 단순 추정이므로, 실제 문단 보존 로직의 결과와 차이가 난다.

| 후보 | 최대 길이 | overlap | 예상 총 청크 | 게시글당 평균 청크 | 1청크 글 | 2청크 글 | 3청크 글 | 4청크 이상 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 600/90 | 600자 | 90자 | 10092 | 7.75 | 187 | 49 | 46 | 1021 |
| 900/135 | 900자 | 135자 | 7085 | 5.44 | 227 | 57 | 26 | 993 |
| 1200/180 | 1200자 | 180자 | 5178 | 3.97 | 256 | 49 | 43 | 955 |
| 1500/225 | 1500자 | 225자 | 4213 | 3.23 | 267 | 61 | 87 | 888 |

실제 현재 저장된 청크 수는 `post_embedding_chunks = 5417`개이고, 게시글당 평균 `4.14`청크다. 단순 추정의 `1200/180 = 5178`개보다 많은 이유는 실제 구현이 문단 경계를 최대한 보존하면서 새 청크를 만들기 때문이다.

넓은 후보군을 잡으면 다음과 같다. `300/45`는 너무 잘게 자르는 쪽의 극단값이고, `2400/360`은 너무 크게 묶는 쪽의 극단값이다. 두 값을 포함해 중간값뿐 아니라 양쪽 극단에서도 성능 변화를 확인했다.

| 후보 | 최대 길이 | overlap | 예상 총 청크 | 게시글당 평균 청크 | 현재 1200/180 대비 |
|---|---:|---:|---:|---:|---:|
| EXTREME-S `300/45` | 300자 | 45자 | 19158 | 14.70 | 3.70배 |
| S `450/68` | 450자 | 68자 | 13084 | 10.04 | 2.53배 |
| S `600/90` | 600자 | 90자 | 10092 | 7.75 | 1.95배 |
| M `750/112` | 750자 | 112자 | 8113 | 6.23 | 1.57배 |
| M `900/135` | 900자 | 135자 | 7085 | 5.44 | 1.37배 |
| M `1050/158` | 1050자 | 158자 | 6108 | 4.69 | 1.18배 |
| BASE `1200/180` | 1200자 | 180자 | 5178 | 3.97 | 1.00배 |
| L `1350/203` | 1350자 | 203자 | 4978 | 3.82 | 0.96배 |
| L `1500/225` | 1500자 | 225자 | 4213 | 3.23 | 0.81배 |
| L `1800/270` | 1800자 | 270자 | 3271 | 2.51 | 0.63배 |
| XL `2100/315` | 2100자 | 315자 | 3214 | 2.47 | 0.62배 |
| EXTREME-L `2400/360` | 2400자 | 360자 | 3150 | 2.42 | 0.61배 |

실제 재임베딩 실험은 이 12개를 모두 돌리는 방식과 6개 대표값을 먼저 비교하는 방식으로 나뉜다. API 호출량을 줄이는 운영 관점에서는 `300/45`, `600/90`, `900/135`, `1200/180`, `1500/225`, `2400/360`의 6개를 먼저 비교하고, 최고 후보 주변을 2차로 좁힌다.

### 청크 크기 12종 오프라인 전수 실험

12개 후보를 모두 돌린 결과는 `backend/build/chunk-size-variant-evaluation.json`에 저장했다. 이 실험은 OpenAI/Qdrant 청크 임베딩을 재생성한 실험이 아니다. DB의 게시글 본문을 후보별로 다시 청킹하고, 동일한 6개 평가 케이스에서 BM25/키워드 기반 청크 근거 품질을 비교한 오프라인 민감도 분석이다.

따라서 이 표는 "실제 운영 RAG 점수"가 아니라 "청크 크기만 바꿨을 때 텍스트 근거 검색이 어느 쪽에서 유리한가"를 보는 자료다.

| 후보 | 실제 청크 수 | 평균 청크/글 | Chunk Precision@5 | Chunk Recall@5 | Chunk MRR@5 | Chunk NDCG@5 | Post+Chunk NDCG@5 |
|---|---:|---:|---:|---:|---:|---:|---:|
| EXTREME-S `300/45` | 23465 | 18.01 | 0.5667 | 0.3280 | 0.8889 | 0.6293 | 0.6551 |
| S `450/68` | 15257 | 11.71 | 0.6333 | 0.3743 | 1.0000 | 0.7067 | 0.6551 |
| S `600/90` | 11203 | 8.60 | 0.6667 | 0.3847 | 0.9167 | 0.7102 | 0.6551 |
| M `750/112` | 8868 | 6.81 | 0.6667 | 0.3847 | 1.0000 | 0.7374 | 0.6760 |
| M `900/135` | 7381 | 5.66 | 0.6333 | 0.3743 | 1.0000 | 0.7229 | 0.6760 |
| M `1050/158` | 6295 | 4.83 | 0.6667 | 0.3894 | 0.9167 | 0.7087 | 0.6760 |
| BASE `1200/180` | 5412 | 4.15 | 0.6000 | 0.3639 | 0.9167 | 0.6728 | 0.6551 |
| L `1350/203` | 5092 | 3.91 | 0.7000 | 0.3998 | 1.0000 | 0.7755 | 0.6551 |
| L `1500/225` | 4353 | 3.34 | 0.6667 | 0.3928 | 0.9167 | 0.6939 | 0.6760 |
| L `1800/270` | 3804 | 2.92 | 0.6333 | 0.3743 | 1.0000 | 0.7116 | 0.6760 |
| XL `2100/315` | 3291 | 2.53 | 0.6667 | 0.3975 | 0.9167 | 0.7165 | 0.6551 |
| EXTREME-L `2400/360` | 3198 | 2.45 | 0.6333 | 0.3737 | 1.0000 | 0.7190 | 0.6551 |

이 실험에서는 `1350/203`이 `Chunk Precision@5`, `Chunk Recall@5`, `Chunk NDCG@5`에서 가장 좋았다. 반대로 `300/45`는 청크 수가 크게 늘었지만 성능이 가장 낮은 편이었다. 너무 작게 자르면 문맥이 끊기고, 너무 크게 자르면 세부 근거가 흐려진다는 가설과 맞는다.

`Post+Chunk NDCG@5`는 후보별 차이가 작다. 현재 운영 검색 구조에서 청크 점수는 `1%` 보조 근거이기 때문이다. 청크 크기 변경이 실제 운영 결과를 크게 바꾸는지 확인하려면, 청크 임베딩을 재생성한 뒤 Qdrant 검색까지 포함해서 다시 평가한다.

이 결과를 기준으로 다음 실제 재임베딩 후보는 `750/112`, `900/135`, `1350/203`이다. `1350/203`은 오프라인 점수가 가장 높고, `750/112`와 `900/135`는 세부 근거와 비용의 균형을 보는 중간 후보로 남긴다.

### 검색 파라미터 오프라인 Ablation

청크 크기 외에도 검색 파이프라인의 주요 조절값을 오프라인으로 비교했다. 결과 파일은 `backend/build/rag-offline-ablation.json`이고, 실행 스크립트는 `scripts/evaluate-rag-offline-ablation.mjs`이다.

이 실험은 MySQL에 저장된 실제 게시글/청크 임베딩을 읽어서 계산했다. 운영 DB와 Qdrant 컬렉션은 변경하지 않았다. 평가 쿼리 임베딩은 OpenAI API로 생성해 `backend/build/rag-query-embedding-cache.json`에 캐시했다.

| 실험군 | 가장 높은 NDCG@5 조합 | Precision@5 | Recall@5 | MRR@5 | NDCG@5 | Hit@5 |
|---|---|---:|---:|---:|---:|---:|
| Query Composition | Title + content | 0.5818 | 0.3080 | 0.7273 | 0.6734 | 0.7273 |
| Vector/BM25 Weight | Vector 0.35 / BM25 0.65 | 0.4545 | 0.2589 | 0.7273 | 0.5581 | 0.7273 |
| Chunk Evidence Weight | Chunk 3% | 0.5636 | 0.3055 | 0.7273 | 0.6628 | 0.7273 |
| Ranked Result Threshold | Ranked >= 0.6 | 0.5455 | 0.2925 | 0.7273 | 0.6530 | 0.7273 |
| Vector Threshold | Vector >= 0.25 | 0.5455 | 0.2925 | 0.7273 | 0.6530 | 0.7273 |
| Hybrid Threshold | Hybrid >= 0.2 | 0.5455 | 0.2925 | 0.7273 | 0.6530 | 0.7273 |
| BM25 Parameters | k1 2 / b 0.25 | 0.5818 | 0.3083 | 0.7273 | 0.6769 | 0.7273 |
| RRF Constant | RRF k=60 | 0.5455 | 0.2925 | 0.7273 | 0.6530 | 0.7273 |
| Query Term Limit | query 12 / guard 6 | 0.5818 | 0.3156 | 0.7273 | 0.6761 | 0.7273 |
| Metadata Filter | Metadata off | 0.6727 | 0.3092 | 0.8182 | 0.7691 | 0.8182 |
| Core Retriever | Current-like full hybrid | 0.5455 | 0.2925 | 0.7273 | 0.6530 | 0.7273 |

이 표에서 바로 적용 후보로 볼 수 있는 것은 다음이다.

- Query Composition: 현재 템플릿보다 `Title + content`가 높았다. 검색용 쿼리는 설명 문구를 많이 넣기보다 제목과 사용자가 쓴 본문 중심으로 단순화하는 후보를 검토한다.
- Vector/BM25 Weight: `Vector 0.35 / BM25 0.65`가 가장 높았다. 현재 "의미 검색보다 키워드 검색을 조금 더 강하게 둔 설계"를 정량적으로 뒷받침한다.
- Chunk Evidence Weight: 현재 1%보다 `3%`가 높았다. 다만 `10%`에서는 NDCG가 떨어졌으므로 청크 점수는 보조 근거 이상으로 키우면 위험하다.
- Ranked Result Threshold: `0.6`이 가장 높고, `0.8`부터 성능이 크게 떨어졌다. `0.99 cutoff`를 폐기한 판단과 같은 방향이다.
- BM25 Parameters: `k1=2, b=0.25`가 가장 높았다. 현재 데이터에서는 문서 길이 정규화를 강하게 거는 것보다, 핵심 단어 빈도를 조금 더 보는 쪽이 유리할 수 있다.
- RRF Constant: `k=60`이 가장 높아 현재 설정을 유지할 근거가 있다.
- Query Term Limit: 현재 `16/8`보다 `12/6`이 높았다. 너무 많은 query term을 넣으면 오히려 부차 단어가 검색 신호에 섞일 수 있다.

주의해서 해석해야 할 항목도 있다.

- Metadata off가 가장 높게 나왔지만, 이를 바로 채택하면 안 된다. 평가 케이스 중 `legacy-id`는 category/tag가 비어 있고, `subject-heuristic`은 제목/본문 키워드 기반으로 정답을 잡는다. 따라서 metadata filter의 UX 목적, 즉 사용자가 선택한 게시판/태그 범위를 존중하는 효과가 과소평가될 수 있다.
- Vector/Hybrid threshold는 이번 범위에서 점수 변화가 없었다. 현재 평가셋에서는 ranked threshold와 BM25/keyword 신호가 더 큰 영향을 주었기 때문에, 이 실험군은 결정력이 낮다.
- Core Retriever에서는 `Current-like full hybrid`가 가장 높은 NDCG를 보였다. 즉 단일 검색기보다 Vector, BM25, keyword, metadata, chunk evidence를 섞는 방향 자체는 유지할 근거가 있다.

이 오프라인 ablation을 기준으로 온라인 검증 후보는 다음으로 좁힌다.

| 축 | 온라인 후보 |
|---|---|
| 청크 크기 | `750/112`, `900/135`, `1350/203`, 현재 `1200/180` |
| Query 구성 | 현재 템플릿, `Title + content` |
| Chunk weight | `1%`, `3%`, `5%` |
| BM25 parameter | 현재 `k1=1.2, b=0.75`, 후보 `k1=2, b=0.25` |
| Query term limit | 현재 `16/8`, 후보 `12/6` |

온라인 실험에서는 위 후보를 한꺼번에 모두 섞지 않는다. 먼저 한 축씩 바꿔 비교하고, 마지막에 좋은 후보끼리 조합한다. 그래야 어떤 변경이 성능을 올렸는지 설명할 수 있다.

### 현재 온라인 기준점

현재 운영 설정을 실제 Spring API와 Qdrant를 통해 다시 평가했다. 실행 명령은 다음과 같다.

```bash
node .\scripts\evaluate-rag-retrieval.mjs
```

결과는 기존 `Lucene Nori + Qdrant 재검사` 단계와 동일한 흐름이다.

| Metric | Score |
|---|---:|
| Hit@5 | 1.0000 |
| Recall@5 | 0.4621 |
| Precision@5 | 0.8000 |
| MRR@5 | 1.0000 |
| NDCG@5 | 0.8599 |

이 값은 온라인 후보 실험의 기준선으로 둔다. 오프라인에서 높은 후보라도 온라인 기준점보다 낮으면 실제 채택하지 않는다.

### 실제 온라인 후보 검증

오프라인 ablation에서 상위 점수를 기록한 후보를 실제 API 경로로 다시 검증했다. 실행 스크립트는 `scripts/evaluate-rag-online-candidates.mjs`이고, 결과 파일은 `backend/build/rag-online-candidate-evaluation.json`이다.

이 실험은 후보마다 별도 Spring Boot 서버를 임시 포트로 실행한 뒤 `/api/ai/similar-posts`를 호출했다. 따라서 단순 계산 스크립트가 아니라 Qdrant 검색, MySQL 원본 조회, OpenAI 쿼리 임베딩, 서버 내부 랭킹 로직이 모두 포함된 온라인 검증이다.

| 후보 | 변경 내용 | Hit@5 | Recall@5 | Precision@5 | MRR@5 | NDCG@5 | 판단 |
|---|---|---:|---:|---:|---:|---:|---|
| Current baseline | 기존 운영 설정 | 1.0000 | 0.4621 | 0.8000 | 1.0000 | 0.8599 | 기준선 |
| Query title + content | 검색 쿼리를 제목+본문 중심으로 단순화 | 1.0000 | 0.4725 | 0.8333 | 1.0000 | 0.9037 | 단독 개선 |
| Chunk evidence 3% | 청크 근거 가중치 `1% -> 3%` | 1.0000 | 0.4621 | 0.8000 | 1.0000 | 0.8624 | 소폭 개선 |
| BM25 k1=2 b=0.25 | BM25 단어 빈도 영향 증가, 길이 보정 완화 | 1.0000 | 0.4534 | 0.8000 | 1.0000 | 0.8550 | 단일 변경 기준 악화 |
| Query terms 12 / guard 6 | 부차 단어 유입을 줄이도록 term 수 축소 | 1.0000 | 0.4758 | 0.8333 | 1.0000 | 0.9014 | 단독 개선 |
| Weighted fusion | RRF 대신 점수 가중합 | 1.0000 | 0.3565 | 0.6000 | 1.0000 | 0.6898 | 폐기 |
| Combined tuned | 상위 후보 조합, RRF 유지 | 1.0000 | 0.4957 | 0.8667 | 1.0000 | 0.9076 | 최종 채택 |
| Combined tuned + weighted fusion | 조합안에 weighted fusion 추가 | 1.0000 | 0.3843 | 0.6333 | 1.0000 | 0.7116 | 폐기 |
| Metadata off diagnostic | metadata 신호 비활성화 | 1.0000 | 0.4621 | 0.8000 | 1.0000 | 0.8599 | 채택 안 함 |

`짧은 쿼리 최종 튜닝`의 `NDCG@5 0.9365`와 `온라인 후보 검증 최종 채택`의 `NDCG@5 0.9076`은 같은 후보군에서 나온 경쟁 결과가 아니다. `짧은 쿼리 최종 튜닝`은 과거 코드 상태에서 세션 로그로 남은 온라인 측정값이고, 이후 Nori/Qdrant/current settings가 들어가면서 검색 경로와 후보군이 바뀌었다.

현재 구조에서 직접 비교해야 하는 기준선은 `Lucene Nori + Qdrant 재검사`와 `Current baseline`의 `NDCG@5 0.8599`다. 최종 채택안은 같은 현재 구조에서 `0.8599 -> 0.9076`으로 개선됐기 때문에 채택했다.

### 오프라인 후보와 온라인 검증의 차이

이번 실험의 핵심 결론은 "오프라인 상위 후보를 그대로 운영 설정으로 채택하지 않는다"이다. 오프라인 실험은 많은 후보를 빠르게 줄이는 데 사용했고, 실제 채택 여부는 온라인 실험에서 결정했다.

| 후보 | 오프라인에서의 기대 | 온라인 결과 | 해석 |
|---|---|---|---|
| `Title + content` query | Query Composition 실험에서 가장 높은 NDCG | `NDCG@5 0.8599 -> 0.9037` | 오프라인 가설이 온라인에서도 맞았다. 검색 쿼리에서 설명 템플릿을 줄이고 사용자 입력 중심으로 가는 방향은 유효했다. |
| Chunk evidence `3%` | Chunk Evidence Weight 실험에서 1%보다 높음 | `NDCG@5 0.8599 -> 0.8624` | 방향은 맞지만 효과는 작았다. 청크는 주 검색기가 아니라 보조 근거라 실제 랭킹을 크게 바꾸지 않았다. |
| BM25 `k1=2, b=0.25` | BM25 Parameters 실험에서 가장 높은 NDCG | `NDCG@5 0.8599 -> 0.8550` | 단일 변경 기준으로는 온라인에서 악화됐다. Qdrant 후보군, metadata, RRF 결합 뒤에는 오프라인 계산과 다른 효과가 나타났다. |
| Query term `12/6` | Query Term Limit 실험에서 가장 높은 NDCG | `NDCG@5 0.8599 -> 0.9014` | 오프라인 가설이 온라인에서도 맞았다. 부차 단어를 줄이는 것이 실제 API 결과에도 도움이 됐다. |
| Metadata off | 오프라인에서 가장 높은 점수 | `NDCG@5 0.8599 -> 0.8599` | 온라인에서는 이득이 없었다. 또한 사용자가 선택한 카테고리/태그 맥락을 무시하게 되므로 UX 관점에서 채택하지 않았다. |
| Weighted fusion | 내부 ablation에서 점수가 높았던 후보 | `NDCG@5 0.8599 -> 0.6898` | 실제 API 경로에서는 크게 악화됐다. 점수 스케일을 직접 섞는 방식보다 RRF가 안정적이었다. |

이 차이가 생기는 이유는 오프라인 실험과 온라인 실험의 평가 경로가 다르기 때문이다.

정확히는 첫 번째 갈림길이 `후보군 생성`이다. 오프라인 실험은 MySQL에 저장된 게시글/청크를 거의 전부 읽어온 뒤 스크립트 안에서 점수를 계산한다. 반면 온라인 실험은 실제 서비스처럼 Qdrant가 먼저 유사 벡터 후보 `postId`를 가져오고, 그 후보에 대해서만 MySQL 원본 조회와 BM25/RRF 재정렬을 수행한다.

```text
오프라인 실험 경로
평가 케이스
-> JS 스크립트에서 query text 생성
-> 저장된 query embedding 또는 새 query embedding 사용
-> MySQL의 게시글/청크/임베딩 전체 로드
-> JS metadata filter
-> JS BM25/vector/RRF/keyword/chunk 계산
-> top5 평가

온라인 실험 경로
평가 케이스
-> /api/ai/similar-posts 호출
-> AiController
-> PostEmbeddingTextBuilder가 실제 서비스 query text 생성
-> OpenAiEmbeddingClient가 query embedding 생성
-> QdrantVectorStoreClient가 post/chunk 후보 postId 검색
-> MySQL에서 Qdrant 후보의 원본 게시글/태그/청크 조회
-> SimilarPostSearchService가 metadata relaxation, BM25, RRF, keyword, chunk evidence 적용
-> top5 평가
```

그래서 온라인에서 Qdrant 1차 후보에 들어오지 못한 게시글은 뒤 단계에서 BM25 점수가 좋아도 복구되지 않는다. 오프라인에서는 전체 후보를 놓고 BM25/RRF를 계산하므로 이런 글이 살아남을 수 있다. `BM25 k1=2, b=0.25`가 오프라인에서는 높은 점수를 기록했지만 온라인 단일 변경에서 `NDCG@5 0.8550`으로 내려간 핵심 이유가 여기에 있다. BM25 자체의 문제가 아니라, 실제 서비스에서는 "Qdrant가 먼저 잘라온 후보 안에서만 BM25가 재정렬한다"는 제약이 붙는다.

차이가 벌어지는 지점은 다음과 같이 정리할 수 있다.

| 갈림 지점 | 오프라인 실험 | 온라인 실험 | 결과에 주는 영향 |
|---|---|---|---|
| 1차 후보군 | MySQL에서 읽은 전체 게시글/청크를 대상으로 계산 | Qdrant가 반환한 postId/chunk postId만 대상으로 계산 | 온라인에서는 1차 후보에서 빠진 글을 후속 BM25/RRF가 살릴 수 없다. |
| 쿼리 텍스트 | 스크립트의 query variant가 생성 | `PostEmbeddingTextBuilder`가 실제 서비스 설정으로 생성 | `Title + content`처럼 쿼리 텍스트 변경 효과가 온라인에서 다시 검증되어야 한다. |
| metadata 처리 | JS `filterPostsByMetadata`, `metadataStages`로 근사 | Java `SearchMetadata.relaxationStages()`, `matchesMetadata()`로 처리 | 카테고리/태그 완화 순서와 후보 수가 달라질 수 있다. |
| 점수 계산 위치 | JS가 BM25, vector, RRF, keyword, chunk 점수를 재현 | Java `SimilarPostSearchService`가 실제 운영 로직으로 계산 | 수식은 맞춰도 threshold, tie-break, 후보 제한이 달라질 수 있다. |
| 데이터/인덱스 기준 | MySQL에 저장된 임베딩을 직접 사용 | Qdrant collection과 MySQL 원본이 모두 필요 | Qdrant 동기화 상태와 topN 제한이 실제 결과에 영향을 준다. |
| 비용/속도 | 빠르고 조합을 많이 돌리기 좋음 | 느리고 API/서버 실행 비용이 듦 | 오프라인은 전수 조사, 온라인은 최종 검증에 적합하다. |

| 구분 | 오프라인 실험 | 온라인 실험 |
|---|---|---|
| 목적 | 후보를 빠르게 좁히는 screening | 실제 채택 여부 결정 |
| 실행 경로 | DB에 저장된 게시글/청크/임베딩을 읽어 스크립트에서 계산 | Spring API, OpenAI query embedding, Qdrant 검색, MySQL 조회, 서버 랭킹 로직 전체 실행 |
| 장점 | 빠르고 저렴하며 많은 조합을 비교하기 좋음 | 실제 서비스 동작과 가장 가까움 |
| 한계 | Qdrant 후보 제한, metadata 완화 로직, API 입력 정규화, 점수 결합 상호작용을 동일하게 재현하지 않음 | 느리고 비용이 들며 한 번에 비교할 수 있는 후보 수가 제한됨 |
| 최종 역할 | "이 후보를 온라인에서 볼 가치가 있는가" 판단 | "이 후보를 기본 설정으로 채택할 것인가" 판단 |

따라서 이번 프로젝트의 평가 절차는 다음처럼 정리한다.

```text
오프라인 전수/ablation
-> 온라인 검증 대상 압축
-> 실제 Spring API 온라인 검증
-> 대표 사용자 시나리오 입출력 확인
-> 최종 채택/폐기
```

즉 오프라인 점수는 가설 생성 도구이고, 최종 채택 근거는 온라인 후보 검증과 시나리오 입출력 평가다.

최종 채택안은 `Combined tuned`다. 개별 실험에서는 `BM25 k1=2, b=0.25`가 단일 변경 기준으로 개선을 만들지 못했지만, `TITLE_CONTENT`, query term 축소, 청크 가중치와 함께 섞었을 때 전체 평균이 가장 높았다. 반대로 `Weighted fusion`은 내부 ablation에서 점수가 높았지만 실제 API 경로에서는 크게 떨어졌으므로 채택하지 않는다.

청크 크기 후보(`750/112`, `900/135`, `1350/203`)는 이 온라인 실험에 포함하지 않았다. 청크 크기 변경은 기존 청크 임베딩과 Qdrant chunk collection을 다시 만들어야 하므로 별도 재임베딩 실험으로 분리한다.

Spring 내부 ablation endpoint도 함께 실행했다. 결과 파일은 `backend/build/rag-online-variants-evaluation.json`이다.

| Variant | Precision@5 | Recall@5 | MRR@5 | NDCG@5 | Hit@5 |
|---|---:|---:|---:|---:|---:|
| Vector only | 0.6800 | 0.3365 | 0.8500 | 0.7800 | 1.0000 |
| Vector + threshold | 0.6800 | 0.3365 | 0.8500 | 0.7800 | 1.0000 |
| Vector + keyword guard | 0.6800 | 0.3365 | 1.0000 | 0.8939 | 1.0000 |
| Vector + BM25 weighted | 0.7200 | 0.4032 | 1.0000 | 0.9408 | 1.0000 |
| Vector + BM25 + RRF | 0.6400 | 0.3337 | 1.0000 | 0.8646 | 1.0000 |
| Hybrid + metadata | 0.3600 | 0.2439 | 0.6000 | 0.5280 | 0.6000 |
| Hybrid + metadata + chunks | 0.3600 | 0.2439 | 0.6000 | 0.5280 | 0.6000 |
| Final production | 0.6400 | 0.3337 | 1.0000 | 0.8676 | 1.0000 |

이 결과는 오프라인 ablation과 같은 방향이다. keyword guard와 BM25가 들어가면 vector-only보다 상위 품질이 올라간다. 반면 metadata/chunk가 낮게 나온 것은 해당 endpoint의 5개 평가 케이스에서 metadata 필터가 후보를 과하게 좁힌 영향이 크다. 따라서 metadata 자체를 제거하는 대신 metadata 완화 단계와 평가셋을 더 정교하게 조정한다.

### 변곡점: 0.99 Score Cutoff

`0.99 cutoff`는 아래 커밋에서 도입됐다.

```text
b2e013e tune: 유사글 표시 점수 0.99 미만 제외
```

도입 배경은 화면에서 `score 0.984`처럼 높은 점수의 결과가 보였지만, 제목 기준으로는 관련성이 낮다고 판단했기 때문이다. 이때는 "점수가 낮은 결과를 잘라내면 화면 신뢰도가 올라갈 것"이라는 가설을 세웠다.

하지만 바로 다음 평가에서 문제가 드러났다.

```text
청크 + 전체글 결합 1%: Precision@5 0.9333, NDCG@5 0.9538
0.99 cutoff 적용: Precision@5 0.1000, NDCG@5 0.5000
```

실제 출력도 `RAG/LLM 서비스 설계`, `Kubernetes/EKS 운영`, `React 프론트엔드 개발` 케이스에서 결과가 아예 비어버렸다. 이 지점이 "점수 높은 결과만 남기기"에서 "실제 입력과 출력 제목이 맞는지 확인하기"로 평가 관점이 바뀐 변곡점이다.

이후 개선 방향은 단순 score cutoff가 아니라 다음 쪽으로 바뀌었다.

- 표시 점수는 cosine similarity가 아니라 Vector, BM25, RRF, keyword alignment가 섞인 랭킹 점수로 해석한다.
- threshold 하나로 자르기보다 query term, 제목, 태그, 카테고리, 청크 근거를 함께 본다.
- `깃허브`, `용인 날씨`처럼 짧은 실제 입력을 넣고 반환 제목을 직접 확인한다.

### 정성 예시

점수표만으로는 실제 사용자 경험을 판단하기 어렵다. 그래서 대표 입력과 실제 출력도 함께 본다.

| 입력 | 검색 구조/상태 | 실제 출력 요약 | 해석 |
|---|---|---|---|
| `RAG/LLM 서비스 설계` | `0.99 cutoff` | 결과 없음 | 점수 기준을 너무 세게 자르면 관련 글까지 사라진다. 사용자는 "검색 실패"처럼 느낀다. |
| `Kubernetes/EKS 운영` | `0.99 cutoff` | 결과 없음 | threshold 하나로 품질을 보장하려는 방식은 위험하다. |
| `React 프론트엔드 개발` | `0.99 cutoff` | 결과 없음 | 관련 후보가 있어도 최종 점수가 0.99보다 낮으면 전부 탈락한다. |
| `GitHub Actions 배포 실패 정리` | 최종 Spring 평가 | `Github Ops`, `Github Actions Secret 관리`, `GitHub Actions 문서`, `GitHub Action 기반 CI 개선` | top5 전체가 GitHub Actions/CI/CD 관련 글이라 정량 점수와 실제 결과가 잘 맞는다. |
| `오늘 날씨 브리핑 작성` | 최종 Spring 평가 | 1위는 `대구 오늘 날씨는`, 2~5위는 데이터/배달/AI/CPU 온도 글 | top1은 맞지만 하위 결과에는 노이즈가 있다. 날씨 도메인은 데이터가 적어 보강 대상이다. |
| `깃허브` | 짧은 쿼리 최종 튜닝 | `GitHub Actions 문서`, `Git 정보 - GitHub 문서`, `Github Actions 1편`, `Github Actions 2편`, `GitHub Action 기반 CI 개선` | 짧은 단어 검색에서도 GitHub 관련 후보가 상위에 잡힌다. |
| `용인 날씨` | 짧은 쿼리 최종 튜닝 | 1위는 `대구 오늘 날씨는`, 이후 배달/AI/마케팅/해커톤 글 | `날씨`라는 대주제는 잡지만 지역/날짜 세부 조건은 약하다. metadata나 외부 날씨 API 연동으로 보완해야 한다. |

따라서 현재 결론은 "점수가 가장 높은 조합이 항상 사용자 경험도 가장 좋다"가 아니다. 같은 평가셋 안에서는 `청크 + 전체글 결합 1%`가 가장 높은 점수를 기록했고, 실제 출력 관점에서도 `post-level primary + chunk evidence boost` 방향이 타당했다. 반면 `0.99 cutoff`는 결과를 비워버려 서비스 품질을 떨어뜨렸다.

Nori 적용 후에는 두 가지 검사를 했다.

1. 단위 테스트: `KoreanTextAnalyzerTest`
2. 검색 품질 평가: `scripts/evaluate-rag-retrieval.mjs`

단위 테스트는 다음을 확인한다.

- `으로`, `했습니다` 같은 조사/어미가 검색 주제어로 남지 않는가
- `깃허브 액션`, `배포`, `시크릿` 같은 표현이 `github`, `actions`, `deploy`, `secrets`처럼 검색에 유리한 토큰으로 정규화되는가
- `7800X3D`, `9800X3D`, `CPU`, `업그레이드` 같은 하드웨어 주제어가 유지되는가

검증 명령:

```powershell
cd C:\Users\cedis\week15_project\backend
.\gradlew.bat test
```

결과: 통과

검색 품질 재검사는 위 단계별 표의 `Lucene Nori + Qdrant 재검사` 행에 반영했다.

실행 명령은 다음과 같다.

```powershell
cd C:\Users\cedis\week15_project
node .\scripts\evaluate-rag-retrieval.mjs
```

이 결과는 "Nori만 단독으로 바꾼 A/B 테스트"가 아니다. 현재 코드는 Nori, Qdrant, 청크 근거, BM25/RRF가 함께 들어간 상태다. 따라서 이 점수는 형태소 분석기 적용 후 현재 검색 파이프라인이 깨지지 않고 정상 동작하는지 확인한 재검사 결과로 해석한다.

케이스별 원자료는 보고서 본문에는 싣지 않는다. `RAG/LLM 서비스 설계`, `React 프론트엔드 개발` 같은 이름은 프론트엔드/백엔드 구조를 뜻하는 것이 아니라 평가용 검색 질의의 주제 이름이다. 세부 케이스 결과는 [rag-measurement-inventory.md](rag-measurement-inventory.md)에 보관한다.

## 7. 조합별 개선 흐름

아래 표는 커밋 흐름과 실제 실험 중 관찰된 문제/개선 방향을 정리한 것이다.

| 단계 | 검색 조합 | 들어간 키워드 | 해결하려던 문제 | 관찰/의미 |
|---|---|---|---|---|
| 1 | Vector similarity only | `Embedding`, `Cosine Similarity` | 의미적으로 비슷한 글 찾기 | 단어가 직접 다른 글도 찾을 수 있지만, 고유명사 검색이 약했다. |
| 2 | Vector + similarity threshold | `Threshold`, `Minimum Score` | 너무 무관한 글 제거 | 낮은 점수 결과를 줄였지만, threshold 하나만으로는 높은 점수의 오탐을 처리하지 못했다. |
| 3 | Vector + keyword guard | `Keyword Guard`, `Subject Terms` | 제목/태그 핵심어와 무관한 결과 제거 | 관련 없는 결과는 줄었지만, 너무 강하면 관련 글까지 누락될 수 있어 soft ranking으로 조정했다. |
| 4 | Vector + BM25 | `BM25`, `Lexical Search` | `GitHub Actions`, `CPU`, `React` 같은 표면 단어 보완 | 고유명사와 기술 키워드가 중요한 검색에서 개선 효과가 있었다. |
| 5 | Vector + BM25 + RRF | `RRF`, `Hybrid Retrieval` | 벡터 순위와 BM25 순위 결합 | 두 점수의 절대값을 억지로 맞추기보다 순위 기반으로 안정적으로 결합했다. |
| 6 | Metadata signal 추가 | `Category`, `Tag`, `Metadata Filter` | 카테고리/태그 맥락 반영 | 사용자가 작성 중인 글의 카테고리와 태그를 후보 필터/완화 조건에 반영했다. |
| 7 | Post embedding + Chunk embedding | `Chunking`, `Chunk Evidence` | 긴 글에서 일부 문단만 관련 있는 경우 탐색 | 전체 글 임베딩이 놓치는 세부 근거를 청크 임베딩으로 보완했다. |
| 8 | Chunk evidence weight 조정 | `Chunk Score`, `Weight Tuning` | 청크 점수가 과하게 결과를 끌어올리는 문제 | 청크는 보조 근거로 쓰고, 전체 게시글 랭킹을 과도하게 흔들지 않게 했다. |
| 9 | Korean Nori analyzer 적용 | `Nori`, `Morphological Analysis`, `Tokenization` | 한국어 조사/어미/복합어 처리 | 검색과 평가가 같은 한국어 분석 기준을 쓰도록 통합했다. |
| 10 | Qdrant Vector DB 적용 | `Qdrant`, `Vector DB`, `HNSW` | MySQL 전체 벡터 스캔 구조 개선 | Qdrant가 벡터 후보 postId를 찾고, MySQL은 원본 데이터 조회를 담당하게 분리했다. |

## 8. 최종 검색 파이프라인

```text
사용자 작성글
-> OpenAI Embedding 생성
-> Qdrant에서 유사 벡터 후보 postId 검색
-> MySQL에서 후보 게시글/태그/청크 원본 조회
-> Nori 기반 토큰화
-> Vector score 계산
-> BM25 score 계산
-> RRF로 vector rank + BM25 rank 결합
-> 키워드 정렬 점수 반영
-> 청크 근거 점수 보조 반영
-> 상위 유사 게시글 반환
-> 선택된 유사글을 근거로 AI 초안 생성
```

### 기술 선택 근거와 외부 자료

아래 자료는 구현 방향을 설명하기 위한 외부 근거다. 최종 채택 여부는 Project Alpha의 온라인 평가값과 시나리오 입출력 평가로 결정했다.

| 기술/개념 | Project Alpha에서의 사용 | 외부 근거 |
|---|---|---|
| OpenAI Embedding | 게시글과 사용자 입력을 `text-embedding-3-small` 벡터로 변환 | [OpenAI Embeddings Guide](https://developers.openai.com/api/docs/guides/embeddings), [text-embedding-3-small model](https://developers.openai.com/api/docs/models/text-embedding-3-small) |
| Qdrant Vector DB | MySQL 원본 데이터와 분리된 벡터 후보 검색 인덱스 | [Qdrant Similarity Search](https://qdrant.tech/documentation/search/search/), [Qdrant Vector Search Overview](https://qdrant.tech/documentation/overview/vector-search/) |
| HNSW | 대량 벡터에서 가까운 후보를 빠르게 찾는 근사 최근접 탐색 방식 | [Qdrant HNSW Indexing Fundamentals](https://qdrant.tech/course/essentials/day-2/what-is-hnsw/) |
| BM25 | `GitHub Actions`, `Redis`, `CPU`처럼 표면 단어가 중요한 검색 보강 | [Apache Lucene BM25Similarity](https://lucene.apache.org/core/8_1_1/core/org/apache/lucene/search/similarities/BM25Similarity.html) |
| RRF | vector rank와 BM25 rank를 점수 스케일 직접 합산 없이 결합 | [Cormack, Clarke, Buettcher, SIGIR 2009](https://research.google/pubs/reciprocal-rank-fusion-outperforms-condorcet-and-individual-rank-learning-methods/) |
| Lucene Nori | 한국어 조사/어미/복합어 처리를 위한 형태소 분석 | [Apache Lucene Nori API](https://lucene.apache.org/core/10_0_0/analysis/nori/) |
| RAGAS | 생성 답변의 faithfulness, answer relevancy, context 품질 측정 | [Ragas Metrics](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/), [Ragas Faithfulness](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/faithfulness/), [Ragas Context Precision](https://docs.ragas.io/en/stable/concepts/metrics/available_metrics/context_precision/) |

## 9. 재현 방법

아래 명령은 Windows PowerShell 기준이다. MySQL, Qdrant, Spring Boot 서버, OpenAI API Key가 준비되어 있어야 온라인 평가가 동작한다.

| 목적 | 명령 | 결과 파일 |
|---|---|---|
| 백엔드 테스트 | `cd C:\Users\cedis\week15_project\backend; .\gradlew.bat test` | Gradle test report |
| 현재 RAG 검색 성능 재측정 | `cd C:\Users\cedis\week15_project; node .\scripts\evaluate-rag-retrieval.mjs` | `backend/build/rag-current-retrieval-evaluation.json` |
| 온라인 후보 조합 검증 | `cd C:\Users\cedis\week15_project; node .\scripts\evaluate-rag-online-candidates.mjs` | `backend/build/rag-online-candidate-evaluation.json` |
| 오프라인 검색 ablation | `cd C:\Users\cedis\week15_project; node .\scripts\evaluate-rag-offline-ablation.mjs` | `backend/build/rag-offline-ablation.json` |
| 청크 크기 오프라인 비교 | `cd C:\Users\cedis\week15_project; node .\scripts\evaluate-chunk-size-variants.mjs` | `backend/build/chunk-size-variant-evaluation.json` |
| 시나리오 입출력 평가 | `cd C:\Users\cedis\week15_project; node .\scripts\evaluate-rag-scenarios.mjs` | `backend/build/rag-scenario-evaluation.json`, `docs/rag-scenario-evaluation.md` |

코드에서 확인할 위치는 다음과 같다.

| 관심사 | 파일 | 역할 |
|---|---|---|
| API 진입점 | `backend/src/main/java/com/jungle_choi/namanmu/api/AiController.java` | 유사글 검색과 AI 초안 생성 요청을 받는다. |
| 검색 query 구성 | `backend/src/main/java/com/jungle_choi/namanmu/service/rag/PostEmbeddingTextBuilder.java` | 사용자 입력을 embedding query text로 만든다. |
| Qdrant 후보 검색 | `backend/src/main/java/com/jungle_choi/namanmu/service/rag/QdrantVectorStoreClient.java` | post/chunk 벡터 후보 `postId`를 가져온다. |
| 최종 랭킹 | `backend/src/main/java/com/jungle_choi/namanmu/service/rag/SimilarPostSearchService.java` | metadata, BM25, RRF, keyword, chunk evidence를 결합한다. |
| 한국어 토큰화 | `backend/src/main/java/com/jungle_choi/namanmu/service/rag/KoreanTextAnalyzer.java` | Nori 기반 토큰화와 검색어 정규화를 담당한다. |
| 운영 설정 | `backend/src/main/resources/application.properties` | query mode, BM25, RRF, chunk weight, threshold 값을 관리한다. |

## 10. 용어 정리

### RAG

Retrieval-Augmented Generation의 약자다. LLM이 바로 답을 만들게 하지 않고, 먼저 관련 문서나 게시글을 검색한 뒤 그 결과를 근거로 답변이나 초안을 생성하는 방식이다.

Project Alpha에서는 사용자가 글을 작성하면 기존 게시글을 검색하고, 그 게시글을 근거로 초안을 생성한다.

### Embedding

텍스트를 숫자 벡터로 바꾼 것이다. 의미가 비슷한 문장은 벡터 공간에서도 가까운 위치에 놓이도록 만든다.

예를 들어 `GitHub Actions 배포 실패`와 `CI workflow secrets 오류`는 단어가 일부 달라도 비슷한 개발 맥락으로 가까워질 수 있다.

### Vector Similarity

두 임베딩 벡터가 얼마나 가까운지 계산하는 방식이다.

Project Alpha에서는 코사인 유사도를 사용한다.

### Cosine Similarity

두 벡터의 방향이 얼마나 비슷한지 보는 지표다.

값이 1에 가까울수록 방향이 비슷하고, 0에 가까울수록 관련성이 낮다고 본다.

### Threshold

최소 점수 기준이다.

예를 들어 유사도가 일정 값보다 낮으면 결과에서 제외한다. 단, threshold만으로는 좋은 검색 품질을 보장하기 어렵다. 점수는 높지만 주제가 어긋나는 경우가 있기 때문이다.

### BM25

단어 기반 검색 점수 알고리즘이다.

검색어에 들어간 중요한 단어가 문서에 얼마나 의미 있게 등장하는지 본다. 흔한 단어보다 희귀하고 구체적인 단어에 더 큰 가중치를 준다.

Project Alpha에서는 `GitHub Actions`, `React`, `CPU`, `9800X3D`처럼 표면 단어가 중요한 검색을 보완하기 위해 사용했다.

### Lexical Search

단어가 직접 겹치는지를 보는 검색 방식이다.

벡터 검색이 의미 유사도를 본다면, lexical search는 실제 단어 매칭을 본다. BM25는 대표적인 lexical search 알고리즘이다.

### Hybrid Retrieval

여러 검색 방식을 섞는 방식이다.

Project Alpha에서는 벡터 유사도와 BM25를 함께 사용한다. 벡터 검색은 의미 유사도에 강하고, BM25는 고유명사와 기술 키워드에 강하다.

### RRF

Reciprocal Rank Fusion의 약자다. 여러 검색 결과의 점수 자체를 직접 더하지 않고, 순위를 기준으로 결과를 합치는 방식이다.

벡터 점수와 BM25 점수는 스케일이 다르기 때문에 단순 가중합보다 RRF가 안정적일 수 있다.

### Metadata Filter

문서의 본문이 아니라 부가 정보를 이용해 검색 후보를 제한하거나 우선순위를 조정하는 방식이다.

Project Alpha에서는 `category`, `tag`를 metadata 신호로 사용한다.

### Keyword Guard

검색 결과가 핵심 주제어와 너무 멀어지지 않도록 잡아주는 장치다.

예를 들어 사용자가 `CPU 업그레이드`를 쓰고 있는데, 단순히 `업그레이드`만 겹치는 전혀 다른 글이 상위로 올라오는 문제를 줄이기 위해 사용했다.

현재는 절대 필터라기보다 soft ranking 신호로 사용한다.

### Subject Terms

검색에서 중요한 주제어를 뜻한다.

조사, 어미, 일반적인 단어를 제외하고 `github`, `actions`, `cpu`, `react`, `weather`처럼 주제를 담은 단어를 뜻한다.

### Chunking

긴 문서를 작은 단위로 나누는 작업이다.

게시글 하나가 길면 전체 임베딩 하나만으로는 중요한 일부 문단이 묻힐 수 있다. 그래서 게시글 본문을 여러 청크로 나누고, 각 청크에도 임베딩을 만든다.

### Chunk Evidence

청크 검색 결과가 특정 게시글을 뒷받침하는 근거 점수다.

Project Alpha에서는 전체 게시글 임베딩이 1차 기준이고, 청크 점수는 보조 근거로 반영한다.

### Nori

Apache Lucene에서 제공하는 한국어 형태소 분석기다.

한국어는 조사와 어미가 붙기 때문에 단순 공백 분리만으로는 검색 품질이 떨어진다. Nori를 사용하면 한국어 토큰을 더 일관되게 분리할 수 있다.

### Tokenization

문장을 검색 가능한 단어 단위로 나누는 작업이다.

예를 들어 `깃허브 액션으로 배포했다`를 `github`, `actions`, `deploy` 같은 검색 토큰으로 정규화한다.

### Qdrant

벡터 검색 전용 데이터베이스다.

Project Alpha에서는 MySQL을 원본 저장소로 유지하고, Qdrant는 임베딩 벡터 후보 검색용 인덱스로 사용한다.

### Vector DB

임베딩 벡터를 저장하고 유사 벡터를 빠르게 찾기 위한 데이터베이스다.

과제 예시에는 Pinecone, FAISS, ChromaDB, PostgreSQL용 pgvector가 있었고, Project Alpha는 MySQL을 유지하기 위해 별도 Vector DB인 Qdrant를 선택했다.

### HNSW

Hierarchical Navigable Small World의 약자다. 대량의 벡터 중 가까운 벡터를 빠르게 찾기 위한 근사 최근접 탐색 알고리즘이다.

Qdrant는 HNSW 기반 벡터 검색을 지원한다.

### Precision@K

상위 K개 검색 결과 중 관련 결과가 몇 개인지 보는 지표다.

예를 들어 `Precision@5 = 0.8`이면 상위 5개 중 4개가 관련 결과라는 뜻이다.

### Recall@K

전체 관련 문서 중 상위 K개 안에 몇 개가 들어왔는지 보는 지표다.

관련 문서가 아주 많고 K가 작으면 낮게 나올 수 있다.

### MRR@K

Mean Reciprocal Rank의 약자다. 첫 번째 관련 결과가 몇 등으로 나왔는지 보는 지표다.

첫 번째 결과가 관련 글이면 1.0이다. Project Alpha처럼 사용자가 상위 추천 몇 개만 보는 기능에서는 중요한 지표다.

### NDCG@K

Normalized Discounted Cumulative Gain의 약자다. 관련 결과가 상위에 잘 배치됐는지 보는 랭킹 품질 지표다.

관련 결과가 1등, 2등처럼 위쪽에 있을수록 점수가 높다.

### Hit@K

상위 K개 안에 관련 결과가 하나라도 있는지 보는 지표다.

`Hit@5 = 1.0`이면 모든 평가 케이스에서 상위 5개 안에 관련 결과가 최소 하나는 있었다는 뜻이다.

## 11. 팀 공유용 요약

최종 조합에서는 Qdrant로 벡터 후보를 찾고, MySQL에서 원본 데이터를 조회한 뒤 BM25/RRF/키워드/청크 근거 점수로 재정렬했다. `k=5` 기준 `Precision@5 = 0.8667`, `Recall@5 = 0.4957`, `MRR@5 = 1.0`, `NDCG@5 = 0.9076`, `Hit@5 = 1.0`이었다.

`Recall@5`는 0.5 미만이다. React/GitHub처럼 관련 글 총량이 많은 케이스에서는 top5만 보는 평가 특성상 낮게 나온다. 따라서 현재 기능은 "관련 글 전체 회수"보다 "작성 중 참고할 상위 추천 제공"에 더 적합하다.

## 12. 확인된 한계와 다음 개선 작업

| 항목 | 현재 상태 | 다음 작업 |
|---|---|---|
| `k=10` 평가 | 현재 공식 표는 `k=5` 기준 | `k=10`에서 Recall과 Precision 변화를 재측정한다. |
| 커밋별 동일 평가 | 주요 실험값은 세션 로그와 JSON에 남아 있음 | 주요 커밋을 체크아웃해 같은 평가셋으로 재측정하면 A/B 표의 신뢰도가 올라간다. |
| 평가용 query embedding | 일부 스크립트는 캐시를 사용하고, 온라인 평가는 실제 API 호출을 사용 | 비용 절감을 위해 온라인 후보 검증에서도 query embedding 캐시 전략을 명확히 분리한다. |
| Reranker | 현재는 Qdrant 후보 + BM25/RRF/keyword/chunk 조합 | cross-encoder reranker 또는 LLM rerank를 topN 후보 뒤에 붙여 NDCG와 Precision을 비교한다. |
| Metadata filter | category/tag를 완화 단계로 사용 | 카테고리/태그가 적은 게시글에서도 후보를 과하게 좁히지 않도록 완화 순서를 조정한다. |
| 관련도 라벨 | 6케이스는 관련 게시글 id 목록, 일부 시나리오는 사람이 판정 | 수동 라벨링 세트를 늘려 평가셋 편향을 줄인다. |
| 청크 크기 | 오프라인 민감도 분석 완료, 운영 재임베딩 실험은 분리 | `750/112`, `900/135`, `1350/203`을 실제 재임베딩하고 Qdrant까지 포함해 온라인 재측정한다. |
