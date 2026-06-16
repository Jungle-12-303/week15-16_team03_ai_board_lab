# 4장. RAG 구현

RAG는 Retrieval-Augmented Generation의 약자입니다. LLM에게 바로 "글 써줘"라고 하는 대신, 먼저 관련 자료를 검색하고 그 자료를 근거로 답변이나 초안을 생성합니다.

Project Alpha의 RAG 기능은 글쓰기 도우미입니다.

```text
사용자가 작성 중인 글
-> 유사 게시글 검색
-> 관련성이 강한 글만 선별
-> LLM에 근거로 전달
-> 초안 생성
```

## 코드로 바로 이동

| 확인할 흐름 | 코드 위치 |
| --- | --- |
| 프론트 RAG 버튼과 상태 | [PostForm.jsx](../../frontend/project-alpha/src/components/PostForm.jsx), [useRagDraft.js](../../frontend/project-alpha/src/hooks/useRagDraft.js), [ragApi.js](../../frontend/project-alpha/src/api/ragApi.js) |
| RAG API 입구 | [AiController.java](../../backend/src/main/java/com/jungle_choi/namanmu/api/AiController.java) |
| 유사 게시글 검색 | [SimilarPostSearchService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/SimilarPostSearchService.java) |
| 초안 생성 | [RagDraftService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/RagDraftService.java), [OpenAiTextClient.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/OpenAiTextClient.java) |
| 임베딩 생성 | [OpenAiEmbeddingClient.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/OpenAiEmbeddingClient.java), [PostEmbeddingService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/PostEmbeddingService.java) |
| 청킹 | [PostChunkTextSplitter.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/PostChunkTextSplitter.java), [PostEmbeddingChunk.java](../../backend/src/main/java/com/jungle_choi/namanmu/domain/embedding/PostEmbeddingChunk.java) |
| 임베딩 작업 큐 | [EmbeddingJobService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/EmbeddingJobService.java), [EmbeddingJobScheduler.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/EmbeddingJobScheduler.java), [EmbeddingJobProcessor.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/EmbeddingJobProcessor.java) |
| 검색 평가 | [RagEvaluationService.java](../../backend/src/main/java/com/jungle_choi/namanmu/service/rag/RagEvaluationService.java), [evaluate-rag-retrieval.mjs](../../scripts/evaluate-rag-retrieval.mjs), [RAGAS README](../../eval/ragas/README.md) |

## 1. Embedding

Embedding은 텍스트를 숫자 벡터로 바꾸는 과정입니다.

```text
"React hook 공부 기록"
-> [0.012, -0.233, ...]
```

이 프로젝트는 OpenAI `text-embedding-3-small` 모델을 사용합니다. embedding 모델을 직접 학습하지 않고, 상용 embedding API를 호출해 벡터를 생성합니다.

## 2. Chunking

게시글이 길면 통째로 하나의 embedding만 만들었을 때 중요한 부분이 묻힐 수 있습니다. 그래서 본문을 청크로 나눕니다.

```text
긴 게시글
-> chunk 0
-> chunk 1
-> chunk 2
```

청크 embedding은 `post_embedding_chunks`에 저장합니다. 게시글 전체 embedding은 `post_embeddings`에 저장합니다.

Project Alpha의 청킹 기준은 퍼센트가 아니라 문자 수와 문단입니다.

| 기준 | 값 |
| --- | ---: |
| 최대 청크 길이 | 1200자 |
| overlap | 180자 |

본문을 먼저 빈 줄 기준의 문단으로 나누고, 가능하면 문단을 보존한 채 1200자 안에 묶습니다. 새 청크를 만들 때는 이전 청크의 마지막 180자를 같이 넣어 경계에서 문맥이 끊기는 문제를 줄입니다. 문단 하나가 1200자를 넘으면 그 문단은 문자 길이 기준으로 강제로 자릅니다.

`1200자`는 정답값이 아니라 초기 기준값입니다. 현재 DB의 게시글 본문 평균이 약 3687자이고 실제 저장된 청크가 게시글당 평균 4.14개라서, 긴 글을 너무 잘게 쪼개지 않으면서도 문단 근거를 찾기 위한 시작점으로 사용했습니다. 이후 성능 비교 대상은 `600/90`, `900/135`, `1200/180`입니다.

## 3. Embedding job

