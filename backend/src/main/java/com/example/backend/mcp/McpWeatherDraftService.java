package com.example.backend.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
public class McpWeatherDraftService {

    private final ObjectMapper objectMapper;

    @Value("${mcp.node-command:node}")
    private String nodeCommand;

    @Value("${mcp.server-dir:../mcp-server}")
    private String mcpServerDir;

    public McpWeatherDraftService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public McpWeatherDraftResponse createWeatherDraft(
        String city,
        Integer forecastDays,
        String requestedBy
    ) {
        String trimmedCity = city == null ? "" : city.trim();

        if (trimmedCity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "도시 이름을 입력해야 합니다.");
        }

        int safeForecastDays = forecastDays == null ? 2 : forecastDays;

        if (safeForecastDays < 1 || safeForecastDays > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "forecastDays는 1~3 사이여야 합니다.");
        }

        Path serverDir = resolveMcpServerDir();
        Path agentPath = serverDir.resolve("dist").resolve("agent.js");

        if (!Files.exists(agentPath)) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MCP 서버 빌드 파일이 없습니다. mcp-server에서 npm run build를 먼저 실행하세요."
            );
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
            nodeCommand,
            agentPath.toString(),
            trimmedCity,
            String.valueOf(safeForecastDays),
            "--json"
        );
        processBuilder.directory(serverDir.toFile());

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(60, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "MCP 날씨 초안 생성이 제한 시간 안에 끝나지 않았습니다."
                );
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            if (process.exitValue() != 0) {
                String message = stderr.isEmpty()
                    ? "MCP 날씨 초안 생성 중 오류가 발생했습니다."
                    : "MCP 날씨 초안 생성 중 오류가 발생했습니다. " + stderr;
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
            }

            if (stdout.isEmpty()) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MCP 에이전트가 초안 결과를 반환하지 않았습니다."
                );
            }

            McpWeatherDraftResponse response = objectMapper.readValue(stdout, McpWeatherDraftResponse.class);
            response.setRequestedBy(requestedBy);
            response.setCity(trimmedCity);
            response.setForecastDays(safeForecastDays);
            return response;
        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "MCP 에이전트 실행에 실패했습니다. node 실행 환경을 확인하세요."
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "MCP 에이전트 실행이 중단되었습니다."
            );
        }
    }

    private Path resolveMcpServerDir() {
        Path configuredPath = Path.of(mcpServerDir).normalize().toAbsolutePath();

        if (Files.exists(configuredPath)) {
            return configuredPath;
        }

        Path fallbackPath = Path.of("mcp-server").normalize().toAbsolutePath();

        if (Files.exists(fallbackPath)) {
            return fallbackPath;
        }

        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "mcp-server 폴더를 찾을 수 없습니다."
        );
    }
}
