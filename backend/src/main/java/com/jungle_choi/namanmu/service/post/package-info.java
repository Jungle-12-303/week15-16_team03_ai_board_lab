/**
 * 게시판의 기본 업무 규칙을 다루는 service 패키지입니다.
 *
 * <p>이 패키지는 AI 기능보다 먼저 이해해야 하는 핵심 흐름입니다. 게시글 CRUD, 댓글, 태그 저장,
 * 상세 조회 시 읽음 기록 저장이 여기에 들어 있습니다. Controller는 HTTP 요청과 응답을 담당하고,
 * 이 패키지의 Service는 "누가 어떤 데이터를 만들고 바꿀 수 있는가" 같은 규칙을 담당합니다.
 *
 * <p>대표 흐름:
 * <pre>
 * PostController
 * -> PostService
 * -> PostRepository / PostTagService / PostReadService
 * </pre>
 */
package com.jungle_choi.namanmu.service.post;
