package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.service.DevSeedService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class DevSeedController {

    private final DevSeedService devSeedService;
    private final boolean seedEnabled;

    public DevSeedController(
            DevSeedService devSeedService,
            @Value("${app.dev-seed.enabled:false}") boolean seedEnabled) {
        this.devSeedService = devSeedService;
        this.seedEnabled = seedEnabled;
    }

    @PostMapping("/seed-posts")
    public DevSeedService.SeedResult seedPosts() {
        if (!seedEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        return devSeedService.seedPosts();
    }
}
