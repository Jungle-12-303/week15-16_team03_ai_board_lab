# 07-08 App2 Step 05: 게시글 목록 state, 검색 state, 임시 조회 흐름 만들기

## 공식문서 URL

- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Updating Arrays in State  
  https://react.dev/learn/updating-arrays-in-state
- React - Updating Objects in State  
  https://react.dev/learn/updating-objects-in-state
- React - Synchronizing with Effects  
  https://react.dev/learn/synchronizing-with-effects
- React DOM - `<input>`  
  https://react.dev/reference/react-dom/components/input
- React DOM - `<form>`  
  https://react.dev/reference/react-dom/components/form

## 목적

정적 목록을 React state 기반 목록으로 바꾼다.

아직 API는 붙이지 않고, 검색과 페이지 조회가 어떤 상태 흐름을 필요로 하는지 확인한다.

## 성공 기준

- [x] `posts` state로 목록을 렌더링한다.
- [x] `query` state로 검색 입력값을 관리한다.
- [x] `pageInfo` state로 현재 페이지 정보를 관리한다.
- [x] `loadPosts(page)` 함수가 임시 데이터를 state에 넣는다.
- [x] 검색 submit 시 page 0부터 다시 조회하는 흐름을 만든다.

## 작업 체크리스트

### 1. posts state 만들기

- [x] 고정 배열을 `initialPosts` 같은 임시 데이터로 분리한다.
- [x] `posts` state를 만든다.
- [x] 목록 렌더링이 `posts.map(...)`을 사용하게 바꾼다.
- [x] `posts.length === 0`일 때 빈 목록 문구를 보여준다.

### 2. pageInfo state 만들기

- [x] `pageInfo` state를 만든다.
- [x] `pageInfo.page`를 현재 페이지로 둔다.
- [x] `pageInfo.totalPages`를 전체 페이지 수로 둔다.
- [x] 이전/다음 버튼을 만든다.
- [x] 첫 페이지에서는 이전 버튼을 비활성화한다.
- [x] 마지막 페이지에서는 다음 버튼을 비활성화한다.

### 3. query state 만들기

- [x] `query` state를 만든다.
- [x] 검색 input의 `value`를 `query` state에 연결한다.
- [x] 태그 검색이 필요하면 `query.tag`도 둔다.
- [x] input 변경 시 `setQuery`로 값을 갱신한다.

### 4. 임시 loadPosts 만들기

- [x] `loadPosts(page)` 함수를 만든다.
- [x] 함수 안에서 조회 결과를 `setPosts`로 넣는다.
- [x] 함수 안에서 `setPageInfo`로 페이지 정보를 넣는다.
- [x] 검색어가 있으면 조회 조건에 반영한다.
- [x] 최초 렌더링 시 `useEffect`에서 `loadPosts(0)`을 호출한다.

### 5. 검색 submit 흐름 만들기

- [x] `handleSearchSubmit(event)`를 만든다.
- [x] `event.preventDefault()`를 호출한다.
- [x] 검색 submit 시 `loadPosts(0)`을 호출한다.
- [x] 검색 후 현재 페이지가 0으로 돌아가는지 확인한다.

## 검증 체크리스트

- [x] 최초 렌더링 시 목록이 보이는지 확인한다.
- [ ] 검색어 입력 후 submit하면 목록이 필터링되는지 확인한다.
- [ ] 검색 결과가 없을 때 빈 목록 문구가 보이는지 확인한다.
- [ ] 이전/다음 버튼이 pageInfo 기준으로 비활성화되는지 확인한다.
- [x] 아직 서버가 없어도 목록 흐름을 설명할 수 있는지 확인한다.

## WHY 정리 질문

- [ ] 왜 목록 데이터는 `posts` state에 둬야 하는가?
- [ ] 왜 검색 입력값은 `query` state에 둬야 하는가?
- [ ] 왜 검색 submit 시 page를 0으로 되돌리는가?
- [ ] 왜 `loadPosts()`를 API 전에 임시 데이터로 먼저 만드는가?

## 다음 단계

- [ ] `07-08-app2-06-post-list-api.md`로 넘어간다.
