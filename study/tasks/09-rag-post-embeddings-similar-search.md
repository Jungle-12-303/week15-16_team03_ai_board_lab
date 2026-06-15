# 09. RAG: post_embeddings, fallback embedding, 유사 글 검색

## 현재 프로젝트 분석

- Service: `backend/src/main/java/com/example/aiknowledgeboard/ai/rag/RagService.java`
- Controller: `backend/src/main/java/com/example/aiknowledgeboard/ai/rag/RagController.java`
- OpenAI/fallback: `backend/src/main/java/com/example/aiknowledgeboard/ai/common/OpenAiClient.java`
- 로그: `backend/src/main/java/com/example/aiknowledgeboard/ai/common/AiLogService.java`
- DB: `post_embeddings` 테이블과 pgvector `<=>` cosine distance 검색

## 학습 목표

- [ ] RAG가 게시판에서 어떤 문제를 해결하는지 설명할 수 있다.
- [ ] 게시글 저장 시 임베딩을 함께 갱신하는 이유를 설명할 수 있다.
- [ ] pgvector SQL은 왜 JPA 메서드보다 `JdbcTemplate`이 적절한지 설명할 수 있다.
- [ ] OpenAI 키가 없을 때 fallback embedding이 왜 데모 안정성에 중요한지 이해한다.

## 공부할 때 참고해야 할 개념

- RAG: 저장된 문서를 검색한 뒤 LLM 답변에 참고 자료로 쓰는 구조
- Embedding: 텍스트 의미를 숫자 벡터로 바꾸는 방식
- Vector similarity: cosine similarity와 distance의 차이
- pgvector: PostgreSQL에서 vector 타입과 유사도 연산자를 제공하는 확장
- `JdbcTemplate`: JPA로 표현하기 어려운 SQL을 직접 실행하는 Spring 도구
- Upsert: `insert ... on conflict do update`로 생성과 갱신을 한 번에 처리하는 SQL
- Fallback design: 외부 API가 없어도 최소 기능이 동작하게 만드는 설계
- Prompt 구성: query, source, instruction을 나눠 LLM 입력을 만드는 방식
- AI logging: AI 기능의 입력, 출력, 실패 원인을 DB에 기록하는 이유
- Transaction 경계: 게시글 저장과 AI 인덱싱 실패 범위를 어디까지 묶을지 결정하는 기준

## 재구현 체크리스트

### 1. `RagController`에서 유사 글 검색 입구부터 만든다

