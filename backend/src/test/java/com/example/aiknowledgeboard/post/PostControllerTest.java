package com.example.aiknowledgeboard.post;

import com.example.aiknowledgeboard.common.PageResponse;
import com.example.aiknowledgeboard.config.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostService postService;

    @MockitoBean
    JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void list_returnsPostsFromService() throws Exception {
        PostSummaryResponse post = new PostSummaryResponse(
                1L,
                "첫 번째 게시글",
                "게시글 내용입니다.",
                10L,
                "tester",
                List.of("spring", "java"),
                3L,
                Instant.parse("2026-06-10T00:00:00Z"),
                Instant.parse("2026-06-10T01:00:00Z")
        );

        when(postService.list(0, 10, null, null))
                .thenReturn(new PageResponse<>(
                        List.of(post),
                        0,
                        10,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("첫 번째 게시글"))
                .andExpect(jsonPath("$.content[0].contentPreview").value("게시글 내용입니다."))
                .andExpect(jsonPath("$.content[0].authorId").value(10))
                .andExpect(jsonPath("$.content[0].authorNickname").value("tester"))
                .andExpect(jsonPath("$.content[0].tags[0]").value("spring"))
                .andExpect(jsonPath("$.content[0].commentCount").value(3))
                .andExpect(jsonPath("$.content[0].createdAt").exists())
                .andExpect(jsonPath("$.content[0].updatedAt").exists())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }
}