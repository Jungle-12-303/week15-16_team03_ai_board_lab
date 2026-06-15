# AGENTS.md

## 학습 중심 원칙

이 프로젝트는 코드를 복사해서 쓰는 것보다 “왜 이렇게 설계했는지”를 이해하는 것을 우선한다.

설명할 때는 다음 구조를 따른다.

1. 전체 구조 설명
2. 설계 이유
3. 핵심 코드 단위 설명
4. 중요한 포인트 정리

## 프로젝트 맥락

- 개인 2주 과제
- React + Spring Boot + PostgreSQL 게시판
- AI 필수 기능: RAG, MCP, Agent
- DevOps: Docker Compose, EC2, Nginx, 로그 기록

## 구현 우선순위

1. 기본 게시판 기능
2. RAG 최소 기능
3. MCP 최소 기능
4. Agent 최소 기능
5. Docker Compose와 배포 문서

## 기술 스택

- Frontend: React + Vite
- Backend: Spring Boot
- DB: PostgreSQL + pgvector
- AI: OpenAI API, 키가 없으면 fallback
- MCP: JSON-RPC 스타일 GitHub API 호출
