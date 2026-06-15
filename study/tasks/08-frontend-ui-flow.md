# 08. Frontend UI: list/detail/edit/auth 상태 흐름

## 현재 프로젝트 분석

- 메인 화면: `frontend/src/App.jsx`
- 스타일: `frontend/src/styles.css`
- API 연결: `frontend/src/api/client.js`
- 현재 MVP는 컴포넌트를 여러 파일로 나누지 않고 `App.jsx` 안에 주요 화면 흐름을 모아 둔다.

## 학습 목표

- [x] React 상태가 화면 모드와 API 데이터를 어떻게 연결하는지 설명할 수 있다.
- [x] 목록, 상세, 작성/수정 화면을 `mode` 값으로 전환하는 이유를 설명할 수 있다.
- [x] 로그인 상태를 `localStorage`와 React state에 함께 두는 흐름을 이해한다.
- [x] MVP에서 컴포넌트를 과하게 쪼개지 않는 이유를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- React component: 화면을 상태와 props 기반 함수로 나누는 방식
- `useState`: 화면에 영향을 주는 값을 상태로 관리하는 Hook
- `useEffect`: 최초 로딩이나 특정 상태 변화 후 부수 효과를 실행하는 Hook
- Controlled component: input 값을 React state로 제어하는 방식
- Conditional rendering: `mode`나 로그인 상태에 따라 다른 UI를 보여주는 방식
- Event handler: form submit, button click에서 상태 변경과 API 호출을 연결하는 함수
- State lifting: 여러 하위 컴포넌트가 공유하는 상태를 상위 컴포넌트에 두는 방식
- Async UI state: loading, notice, error처럼 비동기 요청 상태를 화면에 반영하는 방식
- `localStorage`와 React state 동기화: 저장된 로그인 정보를 화면 상태로 복원하는 흐름
- Props: 부모 컴포넌트가 자식 컴포넌트에 데이터와 함수를 전달하는 방식

## 재구현 체크리스트

### 1. 첫 화면에서 목록을 보여주는 흐름부터 만든다

- [x] `App.jsx`에서 게시글 목록 영역을 먼저 렌더링한다.
- [x] `posts` 목록 상태를 둔다.
- [x] 앱 최초 렌더링 시 `loadPosts(0)`을 호출한다.
- [x] 목록 로딩 중 `loading` 상태를 켠다.
- [x] 목록 응답의 `content`, `page`, `totalPages`를 상태에 저장한다.
- [x] `pageInfo` 상태에 현재 페이지와 총 페이지를 둔다.
- [x] 페이지 이전/다음 버튼을 만든다.

### 2. 검색이 필요해지는 순간 query 상태를 추가한다

- [x] `query` 상태에 `keyword`, `tag`를 둔다.
- [x] 검색 폼 submit 시 page 0부터 다시 조회한다.
- [x] 검색어와 태그 필터가 목록 API params로 전달되는지 확인한다.
- [x] `notice`에 사용자에게 보여줄 메시지를 둔다.
- [x] 목록 실패 시 `getErrorMessage()` 결과를 화면에 표시한다.

### 3. 게시글 클릭 후 상세 화면이 필요해지는 순간 mode를 추가한다

- [x] `selectedPost` 상태에 상세 게시글을 둔다.
- [x] `mode` 상태를 `list`, `detail`, `edit` 중 하나로 둔다.
- [x] 게시글 클릭 시 `loadPost(id)`로 상세를 불러온다.
- [x] 상세 조회 성공 시 `selectedPost`를 저장하고 `mode`를 `detail`로 바꾼다.
- [x] 댓글 목록을 상세 응답에서 렌더링한다.
- [x] 작성자 본인일 때만 수정/삭제 버튼을 보여준다.

### 4. 글쓰기 버튼을 누르는 순간 작성/수정 상태를 추가한다

- [x] `editor` 상태에 제목, 본문, 태그 입력값을 둔다.
- [x] `editingId`로 작성과 수정을 구분한다.
- [x] `startCreate()`에서 editor를 빈 값으로 초기화한다.
- [x] `startEdit(post)`에서 기존 게시글 값을 editor에 채운다.
- [x] 태그 입력 문자열을 쉼표 기준 배열로 바꾼다.
- [x] `editingId`가 있으면 수정 API, 없으면 작성 API를 호출한다.
- [x] 저장 후 목록을 새로 불러오고 저장된 게시글 상세로 이동한다.

### 5. 쓰기 API가 막히는 순간 로그인 상태와 `AuthPanel`을 추가한다

- [x] `user` 상태를 `localStorage.user`에서 초기화한다.
- [x] 저장 시 로그인 여부를 먼저 확인한다.
- [x] 로그인/회원가입 모드를 상태로 관리한다.
- [x] 로그인 성공 시 token과 user를 `localStorage`에 저장한다.
- [x] 로그인 성공 시 React `user` 상태도 갱신한다.
- [x] 로그아웃 시 token과 user를 제거한다.
- [x] 로그아웃 시 React `user` 상태를 `null`로 바꾼다.

### 6. 상세/작성 화면에서 필요해지는 AI 패널을 추가한다

- [x] `aiState`에 RAG, MCP, Agent 결과와 loading 상태를 둔다.
- [x] 상세 화면에서는 선택된 게시글 본문으로 유사 글을 검색한다.
- [x] 작성 화면에서는 editor 초안으로 Agent와 RAG를 실행한다.
- [x] MCP 버튼은 GitHub username을 JSON-RPC params로 보낸다.
- [x] AI 요청 중에는 버튼을 비활성화한다.
- [x] 결과 영역에서 Agent step, RAG source, MCP JSON을 구분해 보여준다.
- [x] Agent 결과의 추천 태그를 기존 태그와 병합한다.

## 검증 체크리스트

- [ ] 첫 화면에서 게시글 목록이 로드되는지 확인한다.
- [ ] 검색어와 태그 필터가 목록에 반영되는지 확인한다.
- [ ] 로그인 후 글쓰기, 댓글 작성, 삭제 흐름이 동작하는지 확인한다.
- [ ] 작성 화면에서 Agent 실행 후 추천 태그가 입력칸에 병합되는지 확인한다.
- [ ] 상세 화면에서 유사 글 검색 결과가 표시되는지 확인한다.
- [ ] 새로고침 후에도 로그인 상태가 유지되는지 확인한다.

## WHY 정리 질문

- [ ] 왜 MVP에서는 라우터 없이 `mode` 상태로 화면을 전환해도 충분한가?
- [ ] 왜 서버 응답을 받은 뒤 목록과 상세를 다시 불러오는가?
- [ ] 왜 로그인 상태를 `localStorage`만 보지 않고 React state로도 들고 있는가?
- [ ] 왜 AI 결과를 게시글 본문에 바로 덮어쓰지 않고 사용자가 참고하도록 보여주는가?
