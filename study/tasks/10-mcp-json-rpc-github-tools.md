# 10. MCP: JSON-RPC request/response, GitHub 도구 호출

## 현재 프로젝트 분석

- Controller: `backend/src/main/java/com/example/aiknowledgeboard/ai/mcp/McpController.java`
- Service: `backend/src/main/java/com/example/aiknowledgeboard/ai/mcp/McpService.java`
- 요청/응답: `JsonRpcRequest`, `JsonRpcResponse`, `JsonRpcError`
- 로그: `McpToolLog`, `McpToolLogRepository`
- 현재 지원 도구: `github.getUser`, `github.getRepo`

## 학습 목표

- [ ] JSON-RPC 스타일 요청/응답 구조를 설명할 수 있다.
- [ ] MCP를 “외부 도구 호출 인터페이스”로 최소 구현하는 이유를 설명할 수 있다.
- [ ] method 이름으로 도구를 선택하고 params를 검증하는 흐름을 이해한다.
- [ ] 외부 API 호출 성공/실패를 로그로 남겨야 하는 이유를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- MCP 개념: AI 또는 Agent가 외부 도구를 일정한 인터페이스로 호출하는 구조
- JSON-RPC: `jsonrpc`, `method`, `params`, `id`, `result`, `error` 형식의 RPC 프로토콜
- RPC와 REST 차이: 리소스 중심 API와 메서드 호출 중심 API의 차이
- Spring `RestClient`: 외부 HTTP API를 호출하는 Spring 클라이언트
- GitHub REST API: public user/repository 정보를 조회하는 외부 API
- Params validation: 도구별 필수 파라미터를 실행 전에 확인하는 방식
- `Map<String, Object>`: 도구마다 다른 params 구조를 유연하게 받는 자료구조
- Transaction propagation: `REQUIRES_NEW`로 로그 저장을 본 작업과 분리하는 방식
- Tool logging: 외부 도구 호출의 요청, 응답, 실패 원인을 추적하는 기록

## 재구현 체크리스트

### 1. `McpController`에서 도구 호출 입구부터 만든다

- [ ] `POST /api/ai/mcp/call` endpoint를 먼저 만든다.
- [ ] 처음에는 임시 응답을 반환해 프론트에서 MCP 버튼이 API를 호출하는지 확인한다.
- [ ] 요청 body에 `jsonrpc`, `method`, `params`, `id`가 필요하다는 것을 확인한다.
- [ ] 반환값도 성공/실패를 같은 구조로 내려야 한다는 것을 확인한다.
- [ ] Controller는 검증/실행을 `McpService`에 위임한다.
- [ ] `McpController` 필드와 생성자에 `McpService` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `McpService`를 넣어주려면 `McpService` Bean이 필요하다는 것을 확인한다.
- [ ] 그래서 `McpService`에 `@Service`를 붙인다.

### 2. 요청/응답 모양이 필요해지는 순간 JSON-RPC DTO를 만든다

- [ ] `JsonRpcRequest`에 `jsonrpc`, `method`, `params`, `id`를 둔다.
- [ ] `params`는 도구마다 구조가 달라질 수 있으므로 `Map<String, Object>`로 둔다.
- [ ] `JsonRpcResponse.success(result, id)` 팩토리를 만든다.
- [ ] `JsonRpcResponse.error(code, message, id)` 팩토리를 만든다.
- [ ] `JsonRpcError`에 `code`, `message`를 둔다.
- [ ] 반환값은 항상 `JsonRpcResponse` 형태로 통일한다.

### 3. `McpService.call`에서 method 기반 도구 선택을 만든다

- [ ] request가 `null`이거나 `jsonrpc != "2.0"`이면 invalid request error를 반환한다.
- [ ] `method` 값으로 실행할 도구를 선택한다.
- [ ] `github.getUser`는 `username` param을 요구한다.
- [ ] `github.getRepo`는 `owner`, `repo` param을 요구한다.
- [ ] 지원하지 않는 method는 에러로 처리한다.
- [ ] 성공 시 result와 id를 담아 반환한다.
- [ ] 실패 시 JSON-RPC error와 id를 담아 반환한다.

### 4. 실제 GitHub 결과가 필요해지는 순간 외부 API 호출을 추가한다

- [ ] `McpService` 생성자에서 `@Value("${github.token}")`로 GitHub token 설정값을 받는다.
- [ ] 외부 HTTP 호출 도구가 필요해지면 `RestClient`를 만든다.
- [ ] `RestClient`의 baseUrl을 `https://api.github.com`으로 둔다.
- [ ] `github.token` 설정값이 있으면 Bearer token을 붙인다.
- [ ] `github.getUser`는 login, name, publicRepos, followers, profileUrl만 추려 반환한다.
- [ ] `github.getRepo`는 fullName, description, stars, forks, language, repoUrl만 추려 반환한다.
- [ ] 외부 응답 전체를 그대로 노출하지 않는다.
- [ ] GitHub API 실패가 JSON-RPC error로 변환되는지 확인한다.

### 5. 실행 추적이 필요해지는 순간 도구 호출 로그를 추가한다

- [ ] `McpService`에서 로그 저장을 하려다 `McpToolLogRepository`가 필요하다는 것을 확인한다.
- [ ] `McpService` 필드와 생성자에 `McpToolLogRepository` 타입을 먼저 추가한다.
- [ ] 이때 Spring이 `McpToolLogRepository`를 넣어주려면 Repository Bean이 필요하다는 것을 확인한다.
- [ ] `mcp_tool_logs` 테이블과 Entity를 매핑한다.
- [ ] `McpToolLogRepository`가 `JpaRepository<McpToolLog, Long>`를 상속하게 한다.
- [ ] toolName, requestSummary, responseSummary, status, errorMessage를 저장한다.
- [ ] 로그 저장은 `REQUIRES_NEW` 트랜잭션으로 분리한다.
- [ ] 긴 요청/응답 문자열은 1000자로 자른다.
- [ ] 성공과 실패 모두 로그를 남긴다.
- [ ] 로그 실패가 실제 도구 호출 응답을 망치지 않게 한다.

## 검증 체크리스트

- [ ] `jsonrpc`가 `"2.0"`이 아니면 에러 응답이 오는지 확인한다.
- [ ] `github.getUser`에 username이 없으면 에러 응답이 오는지 확인한다.
- [ ] `github.getUser`로 `openai`를 조회하면 login/profileUrl이 반환되는지 확인한다.
- [ ] `github.getRepo`로 공개 저장소를 조회하면 stars/forks가 반환되는지 확인한다.
- [ ] 지원하지 않는 method가 JSON-RPC error로 처리되는지 확인한다.
- [ ] 성공/실패 모두 `mcp_tool_logs`에 기록되는지 확인한다.

## WHY 정리 질문

- [ ] 왜 MCP 요청을 일반 REST endpoint 여러 개로 만들지 않고 JSON-RPC 스타일 하나로 받는가?
- [ ] 왜 `params`를 고정 DTO가 아니라 Map으로 받는가?
- [ ] 왜 GitHub API 응답 전체를 그대로 프론트엔드에 넘기지 않는가?
- [ ] 왜 로그 저장 트랜잭션을 본 호출과 분리하는가?
