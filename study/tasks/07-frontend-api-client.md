# 07. Frontend API client: Axios baseURL, token interceptor

# 공식문서 URL
# Vite - index.html and Project Root
https://vite.dev/guide/#index-html-and-project-root
네가 올린 index.html에서 왜 <script type="module" src="/src/main.jsx">가 앱의 시작점인지 설명하는 문서.

# React - createRoot
https://react.dev/reference/react-dom/client/createRoot
document.getElementById('root')를 React 앱의 root로 만들고, 그 안을 React가 관리하는 이유를 설명하는 문서.

# React - Render and Commit
https://react.dev/learn/render-and-commit
React가 컴포넌트를 렌더링하고 실제 DOM에 반영하는 흐름을 설명하는 가장 중요한 문서.

# React - Understanding Your UI as a Tree
https://react.dev/learn/understanding-your-ui-as-a-tree
React가 UI를 트리 구조로 바라본다는 개념을 설명함. “가상 DOM”을 이해하기 전에 이걸 먼저 보면 좋음.

# React - Preserving and Resetting State
https://react.dev/learn/preserving-and-resetting-state
React가 같은 위치의 컴포넌트는 유지하고, 다른 위치나 다른 타입이면 새로 만든다는 개념을 설명함.

# React Legacy - Reconciliation
https://legacy.reactjs.org/docs/reconciliation.html
“Virtual DOM”, “diffing”, “변경된 부분만 업데이트”에 가장 가까운 공식 설명. 오래된 문서지만 이 개념을 이해하기에는 좋음.

# React - Writing Markup with JSX
https://react.dev/learn/writing-markup-with-jsx
JSX가 HTML처럼 보이지만 실제로는 JavaScript 객체로 변환된다는 설명이 있음.

# 개념참고 공식문서
# Axios instance	Axios - Creating an instance, Axios - Request Config
https://axios-http.com/docs/instance, https://axios-http.com/docs/req_config
# Request interceptor	Axios - Interceptors
https://axios.rest/pages/advanced/interceptors
# Axios error handling	Axios - Handling Errors, Axios - Response Schema
https://axios-http.com/docs/handling_errors, https://axios-http.com/docs/res_schema
# Promise	MDN - Promise, ECMAScript Spec - Promise Objects
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise,
https://tc39.es/ecma262/multipage/control-abstraction-objects.html#sec-promise-objects
# async/await	MDN - async function, MDN - await, ECMAScript Spec - Async Function Definitions
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Statements/async_function, 
https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/await,
https://tc39.es/ecma262/multipage/ecmascript-language-functions-and-classes.html#sec-async-function-definitions
# Vite 환경변수	Vite - Env Variables and Modes
https://vite.dev/guide/env-and-mode
Browser localStorage	MDN - Window.localStorage, WHATWG HTML Standard - Web Storage
https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
https://html.spec.whatwg.org/multipage/webstorage.html
# REST endpoint mapping	Spring Framework - Mapping Requests, MDN - HTTP request methods
https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html
https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Methods
# API client 분리	Axios - Creating an instance, Axios - Request Config
https://axios.rest/pages/advanced/create-an-instance
https://axios-http.com/docs/req_config
# Bearer token	RFC 6750 - Bearer Token Usage, MDN - Authorization header
https://www.rfc-editor.org/info/rfc6750/
https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization

## 현재 프로젝트 분석

- API client: `frontend/src/api/client.js`
- 사용 위치: `frontend/src/App.jsx`
- 현재 구조: `authApi`, `postApi`, `aiApi`로 API 호출을 기능별로 묶는다.
- 인증 토큰은 `localStorage`에서 읽어 `Authorization: Bearer ...` 헤더로 붙인다.

## 학습 목표

- [x] 화면 컴포넌트에서 Axios 세부 설정을 분리하는 이유를 설명할 수 있다.
- [x] `baseURL`을 환경변수와 기본값으로 관리하는 이유를 설명할 수 있다.
- [x] token interceptor가 모든 인증 API 호출에 일관되게 적용되는 흐름을 이해한다.
- [x] API 에러 메시지를 한 곳에서 해석해야 하는 이유를 설명할 수 있다.

## 공부할 때 참고해야 할 개념

- Axios instance: 공통 `baseURL`, timeout, interceptor를 공유하는 HTTP client
- Request interceptor: 요청이 서버로 나가기 전에 token 같은 공통 헤더를 붙이는 기능
- Promise와 async/await: 비동기 API 호출을 순서대로 읽기 쉽게 쓰는 JavaScript 문법
- Vite 환경변수: `import.meta.env`와 `VITE_` prefix 규칙
- Browser `localStorage`: 새로고침 후에도 token/user를 유지하는 저장소
- REST endpoint mapping: 프론트 API 함수와 백엔드 URL을 대응시키는 방식
- Error handling: Axios error 객체에서 `response`, `status`, `data`를 읽는 방법
- API client 분리: UI 컴포넌트가 HTTP 세부 구현을 몰라도 되게 하는 구조
- Bearer token: Authorization 헤더로 JWT를 전달하는 표준적인 방식

