package com.jungle_choi.namanmu.api;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of("message", "Project Alpha API server is running.");
    }
}
