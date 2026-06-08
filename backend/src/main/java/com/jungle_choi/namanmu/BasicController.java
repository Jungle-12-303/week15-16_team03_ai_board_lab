package com.jungle_choi.namanmu;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicController {

    @GetMapping("/")
    public Map<String, String> index() {
        return Map.of("message", "Project Alpha API server is running.");
    }
}
