# Project Alpha 면접 질문 대비

이 문서는 Project Alpha를 처음 보는 면접관, 발표 심사자, 팀원이 던질 수 있는 질문을 강도별로 정리한 것이다. 답변은 방어적으로 말하기보다, 선택 이유와 한계를 함께 설명하는 방향으로 준비한다.

## 답변 기본 구조

질문이 날카로울수록 아래 순서로 답하면 안정적이다.

```text
인정
-> 선택 이유
-> 현재 구현
-> 한계
-> 다음 개선
```

예시:

```text
맞습니다. 현재 평가는 6케이스라 일반화에는 한계가 있습니다.
그래서 저는 이 점수를 절대 성능으로 보지 않고, 현재 데이터셋에서의 운영 기준선으로 봤습니다.
대신 offline/online 평가를 분리했고, 최종 채택은 실제 Spring API, Qdrant, MySQL, OpenAI 경로를 모두 태운 online 결과로 판단했습니다.
다음 단계는 holdout 평가셋 확장입니다.
```

## 1. 무심한 면접관

짧고 명확하게 답한다. 기능을 길게 설명하기보다 "무엇을 만들었는지"를 먼저 고정한다.

| 질문 | 답변 핵심 |
|---|---|
| 이 프로젝트 한 줄로 설명해보세요. | React/Spring Boot 기반 게시판에 RAG, MCP, Agent를 실제 글쓰기 흐름에 붙인 AI 게시판입니다. |
| RAG 기능은 뭔가요? | 작성 중인 제목/본문/태그로 기존 게시글을 검색하고, 그 게시글을 근거로 초안을 생성합니다. |
| MCP는 어디에 썼나요? | 게시글 상세에서 fact check 기능으로 사용했습니다. 내부 MCP server가 외부 GitHub/날씨 API를 호출하고 글의 주장과 비교합니다. |
| Agent는 뭘 하나요? | 사용자별 읽음 기록을 보고 이미 읽은 글은 제외한 뒤, 관심 태그/카테고리 기반으로 놓친 글 5개를 추천합니다. |
| DB는 뭘 썼나요? | MySQL은 원본 데이터, Qdrant는 벡터 검색 인덱스 역할로 분리했습니다. |
| 프론트와 백엔드는 무엇으로 만들었나요? | Frontend는 React/Vite, Backend는 Spring Boot 3.5와 Java 25로 만들었습니다. |

## 2. 관심 있는 면접관

구현 선택의 이유를 묻는 질문이다. "그냥 써봤다"가 아니라, 프로젝트 조건과 기능 요구에 맞춘 선택임을 설명한다.

| 질문 | 답변 핵심 |
|---|---|
| 왜 Spring Boot를 선택했나요? | 과제 선택지 중 하나였고, 인증/JPA/REST API/계층 분리에 적합했습니다. Java와 Spring의 백엔드 구조를 학습하기에도 좋았습니다. |
| 왜 MySQL만 쓰지 않고 Qdrant를 붙였나요? | MySQL은 게시글 원본과 관계형 데이터를 관리하고, Qdrant는 벡터 후보 검색을 담당하게 역할을 나눴습니다. 처음에는 MySQL 기반으로 했지만 데이터가 늘면 전체 벡터 스캔 비용이 커져 전용 Vector DB를 붙였습니다. |
| RAG 검색은 정확히 어떤 순서인가요? | 입력 글 임베딩 생성 -> Qdrant에서 유사 postId 후보 조회 -> MySQL에서 원본 게시글/태그/청크 조회 -> BM25, RRF, metadata, keyword, chunk evidence로 재정렬 -> 상위 결과를 초안 근거로 사용합니다. |
| BM25는 왜 넣었나요? | embedding만으로는 GitHub, Redis, CPU 같은 고유명사나 짧은 쿼리에서 흔들렸습니다. BM25는 단어 일치 신호를 잘 잡기 때문에 vector search를 보완했습니다. |
| RRF는 왜 썼나요? | vector 순위와 BM25 순위처럼 점수 스케일이 다른 검색 결과를 직접 더하기보다, 순위 기반으로 안정적으로 결합하기 위해 사용했습니다. |
| 한국어 형태소 분석기는 왜 넣었나요? | 한국어는 조사/어미 때문에 단순 공백 토큰화가 약합니다. Nori를 사용해 검색어의 의미 단위를 더 잘 잡도록 했습니다. |
| 청킹은 왜 했나요? | 긴 게시글 전체 하나만 임베딩하면 세부 근거가 묻힐 수 있습니다. 게시글 전체 임베딩은 주제 검색에 쓰고, 청크는 근거 보강용으로 약하게 반영했습니다. |
| 왜 RAG 기능을 챗봇이 아니라 글쓰기 모달에 넣었나요? | 이 서비스의 핵심 흐름이 게시글 작성이기 때문입니다. 사용자가 글을 쓰는 순간 기존 글을 찾고 초안을 만드는 편이 기능 목적과 더 잘 맞습니다. |

## 3. 의심하는 면접관

구현의 진짜 깊이, 한계 인식, 과장 여부를 확인하는 질문이다. 이 단계에서는 한계를 인정하되, 무엇을 검증했는지까지 같이 말한다.

| 질문 | 답변 방향 |
|---|---|
| 이거 그냥 GPT 붙인 것 아닌가요? | 단순 GPT 호출이면 사용자의 입력만 보내고 끝납니다. 이 프로젝트는 게시글 저장, 임베딩 작업, Qdrant 동기화, hybrid retrieval, RAGAS/검색 지표 평가, MCP 외부 도구 호출, Agent 읽음 상태 관리까지 서비스 흐름으로 연결했습니다. |
| MCP가 진짜 필요한가요? 그냥 API 호출 아닌가요? | 내부적으로는 API 호출이 맞습니다. 다만 MCP의 핵심은 AI 기능이 외부 도구를 정해진 인터페이스로 호출하게 만드는 것입니다. 이 프로젝트에서는 JSON-RPC 형태의 MCP server와 tool schema를 두고 GitHub/날씨 도구를 분리했습니다. |
| Agent가 그냥 추천 로직 아닌가요? | 단순 랜덤 추천은 아닙니다. observe, infer, retrieve, rank 단계로 나눴고, 사용자별 읽음 기록을 기반으로 관심사를 추론한 뒤 읽은 글을 제외합니다. 다만 자율 행동 범위는 제한적이라 운영형 autonomous agent보다는 상태 기반 추천 agent에 가깝다고 설명하는 것이 정확합니다. |
| 평가 케이스 6개면 너무 적지 않나요? | 맞습니다. 그래서 일반 성능이라고 주장하지 않았고, 현재 데이터셋의 운영 기준선으로 제한했습니다. 대신 같은 케이스에서 vector-only, BM25, metadata, chunk, Qdrant 조합을 비교했고, 최종은 실제 API를 태운 online 평가로 봤습니다. 다음 개선은 holdout 평가셋 확장입니다. |
| 점수 높은 조합이 아니라 낮은 조합을 채택한 이유는요? | 과거 점수와 현재 점수를 직접 비교하면 안 되는 구간이 있었습니다. Qdrant, Nori, 설정 파라미터화가 들어가며 검색 경로가 달라졌기 때문입니다. 최종 채택은 같은 현재 구조 안에서 baseline `NDCG@5 0.8599`에서 `0.9076`으로 개선된 online 후보 검증 결과를 기준으로 했습니다. |
| RAGAS 점수가 낮은 항목도 있던데요? | Answer relevancy가 낮은 실행이 있었습니다. 그래서 RAGAS를 검색 조합 선택의 주 지표로 쓰지 않고 생성 품질 보조 지표로 사용했습니다. 검색 품질은 MRR/NDCG/Precision을 보고, 생성 품질은 RAGAS와 시나리오 입출력으로 따로 봤습니다. |
| 크롤링 데이터는 괜찮나요? | 제출/학습용 로컬 데이터로 사용했습니다. 상업 서비스라면 저작권, robots.txt, 출처 표시, 원문 저장 범위 제한, 요약 저장 정책을 별도로 설계해야 합니다. 이 부분은 현재 한계로 명확히 인정합니다. |
| JWT를 localStorage에 저장하면 위험하지 않나요? | 위험합니다. 그래서 현재 구현은 access token을 localStorage에 두지 않고 httpOnly cookie로 내려보냅니다. 프론트는 token 값을 직접 읽지 않고 `credentials: include`로 인증 요청을 보냅니다. |
| `ddl-auto=update`는 실서비스에서 쓰면 안 되지 않나요? | 맞습니다. 로컬 기본값은 개발 속도를 위해 `update`지만, 설정값을 환경변수로 분리했고 Flyway 초기 migration을 추가했습니다. 배포 환경에서는 `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`와 Flyway migration을 기준으로 운영하는 게 맞습니다. |

