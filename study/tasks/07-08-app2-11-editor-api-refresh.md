# 07-08 App2 Step 11: 작성/수정 API와 저장 후 재조회 흐름 붙이기

## 공식문서 URL

- Axios - API reference  
  https://axios-http.com/docs/api_intro
- Axios - Request Config  
  https://axios-http.com/docs/req_config
- Axios - Response Schema  
  https://axios-http.com/docs/res_schema
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- MDN - HTTP Authorization header  
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization
- MDN - Window localStorage  
  https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage

## 목적

작성/수정 submit 흐름을 실제 API로 연결한다.

저장 후에는 서버 상태와 화면 상태를 맞추기 위해 목록과 상세를 다시 불러온다.

## 성공 기준

- [x] 작성 모드에서 `postApi.create(payload)`를 호출한다.
- [x] 수정 모드에서 `postApi.update(id, payload)`를 호출한다.
- [x] Authorization header는 `client.js` interceptor가 자동으로 붙인다.
- [x] 저장 성공 후 목록을 다시 불러온다.
- [x] 저장된 게시글의 상세 화면으로 이동한다.
- [x] 이 단계에서 header 반복이 확실히 보인다.

## 작업 체크리스트

### 1. token 준비하기

- [x] 저장 전 로그인 상태를 확인한다.
- [x] `client.js` interceptor가 `localStorage.getItem('token')`으로 token을 읽는다.
- [x] token이 없으면 저장을 막고 메시지를 보여준다.
- [x] API client가 `Authorization: Bearer ${token}` 헤더를 자동으로 넣는다.

### 2. 작성 API 연결하기

- [x] `editingId === null`인 경우 작성 API를 호출한다.
- [x] `postApi.create(payload)`를 사용한다.
- [x] API helper가 응답의 `response.data`를 꺼낸다.
- [x] 생성된 게시글 id를 확인한다.
- [x] 성공 메시지를 표시한다.

### 3. 수정 API 연결하기

- [x] `editingId !== null`인 경우 수정 API를 호출한다.
- [x] `postApi.update(editingId, payload)`를 사용한다.
- [x] API helper가 응답의 `response.data`를 꺼낸다.
- [x] 수정된 게시글 id를 확인한다.
- [x] 성공 메시지를 표시한다.

### 4. 저장 후 화면 동기화하기

- [x] 저장 성공 후 `loadPosts(0)` 또는 현재 페이지 재조회를 호출한다.
- [x] 저장된 게시글 id로 `loadPost(savedId)`를 호출한다.
- [x] editor 입력값을 초기화한다.
- [x] `editingId`를 `null`로 초기화한다.

### 5. 실패 처리하기

- [x] 작성 실패 시 실패 메시지를 보여준다.
- [x] 수정 실패 시 실패 메시지를 보여준다.
- [x] 권한 없음 응답이 오면 로그인 또는 작성자 권한 문제로 안내한다.
- [x] 서버 에러 시 화면이 edit 모드에 남아 입력값을 잃지 않게 한다.

### 6. 반복 지점 표시하기

- [x] 로그인 API와 목록 API와 저장 API에서 URL 문자열이 반복되는지 확인한다.
- [x] `response.data` 꺼내기가 반복되는지 확인한다.
- [x] token header 붙이는 코드가 반복되는지 확인한다.
- [x] try/catch 에러 메시지 코드가 반복되는지 확인한다.

## 검증 체크리스트

- [ ] 로그인하지 않고 저장하면 API 요청 없이 막히는지 확인한다.
- [ ] 로그인 후 새 게시글 작성이 되는지 확인한다.
- [ ] 작성 후 목록에 새 게시글이 보이는지 확인한다.
- [ ] 작성 후 상세 화면으로 이동하는지 확인한다.
- [ ] 수정 후 상세 화면의 내용이 최신으로 보이는지 확인한다.
- [ ] 저장 실패 시 입력값이 사라지지 않는지 확인한다.

## WHY 정리 질문

- [ ] 왜 저장 API에는 token이 필요한가?
- [ ] 왜 저장 후 목록을 다시 불러와야 하는가?
- [ ] 왜 저장 후 상세도 다시 불러와야 하는가?
- [ ] 왜 실패했을 때 editor 입력값을 유지해야 하는가?
- [ ] 왜 이제 `client.js` 분리가 필요하다는 느낌이 드는가?

## 다음 단계

- [ ] `07-08-app2-12-ai-reference-panel.md`로 넘어간다.
