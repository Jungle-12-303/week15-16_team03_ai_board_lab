# 07-08 App2 Step 12: AI 참고 결과 패널 붙이기

## 공식문서 URL

- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Conditional Rendering  
  https://react.dev/learn/conditional-rendering
- React - Choosing the State Structure  
  https://react.dev/learn/choosing-the-state-structure
- React - Responding to Events  
  https://react.dev/learn/responding-to-events
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- MDN - JSON.stringify()  
  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/stringify

## 목적

AI 결과를 게시글 본문에 바로 덮어쓰지 않고, 사용자가 참고할 수 있는 별도 결과 영역으로 보여준다.

이 단계는 RAG, MCP, Agent 백엔드 구현을 깊게 다루지 않고 프론트 화면 흐름만 다룬다.

## 성공 기준

- [x] `aiState` state로 AI loading, result, error를 관리한다.
- [x] 상세 화면에서 유사 글 검색 결과를 참고 영역에 보여준다.
- [x] 작성/수정 화면에서 작성 보조 결과를 참고 영역에 보여준다.
- [x] AI 요청 중 버튼을 비활성화한다.
- [x] AI 결과가 editor 본문을 자동으로 덮어쓰지 않는다.

## 작업 체크리스트

### 1. aiState 구조 정하기

- [x] `aiState.loading`을 둔다.
- [x] `aiState.error`를 둔다.
- [x] `aiState.ragResult`를 둔다.
- [x] `aiState.agentResult`를 둔다.
- [x] 필요하면 `aiState.mcpResult`를 둔다.

### 2. 상세 화면 RAG 버튼 만들기

- [x] 상세 화면에 유사 글 찾기 버튼을 둔다.
- [x] 선택된 게시글 본문이나 id를 요청 payload로 사용할지 정한다.
- [x] 요청 중 버튼을 비활성화한다.
- [x] 결과가 오면 유사 글 제목과 요약을 표시한다.
- [x] 실패하면 AI 영역에 실패 메시지를 표시한다.

### 3. 작성/수정 화면 Agent 버튼 만들기

- [x] editor 화면에 작성 보조 버튼을 둔다.
- [x] 현재 `editor.title`, `editor.content`, `editor.tagsText`를 payload로 만든다.
- [x] 요청 중 버튼을 비활성화한다.
- [x] 결과가 오면 추천 제목, 요약, 추천 태그를 참고 영역에 표시한다.
- [x] 추천 태그를 자동 적용하지 않고 사용자가 선택할 수 있게 둔다.

### 4. 결과 영역 렌더링하기

- [x] 로딩 중 문구를 보여준다.
- [x] RAG 결과, MCP 결과, Agent 결과를 구분해서 보여준다.
- [x] 결과가 없을 때는 영역을 과하게 차지하지 않게 한다.
- [x] JSON 형태 결과는 읽을 수 있게 pre 영역에 표시한다.

### 5. 본문 자동 덮어쓰기 막기

- [x] AI 결과를 받았을 때 `setEditor({ content: aiText })`를 바로 호출하지 않는다.
- [x] 사용자가 적용 버튼을 누른 경우에만 일부 값을 반영할 수 있게 한다.
- [x] 적용 버튼을 만들더라도 기존 본문을 완전히 지우지 않게 주의한다.

## 검증 체크리스트

- [ ] AI 요청 중 버튼이 중복 클릭되지 않는지 확인한다.
- [ ] AI 실패 시 게시글 작성/수정 화면이 깨지지 않는지 확인한다.
- [ ] AI 결과가 본문을 자동으로 바꾸지 않는지 확인한다.
- [ ] 사용자가 결과를 보고 직접 수정할 수 있는 흐름인지 확인한다.
- [ ] AI 기능이 실패해도 기본 게시판 기능은 계속 가능한지 확인한다.

## WHY 정리 질문

- [ ] 왜 AI 결과를 본문에 바로 덮어쓰지 않는가?
- [ ] 왜 AI 상태를 일반 editor state와 분리하는가?
- [ ] 왜 AI 실패가 게시글 저장 실패로 이어지면 안 되는가?
- [ ] 왜 결과 영역에서 RAG, MCP, Agent 결과를 구분해야 하는가?

## 다음 단계

- [ ] `07-08-app2-13-api-client-refactor.md`로 넘어간다.