## 4. RAG 깊이 질문

RAG는 이 프로젝트에서 가장 질문이 많이 나올 수 있는 영역이다. 단어 뜻보다 실제 파이프라인을 설명하는 데 집중한다.

| 질문 | 답변 핵심 |
|---|---|
| Retriever는 무엇인가요? | 사용자의 입력과 관련된 후보 문서를 찾아오는 컴포넌트입니다. Project Alpha에서는 Qdrant vector search가 1차 후보를 찾고, Spring Boot가 BM25/RRF 등으로 재정렬합니다. |
| 임베딩은 무엇인가요? | 텍스트를 의미를 담은 숫자 벡터로 바꾸는 것입니다. 의미가 가까운 문장은 벡터 공간에서도 가깝게 배치되도록 만들어 검색에 사용합니다. |
| cosine similarity는 왜 쓰나요? | 벡터의 방향 유사도를 봅니다. 텍스트 길이나 크기보다 의미 방향이 가까운지 보는 데 적합해서 임베딩 검색에서 흔히 사용됩니다. |
| cosine 외에 어떤 방식이 있나요? | dot product, euclidean distance, BM25 같은 lexical retrieval, hybrid retrieval, cross-encoder reranker, HNSW 기반 ANN 검색이 있습니다. |
| Qdrant는 정확히 무슨 일을 하나요? | OpenAI embedding으로 만든 벡터를 저장하고, query embedding과 가까운 postId/chunk postId 후보를 빠르게 찾아줍니다. |
| Qdrant와 MySQL의 데이터 정합성은 어떻게 맞추나요? | 게시글 저장 후 embedding job을 만들고, embedding 생성 시 MySQL에 저장한 뒤 Qdrant에 upsert합니다. 기존 임베딩은 sync API로 Qdrant에 재동기화할 수 있습니다. |
| 임베딩 실패하면 어떻게 되나요? | `embedding_jobs` 테이블로 작업 상태를 관리하고, 실패 상태를 남길 수 있습니다. 현재는 로컬 테스트 중심이라 재시도/백오프는 보완 대상입니다. |
| 왜 pgvector가 아니라 Qdrant인가요? | 과제 DB로 MySQL을 유지하면서 벡터 검색만 분리하기 위해 Qdrant를 선택했습니다. Postgres를 썼다면 pgvector도 좋은 선택이었을 겁니다. |
| 왜 1200자 청크였나요? | 너무 작으면 문맥이 깨지고, 너무 크면 세부 근거가 희석됩니다. 750/900/1200/1350 등 후보를 비교했고, 현재는 전체글 primary + chunk evidence 보조 방식으로 사용했습니다. |
| 왜 청크 검색을 primary로 쓰지 않았나요? | 청크만 primary로 쓰면 세부 단락은 잘 잡지만 게시글 전체 주제가 약해질 수 있습니다. 그래서 전체글 임베딩을 주 검색기로 쓰고, 청크는 근거 보강용으로 약하게 반영했습니다. |
| score cutoff는 왜 최종 채택하지 않았나요? | 단순 threshold는 낮은 점수의 잡음은 줄일 수 있지만, 높은 점수의 오탐을 처리하지 못했습니다. 실제 제목/본문을 보니 점수만 높고 주제가 어긋나는 경우가 있어 최종 구조에는 강한 cutoff를 넣지 않았습니다. |

## 5. MCP 질문

MCP는 "API 호출을 포장한 것"처럼 보일 수 있다. 핵심은 도구 호출의 표준화와 외부 시스템 접근 책임 분리라고 설명한다.

| 질문 | 답변 핵심 |
|---|---|
| MCP를 한 문장으로 설명하면요? | AI 기능이 외부 시스템을 정해진 도구 인터페이스로 호출하게 만드는 연결 규약입니다. |
| 이 프로젝트의 MCP server는 어디에 있나요? | Spring Boot 내부에 JSON-RPC 형태의 MCP server 계층을 두고, GitHub repository summary와 weather forecast 도구를 연결했습니다. |
| fact check는 어떻게 동작하나요? | 게시글 내용에서 GitHub 저장소나 날씨 관련 주장을 찾고, MCP tool로 외부 데이터를 가져온 뒤, 게시글 주장과 실제 데이터를 비교해 판정을 반환합니다. |
| 왜 MCP를 글 생성 버튼에 두지 않고 상세 페이지로 뺐나요? | RAG 초안 생성과 MCP 외부 데이터 호출이 모두 글 작성에 있으면 기능 경계가 흐려집니다. 그래서 RAG는 글쓰기 보조, MCP는 이미 작성된 글의 외부 사실 검증으로 역할을 나눴습니다. |
| MCP의 한계는요? | 현재는 GitHub와 날씨 도구 중심입니다. 더 좋은 fact check를 위해서는 뉴스, 공공데이터, Jira/Slack 등 도구를 확장하고, tool 선택 로직도 정교하게 만들어야 합니다. |

## 6. Agent 질문

Agent라는 이름이 과하게 보이지 않도록 "자율 범위가 제한된 상태 기반 추천 agent"라고 정직하게 설명한다.

| 질문 | 답변 핵심 |
|---|---|
| Agent는 무엇을 관찰하나요? | 사용자별 최근 읽은 글, 태그, 카테고리 흐름을 관찰합니다. |
| Agent는 어떻게 추천하나요? | observe 단계에서 읽음 기록을 보고, infer 단계에서 관심사를 추론하고, retrieve 단계에서 후보 글을 가져온 뒤, rank 단계에서 읽은 글 제외와 관심사 점수화를 수행합니다. |
| 추천 5개는 랜덤인가요? | 아닙니다. 읽음 기록과 태그/카테고리 기반 점수를 사용하며, 이미 읽은 글은 제외합니다. |
| 읽음 기록은 무한히 저장하나요? | 사용자별 최근 읽음 기록은 과도하게 늘지 않도록 제한하는 방향으로 설계했습니다. 실서비스에서는 retention 정책과 집계 테이블을 별도로 둘 수 있습니다. |
| Agent가 스스로 도구를 선택하나요? | 현재 Agent는 추천 목적에 맞춰 제한된 상태와 도구를 사용합니다. 완전 자율형 agent라기보다, 추론 단계를 명시한 추천 agent에 가깝습니다. |

## 7. Frontend 질문

