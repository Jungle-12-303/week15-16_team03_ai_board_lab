# 07-08 App2 Step 06: 게시글 목록 API 연결하기

## 공식문서 URL

- Axios - API reference  
  https://axios-http.com/docs/api_intro
- Axios - Request Config  
  https://axios-http.com/docs/req_config
- Axios - Response Schema  
  https://axios-http.com/docs/res_schema
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- React - Synchronizing with Effects  
  https://react.dev/learn/synchronizing-with-effects
- Vite - Server proxy  
  https://vite.dev/config/server-options.html#server-proxy

## 목적

임시 목록 조회 흐름을 실제 백엔드 목록 API로 교체한다.

아직 `postApi.list()`로 감싸지 않고, `App2.jsx` 안에서 axios를 직접 호출한다.

## 성공 기준

- [x] `loadPosts(page)`에서 `axios.get('/api/posts', { params })`를 호출한다.
- [x] 응답의 `response.data.content`를 `posts`에 저장한다.
- [x] 응답의 페이지 정보를 `pageInfo`에 저장한다.
- [x] 목록 로딩 중 상태와 실패 메시지를 화면에 반영한다.
- [x] URL, params, `response.data` 반복이 보이기 시작한다.

## 작업 체크리스트

### 1. loadPosts를 async 함수로 바꾸기

- [x] `loadPosts(page)` 앞에 `async`를 붙인다.
- [x] `loading` state를 만든다.
- [x] 요청 시작 전에 `setLoading(true)`를 호출한다.
- [x] 요청 종료 후 `setLoading(false)`를 호출한다.
- [x] `try/catch/finally` 구조를 사용한다.

### 2. axios GET 요청 붙이기

- [x] `axios.get('/api/posts', { params })`를 호출한다.
- [x] params에 `page`, `size`, `keyword`, `tag`를 넣는다.
- [x] 검색어가 비어 있으면 빈 문자열을 보낼지 생략할지 결정한다.
- [x] 응답을 `const data = response.data`로 꺼낸다.

### 3. 응답을 state에 반영하기

- [x] `data.content`를 `setPosts`에 넣는다.
- [x] `data.page` 또는 현재 프로젝트 응답 구조에 맞는 현재 페이지 값을 확인한다.
- [x] `data.totalPages`를 `pageInfo.totalPages`에 넣는다.
- [ ] 응답 구조가 다르면 실제 백엔드 `PageResponse`에 맞춘다.

### 4. 실패 처리 만들기

- [x] 요청 실패 시 `notice`에 메시지를 저장한다.
- [x] 서버가 꺼져 있을 때 화면이 깨지지 않게 한다.
- [ ] 실패해도 기존 목록을 유지할지 비울지 결정한다.

### 5. 중복과 불편함 표시하기

- [x] `'/api/posts'` URL이 컴포넌트 안에 들어온 것을 확인한다.
- [x] `response.data`를 직접 꺼내는 코드가 생긴 것을 확인한다.
- [x] try/catch 메시지 처리 방식이 로그인 기능과 비슷해졌는지 확인한다.

## 검증 체크리스트

- [ ] 백엔드가 켜져 있을 때 목록이 서버 데이터로 보이는지 확인한다.
- [ ] 검색어가 params로 전달되는지 브라우저 Network 탭에서 확인한다.
- [ ] 다음/이전 페이지 이동 시 다른 page 요청이 나가는지 확인한다.
- [ ] 서버가 꺼져 있을 때 실패 메시지가 보이는지 확인한다.
- [ ] 이 코드를 나중에 `postApi.list(params)`로 바꾸면 무엇이 줄어드는지 표시한다.

## WHY 정리 질문

- [ ] 왜 API 연결 전에 임시 `loadPosts()`를 먼저 만들었는가?
- [ ] 왜 로딩 상태가 필요한가?
- [ ] 왜 실패해도 화면 전체가 깨지면 안 되는가?
- [ ] 왜 지금 단계에서는 axios를 직접 써서 불편함을 확인하는가?

## 다음 단계

- [ ] `07-08-app2-07-post-detail-screen-mode.md`로 넘어간다.