- [ ] 유사 글 검색 endpoint를 먼저 만들고 임시 빈 결과를 반환한다.
- [ ] 화면에서 보낼 `query`, `excludePostId`, `limit` 값이 필요하다는 것을 확인한다.
- [ ] Controller가 검색 로직을 직접 처리하지 않고 `RagService.findSimilarPosts(...)`를 호출한다.
- [ ] `RagController` 필드와 생성자에 `RagService` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `RagService`를 넣어주려면 `RagService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `RagService`에 `@Service`를 붙인다.
- [ ] 유사 글이 없을 때도 API가 실패하지 않고 빈 목록이나 안내 메시지를 반환하게 한다.
- [ ] 검색 결과에는 `summary`와 `sources`가 필요하다는 것을 확인한다.

### 2. 검색하려는 순간 embedding 생성 도구가 필요해지는 것을 확인한다

- [ ] query를 embedding으로 변환한다.
- [ ] `RagService`에서 embedding을 만들려다 `OpenAiClient`가 필요하다는 것을 확인한다.
- [ ] `RagService` 필드와 생성자에 `OpenAiClient` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `OpenAiClient`를 넣어주려면 `OpenAiClient` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `OpenAiClient`에 `@Component`를 붙인다.
- [ ] `OpenAiClient`에서 API 키가 있으면 `/embeddings` API를 호출한다.
- [ ] 응답 embedding 배열을 `double[]`로 변환한다.
- [ ] API 키가 없으면 즉시 local embedding으로 fallback한다.
- [ ] 외부 API 호출 실패 시에도 local embedding으로 fallback한다.
- [ ] `toVectorLiteral(double[])`로 pgvector가 받을 수 있는 `[0.1,0.2,...]` 문자열을 만든다.

### 3. API 키 없이도 동작해야 하는 순간 local fallback embedding을 만든다

- [ ] 입력 텍스트를 소문자로 정규화한다.
- [ ] 영어/숫자/한글 토큰 기준으로 분리한다.
- [ ] 각 토큰을 hash해서 vector index를 정한다.
- [ ] hash 값으로 부호를 정해 벡터에 더한다.
- [ ] 마지막에 L2 normalize를 적용한다.
- [ ] 빈 텍스트는 0 벡터로 남게 한다.
- [ ] fallback 품질은 낮아도 데모 안정성에는 가치가 있음을 확인한다.

### 4. 검색할 저장소가 필요해지는 순간 `post_embeddings` 테이블을 이해한다

- [ ] `post_id`를 `UNIQUE`로 두어 게시글 1개당 임베딩 1개만 저장한다.
- [ ] `embedding vector(1536)`으로 벡터 값을 저장한다.
- [ ] `source_text`에 임베딩 생성에 사용한 원문을 저장한다.
- [ ] 게시글 삭제 시 임베딩도 삭제되도록 `ON DELETE CASCADE`를 둔다.
- [ ] cosine 검색용 ivfflat 인덱스를 둔다.
- [ ] pgvector SQL은 JPA query method보다 `JdbcTemplate`이 적절한 이유를 확인한다.

### 5. `RagService.findSimilarPosts`에 벡터 검색을 추가한다

- [ ] `RagService`에서 직접 pgvector SQL을 실행하려다 `JdbcTemplate`이 필요하다는 것을 확인한다.
- [ ] `RagService` 필드와 생성자에 `JdbcTemplate` 타입을 먼저 추가한다.
- [ ] `JdbcTemplate`은 Spring Boot JDBC 자동 설정으로 Bean이 준비된다는 것을 확인한다.
- [ ] `with q as (...)`로 query vector를 SQL 안에서 재사용한다.
- [ ] `post_embeddings`, `posts`, `users`를 join한다.
- [ ] `excludePostId`가 있으면 현재 게시글을 결과에서 제외한다.
- [ ] `<=>` cosine distance로 가까운 순서대로 정렬한다.
- [ ] score는 `1 - distance`로 계산한다.
- [ ] limit은 1 이상 10 이하로 제한한다.
- [ ] 검색 실패 시 빈 목록을 반환하고 실패 로그를 남긴다.

### 6. 게시글 저장 후 검색 대상이 없다는 문제를 보고 인덱싱을 추가한다

- [ ] `PostService` 작성/수정 성공 후 `RagService.indexPost(post)`를 호출한다.
- [ ] `PostService` 생성자에 `RagService` 타입을 추가했기 때문에 `RagService`가 Bean이어야 한다는 연결을 다시 확인한다.
- [ ] 게시글의 `title + content`를 `sourceText`로 만든다.
- [ ] sourceText로 embedding을 만든다.
- [ ] vector literal로 변환한다.
- [ ] `insert ... on conflict (post_id) do update`로 upsert한다.
- [ ] 인덱싱 성공/실패 로그가 필요해지면 `RagService` 생성자에 `AiLogService`를 추가한다.
- [ ] 이때 Spring이 `AiLogService`를 넣어주려면 `AiLogService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `AiLogService`에 `@Service`를 붙이고, `AiLogRepository`는 `JpaRepository`로 만든다.
- [ ] 성공하면 `ai_logs`에 `RAG_INDEX` 성공 로그를 남긴다.
- [ ] 실패하면 실패 로그를 남긴다.
- [ ] 게시글 저장 자체가 실패하지 않도록 호출 위치에서 RAG 예외를 격리한다.

### 7. 최종 사용자 응답이 필요해지는 순간 요약을 추가한다

- [ ] 유사 글이 없으면 안내 메시지를 반환한다.
- [ ] 유사 글 제목, 내용 일부, 링크를 prompt에 포함한다.
- [ ] OpenAI chat API가 있으면 요약을 생성한다.
- [ ] API 키가 없거나 실패하면 fallback 요약을 반환한다.
- [ ] 최종 응답은 `summary`와 `sources`로 나눈다.

## 검증 체크리스트

- [ ] 게시글 작성 후 `post_embeddings`에 row가 생기는지 확인한다.
- [ ] 게시글 수정 후 같은 `post_id` row가 update되는지 확인한다.
- [ ] OpenAI API 키 없이도 유사 글 검색 API가 실패하지 않는지 확인한다.
- [ ] `excludePostId`를 넘기면 자기 자신이 검색 결과에서 빠지는지 확인한다.
- [ ] 관련 게시글을 여러 개 넣고 score 순서가 유사도 기준으로 나오는지 확인한다.
- [ ] `ai_logs`에 RAG index/query 기록이 남는지 확인한다.

## WHY 정리 질문

- [ ] 왜 RAG 인덱싱은 게시글 저장 트랜잭션과 완전히 같은 실패 조건으로 묶지 않는가?
- [ ] 왜 벡터 검색 SQL은 JPA Repository 메서드 이름으로 구현하지 않았는가?
- [ ] 왜 fallback embedding 품질이 낮아도 과제 MVP에는 가치가 있는가?
- [ ] 왜 RAG 응답에 요약뿐 아니라 출처 게시글 목록을 함께 반환하는가?
