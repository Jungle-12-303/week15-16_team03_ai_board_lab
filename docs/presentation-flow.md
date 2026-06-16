# Project Alpha 7분 발표 흐름

이 문서는 팀 발표에서 한 명이 7분 안에 Project Alpha를 설명하고 시연하기 위한 흐름이다.

## 발표 목표

| 목표 | 설명 |
|---|---|
| 과제 충족 | React, Spring Boot, MySQL, RAG, MCP, AI Agent를 모두 구현했음을 보인다. |
| 기술 선택 설명 | 왜 Qdrant, BM25, RRF, Nori, OpenAI를 조합했는지 말한다. |
| 실제 동작 증명 | 화면에서 글 작성, 유사글 검색, MCP 검증, Agent 추천이 동작하는 것을 보여준다. |
| 회고 | 구현하면서 배운 점, 한계, 다음 개선 방향을 명확히 말한다. |

## 7분 타임라인

| 시간 | 내용 | 화면/자료 | 말할 핵심 |
|---:|---|---|---|
| 0:00-0:40 | 프로젝트 소개 | README 첫 화면 | Project Alpha는 LinkedIn 스타일 기록 게시판에 AI 기능을 붙인 서비스다. |
| 0:40-1:20 | 요구사항 대응 | README 요구사항 표 | 기본 게시판, RAG, MCP, Agent 요구사항을 각각 구현했다. |
| 1:20-2:00 | 아키텍처 설명 | 아키텍처 이미지 또는 Mermaid | React, Spring Boot, MySQL, Qdrant, OpenAI, 외부 API가 역할을 나눠 가진다. |
| 2:00-3:20 | RAG 데모 | 글 작성 모달 | 작성 중인 제목/본문/태그로 유사 게시글을 찾고, 근거 기반 초안을 만든다. |
| 3:20-4:20 | RAG 개선 설명 | RAG 보고서 핵심 표 | vector-only에서 Qdrant + BM25 + RRF + Nori + chunk evidence 구조로 개선했다. |
| 4:20-5:10 | MCP 데모 | 게시글 상세 fact check | MCP server가 GitHub/날씨 같은 외부 도구를 호출하고 글의 주장과 비교한다. |
| 5:10-5:50 | Agent 데모 | Missed posts 패널 | 사용자 읽음 기록 기반으로 이미 본 글을 제외하고 놓친 글 5개를 추천한다. |
| 5:50-6:35 | 평가와 한계 | RAG 성능 보고서 | `MRR@5 1.0`, `NDCG@5 0.9076`, RAGAS 보조 평가, 과적합/holdout 한계를 설명한다. |
| 6:35-7:00 | 회고와 개선 | 한계점 표 | reranker, 평가셋 확장, 배포 migration, MCP 도구 확장이 다음 과제다. |

## 발표 스크립트 요약

### 1. 소개

Project Alpha는 개발, 학습, 프로젝트, 일상 기록을 남기는 게시판입니다. 단순 CRUD 게시판이 아니라, 사용자가 글을 쓰는 순간 기존 게시글을 찾아주고, 이미 쓴 글은 외부 데이터로 검증하며, 읽지 않은 글을 개인화 추천하는 흐름을 목표로 했습니다.

### 2. 기술 구조

프론트엔드는 React, 백엔드는 Spring Boot와 Java로 구현했습니다. MySQL은 원본 데이터를 저장하고, Qdrant는 임베딩 벡터 후보 검색을 담당합니다. OpenAI는 embedding과 초안 생성을 맡고, MCP는 GitHub와 날씨 같은 외부 데이터를 호출하는 도구 계층으로 구현했습니다.

### 3. RAG

RAG는 작성 중인 제목, 본문, 태그를 query로 사용합니다. Qdrant가 먼저 유사 벡터 후보를 찾고, Spring Boot에서 MySQL 원본을 조회한 뒤 BM25, RRF, metadata, keyword, chunk evidence를 조합해 재정렬합니다. 최종적으로 관련 게시글을 근거로 초안을 생성합니다.

### 4. MCP

MCP는 LLM이 외부 시스템을 호출할 수 있게 만드는 계층으로 구현했습니다. 예를 들어 게시글에 GitHub 저장소 주장이 있으면 MCP tool이 GitHub REST API를 호출하고, 실제 저장소 정보와 게시글 내용을 비교해 `Supported`, `Contradicted`, `Insufficient` 같은 판정을 반환합니다.

### 5. Agent

Agent는 사용자별 최근 읽은 글을 관찰하고, 태그와 카테고리 흐름을 추론한 뒤, 이미 읽은 글을 제외하고 놓친 글 5개를 추천합니다. 내부 흐름은 observe, infer, retrieve, rank 단계로 나누었습니다.

### 6. 평가

RAG 검색은 `Precision@5`, `Recall@5`, `MRR@5`, `NDCG@5`, `Hit@5`로 평가했습니다. 최종 온라인 검증 기준으로 `Precision@5 0.8667`, `MRR@5 1.0`, `NDCG@5 0.9076`, `Hit@5 1.0`이 나왔습니다. RAGAS는 검색 랭킹을 대체하지 않고, 생성 초안의 근거성과 응답 관련성을 보는 보조 평가로 사용했습니다.

### 7. 회고

가장 큰 배움은 RAG 성능이 embedding 하나로 결정되지 않는다는 점입니다. 실제 서비스에서는 Qdrant 후보 생성, BM25 단어 검색, RRF 순위 결합, metadata 완화, 한국어 형태소 분석, chunk 근거가 함께 작동해야 했습니다. 다음 개선은 수동 라벨 평가셋 확장, holdout 평가, reranker 도입, MCP 도구 확장입니다.

## 데모 실패 시 대체 흐름

| 실패 상황 | 대체 자료 |
|---|---|
| OpenAI API 지연 | `docs/demo/rag-composer.png`와 RAG 보고서 점수표로 설명 |
| GitHub API rate limit | `docs/demo/mcp-fact-check.png` 스크린샷으로 대체 |
| 서버 장애 | `docs/project-alpha-architecture.png`, `docs/demo-scenario.md`로 구조 설명 |
| 시간이 부족함 | RAG 데모만 실제 화면으로 보여주고 MCP/Agent는 스크린샷과 문서로 설명 |
