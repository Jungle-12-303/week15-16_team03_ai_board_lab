package com.example.aiknowledgeboard.post;

import com.example.aiknowledgeboard.ai.rag.RagService;
import com.example.aiknowledgeboard.auth.CurrentUserService;
import com.example.aiknowledgeboard.comment.CommentRepository;
import com.example.aiknowledgeboard.comment.CommentResponse;
import com.example.aiknowledgeboard.comment.CommentService;
import com.example.aiknowledgeboard.tag.Tag;
import com.example.aiknowledgeboard.tag.TagService;
import com.example.aiknowledgeboard.user.UserEntity;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    PostRepository postRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    CommentService commentService;

    @Mock
    TagService tagService;

    @Mock
    CurrentUserService currentUserService;

    @Mock
    RagService ragService;

    @InjectMocks
    PostService postService;

    @Test
    void create_allowsLoggedInUserToCreatePost() {
        UserEntity mockLoggedInUser = new UserEntity("test@example.com", "password", "tester");
        PostRequest mockPostRequest = new PostRequest(
                "첫 게시글",
                "게시글 내용입니다.",
                List.of()
        );
        Set<Tag> mockTags = Set.of();
        List<CommentResponse> mockComments = List.of();

        when(currentUserService.getCurrentUser()).thenReturn(mockLoggedInUser);
        when(tagService.getOrCreateTags(mockPostRequest.tags())).thenReturn(mockTags);
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tagService.toNames(mockTags)).thenReturn(List.of());
        when(commentService.findByPost(any())).thenReturn(mockComments);

        PostDetailResponse response = postService.create(mockPostRequest);

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        Post capturedPost = postCaptor.getValue();

        assertThat(capturedPost.getAuthor()).isEqualTo(mockLoggedInUser);
        assertThat(capturedPost.getTitle()).isEqualTo("첫 게시글");
        assertThat(capturedPost.getContent()).isEqualTo("게시글 내용입니다.");

        assertThat(response.title()).isEqualTo("첫 게시글");
        assertThat(response.content()).isEqualTo("게시글 내용입니다.");
    }
}