React를 처음 배운 프로젝트라는 점을 숨기지 말고, 컴포넌트와 hook을 왜 나눴는지 설명한다.

| 질문 | 답변 핵심 |
|---|---|
| React에서 커스텀 훅은 왜 만들었나요? | API 호출과 상태 관리가 App 컴포넌트에 몰리면 읽기 어렵습니다. 게시글 목록/검색/페이지 상태를 hook으로 분리해 화면 컴포넌트는 렌더링에 집중하게 했습니다. |
| 커스텀 훅은 어떻게 만들었나요? | `use`로 시작하는 함수를 만들고, 내부에서 `useState`, `useEffect`, API 호출 함수를 조합했습니다. 반환값으로 화면에서 필요한 상태와 이벤트 핸들러를 제공했습니다. |
| 모달로 글 작성을 뺀 이유는요? | 메인 피드는 게시글 탐색에 집중하고, 글 작성은 별도 흐름으로 분리하기 위해서입니다. 작성 중 RAG 버튼도 같이 있어 글쓰기 UX가 한 화면에 모입니다. |
| 페이징은 왜 그렇게 했나요? | 페이지가 많아질 때 전체 번호를 나열하면 UI가 깨집니다. 그래서 첫 페이지, 현재 주변, 마지막 페이지 중심으로 보여주도록 했습니다. |
| 왜 AI 기능을 별도 페이지로 빼지 않았나요? | 이 프로젝트의 목표는 AI 데모 페이지가 아니라 AI가 게시판 사용 흐름 안에서 조용히 작동하는 것입니다. 그래서 RAG는 글쓰기, MCP는 상세, Agent는 메인 추천에 배치했습니다. |

## 8. Backend 질문

Spring Boot 기본 구조와 JPA 설계를 설명하는 질문이다.

| 질문 | 답변 핵심 |
|---|---|
| Controller, Service, Repository를 왜 나눴나요? | Controller는 HTTP 요청/응답, Service는 비즈니스 로직, Repository는 DB 접근으로 책임을 분리하기 위해서입니다. |
| Entity와 DTO를 왜 나눴나요? | Entity를 그대로 API에 노출하면 DB 구조와 API 계약이 강하게 묶입니다. DTO를 두면 응답 형태를 안정적으로 관리할 수 있습니다. |
| JPA 연관관계는 어떻게 잡았나요? | User-Post, Post-Comment, Post-Tag, PostRead 등 기능별 테이블로 분리했습니다. 태그는 다대다 성격이라 중간 테이블을 둔 구조입니다. |
| 테이블은 SQL로 직접 만든 건가요? | 로컬 개발에서는 JPA Entity와 `ddl-auto=update`를 통해 Hibernate가 테이블을 생성/갱신했습니다. 배포에서는 migration 도구로 관리하는 것이 맞습니다. |
| 인증은 어떻게 처리했나요? | 로그인 성공 시 서버가 access token과 refresh token을 httpOnly cookie로 내려줍니다. Spring Security filter가 access token을 검증하고, access token이 만료되면 refresh token rotation으로 새 토큰을 발급합니다. |
| 예외 처리는 어떻게 했나요? | 기능별로 필요한 실패 응답을 정리했습니다. 아직 공통 예외 처리와 에러 응답 표준화는 개선 여지가 있습니다. |

## 9. 보안과 운영 질문

학습 프로젝트의 선택과 실서비스 기준을 구분해서 답한다.

| 질문 | 답변 핵심 |
|---|---|
| API key는 어떻게 관리하나요? | `.env`나 환경 변수로 주입하고 Git에는 커밋하지 않습니다. `.env.example`에는 키 이름만 둡니다. |
| AWS에 올리면 어떤 점을 바꿔야 하나요? | RDS/MySQL, ECS 또는 EC2, S3, CloudWatch 비용 알람, 보안 그룹, VPC, secret 관리, migration 전략을 추가해야 합니다. |
| 비용은 어떻게 관리하나요? | OpenAI 호출은 embedding job과 draft generation에서 비용이 발생합니다. 대량 import/평가에는 limit와 캐시를 두고, 실서비스에서는 rate limit과 batch 처리가 필요합니다. |
| 장애 대응은요? | 현재는 로컬 개발 기준입니다. 실서비스라면 OpenAI/Qdrant/GitHub API 장애에 대한 timeout, retry, fallback message, circuit breaker를 설계해야 합니다. |
| 개인정보 이슈는요? | 게시글/읽음 기록이 개인화 추천에 쓰이므로, 실서비스에서는 보관 기간, 삭제 요청, 데이터 최소화, 접근 권한 관리가 필요합니다. |

## 10. 마지막 압박 질문

면접 후반에 자주 나오는 질문이다. 가장 중요한 것은 "다음 개선 우선순위"를 말할 수 있는지다.

| 질문 | 좋은 답변 방향 |
|---|---|
| 다시 만든다면 뭐부터 고칠 건가요? | 평가셋 확장, migration 도입, 관리자용 세션 관리, embedding job retry, reranker 도입을 우선하겠습니다. |
| 가장 어려웠던 점은요? | RAG가 embedding 하나로 끝나지 않는다는 점이었습니다. 실제로는 검색 후보 생성, BM25, RRF, 형태소 분석, 청크 근거, 평가 방식이 함께 맞물렸습니다. |
| 본인이 직접 이해한 부분은 어디까지인가요? | React 상태 관리부터 Spring Boot API, JPA 테이블 설계, JWT 인증, RAG 검색 파이프라인, Qdrant 동기화, MCP 도구 호출, Agent 추천 로직까지 전체 흐름을 직접 따라가며 구현했습니다. |
| 이 프로젝트의 가장 큰 한계는요? | 평가셋 규모와 실서비스 운영 안정성입니다. 기능은 end-to-end로 구현했지만, 실제 서비스 수준으로 가려면 대규모 평가셋, 배포 migration, 보안 강화, 장애 대응이 필요합니다. |
| 이 프로젝트에서 가장 자랑할 만한 점은요? | AI 기능을 따로 붙인 데모가 아니라, 게시판의 글쓰기, 상세 검증, 추천 흐름 안에 RAG/MCP/Agent를 각각 다른 책임으로 배치한 점입니다. |

## 11. 정글 코치진 예상 질문

정글 코치진은 "잘 만들었는가"뿐 아니라 "본인이 이해하면서 만들었는가", "과제 요구를 정확히 읽었는가", "학습 과정이 남아 있는가"를 볼 가능성이 높다.

### 학습 과정 검증

| 질문 | 답변 핵심 |
|---|---|
| React를 처음 배웠다고 했는데, 어디까지 직접 이해했나요? | 컴포넌트, props, state, list rendering, conditional rendering, custom hook, router, API 호출 흐름까지 직접 작은 예제로 확인하고 게시판에 적용했습니다. |
| Spring Boot는 처음이었는데 어떤 순서로 배웠나요? | Controller/Service/Repository/Entity 구조, JPA, MySQL 연결, Spring Security/JWT, API 예외 처리, 외부 API 호출 순서로 학습했습니다. |
| AI 기능부터 만들지 않고 게시판부터 만든 이유는요? | RAG/MCP/Agent가 붙을 실제 서비스 맥락이 필요했습니다. 게시글, 댓글, 태그, 로그인, 검색이 있어야 AI 기능도 의미 있게 붙습니다. |
| 처음 설계와 최종 구현이 달라진 지점은요? | 처음에는 MySQL에 임베딩을 저장하고 서버에서 계산하는 구조였지만, 데이터가 늘며 Qdrant를 도입했습니다. MCP도 작성 버튼의 외부 데이터 생성에서 상세 페이지 fact check로 역할을 바꿨습니다. |
| 가장 많이 헤맨 개념은 무엇인가요? | RAG에서 embedding만 잘하면 되는 줄 알았는데, 실제로는 retriever, BM25, RRF, metadata, chunk, 평가 지표가 함께 작동해야 한다는 점이 어려웠습니다. |
| 학습 프로젝트로서 가장 얻은 것은요? | 프론트, 백엔드, DB, 인증, AI 기능, 평가를 따로 보는 것이 아니라 하나의 요청 흐름으로 연결해서 이해한 점입니다. |
| AI에게 도움을 받았다면 본인은 무엇을 했나요? | 방향과 코드를 검토하며 단계별로 직접 실행, diff 확인, 오류 수정, 커밋 단위 학습을 했습니다. 최종적으로 각 기능의 흐름과 선택 이유를 설명할 수 있게 만드는 것을 목표로 했습니다. |

