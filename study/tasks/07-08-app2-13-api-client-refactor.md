# 07-08 App2 Step 13: 반복되는 axios 코드를 client.js로 리팩토링하기

## 공식문서 URL

- Axios - Creating an instance  
  https://axios-http.com/docs/instance
- Axios - Request Config  
  https://axios-http.com/docs/req_config
- Axios - Response Schema  
  https://axios-http.com/docs/res_schema
- Axios - Interceptors  
  https://axios-http.com/docs/interceptors
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- Vite - Env Variables and Modes  
  https://vite.dev/guide/env-and-mode
- MDN - HTTP Authorization header  
  https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Authorization

## 목적

`App2.jsx` 안에서 직접 작성한 axios 호출의 반복을 확인한 뒤 `frontend/src/api/client.js`로 분리한다.

이 리팩토링은 처음부터 예쁜 구조를 만들기 위한 작업이 아니라, 실제로 생긴 중복을 줄이기 위한 작업이다.

## 성공 기준

- [x] `axios.create()`로 공통 API 인스턴스를 만든다.
- [x] `baseURL`을 `import.meta.env.VITE_API_BASE_URL || '/api'`로 둔다.
- [x] request interceptor에서 token을 자동으로 붙인다.
- [x] `unwrap()`으로 `response.data` 반복을 제거한다.
- [x] `getErrorMessage()`로 에러 메시지 파싱을 모은다.
- [x] `authApi`, `postApi`, `aiApi`로 기능별 API 함수를 묶는다.
- [x] `App2.jsx`에서 직접 axios 호출을 의미 있는 API 함수 호출로 바꾼다.

## 작업 체크리스트

### 1. 반복 지점 목록화하기

- [x] URL 문자열이 반복되는 위치를 표시한다.
- [x] `response.data`를 직접 꺼내는 위치를 표시한다.
- [x] token header를 직접 붙이는 위치를 표시한다.
- [x] try/catch 안의 에러 메시지 파싱이 반복되는 위치를 표시한다.
- [x] 이 반복이 실제 문제인지 설명한다.

### 2. axios 공통 인스턴스 만들기

- [x] `frontend/src/api/client.js`에서 `axios.create()`를 사용한다.
- [x] `baseURL`은 `import.meta.env.VITE_API_BASE_URL || '/api'`로 둔다.
- [x] timeout을 적절히 둔다.
- [x] 공통 인스턴스 자체보다 기능별 API 객체를 export한다.

### 3. token interceptor 만들기

- [x] request interceptor를 추가한다.
- [x] `localStorage.getItem('token')`으로 token을 읽는다.
- [x] token이 있으면 `Authorization: Bearer ${token}`을 붙인다.
- [x] token이 없으면 Authorization header를 붙이지 않는다.
- [x] interceptor가 요청 body를 바꾸지 않는지 확인한다.

### 4. unwrap helper 만들기

- [x] `const unwrap = (response) => response.data` 형태의 helper를 만든다.
- [x] API 함수 내부에서 `.then(unwrap)` 또는 `await` 후 unwrap을 사용한다.
- [x] `App2.jsx`가 Axios 응답 구조를 직접 몰라도 되게 한다.

### 5. getErrorMessage helper 만들기

- [x] 백엔드 응답의 `message`를 우선 사용한다.
- [x] 응답은 있지만 message가 없으면 status 기반 메시지를 만든다.
- [x] 응답 자체가 없으면 서버 연결 실패 메시지를 반환한다.
- [x] `getErrorMessage(error)`를 export한다.

### 6. authApi 만들기

- [x] `authApi.signup(payload)`를 만든다.
- [x] `authApi.login(payload)`를 만든다.
- [ ] 필요하면 `authApi.me()`를 만든다.
- [x] `App2.jsx`의 회원가입 axios 호출을 `authApi.signup(payload)`로 바꾼다.
- [x] `App2.jsx`의 로그인 axios 호출을 `authApi.login(payload)`로 바꾼다.

### 7. postApi 만들기

- [x] `postApi.list(params)`를 만든다.
- [x] `postApi.get(id)`를 만든다.
- [x] `postApi.create(payload)`를 만든다.
- [x] `postApi.update(id, payload)`를 만든다.
- [x] `postApi.remove(id)`를 만든다.
- [x] `App2.jsx`의 목록/상세/작성/수정 axios 호출을 postApi로 바꾼다.

### 8. aiApi 만들기

- [x] `aiApi.similar(payload)` 또는 현재 백엔드 endpoint에 맞는 함수를 만든다.
- [x] `aiApi.agent(payload)`를 만든다.
- [x] 필요하면 `aiApi.mcp(payload)`를 만든다.
- [x] `App2.jsx`의 AI axios 호출을 aiApi로 바꾼다.

### 9. App2 정리하기

- [x] `App2.jsx`에서 axios import를 제거한다.
- [x] `App2.jsx`에서 직접 Authorization header를 붙이는 코드를 제거한다.
- [x] `App2.jsx`에서 `response.data`를 직접 꺼내는 코드를 제거한다.
- [x] `App2.jsx`의 try/catch에서 `getErrorMessage(error)`를 사용한다.
- [x] 화면 흐름 함수 이름이 더 읽기 쉬워졌는지 확인한다.

## 검증 체크리스트

- [ ] 회원가입이 리팩토링 전과 동일하게 동작하는지 확인한다.
- [ ] 로그인이 리팩토링 전과 동일하게 동작하는지 확인한다.
- [ ] 목록 조회가 리팩토링 전과 동일하게 동작하는지 확인한다.
- [ ] 상세 조회가 리팩토링 전과 동일하게 동작하는지 확인한다.
- [ ] 작성/수정 요청에 Authorization header가 자동으로 붙는지 확인한다.
- [ ] API 실패 시 `getErrorMessage()` 결과가 화면에 보이는지 확인한다.
- [ ] `App2.jsx`를 읽었을 때 UI 흐름이 axios 설정보다 먼저 보이는지 확인한다.

## WHY 정리 질문

- [ ] 왜 처음부터 `client.js`를 만들지 않고 마지막에 분리했는가?
- [ ] 왜 API 함수를 `authApi`, `postApi`, `aiApi`로 묶는가?
- [ ] 왜 `axios.create()`가 필요한가?
- [ ] 왜 `baseURL`을 환경변수와 `/api` fallback으로 관리하는가?
- [ ] 왜 token header는 interceptor에서 자동으로 붙이는가?
- [ ] 왜 `unwrap()`으로 `response.data` 반복을 제거하는가?
- [ ] 왜 에러 메시지 파싱을 `getErrorMessage()` 하나로 모으는가?
