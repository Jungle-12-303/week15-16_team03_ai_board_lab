# 07-08 App2 Step 02: 로그인/회원가입 form state와 console 흐름 만들기

## 공식문서 URL

- React - State: A Component's Memory  
  https://react.dev/learn/state-a-components-memory
- React - Reacting to Input with State  
  https://react.dev/learn/reacting-to-input-with-state
- React - Responding to Events  
  https://react.dev/learn/responding-to-events
- React - Updating Objects in State  
  https://react.dev/learn/updating-objects-in-state
- React DOM - `<input>`  
  https://react.dev/reference/react-dom/components/input
- React DOM - `<form>`  
  https://react.dev/reference/react-dom/components/form

## 목적

정적 입력칸을 React가 제어하는 form으로 바꾼다.

아직 서버로 요청하지 않고, submit 시 payload가 제대로 만들어지는지만 `console.log`로 확인한다.

## 성공 기준

- [x] 로그인 입력값이 React state에 저장된다.
- [x] 회원가입 입력값이 React state에 저장된다.
- [x] submit 시 서버가 받을 payload 모양을 만들 수 있다.
- [ ] 아직 axios 호출은 하지 않는다.

## 작업 체크리스트

### 1. 로그인 form state 만들기

- [x] `loginForm` state를 만든다.
- [x] `loginForm.email`을 이메일 input의 `value`에 연결한다.
- [x] `loginForm.password`를 비밀번호 input의 `value`에 연결한다.
- [x] input 변경 시 `setLoginForm`으로 해당 필드만 갱신한다.

### 2. 회원가입 form state 만들기

- [x] `signupForm` state를 만든다.
- [x] `signupForm.email`을 이메일 input의 `value`에 연결한다.
- [x] `signupForm.nickname`을 닉네임 input의 `value`에 연결한다.
- [x] `signupForm.password`를 비밀번호 input의 `value`에 연결한다.
- [x] input 변경 시 `setSignupForm`으로 해당 필드만 갱신한다.

### 3. submit handler 만들기

- [x] `handleLoginSubmit(event)`를 만든다.
- [x] `event.preventDefault()`를 호출한다.
- [x] 로그인 payload를 만든다.
- [ ] payload를 `console.log`로 확인한다.
- [x] `handleSignupSubmit(event)`를 만든다.
- [x] 회원가입 payload를 만든다.
- [ ] payload를 `console.log`로 확인한다.

### 4. 간단한 입력 검증 추가하기

- [x] 이메일이 비어 있으면 submit을 막는다.
- [x] 비밀번호가 비어 있으면 submit을 막는다.
- [x] 회원가입에서는 닉네임이 비어 있으면 submit을 막는다.
- [x] 실패 메시지를 `notice` state로 보여준다.

## 검증 체크리스트

- [x] 로그인 email 입력 시 `loginForm.email`이 바뀌는지 확인한다.
- [x] 로그인 password 입력 시 `loginForm.password`가 바뀌는지 확인한다.
- [x] 회원가입 nickname 입력 시 `signupForm.nickname`이 바뀌는지 확인한다.
- [ ] 로그인 submit 시 `{ email, password }` payload가 콘솔에 찍히는지 확인한다.
- [ ] 회원가입 submit 시 `{ email, password, nickname }` payload가 콘솔에 찍히는지 확인한다.
- [ ] 빈 값 submit 시 API 호출 없이 메시지가 나오는지 확인한다.

## WHY 정리 질문

- [ ] 왜 input 값을 DOM에서 직접 읽지 않고 React state에 둬야 하는가?
- [ ] 왜 로그인 form과 회원가입 form state를 분리하는가?
- [ ] 왜 submit에서 바로 axios를 호출하지 않고 먼저 payload를 확인하는가?
- [ ] 왜 `event.preventDefault()`가 필요한가?

## 다음 단계

- [ ] `07-08-app2-03-auth-api-localstorage.md`로 넘어간다.