### 과제 요구사항 검증

| 질문 | 답변 핵심 |
|---|---|
| 과제의 필수 요구사항을 모두 충족했나요? | React frontend, Spring Boot backend, MySQL DB, 회원가입/로그인, 게시글 CRUD, 댓글, 태그, 페이징, 검색, RAG, MCP, Agent를 구현했습니다. |
| 게시판 CRUD 중 실제 DB와 연결된 것은 어디까지인가요? | 게시글 목록/상세/생성/수정/삭제, 댓글, 태그, 검색, 페이징이 서버 API와 MySQL 기준으로 동작합니다. |
| RAG, MCP, Agent가 서로 어떻게 다릅니까? | RAG는 내부 게시글 검색과 초안 생성, MCP는 외부 시스템 호출과 fact check, Agent는 사용자 상태 기반 추천입니다. |
| 왜 AI 기능을 세 개 다 비슷한 글 생성으로 만들지 않았나요? | 요구사항을 기능적으로 구분하기 위해 RAG는 글쓰기 보조, MCP는 외부 사실 검증, Agent는 놓친 글 추천으로 책임을 분리했습니다. |
| 데모에서 가장 먼저 보여줄 기능은 무엇인가요? | 기본 게시판이 서버 DB로 동작하는 것을 먼저 보여주고, 그 위에서 RAG 글쓰기, MCP 검증, Agent 추천을 순서대로 보여줍니다. |
| 발표 시간이 짧으면 무엇을 생략하겠습니까? | RAG를 live demo로 보여주고, MCP/Agent는 스크린샷과 구조 설명으로 압축하겠습니다. |

### 구현 직접성 검증

| 질문 | 답변 핵심 |
|---|---|
| 이 코드에서 가장 자신 있게 설명할 수 있는 파일은요? | RAG 검색 흐름을 담당하는 service 계층, 게시글 API 흐름, React custom hook과 글 작성 모달 흐름을 설명할 수 있습니다. |
| API 하나를 처음부터 끝까지 따라가보세요. | 예를 들어 글 작성은 React form submit -> postApi POST -> Spring Controller -> Service -> Repository/JPA -> MySQL 저장 -> embedding job 생성 흐름입니다. |
| RAG 버튼을 누르면 서버에서 어떤 일이 일어나나요? | 입력 텍스트로 query embedding을 만들고 Qdrant 후보를 찾은 뒤, MySQL 원본을 조회하고 hybrid ranking으로 재정렬해 응답합니다. |
| MCP fact check 버튼을 누르면 서버에서 어떤 일이 일어나나요? | 게시글 내용을 분석해 GitHub/날씨 도구 중 적절한 도구를 선택하고, 외부 API 응답과 게시글 claim을 비교해 verdict를 반환합니다. |
| Agent 추천 버튼을 누르면 서버에서 어떤 일이 일어나나요? | 로그인 사용자의 읽음 기록을 조회하고 관심 태그/카테고리를 추론한 뒤, 읽은 글을 제외한 후보를 점수화해 5개를 반환합니다. |

### 회고와 성장 질문

| 질문 | 답변 핵심 |
|---|---|
| 이번 프로젝트에서 설계를 바꾼 가장 좋은 결정은요? | MCP를 글 생성 기능에서 분리해 fact check로 바꾼 것입니다. RAG와 역할이 겹치지 않게 되었고 외부 도구 사용 이유가 더 명확해졌습니다. |
| 가장 아쉬운 결정은요? | 초기에 평가셋을 더 일찍 설계하지 못한 점입니다. 검색 품질을 체감으로만 보면 방향이 흔들려서, 지표와 시나리오 평가를 나중에 보강했습니다. |
| 다음 주에 하루 더 있다면 무엇을 하겠습니까? | RAG 결과 캡처 추가, holdout 평가셋 확장, MCP 도구 추가, 관리자용 세션 관리, migration 도입 중 하나를 우선하겠습니다. |
| 팀 프로젝트라면 어떻게 나누겠습니까? | frontend/UI, Spring API/JPA, RAG/search, MCP/external tools, Agent/evaluation으로 역할을 나눌 수 있습니다. |
| 본인이 이 프로젝트에서 가장 많이 성장한 부분은요? | 단일 기능 구현보다 기능을 끝까지 연결하고 평가/문서/발표까지 준비하는 흐름을 배운 점입니다. |

## 12. 꼬리질문 세트

면접에서는 한 질문 뒤에 바로 꼬리질문이 붙는다. 아래는 질문 흐름 자체를 대비하기 위한 표다.

| 시작 질문 | 이어질 수 있는 꼬리질문 | 답변 방향 |
|---|---|---|
| 왜 Qdrant를 썼나요? | pgvector, Chroma, FAISS와 비교하면요? | MySQL 유지 조건 때문에 별도 Vector DB가 적합했다. 운영형 API와 dashboard, HNSW 기반 검색을 제공하는 Qdrant를 선택했다. |
| 왜 BM25를 넣었나요? | embedding이 의미 검색인데 BM25가 왜 필요하죠? | embedding은 의미 유사도에 강하지만 고유명사/짧은 쿼리에 약할 수 있다. BM25는 단어 일치 신호를 보완한다. |
| RRF가 뭔가요? | 그냥 점수 더하면 안 되나요? | vector score와 BM25 score는 스케일이 다르다. RRF는 순위 기반 결합이라 서로 다른 검색기의 결과를 안정적으로 합친다. |
| RAGAS는 왜 썼나요? | 그럼 RAGAS 점수가 최종 품질인가요? | 아니다. RAGAS는 생성 답변의 근거성과 관련성을 보는 보조 지표이고, 검색 랭킹은 MRR/NDCG/Precision으로 따로 평가했다. |
| Agent라고 부를 수 있나요? | 자율적으로 행동하지 않는데요? | 완전 자율형 agent는 아니다. 상태 관찰, 추론, 후보 검색, 랭킹 단계를 명시한 제한형 추천 agent로 보는 것이 정확하다. |
| MCP가 꼭 필요한가요? | 그냥 service에서 API 호출하면 안 되나요? | 구현만 보면 가능하다. 하지만 과제 요구는 MCP였고, 외부 도구 호출을 JSON-RPC/tool 단위로 분리해 AI 기능과 외부 시스템 사이의 경계를 만들었다. |
| JPA로 테이블을 만들었다고요? | 운영에서도 그렇게 하나요? | 로컬 개발은 `ddl-auto=update`를 병행했다. 다만 설정값을 환경변수로 뺐고, 초기 스키마는 Flyway migration 파일로 기록했다. 운영에서는 `validate`와 Flyway migration이 맞다. |
| JWT는 안전한가요? | localStorage면 XSS에 취약하지 않나요? | 현재는 localStorage가 아니라 httpOnly cookie를 쓴다. 쿠키 인증으로 바꾼 뒤에는 Spring Security CSRF token과 refresh token rotation도 함께 적용했다. 운영 수준에서는 secure cookie, CSP/XSS 대응, 관리자용 세션 관리까지 더 봐야 한다. |
| 크롤링 데이터는 합법인가요? | 실제 서비스라면요? | 학습/로컬 평가용으로 사용했다. 실제 서비스는 robots.txt, 저작권, 출처, 저장 범위, 삭제 정책을 설계해야 한다. |
| 평가 점수가 좋은데 믿을 수 있나요? | 6케이스 아닌가요? | 일반화 점수가 아니라 현재 데이터셋 기준선이다. 그래서 한계로 명시했고 holdout 확장이 다음 개선이다. |

