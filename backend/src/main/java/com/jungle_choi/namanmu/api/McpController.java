package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.service.mcp.McpServerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcp")
public class McpController {

    private final McpServerService mcpServerService;

    public McpController(McpServerService mcpServerService) {
        this.mcpServerService = mcpServerService;
    }

    @PostMapping
    public McpServerService.McpJsonRpcResponse handle(
            @RequestBody McpServerService.McpJsonRpcRequest request) {
        return mcpServerService.handle(request);
    }
}
