/**
 * MCP와 외부 도구 기반 fact check를 다루는 service 패키지입니다.
 *
 * <p>MCP는 외부 시스템을 도구처럼 호출하기 위한 통로입니다. 이 프로젝트에서는 Spring Boot 내부에
 * JSON-RPC 스타일의 MCP endpoint를 만들고, GitHub와 Weather API를 도구로 연결했습니다.
 * 게시글 상세 화면의 fact check는 외부 데이터를 단순히 보여주는 것이 아니라, 글의 주장과 비교합니다.
 *
 * <p>대표 흐름:
 * <pre>
 * PostFactCheckController
 * -> McpFactCheckService
 * -> GitHubFactCheckService / WeatherFactCheckService
 * -> McpServerService
 * </pre>
 */
package com.jungle_choi.namanmu.service.mcp;
