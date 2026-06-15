# 07-08 App2 Step 03: 로그인/회원가입 API와 localStorage 연결하기

## 공식문서 URL

- Axios - API reference  
  https://axios-http.com/docs/api_intro
- Axios - Request Config  
  https://axios-http.com/docs/req_config
- Axios - Handling Errors  
  https://axios-http.com/docs/handling_errors
- MDN - Window localStorage  
  https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage
- MDN - JSON.parse()  
  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON/parse
- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory

## 목적

form submit에서 만든 payload를 실제 백엔드 API로 보낸다.

아직 `client.js`로 분리하지 않고, `App2.jsx` 안에서 axios를 직접 호출해서 반복과 불편함을 경험한다.

## 성공 기준

- [x] 회원가입 submit에서 `axios.post('/api/auth/signup', payload)` 흐름을 만든다.
- [x] 로그인 submit에서 `axios.post('/api/auth/login', payload)` 흐름을 만든다.
- [x] 로그인 성공 시 token과 user를 `localStorage`에 저장한다.
- [x] 로그인 성공 시 React `user` state도 갱신한다.
- [x] 로그아웃 시 `localStorage`와 React state를 함께 비운다.

## 작업 체크리스트

### 1. axios 직접 호출 준비하기

- [x] `axios`를 import한다.
- [x] 회원가입 submit에서 `axios.post('/api/auth/signup', payload)`를 호출한다.
- [x] 로그인 submit에서 `axios.post('/api/auth/login', payload)`를 호출한다.
- [x] 응답에서 `response.data`를 직접 꺼낸다.

### 2. 회원가입 성공 흐름 만들기

- [x] 회원가입 요청 성공 시 성공 메시지를 `notice`에 저장한다.
- [x] 회원가입 성공 후 `signupForm`을 초기화한다.
- [x] 회원가입 성공 후 로그인 form으로 자연스럽게 이어질 수 있게 한다.
- [x] 회원가입 실패 시 에러 메시지를 화면에 보여준다.

### 3. 로그인 성공 흐름 만들기

- [x] `user` state를 만든다.
- [x] 로그인 응답에서 token 값을 확인한다.
- [x] 로그인 응답에서 user 값을 확인한다.
- [x] token을 `localStorage.setItem('token', token)`으로 저장한다.
- [x] user를 `localStorage.setItem('user', JSON.stringify(user))`로 저장한다.
- [x] React `user` state에 user를 저장한다.
- [x] 로그인 form을 초기화한다.

### 4. 새로고침 후 로그인 복원 만들기

- [x] `useState` 초기값에서 `localStorage.user`를 읽는다.
- [x] 저장된 user JSON이 있으면 객체로 변환한다.
- [x] 저장된 user가 없으면 `null`을 사용한다.
- [x] JSON 파싱 실패 가능성을 간단히 처리한다.

### 5. 로그아웃 흐름 만들기

- [x] 로그아웃 버튼을 만든다.
- [x] 로그아웃 시 `localStorage.removeItem('token')`을 호출한다.
- [x] 로그아웃 시 `localStorage.removeItem('user')`를 호출한다.
- [x] 로그아웃 시 `setUser(null)`을 호출한다.
- [x] 로그아웃 후 로그인/회원가입 영역이 다시 보이는지 확인한다.

## 검증 체크리스트

- [ ] 회원가입 성공 시 화면에 성공 메시지가 보이는지 확인한다.
- [ ] 중복 이메일 등 실패 응답이 오면 실패 메시지가 보이는지 확인한다.
- [ ] 로그인 성공 시 `localStorage`에 token과 user가 저장되는지 확인한다.
- [ ] 로그인 성공 시 화면 상단에 사용자 정보가 보이는지 확인한다.
- [ ] 새로고침 후에도 로그인 상태가 유지되는지 확인한다.
- [ ] 로그아웃 후 `localStorage`와 React 화면이 모두 로그아웃 상태가 되는지 확인한다.

## WHY 정리 질문

- [ ] 왜 로그인 성공 후 token은 `localStorage`에 저장하는가?
- [ ] 왜 user를 `localStorage`에만 두지 않고 React state에도 둬야 하는가?
- [ ] 왜 이 단계에서는 axios URL과 `response.data`를 일부러 App2 안에 둬도 되는가?
- [ ] 왜 로그아웃할 때 저장소와 state를 둘 다 비워야 하는가?

## 다음 단계

- [ ] `07-08-app2-04-post-list-static-screen.md`로 넘어간다.
