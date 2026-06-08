package com.jungle_choi.namanmu.api;

import java.util.List;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "http://localhost:5173")
public class PostController {

    @GetMapping
    public List<PostResponse> listPosts() {
        return List.of(
                new PostResponse(
                        1L,
                        "cedis",
                        "Learning",
                        "Today",
                        "Spring Boot first API",
                        "React will load this post from the Spring Boot server.",
                        List.of("Spring Boot", "REST API"),
                        List.of(new CommentResponse(1L, "cedis", "The next step is MySQL and JPA."))),
                new PostResponse(
                        2L,
                        "alpha",
                        "Project",
                        "Today",
                        "Project Alpha backend direction",
                        "The board UI stays in React, and Spring Boot provides JSON APIs.",
                        List.of("React", "Spring"),
                        List.of()));
    }

    public record PostResponse(
            Long id,
            String author,
            String category,
            String createdAt,
            String title,
            String content,
            List<String> tags,
            List<CommentResponse> comments) {
    }

    public record CommentResponse(Long id, String author, String content) {
    }
}