## 13. 추가 질문 은행

아래 질문은 전부 답변을 길게 준비할 필요는 없지만, 최소한 한두 문장으로 방향을 말할 수 있어야 한다.

### 아키텍처

| 질문 | 답변 키워드 |
|---|---|
| 전체 요청 흐름을 그림 없이 설명해보세요. | Browser -> React -> Spring REST API -> MySQL/Qdrant/OpenAI/MCP |
| 왜 frontend와 backend 서버를 따로 띄웠나요? | 개발 단계에서 Vite dev server와 Spring Boot API server 분리 |
| 실무에서도 서버를 두 개 띄우나요? | 개발은 분리, 배포는 정적 빌드 서빙/API 분리 또는 reverse proxy 구성 가능 |
| API gateway나 nginx는 왜 없나요? | 로컬 학습 프로젝트라 제외, 배포 시 도입 가능 |
| Docker Compose는 어떤 역할인가요? | MySQL/Qdrant 로컬 인프라 재현 |
| 상태를 어디에 저장하나요? | 인증 token은 frontend, 원본 데이터와 읽음 기록은 MySQL, 벡터 인덱스는 Qdrant |
| 서버를 재시작하면 데이터가 유지되나요? | Docker volume으로 MySQL/Qdrant 데이터 유지 |

### 데이터베이스와 JPA

| 질문 | 답변 키워드 |
|---|---|
| users, posts, comments, tags의 관계는요? | User 1:N Post, Post 1:N Comment, Post N:M Tag |
| 태그를 문자열로 저장하지 않은 이유는요? | 검색/중복 관리/관계 표현을 위해 별도 tags와 post_tags 사용 |
| post_reads는 왜 필요한가요? | Agent 추천에서 사용자별 읽음 기록 기반 제외/관심사 추론 |
| embedding_jobs는 왜 필요한가요? | 게시글 저장과 임베딩 생성을 분리하는 비동기 작업 큐 역할 |
| post_embeddings와 Qdrant가 둘 다 있으면 중복 아닌가요? | MySQL은 원본/재동기화 기준, Qdrant는 검색 인덱스 |
| N+1 문제는 없나요? | 현재 규모에서는 큰 병목은 아니지만 fetch join/entity graph/쿼리 최적화가 개선 포인트 |
| 삭제 시 연관 데이터는 어떻게 되나요? | 댓글/태그 연결/임베딩/읽음 기록의 정리 정책이 필요하며 cascade와 서비스 삭제 로직으로 관리 |
| 인덱스는 충분한가요? | 검색/페이징/외래키 기준으로 추가 가능, 현재는 기능 구현 중심 |

### 인증과 보안

| 질문 | 답변 키워드 |
|---|---|
| 비밀번호는 어떻게 저장하나요? | 평문 저장 금지, password encoder 사용 |
| JWT secret은 어디에 있나요? | 환경 변수, `.env.example`에는 예시만 |
| 기본 secret으로 운영 서버가 뜨면 위험하지 않나요? | `APP_SECURITY_PRODUCTION_MODE=true`일 때 기본 JWT secret을 쓰면 서버가 시작되지 않게 막았습니다. 운영에서는 secret manager나 배포 환경변수로 별도 secret을 주입해야 합니다. |
| 토큰 만료는 어떻게 처리하나요? | access token과 refresh token을 분리했고, refresh token은 DB에 해시로 저장한 뒤 재발급 때마다 rotation합니다. |
| 로그인 비밀번호를 계속 틀리면 어떻게 되나요? | `LoginAttemptService`가 계정 기준 실패 횟수를 기록합니다. 기본값은 10분 안에 5회 실패 시 5분 잠금입니다. 현재는 단일 서버 인메모리라 다중 서버 운영에서는 Redis 같은 공유 저장소로 옮겨야 합니다. |
| CORS는 왜 필요하나요? | Vite dev server와 Spring Boot API origin이 다르기 때문 |
| 권한 체크는 어디서 하나요? | Spring Security filter와 API/service owner 검증 |
| 다른 사용자의 게시글을 수정할 수 있나요? | owner 검증 필요, 발표 전 실제 동작 확인 포인트 |
| API key가 노출되면 어떻게 되나요? | 즉시 폐기/재발급, secret manager 사용 |

### RAG 평가

| 질문 | 답변 키워드 |
|---|---|
| Hit@5는 정확도를 의미하나요? | 아니다. top5 안에 관련 글이 하나라도 있는지 |
| Precision@5와 Recall@5 차이는요? | top5 중 관련 비율 vs 전체 관련 글 중 top5 회수 비율 |
| MRR@5가 중요한 이유는요? | 첫 관련 글이 얼마나 위에 나오는지, 글쓰기 UX에서 중요 |
| NDCG@5는 왜 보나요? | 관련 글이 상위에 잘 배치됐는지 |
| Recall이 낮은데 괜찮나요? | 이 기능은 전체 탐색보다 top3~5 추천이 목적이라 MRR/NDCG를 더 중시 |
| 오프라인 평가와 온라인 평가 차이는요? | 오프라인은 DB/스크립트 계산, 온라인은 실제 API/Qdrant/OpenAI/MySQL 경로 |
| 왜 오프라인 점수 높은 조합을 바로 채택하지 않았나요? | 실제 Qdrant 후보 제한과 서버 ranking 상호작용을 반영하지 못하기 때문 |
| 평가셋 라벨은 누가 만들었나요? | 프로젝트 대표 시나리오 기준으로 관련 post id를 고정 |
| 점수 외에 실제 출력도 봤나요? | 시나리오 입출력 평가로 추천 제목과 초안 일부를 확인 |

### RAG 구현

| 질문 | 답변 키워드 |
|---|---|
| 새 글을 작성하면 언제 임베딩되나요? | 게시글 저장 후 embedding job 생성, worker가 처리 |
| 임베딩 모델을 바꾸면 어떻게 되나요? | 기존 벡터와 차원이 달라질 수 있어 재임베딩 필요 |
| Qdrant collection은 몇 개인가요? | post-level collection과 chunk-level collection |
| query에 title과 content를 모두 쓰는 이유는요? | 제목은 핵심 의도, 본문은 문맥 |
| 태그는 검색에 어떻게 반영되나요? | metadata/category/tag signal로 ranking에 보조 반영 |
| LLM이 근거와 다른 말을 하면요? | prompt 제한, source filtering, MCP fact check, RAGAS 평가로 보완 |
| 유사글이 없을 때는요? | 억지 생성보다 근거 부족 안내 또는 약한 결과로 처리 |

### MCP 구현

