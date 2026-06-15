# 07-08 App2 Step 07: 게시글 상세 화면과 mode, selectedPost 흐름 만들기

## 공식문서 URL

- React - Conditional Rendering  
  https://react.dev/learn/conditional-rendering
- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Choosing the State Structure  
  https://react.dev/learn/choosing-the-state-structure
- React - Responding to Events  
  https://react.dev/learn/responding-to-events
- React - Preserving and Resetting State  
  https://react.dev/learn/preserving-and-resetting-state

## 목적

목록에서 게시글 하나를 선택하면 상세 화면으로 전환되는 흐름을 만든다.

아직 상세 API는 호출하지 않고, 목록에 있던 게시글 객체를 그대로 상세에 보여준다.

## 성공 기준

- [x] `mode` state로 목록 화면과 상세 화면을 전환한다.
- [x] `selectedPost` state로 선택된 게시글 하나를 저장한다.
- [x] 목록 아이템 클릭 시 상세 화면으로 이동한다.
- [x] 상세 화면에서 목록으로 돌아갈 수 있다.
- [ ] 아직 상세 API는 호출하지 않는다.

## 작업 체크리스트

### 1. mode state 만들기

- [x] `mode` state를 만든다.
- [x] 초기값은 `'list'`로 둔다.
- [x] `mode === 'list'`일 때 목록 영역을 보여준다.
- [x] `mode === 'detail'`일 때 상세 영역을 보여준다.

### 2. selectedPost state 만들기

- [x] `selectedPost` state를 만든다.
- [x] 초기값은 `null`로 둔다.
- [x] 상세 화면에서는 `selectedPost`가 없을 때의 예외 화면을 준비한다.

### 3. 상세 열기 함수 만들기

- [ ] `openDetail(post)` 함수를 만든다.
- [ ] 함수 안에서 `setSelectedPost(post)`를 호출한다.
- [ ] 함수 안에서 `setMode('detail')`을 호출한다.
- [x] 목록 아이템 클릭 시 `openDetail(post)`를 호출한다.

### 4. 상세 화면 만들기

- [x] 상세 화면에 제목을 표시한다.
- [x] 작성자 닉네임을 표시한다.
- [x] 작성일을 표시한다.
- [x] 본문이 있으면 본문을 표시한다.
- [x] 목록으로 돌아가기 버튼을 만든다.
- [x] 돌아가기 버튼 클릭 시 `setMode('list')`를 호출한다.

## 검증 체크리스트

- [ ] 목록에서 첫 번째 게시글 클릭 시 첫 번째 상세가 보이는지 확인한다.
- [ ] 목록에서 다른 게시글 클릭 시 다른 상세가 보이는지 확인한다.
- [ ] 목록으로 돌아가기 버튼이 동작하는지 확인한다.
- [ ] `selectedPost`가 `null`일 때 화면이 터지지 않는지 확인한다.
- [ ] 아직 URL 라우터 없이도 화면 전환을 설명할 수 있는지 확인한다.

## WHY 정리 질문

- [ ] 왜 게시글 목록은 `posts` 배열이고 상세는 `selectedPost` 하나인가?
- [ ] 왜 MVP에서는 라우터보다 `mode`가 먼저 적절한가?
- [ ] 왜 상세 API를 바로 붙이지 않고 목록 객체로 먼저 확인하는가?
- [ ] 왜 상세 화면에 `selectedPost === null` 방어가 필요한가?

## 다음 단계

- [ ] `07-08-app2-08-post-detail-api-owner-actions.md`로 넘어간다.
