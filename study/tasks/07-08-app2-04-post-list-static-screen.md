# 07-08 App2 Step 04: 게시글 목록 정적 화면 만들기

## 공식문서 URL

- React - Rendering Lists  
  https://react.dev/learn/rendering-lists
- React - Conditional Rendering  
  https://react.dev/learn/conditional-rendering
- React - Writing Markup with JSX  
  https://react.dev/learn/writing-markup-with-jsx
- MDN - Array.prototype.map()  
  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/map
- MDN - HTML `<button>` element  
  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/button

## 목적

게시판의 시작 화면인 목록 UI를 먼저 만든다.

아직 서버 데이터와 연결하지 않고, 화면에 어떤 정보가 필요한지 파악한다.

## 성공 기준

- [x] 게시글 목록 영역이 화면에 보인다.
- [x] 검색 입력칸과 검색 버튼이 보인다.
- [x] 글쓰기 버튼이 보인다.
- [x] 임시 게시글 2~3개가 화면에 반복 표시된다.
- [x] 아직 목록 API를 호출하지 않는다.

## 작업 체크리스트

### 1. 목록 섹션 만들기

- [x] `section` 또는 `main` 안에 게시글 목록 영역을 만든다.
- [x] 목록 제목을 표시한다.
- [x] 검색 form 자리를 만든다.
- [x] 글쓰기 버튼 자리를 만든다.

### 2. 임시 게시글 데이터 만들기

- [x] 함수 바깥 또는 컴포넌트 안에 임시 배열을 만든다.
- [x] 각 게시글에 `id`, `title`, `authorNickname`, `createdAt`을 둔다.
- [x] 필요하면 `summary` 또는 `content` 일부를 둔다.
- [x] 지금은 `useState` 없이 고정 배열로 시작한다.

### 3. 목록 렌더링 만들기

- [x] 임시 배열을 `map`으로 반복 렌더링한다.
- [x] 게시글 제목을 표시한다.
- [x] 작성자 닉네임을 표시한다.
- [x] 작성일을 표시한다.
- [x] 게시글이 없을 때 보여줄 빈 목록 문구를 준비한다.

### 4. 버튼 위치 잡기

- [x] 검색 버튼 위치를 확인한다.
- [x] 글쓰기 버튼 위치를 확인한다.
- [x] 목록 아이템이 클릭 가능한 형태인지 확인한다.
- [x] 아직 실제 클릭 동작은 없어도 된다.

## 검증 체크리스트

- [x] 임시 게시글 여러 개가 목록으로 보이는지 확인한다.
- [ ] 게시글 배열이 비었을 때 빈 목록 문구가 자연스러운지 확인한다.
- [x] 로그인 여부와 관계없이 목록은 볼 수 있는지 확인한다.
- [x] 글쓰기 버튼은 로그인 이후에만 활성화할지 기준을 생각한다.

## WHY 정리 질문

- [ ] 왜 게시글 목록은 게시판 기능의 시작점인가?
- [ ] 왜 목록은 객체 하나가 아니라 배열 구조가 필요한가?
- [ ] 왜 정적 배열로 먼저 화면을 확인하는가?
- [ ] 왜 아직 `loadPosts()`를 만들지 않아도 되는가?

## 다음 단계

- [ ] `07-08-app2-05-post-list-state-search.md`로 넘어간다.