| 질문 | 답변 키워드 |
|---|---|
| JSON-RPC는 왜 나왔나요? | MCP 요청/응답 형식 구현 |
| tool schema는 왜 필요한가요? | 외부 도구 호출 인터페이스 명확화 |
| GitHub 도구는 어떤 데이터를 가져오나요? | repository 이름, 설명, 주 언어, 기본 브랜치, 라이선스 등 |
| 날씨 도구는 왜 유지하나요? | 외부 공공/실시간 데이터 도구 예시, fact check 확장 가능성 |
| tool 선택은 완전 자동인가요? | 현재는 게시글 내용 기반 제한적 선택, 더 정교한 router 가능 |
| 외부 API 실패 시에는요? | timeout/fallback/error message 필요, 현재 개선 포인트 |

### Agent 구현

| 질문 | 답변 키워드 |
|---|---|
| 추천 근거를 사용자에게 보여주나요? | reasoning steps로 observe/infer/retrieve/rank 흐름 제공 |
| 읽음 기록이 없으면요? | 최신 글/인기 태그/카테고리 기본 추천으로 fallback 가능 |
| 같은 글이 계속 추천되면요? | 읽음 기록 제외, 추천 이력까지 추가하면 개선 가능 |
| 50개 제한은 왜 있나요? | 사용자별 상태가 무한히 커지는 것을 막기 위해 |
| 개인화가 과하면 필터버블 아닌가요? | 다양성 점수나 탐색 후보를 섞는 방식으로 개선 가능 |

### Frontend와 UI/UX

| 질문 | 답변 키워드 |
|---|---|
| 왜 사이드바에 태그 전체 목록을 안 뒀나요? | 태그는 무한히 늘 수 있어 검색/필터 입력 방식이 더 적합 |
| 왜 글 작성 영역을 항상 노출하지 않았나요? | 피드 탐색과 작성 흐름 분리, 모달 집중도 |
| 긴 글은 어떻게 처리했나요? | 5줄 이상 접기/펼쳐보기 |
| 댓글은 왜 상세 페이지 중심인가요? | 메인 피드 복잡도 감소, 상세에서 맥락 확인 |
| AI 기능 버튼이 너무 많아 보이지 않나요? | 기능별 위치를 분리해 RAG는 작성, MCP는 상세, Agent는 메인 추천 |
| 접근성은 고려했나요? | 기본 form/button 구조 사용, 더 개선하려면 aria와 keyboard focus 보강 |

### 테스트와 품질

| 질문 | 답변 키워드 |
|---|---|
| 어떤 테스트를 돌렸나요? | backend `gradlew test`, frontend `npm run build`, 수동 QA checklist |
| 자동 테스트가 충분한가요? | 충분하지 않다. 핵심 service unit test와 API integration test 확장 필요 |
| 발표 전 QA는 어떻게 하나요? | docs/qa-checklist.md 기준으로 로그인, CRUD, RAG, MCP, Agent 수동 확인 |
| 가장 깨지기 쉬운 기능은요? | 외부 API와 OpenAI/Qdrant 의존성이 있는 RAG/MCP |
| 로그는 어떻게 확인하나요? | Spring Boot console log, DB table, Qdrant dashboard |
| 장애 재현은 어떻게 하나요? | OpenAI key 제거, Qdrant 중지, GitHub URL 없는 글로 fact check |

### 배포와 AWS

| 질문 | 답변 키워드 |
|---|---|
| AWS에 올린다면 어떤 구조인가요? | React 정적 빌드, Spring Boot 서버, RDS MySQL, Qdrant 컨테이너/관리형 대안, CloudWatch |
| S3는 어디에 쓰나요? | 정적 파일 배포 또는 첨부파일 저장 |
| EC2와 ECS 중 무엇을 쓰겠나요? | 학습/단순 배포는 EC2, 컨테이너 운영은 ECS |
| RDS를 쓰면 뭐가 좋아지나요? | 백업, 모니터링, 관리형 DB 운영 |
| 비용 알람은 왜 필요한가요? | OpenAI/AWS 리소스 비용 초과 방지 |
| HTTPS는 어떻게 붙이나요? | ALB/Nginx/CloudFront와 인증서 사용 |

## 14. 프레임워크 질문

과제의 학습 키워드에 React와 backend framework가 포함되어 있으므로, 프레임워크 자체에 대한 질문은 높은 확률로 나온다. 단순히 "사용했다"가 아니라 "프레임워크가 대신 해준 일"과 "내가 직접 작성한 일"을 구분해서 답한다.

### 공통 프레임워크 질문

| 질문 | 답변 핵심 |
|---|---|
| 프레임워크가 뭔가요? | 애플리케이션의 기본 구조와 실행 흐름을 제공하는 도구입니다. 개발자는 그 흐름 안에 필요한 컴포넌트, API, 비즈니스 로직을 채웁니다. |
| 라이브러리와 프레임워크 차이는요? | 라이브러리는 내가 필요할 때 호출하는 도구에 가깝고, 프레임워크는 실행 흐름의 큰 틀을 잡고 내 코드를 호출합니다. 제어의 흐름이 누구에게 있느냐가 핵심입니다. |
| 이 프로젝트에서 쓴 프레임워크는 무엇인가요? | Frontend는 React, Backend는 Spring Boot입니다. JPA/Hibernate와 Spring Security도 Spring 생태계에서 데이터 접근과 인증을 담당했습니다. |
| 프레임워크를 쓰지 않고 만들면 어떻게 되나요? | routing, HTTP 요청 처리, DI, DB 연결, 인증 filter, 상태 관리 같은 반복 구조를 직접 만들어야 합니다. 학습 가치는 있지만 과제 기간 내 서비스 완성도는 떨어질 수 있습니다. |
| 프레임워크가 해준 일과 직접 구현한 일을 나눠보세요. | React/Spring Boot는 렌더링, routing, HTTP 처리, DI, JPA, Security 흐름을 제공했습니다. 저는 게시판 도메인, API, RAG 검색 로직, MCP 도구, Agent 추천, 평가 문서를 구현했습니다. |
| 왜 여러 프레임워크를 한 번에 쓰지 않았나요? | 과제 선택지는 하나 이상이었지만, 학습 단계에서 너무 많은 백엔드 프레임워크를 섞으면 깊이가 얕아집니다. 그래서 backend는 Spring Boot 하나로 집중했습니다. |

### React 질문