게시글을 저장할 때 바로 OpenAI API를 호출하지 않고 `embedding_jobs`에 작업을 예약합니다.

이유는 두 가지입니다.

1. 게시글 저장 요청이 외부 API 속도에 묶이지 않게 하기 위해
2. 실패한 embedding 작업을 다시 처리할 수 있게 하기 위해

```text
PostService.createPost
-> EmbeddingJobService.enqueuePostEmbedding
-> EmbeddingJobScheduler
-> EmbeddingJobProcessor
-> OpenAiEmbeddingClient
-> PostEmbeddingService
```

작업 처리 중 OpenAI API 오류나 일시 장애가 발생하면 `attempt_count`를 올리고 최대 3회까지 다시 `PENDING` 상태로 돌립니다. 마지막 실패에서만 `FAILED`로 고정해 운영자가 실패 원인을 확인할 수 있게 했습니다.

## 4. Retrieval

Retrieval은 사용자가 쓴 글과 관련 있는 기존 게시글을 찾는 과정입니다.

Project Alpha는 다음 신호를 섞습니다.

| 신호 | 의미 |
| --- | --- |
| Vector similarity | 의미가 비슷한가 |
| BM25 | 중요한 단어가 직접 겹치는가 |
| Keyword alignment | 핵심 주제어가 맞는가 |
| Category/tag | 사용자가 선택한 분류와 태그가 맞는가 |
| Chunk evidence | 어느 청크에서 근거가 잡혔는가 |

Vector similarity는 의미가 비슷한 문장을 찾는 데 강하고, BM25는 `GitHub Actions`, `JWT`, `9800X3D`처럼 표면 단어가 중요한 검색에 강합니다. Project Alpha는 두 신호와 metadata 신호를 함께 사용합니다.

## 5. BM25를 왜 넣었나

BM25는 단어 기반 검색 알고리즘입니다. 검색어의 중요한 단어가 문서에 얼마나 의미 있게 등장하는지 봅니다.

BM25는 다음 요소를 반영합니다.

- 검색어 단어가 문서에 등장하는가
- 너무 흔한 단어인가, 드문 단어인가
- 문서가 너무 길어서 단어가 우연히 많이 나온 것은 아닌가

RAG에서 BM25는 vector search의 약점을 보완합니다. 예를 들어 `GitHub Actions`, `JWT`, `9800X3D` 같은 고유한 단어는 의미 벡터보다 단어 매칭이 더 강한 신호가 될 때가 많습니다.

## 6. Generation

검색된 유사 게시글을 모두 LLM에 넣지는 않습니다. 관련성이 약한 글이 프롬프트에 들어가면 초안의 근거 품질이 낮아집니다.

`RagDraftService`는 다음 기준으로 초안 근거를 줄입니다.

- 최소 점수 이하 source 제외
- matched terms가 부족한 source 제외
- 지역명 같은 민감한 조건이 어긋나는 source 제외
- 프롬프트에 넣는 source는 최대 3개로 제한

이 기준은 초안 생성보다 근거 품질을 우선합니다. 유사글이 부족하면 초안 생성 대신 fallback 메시지를 반환합니다.

## 7. 평가

Project Alpha는 두 종류의 평가를 둡니다.

| 평가 | 위치 | 목적 |
| --- | --- | --- |
| custom retrieval evaluation | `scripts/evaluate-rag-retrieval.mjs` | retrieval 개선 전후 점수 비교 |
| RAGAS | `eval/ragas` | 생성 결과의 faithfulness, relevancy 등을 평가 |

RAGAS 점수는 평가 케이스와 reference 품질의 영향을 받습니다. 이 프로젝트에서는 절대 점수보다 개선 전후 추세와 실패 케이스 탐색에 사용합니다.

## 8. 검색 구조의 적용 범위와 확장

현재 구현은 로컬 MySQL 중심의 검색 구조입니다.

- MySQL JSON vector를 서버에서 계산하므로 데이터가 많아지면 느려집니다.
- 전용 Vector DB의 ANN index를 쓰지 않습니다.
- Cross-encoder reranker는 아직 없습니다.
- 한국어 형태소 분석기를 본격적으로 쓰지 않습니다.

확장 후보:

- pgvector, OpenSearch, Chroma, Pinecone
- 형태소 분석 기반 BM25
- Cross-encoder reranker
- Query rewrite
- 검색 결과에 대한 LLM judge
