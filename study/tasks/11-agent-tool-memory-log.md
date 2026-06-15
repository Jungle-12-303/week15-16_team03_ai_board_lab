# 11. Agent: 도구 선택, 실행 제한, memory/log 저장

## 현재 프로젝트 분석

- Controller: `backend/src/main/java/com/example/aiknowledgeboard/ai/agent/AgentController.java`
- Service: `backend/src/main/java/com/example/aiknowledgeboard/ai/agent/AgentService.java`
- 요청/응답: `AgentRequest`, `AgentResponse`, `AgentStepResponse`
- Memory: `AgentMemory`, `AgentMemoryRepository`
- 사용하는 도구: RAG 검색, MCP 호출, 태그 추천, 초안 요약

## 학습 목표

- [ ] Agent를 완전 자율 실행기가 아니라 제한된 도구 실행기로 설계한 이유를 설명할 수 있다.
- [ ] 도구 선택, 도구 실행, 최종 메시지 생성 흐름을 단계별로 설명할 수 있다.
- [ ] 반복 실행 제한과 실패 제한이 왜 필요한지 이해한다.
- [ ] memory와 ai log가 데모와 디버깅에서 어떤 역할을 하는지 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- Agent loop: 입력을 보고 도구를 선택하고 실행 결과를 바탕으로 응답을 만드는 흐름
- Tool use: RAG 검색, MCP 호출, 태그 추천처럼 Agent가 사용할 수 있는 제한된 기능
- Rule-based routing: LLM 판단 대신 명확한 규칙으로 도구를 고르는 방식
- Iteration limit: 무한 반복과 과도한 외부 호출을 막는 실행 제한
- Fallback response: 일부 도구가 실패해도 사용자에게 최소 결과를 돌려주는 방식
- Memory: 세션별 최근 추천 태그와 요약을 저장하는 간단한 상태
- AI log: Agent 실행 입력/출력/성공 여부를 추적하는 기록
- Regex: GitHub URL에서 username을 추출하는 문자열 패턴
- `LinkedHashSet`: 도구 실행 순서를 유지하면서 중복을 제거하는 자료구조
- Human-in-the-loop: Agent가 자동 수정하지 않고 사용자가 선택할 추천만 제공하는 설계

## 재구현 체크리스트

### 1. `AgentController`에서 글쓰기 도우미 입구부터 만든다

