# 07-08 App2 Step 01: 로그인/회원가입 정적 화면 만들기

## 공식문서 URL

- React - Your First Component  
  https://react.dev/learn/your-first-component
- React - Importing and Exporting Components  
  https://react.dev/learn/importing-and-exporting-components
- React - Writing Markup with JSX  
  https://react.dev/learn/writing-markup-with-jsx
- React DOM - `<form>`  
  https://react.dev/reference/react-dom/components/form
- React DOM - `<input>`  
  https://react.dev/reference/react-dom/components/input
- MDN - HTML `<form>` element  
  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/form
- MDN - HTML `<input>` element  
  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/input

## 목적

로그인/회원가입 기능의 첫 단계는 API가 아니라 화면이다.

사용자가 어떤 값을 입력해야 하는지 먼저 보이게 만든다.

## 성공 기준

- [x] `frontend/src/App2.jsx`에서 로그인 영역과 회원가입 영역이 화면에 보인다.
- [x] 이메일, 비밀번호, 닉네임 입력칸의 역할을 구분할 수 있다.
- [ ] 아직 `useState`, `axios`, `client.js`를 사용하지 않는다.
- [ ] 버튼은 있어도 실제 동작은 없어도 된다.

## 작업 체크리스트

### 1. App2 기본 틀 만들기

- [x] `App2.jsx`에서 `function App2()`를 만든다.
- [x] 최상위에 `className="app"` 같은 루트 영역을 만든다.
- [x] 화면 상단에 서비스 이름을 표시한다.
- [x] 현재 단계가 인증 화면이라는 것을 알아볼 수 있게 제목을 둔다.

### 2. 로그인 화면 만들기

- [x] 로그인 form 영역을 만든다.
- [x] 이메일 입력칸을 만든다.
- [x] 비밀번호 입력칸을 만든다.
- [x] 로그인 버튼을 만든다.
- [ ] form submit 동작은 아직 구현하지 않는다.

### 3. 회원가입 화면 만들기

- [x] 회원가입 form 영역을 만든다.
- [x] 이메일 입력칸을 만든다.
- [x] 닉네임 입력칸을 만든다.
- [x] 비밀번호 입력칸을 만든다.
- [x] 회원가입 버튼을 만든다.
- [ ] form submit 동작은 아직 구현하지 않는다.

### 4. 화면만 보고 점검하기

- [x] 로그인과 회원가입 영역이 시각적으로 구분되는지 확인한다.
- [x] 닉네임은 회원가입에만 필요하다는 점이 화면에서 드러나는지 확인한다.
- [x] 입력칸 label이 없어도 사용 목적을 알 수 있는지 확인한다.

## 검증 체크리스트

- [ ] `npm run dev`로 화면이 열리는지 확인한다.
- [x] 콘솔에 React 렌더링 에러가 없는지 확인한다.
- [x] 입력칸에 글자를 입력할 수 있는지 확인한다.
- [ ] 버튼 클릭 시 아직 아무 일도 일어나지 않는 것이 자연스러운 단계인지 확인한다.

## WHY 정리 질문

- [ ] 왜 로그인/회원가입 기능에서 API보다 화면을 먼저 만드는가?
- [ ] 왜 닉네임 입력칸은 로그인 form에는 없어야 하는가?
- [ ] 왜 이 단계에서는 `useState`를 아직 쓰지 않는가?
- [ ] 왜 버튼 동작 없이 화면만 먼저 확인해도 의미가 있는가?

## 다음 단계

- [ ] `07-08-app2-02-auth-form-state-console.md`로 넘어간다.