| 질문 | 답변 핵심 |
|---|---|
| React는 프레임워크인가요, 라이브러리인가요? | 공식적으로는 UI 라이브러리에 가깝지만, React Router, Vite, 상태 관리 패턴과 함께 쓰면 프론트엔드 애플리케이션 구조를 만드는 프레임워크처럼 사용됩니다. 답변할 때는 "React 기반 frontend stack"이라고 말하는 것이 안전합니다. |
| React를 왜 선택했나요? | 과제 필수 조건이었고, 컴포넌트 단위로 화면을 쪼개 게시판, 작성 모달, 상세 화면, AI 패널을 관리하기 좋았습니다. |
| React의 핵심 개념은 무엇인가요? | component, props, state, rendering, event handling, hook입니다. 화면을 작은 컴포넌트로 나누고 state가 바뀌면 UI가 다시 렌더링됩니다. |
| Virtual DOM을 설명해보세요. | React가 실제 DOM을 바로 매번 조작하지 않고, 변경된 UI 구조를 메모리상의 표현과 비교해 필요한 부분만 실제 DOM에 반영하는 방식입니다. |
| useState는 왜 쓰나요? | 컴포넌트 안에서 값의 변화가 UI에 반영되어야 할 때 사용합니다. 예를 들어 검색어, 페이지 번호, 작성 모달 입력값 같은 상태를 관리합니다. |
| useEffect는 왜 쓰나요? | 렌더링 이후 API 호출, 초기 데이터 로드, 특정 상태 변경에 따른 side effect를 처리할 때 사용합니다. |
| custom hook은 왜 만들었나요? | API 호출과 상태 관리가 컴포넌트에 몰리면 읽기 어렵습니다. 게시글 목록, 검색, 페이지 상태 같은 재사용 가능한 로직을 hook으로 분리했습니다. |
| React Router는 왜 썼나요? | 메인 게시판, 상세 페이지, 로그인 페이지처럼 URL에 따라 다른 화면을 보여주기 위해 사용했습니다. |
| CSR의 한계는요? | 초기 로딩과 SEO에서 SSR보다 불리할 수 있습니다. 이 프로젝트는 로그인 기반 게시판이고 SEO가 핵심이 아니라 CSR 구조가 적합했습니다. |
| Next.js를 쓰지 않은 이유는요? | Next.js도 과제 선택지였지만 Spring Boot backend를 학습 목표로 잡았기 때문에 frontend는 React/Vite로 단순하게 두었습니다. SSR이나 file-based routing이 핵심 요구는 아니었습니다. |
| 상태 관리 라이브러리를 왜 안 썼나요? | 현재 규모에서는 React 기본 state와 custom hook으로 충분했습니다. 전역 상태가 복잡해지면 Zustand, Redux, TanStack Query 등을 검토할 수 있습니다. |
| TanStack Query 같은 서버 상태 라이브러리는 왜 안 썼나요? | 학습 초기에는 직접 API 호출과 loading/error 상태를 다루며 흐름을 이해하는 것이 우선이었습니다. 실서비스로 커지면 cache, stale time, retry를 위해 도입 가치가 있습니다. |

### Vite 질문

| 질문 | 답변 핵심 |
|---|---|
| Vite는 어떤 역할인가요? | 개발 서버와 빌드 도구입니다. React 코드를 빠르게 실행하고, production build를 만들어줍니다. |
| 왜 Vite를 썼나요? | React 프로젝트를 빠르게 시작할 수 있고 개발 서버가 가볍습니다. 과제에서는 frontend 학습과 구현 속도에 적합했습니다. |
| Vite dev server와 Spring Boot server를 따로 띄운 이유는요? | 개발 중에는 React hot reload와 Spring Boot API 실행을 분리하는 것이 편합니다. 배포 시에는 React build 결과를 정적 파일로 서빙하거나 nginx/S3/CloudFront로 분리할 수 있습니다. |

### Spring Boot 질문

| 질문 | 답변 핵심 |
|---|---|
| Spring Boot는 Spring과 무엇이 다른가요? | Spring Framework 위에서 auto-configuration, embedded server, starter dependency를 제공해 빠르게 애플리케이션을 시작하게 해주는 도구입니다. |
| Spring Boot를 왜 선택했나요? | 과제 선택지였고, REST API, JPA, Security, validation, 계층 구조를 학습하기 좋았습니다. Java 기반 백엔드 흐름을 익히는 데도 적합했습니다. |
| Spring Boot가 서버를 어떻게 띄우나요? | embedded Tomcat을 포함해 `bootRun` 실행 시 별도 WAS 설치 없이 HTTP server가 실행됩니다. |
| Spring MVC 흐름을 설명해보세요. | 요청이 DispatcherServlet으로 들어오고, HandlerMapping을 통해 Controller 메서드를 찾고, Controller가 Service를 호출해 결과를 Response로 반환합니다. |
| Controller는 어떤 역할인가요? | HTTP 요청을 받고, request DTO를 받아 service를 호출한 뒤 response DTO로 반환합니다. |
| Service는 어떤 역할인가요? | 게시글 생성, RAG 검색, MCP fact check, Agent 추천 같은 비즈니스 로직을 담당합니다. |
| Repository는 어떤 역할인가요? | JPA를 통해 DB CRUD와 query를 담당합니다. |
| DI/IoC가 뭔가요? | 객체 생성과 의존성 연결을 개발자가 직접 new로 관리하지 않고 Spring container가 관리하는 구조입니다. 테스트와 변경에 유리합니다. |
| Bean이 뭔가요? | Spring container가 생성하고 관리하는 객체입니다. Controller, Service, Repository 등이 Bean으로 등록됩니다. |
| `@RestController`는 무엇인가요? | Controller 메서드의 반환값을 view가 아니라 HTTP response body로 직렬화해 반환하는 annotation입니다. |
| `@Service`, `@Repository`는 왜 붙이나요? | 계층의 역할을 명확히 하고 Spring Bean으로 등록하기 위해 사용합니다. `@Repository`는 DB 예외 변환 역할도 가집니다. |
| Validation은 어디에 쓰나요? | 회원가입, 로그인, 게시글 생성/수정 요청에서 필수값과 형식 검증에 사용할 수 있습니다. |
| 예외 처리는 어떻게 확장할 건가요? | `@RestControllerAdvice`로 공통 에러 응답 구조를 만들고, 도메인별 예외를 표준화하겠습니다. |

### Spring Data JPA / Hibernate 질문

| 질문 | 답변 핵심 |
|---|---|
| JPA는 무엇인가요? | Java 객체와 관계형 DB 테이블을 매핑하는 표준 ORM 기술입니다. |
| Hibernate는 무엇인가요? | JPA 구현체입니다. 실제 SQL 생성과 영속성 관리를 수행합니다. |
| Entity는 무엇인가요? | DB 테이블과 매핑되는 Java 객체입니다. 예를 들어 User, Post, Comment, Tag가 있습니다. |
| Repository interface만 만들었는데 어떻게 쿼리가 실행되나요? | Spring Data JPA가 interface를 기반으로 구현체를 런타임에 만들어주고, 메서드 이름이나 `@Query`를 해석해 SQL을 실행합니다. |
| 영속성 컨텍스트가 뭔가요? | JPA가 Entity를 관리하는 1차 캐시/변경 감지 공간입니다. transaction 안에서 entity 변경을 추적합니다. |
| Dirty checking이 뭔가요? | 영속 상태의 entity 값이 바뀌면 transaction commit 시 변경 내용을 감지해 update SQL을 생성하는 기능입니다. |
| Lazy loading은 무엇인가요? | 연관 객체를 즉시 가져오지 않고 실제 접근 시점에 조회하는 방식입니다. N+1 문제와 연결될 수 있습니다. |
| N+1 문제는 무엇인가요? | 목록 1번 조회 후 각 row의 연관 데이터를 추가로 N번 조회하는 문제입니다. fetch join, entity graph, batch size로 개선할 수 있습니다. |
| `ddl-auto=update`는 왜 썼나요? | 로컬 개발 속도를 위해 사용했습니다. 지금은 환경변수로 분리해 운영에서 `validate`로 바꿀 수 있고, 초기 스키마는 Flyway migration으로 기록했습니다. |
| DTO를 쓰는 이유는요? | Entity를 API 응답에 직접 노출하지 않고, 화면에 필요한 데이터와 API 계약을 분리하기 위해서입니다. |

### Spring Security / JWT 질문