## 재구현 체크리스트

### 1. `App.jsx`에서 API 함수를 먼저 호출해 필요성을 만든다

- [x] 로그인/회원가입 form submit에서 `authApi.signup(payload)`와 `authApi.login(payload)`를 호출한다고 가정한다.
- [x] 게시글 목록 화면에서 `postApi.list(params)`를 호출한다고 가정한다.
- [x] 컴포넌트 안에 URL, header, Axios 세부 설정이 섞이면 복잡해진다는 문제를 확인한다.
- [x] 그래서 `frontend/src/api/client.js`에 API 호출을 모아야 한다는 결론을 만든다.

### 2. 가장 먼저 필요한 기능별 wrapper를 만든다

- [x] `authApi.signup(payload)`를 만든다.
- [x] `authApi.login(payload)`를 만든다.
- [x] `postApi.list(params)`를 만든다.
- [x] `postApi.get(id)`를 만든다.
- [x] 화면에서 직접 `axios.post('/auth/signup')` 같은 코드를 쓰지 않게 한다.
- [x] API 함수 이름만 보고 어떤 백엔드 endpoint를 호출하는지 추적할 수 있게 한다.

### 3. 중복 URL 설정이 보이는 순간 Axios 인스턴스를 만든다

- [x] `axios.create()`로 공용 인스턴스를 만든다.
- [x] `baseURL`은 `import.meta.env.VITE_API_BASE_URL || '/api'`로 둔다.
- [x] timeout을 설정해 무한 대기를 피한다.
- [x] API 인스턴스를 직접 export하지 않고 기능별 wrapper를 export한다.
- [ ] Vite 개발 환경과 Nginx 배포 환경에서 `/api`가 어떻게 달라지는지 확인한다.

### 4. 응답 모양이 반복되는 순간 `unwrap()`을 만든다

- [x] Axios 응답 전체가 아니라 `response.data`만 반환하는 `unwrap()`을 만든다.
- [x] 화면 컴포넌트가 `response.data.content` 같은 Axios 구조를 몰라도 되게 한다.
- [x] 삭제 API처럼 body가 필요 없는 호출은 필요 이상으로 감싸지 않는다.
- [x] `postApi.create(payload)`, `update(id, payload)`, `remove(id)`를 추가한다.
- [x] `postApi.comment(postId, payload)`, `removeComment(commentId)`를 추가한다.
- [x] `aiApi.similar(payload)`, `mcp(payload)`, `agent(payload)`를 추가한다.

### 5. 로그인 후 쓰기 API가 막히는 순간 token interceptor를 추가한다

- [x] 요청 interceptor에서 `localStorage.getItem('token')`을 읽는다.
- [x] 토큰이 있으면 `config.headers.Authorization`에 `Bearer` 토큰을 붙인다.
- [x] 토큰이 없으면 헤더를 추가하지 않는다.
- [x] interceptor는 요청 데이터 자체를 바꾸지 않는다.
- [ ] 로그인 전 요청과 로그인 후 요청의 Authorization 헤더 차이를 확인한다.

### 6. 실패 메시지가 흩어지는 순간 에러 helper를 만든다

- [x] 백엔드 에러 응답의 `message`가 있으면 그 값을 우선 반환한다.
- [x] HTTP status만 있으면 상태 코드 기반 메시지를 반환한다.
- [x] 응답이 없으면 서버 연결 실패 메시지를 반환한다.
- [x] 화면 컴포넌트는 에러 구조를 직접 파싱하지 않게 한다.

## 검증 체크리스트

- [ ] 로그인 전 `POST /api/posts` 요청에 Authorization 헤더가 없는지 확인한다.
- [ ] 로그인 후 쓰기 API 요청에 Authorization 헤더가 붙는지 확인한다.
- [ ] `VITE_API_BASE_URL`이 없으면 `/api`로 요청되는지 확인한다.
- [ ] API 실패 시 화면에 백엔드 `message`가 표시되는지 확인한다.
- [ ] API 함수 이름만 보고 어떤 백엔드 endpoint를 호출하는지 추적할 수 있는지 확인한다.

## WHY 정리 질문

- [ ] 왜 모든 컴포넌트에서 `axios.get()`을 직접 부르지 않는가?
- [ ] 왜 토큰을 매 API 함수마다 붙이지 않고 interceptor로 처리하는가?
- [ ] 왜 프론트엔드 개발 서버에서는 `/api` proxy 또는 Nginx proxy 구조가 필요한가?
- [ ] 왜 에러 메시지 파싱을 `getErrorMessage()` 하나로 모으는가?
