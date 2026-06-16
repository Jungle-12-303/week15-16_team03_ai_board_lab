/**
 * 사용자 상태를 기반으로 놓친 글을 추천하는 Agent service 패키지입니다.
 *
 * <p>이 프로젝트의 Agent는 별도 프레임워크를 쓰지 않고, 읽음 기록을 관찰하고 관심 태그를 추론한 뒤
 * 아직 읽지 않은 글을 점수화합니다. 핵심은 단순 랜덤 추천이 아니라, 사용자별 상태를 바탕으로
 * observe, infer, retrieve, rank 단계를 거친다는 점입니다.
 *
 * <p>대표 흐름:
 * <pre>
 * AgentController
 * -> AgentRecommendationService
 * -> PostReadRepository / PostRepository / PostTagRepository
 * </pre>
 */
package com.jungle_choi.namanmu.service.agent;