| 질문 | 답변 핵심 |
|---|---|
| Spring Security는 어떤 역할인가요? | 인증과 인가 흐름을 filter chain 기반으로 처리하는 프레임워크입니다. |
| JWT 인증 흐름을 설명해보세요. | 로그인 성공 시 서버가 access token JWT를 httpOnly cookie로 내려줍니다. 이후 요청에서 `JwtAuthenticationFilter`가 cookie의 JWT를 검증해 인증 객체를 만듭니다. access token이 만료되면 refresh token cookie로 `/api/auth/refresh`를 호출합니다. |
| 세션 방식 대신 JWT를 쓴 이유는요? | 프론트와 백엔드가 분리된 구조에서 stateless API 인증을 연습하기 적합했습니다. |
| JWT의 단점은요? | 탈취되면 만료 전까지 위험하고, 서버에서 즉시 무효화하기 어렵습니다. 그래서 access token은 cookie로 숨기고, refresh token은 DB에 해시 저장 후 rotation합니다. 남은 과제는 access token blocklist나 전체 세션 강제 종료입니다. |
| localStorage 저장은 안전한가요? | 인증 token은 localStorage에 저장하지 않도록 개선했습니다. 현재는 httpOnly cookie, CSRF token, refresh token rotation을 함께 사용하고, 운영 배포에서는 HTTPS 기반 secure cookie와 CSP까지 묶어야 합니다. |
| 인가와 인증 차이는요? | 인증은 사용자가 누구인지 확인하는 것이고, 인가는 그 사용자가 특정 작업을 할 권한이 있는지 확인하는 것입니다. |
| CORS는 Spring Security와 왜 같이 설정하나요? | 브라우저가 다른 origin의 API 호출을 제한하기 때문에, frontend dev server에서 backend API를 호출하려면 CORS 허용이 필요합니다. |

### 백엔드 프레임워크 선택 비교

| 질문 | 답변 핵심 |
|---|---|
| Nest.js 대신 Spring Boot를 고른 이유는요? | Nest.js는 TypeScript 기반이라 frontend와 언어를 맞출 수 있지만, 이번에는 Java/Spring/JPA/Security 학습을 목표로 Spring Boot를 선택했습니다. |
| FastAPI 대신 Spring Boot를 고른 이유는요? | FastAPI는 빠른 API 작성과 Python AI 생태계 연동에 강합니다. 하지만 이 프로젝트는 게시판, 인증, JPA, 계층형 backend 학습이 중요해서 Spring Boot가 더 적합했습니다. |
| Next.js fullstack으로 만들지 않은 이유는요? | Next.js만으로도 API route를 만들 수 있지만, 과제의 backend framework 학습 목표와 Java/Spring 경험을 위해 frontend와 backend를 분리했습니다. |
| Spring Boot의 단점은요? | 초기 개념이 많고 설정/annotation/계층 구조를 이해해야 합니다. 작은 프로젝트에는 무겁게 느껴질 수 있습니다. |
| 그래도 Spring Boot를 쓴 장점은요? | 인증, DB, validation, REST API, test, 운영 설정까지 표준적인 백엔드 구조를 익힐 수 있습니다. |

### 프레임워크 학습 검증 꼬리질문

| 질문 | 답변 방향 |
|---|---|
| annotation을 많이 썼는데 직접 동작을 이해하나요? | 완벽히 내부 구현을 모두 아는 것은 아니지만, `@RestController`, `@Service`, `@Repository`, `@Entity`, constructor injection이 어떤 계층과 Bean 등록을 의미하는지는 설명할 수 있습니다. |
| Spring이 객체를 대신 만들어준다는 게 무슨 뜻인가요? | 개발자가 직접 `new Service()`로 연결하지 않고, Spring container가 필요한 Bean을 만들고 생성자 주입으로 연결합니다. |
| React가 state 바뀔 때 어떻게 화면을 바꾸나요? | state setter가 호출되면 React가 해당 컴포넌트를 다시 렌더링하고, 변경된 결과를 실제 DOM에 반영합니다. |
| 프레임워크를 쓰면서 가장 헷갈린 점은요? | 처음에는 파일이 많아져서 복잡해 보였지만, Controller/Service/Repository/Entity와 React component/hook 역할을 나누니 변경 위치가 명확해졌습니다. |

## 15. 위험 질문과 피해야 할 답변

| 위험 질문 | 피해야 할 답변 | 더 나은 답변 |
|---|---|---|
| 이거 AI가 다 짜준 것 아닌가요? | 네, 거의 AI가 해줬습니다. | AI 도움을 받았지만 단계별로 직접 실행, diff 확인, 오류 수정, 구조 이해를 했고 최종 흐름을 설명할 수 있습니다. |
| Agent 맞나요? | 네, 그냥 Agent입니다. | 완전 자율형은 아니고, 상태 관찰과 추론 단계를 명시한 제한형 추천 agent입니다. |
| MCP 맞나요? | API 호출이긴 한데요. | 외부 API 호출을 JSON-RPC/tool 인터페이스로 분리해 MCP 형태로 구현했습니다. |
| 평가가 충분한가요? | 점수 좋게 나왔습니다. | 현재 케이스 규모는 작아 한계가 있습니다. 그래서 online/offline을 분리했고 holdout 확장이 다음 개선입니다. |
| 보안 괜찮나요? | 로컬이라 괜찮습니다. | 인증은 httpOnly cookie, CSRF token, refresh token rotation까지 보강했다. 또한 production mode에서 기본 JWT secret과 insecure cookie를 막았다. 그래도 운영에서는 migration, secret manager, 관리자용 세션 관리가 추가로 필요합니다. |
| 왜 이 기술을 썼나요? | 그냥 많이 쓴다길래요. | 과제 조건, 학습 목표, 현재 DB 선택, 기능 요구를 기준으로 선택했습니다. |

## 16. 강도별 답변 길이

같은 질문이라도 상황에 따라 답변 길이를 조절한다.

### 15초 답변

```text
RAG는 작성 중인 글을 query로 기존 게시글을 찾고, 그 결과를 근거로 초안을 생성하는 기능입니다.
```

### 30초 답변

```text
작성 중인 제목, 본문, 태그를 OpenAI embedding으로 바꾸고 Qdrant에서 유사 게시글 후보를 찾습니다.
그 뒤 MySQL에서 원본 게시글과 태그, 청크를 가져와 BM25, RRF, metadata, chunk evidence로 재정렬합니다.
최종 상위 글은 AI 초안 생성의 근거로 사용합니다.
```

### 1분 답변

```text
처음에는 vector similarity만 생각했지만, 실제로 GitHub, Redis, CPU 같은 고유명사와 짧은 쿼리에서 결과가 흔들렸습니다.
그래서 Qdrant vector search를 1차 후보 생성기로 두고, Spring Boot에서 BM25와 RRF를 결합했습니다.
한국어 검색을 위해 Nori 형태소 분석도 넣었고, 긴 글의 세부 근거를 보강하려고 chunk evidence를 약하게 반영했습니다.
평가는 offline 실험으로 후보를 줄이고, 최종 채택은 실제 Spring API, Qdrant, MySQL, OpenAI 경로를 모두 태운 online 결과로 판단했습니다.
```

## 17. 짧은 답변 암기본

발표 직전에는 아래 문장만 외워도 방어력이 올라간다.

```text
Project Alpha는 React와 Spring Boot로 만든 AI 게시판입니다.
RAG는 작성 중인 글과 유사한 기존 게시글을 찾아 초안 근거로 사용합니다.
MCP는 GitHub/날씨 같은 외부 데이터를 도구로 호출해 게시글의 사실성을 검증합니다.
Agent는 사용자별 읽음 기록을 바탕으로 이미 본 글을 제외하고 놓친 글 5개를 추천합니다.
RAG 검색은 Qdrant vector search만 쓰지 않고 BM25, RRF, metadata, Nori, chunk evidence를 조합했습니다.
최종 성능은 실제 API 경로를 태운 online 평가를 기준으로 판단했습니다.
현재 한계는 평가셋 규모, 운영 보안, migration, 장애 대응이며 다음 개선 우선순위입니다.
```
