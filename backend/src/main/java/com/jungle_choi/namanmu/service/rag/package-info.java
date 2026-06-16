/**
 * RAG 기능을 구성하는 service 패키지입니다.
 *
 * <p>이 패키지는 게시글을 embedding으로 바꾸고, 유사 게시글을 검색하고, 검색된 글을 근거로
 * 초안을 생성하는 흐름을 담당합니다. Project Alpha는 전용 Vector DB 대신 MySQL에 벡터 JSON과
 * 청크 데이터를 저장하고, 서버 코드에서 vector similarity, BM25, keyword signal을 조합합니다.
 *
 * <p>대표 흐름:
 * <pre>
 * AiController
 * -> SimilarPostSearchService
 * -> RagDraftService
 * -> OpenAiEmbeddingClient / OpenAiTextClient
 * </pre>
 */
package com.jungle_choi.namanmu.service.rag;
