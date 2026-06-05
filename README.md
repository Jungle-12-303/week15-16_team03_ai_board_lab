AI 응용 기술을 활용한 게시판 구현 개인 과제 제출 저장소

과제 개요
이번 과제는 상용 LLM과 AI 응용 기술을 활용하여 실제 서비스 수준의 게시판 웹 애플리케이션을 개인별로 설계하고 구현
각 개인은 프론트엔드, 백엔드, 데이터베이스, RAG, MCP, AI Agent 기능을 직접 구현

제출 방식
이 저장소는 팀 공동 구현물이 아니라, 개인별 과제 제출물을 브랜치 단위로 관리하는 저장소
main 브랜치는 전체 제출용, 각 개인의 실제 소스코드와 상세 README는 개인별 제출 브랜치에 있습니다.

브랜치 목록
이름	브랜치	Backend	Database	주요 AI 기능
주호석	beomsang	Spring Boot	MySQL	RAG, MCP, AI Agent
황정연	member-a	Spring Boot	PostgresDB	RAG, MCP, AI Agent
최현진	member-b	FastAPI	TBD	RAG, MCP, AI Agent
이우진	member-c	FastAPI	TBD	RAG, MCP, AI Agent
조범상	member-d	Spring Boot	MySQL	RAG, MCP, AI Agent

각 개인 브랜치는 아래 구조를 기준으로 구성합니다.
submission/name
 ├─ frontend/
 ├─ backend/
 ├─ docs/
 ├─ docker-compose.yml
 ├─ README.md
 └─ .gitignore

각 개인 브랜치의 README.md에는 다음 내용을 포함
1. 프로젝트 개요
2. 주요 구현 기능
3. 전체 아키텍처 구조
4. RAG 기능 구조
5. MCP 기능 구조
6. AI Agent 기능 구조
7. 실행 방법
8. 데모 스크린샷
9. 회고, 한계점, 개선 아이디어
