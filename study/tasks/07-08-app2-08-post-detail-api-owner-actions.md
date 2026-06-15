# 07-08 App2 Step 08: 게시글 상세 API와 작성자 본인 버튼 조건 붙이기

## 공식문서 URL

- Axios - API reference  
  https://axios-http.com/docs/api_intro
- Axios - Response Schema  
  https://axios-http.com/docs/res_schema
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- React - Conditional Rendering  
  https://react.dev/learn/conditional-rendering
- React - Choosing the State Structure  
  https://react.dev/learn/choosing-the-state-structure
- MDN - HTTP Authorization header  
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization

## 목적

목록에서 받은 요약 데이터가 아니라, 게시글 id로 상세 API를 호출해 실제 상세 데이터를 보여준다.

그리고 로그인한 사용자가 작성자 본인일 때만 수정/삭제 버튼을 보여준다.

## 성공 기준

- [x] 목록 아이템 클릭 시 `loadPost(id)`를 호출한다.
- [x] `loadPost(id)`에서 `axios.get('/api/posts/{id}')`를 직접 호출한다.
- [x] 상세 응답을 `selectedPost`에 저장한다.
- [x] 상세 조회 성공 후 `mode`를 `'detail'`로 바꾼다.
- [x] 작성자 본인일 때만 수정/삭제 버튼을 보여준다.

## 작업 체크리스트

### 1. loadPost 함수 만들기

- [x] `loadPost(id)` async 함수를 만든다.
- [x] 요청 시작 전에 로딩 상태를 켠다.
- [x] `axios.get(`/api/posts/${id}`)`를 호출한다.
- [x] `response.data`를 꺼낸다.
- [x] `setSelectedPost(data)`를 호출한다.
- [x] `setMode('detail')`을 호출한다.
- [x] 실패 시 `notice`에 메시지를 표시한다.

### 2. 목록 클릭 흐름 교체하기

- [x] 기존 `openDetail(post)` 직접 선택 흐름을 확인한다.
- [x] 목록 아이템 클릭 시 `loadPost(post.id)`를 호출하게 바꾼다.
- [ ] 목록 요약 데이터와 상세 데이터의 차이를 확인한다.

### 3. 상세 데이터 렌더링하기

- [x] 상세 응답의 제목을 표시한다.
- [x] 상세 응답의 본문을 표시한다.
- [x] 상세 응답의 작성자 정보를 표시한다.
- [x] 태그가 있으면 태그 목록을 표시한다.
- [x] 댓글이 응답에 포함되어 있으면 댓글 목록을 표시한다.

### 4. 작성자 본인 여부 계산하기

- [x] `user`가 로그인 상태인지 확인한다.
- [x] `selectedPost.authorId` 또는 현재 응답의 작성자 식별 필드를 확인한다.
- [x] `user.id`와 게시글 작성자 id를 비교한다.
- [x] `const isOwner = ...` 형태로 본인 여부를 계산한다.

### 5. 수정/삭제 버튼 조건부 렌더링

- [x] `isOwner`가 true일 때만 수정 버튼을 보여준다.
- [x] `isOwner`가 true일 때만 삭제 버튼을 보여준다.
- [x] 비로그인 사용자는 수정/삭제 버튼을 볼 수 없어야 한다.
- [x] 다른 사용자의 게시글에서도 수정/삭제 버튼이 보이지 않아야 한다.

## 검증 체크리스트

- [ ] 목록 클릭 시 상세 API 요청이 나가는지 확인한다.
- [ ] 상세 응답의 본문이 화면에 보이는지 확인한다.
- [ ] 비로그인 상태에서는 수정/삭제 버튼이 보이지 않는지 확인한다.
- [ ] 작성자 본인으로 로그인하면 수정/삭제 버튼이 보이는지 확인한다.
- [ ] 다른 사용자로 로그인하면 수정/삭제 버튼이 보이지 않는지 확인한다.
- [ ] 상세 조회 실패 시 목록 화면이 깨지지 않는지 확인한다.

## WHY 정리 질문

- [ ] 왜 목록 데이터만으로 상세 화면을 끝내지 않는가?
- [ ] 왜 상세 조회 성공 후에 `mode`를 바꾸는가?
- [ ] 왜 수정/삭제 버튼은 프론트에서도 숨겨야 하는가?
- [ ] 왜 버튼 숨김만으로 보안을 보장할 수 없고 백엔드 검증도 필요한가?

## 다음 단계

- [ ] `07-08-app2-09-editor-static-screen.md`로 넘어간다.