- [ ] `/api/ai/agent/write-helper` endpoint를 먼저 만든다.
- [ ] 처음에는 초안을 받아 임시 추천 메시지를 반환한다.
- [ ] 요청에 `draft`, `intention`, `sessionId`가 필요하다는 것을 확인한다.
- [ ] 응답에는 최종 메시지뿐 아니라 실행 단계도 필요하다는 것을 확인한다.
- [ ] Controller는 Agent 실행을 `AgentService`에 위임한다.
- [ ] `AgentController` 필드와 생성자에 `AgentService` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `AgentService`를 넣어주려면 `AgentService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `AgentService`에 `@Service`를 붙인다.

### 2. 요청/응답 모양이 필요해지는 순간 Agent DTO를 만든다

- [ ] `AgentRequest`에 `draft`, `intention`, `sessionId`를 둔다.
- [ ] `draft`는 `@NotBlank`로 검증한다.
- [ ] `AgentStepResponse`에 `tool`, `status`, `message`를 둔다.
- [ ] `AgentResponse`에 finalMessage, recommendedTags, similarPosts, mcpResult, steps, fallback을 둔다.
- [ ] 최종 결과와 중간 실행 단계를 함께 반환한다.

### 3. `AgentService`에 최소 실행 흐름을 만든다

- [ ] 현재 로그인 사용자를 조회한다.
- [ ] 현재 로그인 사용자 조회가 필요해지면 `AgentService` 생성자에 `CurrentUserService`를 추가한다.
- [ ] `sessionId`가 없으면 새 UUID를 만든다.
- [ ] 추천 태그, 유사 글, MCP 결과의 초기값을 정한다.
- [ ] `steps`에 실행 과정을 남길 준비를 한다.
- [ ] 최대 실행 횟수를 `MAX_ITERATIONS = 3`으로 제한한다.
- [ ] 처음에는 태그 추천만 실행해도 응답이 만들어지게 한다.

### 4. 어떤 도구를 실행할지 필요해지는 순간 선택 로직을 만든다

- [ ] `chooseTools(draft, intention)`로 실행할 도구 목록을 정한다.
- [ ] 기본 도구로 `recommend_tags`를 추가한다.
- [ ] 기본 도구로 `search_similar_posts`를 추가한다.
- [ ] 초안 또는 의도에 `github`, `깃허브`가 있으면 `call_mcp_external_api`를 추가한다.
- [ ] GitHub 관련 문맥이 없으면 `summarize_draft`를 추가한다.
- [ ] `LinkedHashSet`으로 도구 순서를 유지하고 중복을 제거한다.

### 5. 첫 번째 도구로 태그 추천을 구현한다

- [ ] 초안을 소문자로 변환한다.
- [ ] 핵심 키워드와 태그 매핑을 만든다.
- [ ] 포함된 키워드가 있으면 해당 태그를 추천한다.
- [ ] 부족하면 본문 토큰에서 3글자 이상 단어를 보충한다.
- [ ] 그래도 없으면 기본 태그를 반환한다.
- [ ] 최대 5개까지만 추천한다.
- [ ] 실행 결과를 `steps`에 남긴다.

### 6. 외부 지식이 필요해지는 순간 RAG와 MCP 도구를 연결한다

- [ ] 유사 글 검색이 필요해지면 `AgentService` 생성자에 `RagService`를 추가한다.
- [ ] 이때 Spring이 `RagService`를 넣어주려면 `RagService` Bean이 필요하다는 것을 확인한다.
- [ ] `search_similar_posts`는 `RagService.findSimilarPosts(draft, null, 3)`을 호출한다.
- [ ] 외부 GitHub 도구 호출이 필요해지면 `AgentService` 생성자에 `McpService`를 추가한다.
- [ ] 이때 Spring이 `McpService`를 넣어주려면 `McpService` Bean이 필요하다는 것을 확인한다.
- [ ] `call_mcp_external_api`는 초안에서 GitHub username을 추출한다.
- [ ] GitHub username이 없으면 데모 기본값으로 `openai`를 사용한다.
- [ ] MCP 호출은 `JsonRpcRequest("2.0", "github.getUser", params, id)`로 만든다.
- [ ] MCP 응답이 성공이면 result, 실패면 error를 결과로 저장한다.
- [ ] RAG와 MCP 결과를 최종 응답에 함께 담는다.
- [ ] 최종 메시지 생성에 AI 호출이 필요해지면 `AgentService` 생성자에 `OpenAiClient`를 추가한다.
- [ ] 이때 Spring이 `OpenAiClient`를 넣어주려면 `OpenAiClient` Bean이 필요하다는 것을 확인한다.

### 7. 도구 실패가 보이는 순간 fallback과 제한을 추가한다

- [ ] 도구 실행 중 예외가 나면 fallback 상태를 켠다.
- [ ] 실패한 도구와 메시지를 `steps`에 남긴다.
- [ ] 같은 도구가 반복 실패하면 중단한다.
- [ ] 일부 도구 실패가 전체 응답 생성을 막지 않게 한다.
- [ ] 최종 메시지는 OpenAI chat 또는 fallback 문자열로 생성한다.

### 8. 실행 결과를 다시 보고 싶어지는 순간 memory/log 저장을 추가한다

- [ ] memory 저장이 필요해지면 `AgentService` 생성자에 `AgentMemoryRepository`를 추가한다.
- [ ] 이때 Spring이 `AgentMemoryRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [ ] `AgentMemoryRepository`가 `JpaRepository<AgentMemory, Long>`를 상속하게 한다.
- [ ] 추천 태그를 `agent_memory`에 저장한다.
- [ ] 최종 요약을 `agent_memory`에 저장한다.
- [ ] AI 실행 로그가 필요해지면 `AgentService` 생성자에 `AiLogService`를 추가한다.
- [ ] 이때 Spring이 `AiLogService`를 넣어주려면 `AiLogService` Bean이 필요하다는 것을 확인한다.
- [ ] 전체 Agent 실행 결과를 `ai_logs`에 저장한다.
- [ ] 성공 여부는 fallback 발생 여부와 연결해 기록한다.
- [ ] memory는 복잡한 장기 기억이 아니라 최근 실행 결과 저장 정도로 제한한다.

## 검증 체크리스트

- [ ] 로그인 사용자가 `/api/ai/agent/write-helper`를 호출할 수 있는지 확인한다.
- [ ] draft가 비어 있으면 validation 에러가 오는지 확인한다.
- [ ] Spring/RAG/Docker 같은 단어가 있으면 관련 태그가 추천되는지 확인한다.
- [ ] GitHub URL이 있는 초안은 MCP 도구가 실행되는지 확인한다.
- [ ] GitHub 문맥이 없는 초안은 요약 도구가 선택되는지 확인한다.
- [ ] steps 배열에 실행한 도구와 상태가 순서대로 기록되는지 확인한다.
- [ ] `agent_memory`, `ai_logs`에 결과가 저장되는지 확인한다.

## WHY 정리 질문

- [ ] 왜 Agent가 게시글을 자동 수정하지 않고 추천만 반환하는가?
- [ ] 왜 도구 실행 횟수를 3회로 제한하는가?
- [ ] 왜 도구 선택을 LLM에게 전부 맡기지 않고 규칙 기반으로 시작하는가?
- [ ] 왜 memory는 복잡한 장기 기억이 아니라 최근 실행 결과 저장 정도로 제한하는가?
