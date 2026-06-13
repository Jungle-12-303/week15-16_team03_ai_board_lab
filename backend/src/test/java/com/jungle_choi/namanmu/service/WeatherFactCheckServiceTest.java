package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WeatherFactCheckServiceTest {

    private final McpServerService mcpServerService = Mockito.mock(McpServerService.class);
    private final OpenAiTextClient openAiTextClient = Mockito.mock(OpenAiTextClient.class);
    private final WeatherFactCheckService weatherFactCheckService = new WeatherFactCheckService(
            mcpServerService,
            openAiTextClient,
            new ObjectMapper());

    @Test
    void checkDoesNotTreatBusinessTextAsRainClaim() {
        WeatherFactCheckService.WeatherFactCheckResult result = weatherFactCheckService.check(
                "Briefing",
                "노코드 자동화 툴로 일잘러 데이터 분석가 되기",
                "비즈니스 데이터 팀은 데이터로 운영과 마케팅을 개선합니다.",
                List.of("Data", "Briefing"));

        assertThat(result.status()).isEqualTo("NOT_SUPPORTED");
        verify(mcpServerService, never()).callWeatherTool(anyString());
    }

    @Test
    void checkDoesNotCallWeatherToolWhenLocationIsMissing() {
        WeatherFactCheckService.WeatherFactCheckResult result = weatherFactCheckService.check(
                "Daily",
                "오늘 날씨 메모",
                "오늘은 비가 올 것 같아서 우산을 챙기려고 한다.",
                List.of("Weather"));

        assertThat(result.status()).isEqualTo("LOCATION_REQUIRED");
        verify(mcpServerService, never()).callWeatherTool(anyString());
    }

    @Test
    void checkCallsWeatherToolOnlyWhenWeatherClaimHasLocation() {
        WeatherApiClient.WeatherReport weatherReport = new WeatherApiClient.WeatherReport(
                "대구",
                "12:00 PM",
                28.0,
                60,
                0.0,
                8.0,
                "맑음",
                30.0,
                22.0,
                0.0,
                "wttr.in");
        when(mcpServerService.callWeatherTool("대구"))
                .thenReturn(new McpServerService.McpToolCallResult(
                        false,
                        List.of(),
                        weatherReport));
        when(openAiTextClient.generateText(anyString(), anyString()))
                .thenReturn(new OpenAiTextClient.TextGenerationResult("판정: 확인됨"));

        WeatherFactCheckService.WeatherFactCheckResult result = weatherFactCheckService.check(
                "Daily",
                "대구 오늘 날씨",
                "대구는 오늘 맑고 덥다.",
                List.of("Weather"));

        assertThat(result.status()).isEqualTo("CHECKED");
        assertThat(result.location()).isEqualTo("대구");
        verify(mcpServerService).callWeatherTool("대구");
    }

    @Test
    void checkParsesStructuredJudgementJson() {
        WeatherApiClient.WeatherReport weatherReport = new WeatherApiClient.WeatherReport(
                "서울",
                "12:00 PM",
                10.0,
                45,
                0.0,
                4.0,
                "맑음",
                12.0,
                6.0,
                0.0,
                "wttr.in");
        when(mcpServerService.callWeatherTool("서울"))
                .thenReturn(new McpServerService.McpToolCallResult(
                        false,
                        List.of(),
                        weatherReport));
        when(openAiTextClient.generateText(anyString(), anyString()))
                .thenReturn(new OpenAiTextClient.TextGenerationResult("""
                        {
                          "claim": "서울은 오늘 비가 온다",
                          "verdict": "contradicted",
                          "comparison": "외부 정보는 맑음과 강수량 0.0mm를 보여준다.",
                          "suggestion": "서울은 오늘 맑은 편이라고 쓰는 것이 안전합니다.",
                          "summary": "게시글의 비 예보 표현은 현재 외부 날씨와 맞지 않습니다."
                        }
                        """));

        WeatherFactCheckService.WeatherFactCheckResult result = weatherFactCheckService.check(
                "Daily",
                "서울 오늘 날씨",
                "서울은 오늘 비가 온다.",
                List.of("Weather"));

        assertThat(result.status()).isEqualTo("CHECKED");
        assertThat(result.claim()).isEqualTo("서울은 오늘 비가 온다");
        assertThat(result.verdict()).isEqualTo("contradicted");
        assertThat(result.comparison()).contains("강수량 0.0mm");
        assertThat(result.suggestion()).contains("맑은 편");
        assertThat(result.judgement()).contains("맞지 않습니다");
    }
}
