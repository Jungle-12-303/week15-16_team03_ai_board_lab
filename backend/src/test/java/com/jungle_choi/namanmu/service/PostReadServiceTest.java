package com.jungle_choi.namanmu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.read.PostRead;
import com.jungle_choi.namanmu.domain.read.PostReadRepository;
import com.jungle_choi.namanmu.domain.user.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

class PostReadServiceTest {

    private final PostReadRepository postReadRepository =
            Mockito.mock(PostReadRepository.class);
    private final PostReadService postReadService =
            new PostReadService(postReadRepository);

    @Test
    void markReadDeletesOldReadHistoryWhenUserHasMoreThanFiftyReads() {
        User user = user(1L, "cedis");
        Post post = post(100L, user, "New post");
        PostRead oldRead = PostRead.create(user, post(10L, user, "Old post"));

        when(postReadRepository.findByUserIdAndPostId(user.getId(), post.getId()))
                .thenReturn(Optional.empty());
        when(postReadRepository.countByUserId(user.getId()))
                .thenReturn(51L);
        when(postReadRepository.findByUserIdOrderByReadAtAsc(eq(user.getId()), any(Pageable.class)))
                .thenReturn(List.of(oldRead));

        postReadService.markRead(user, post);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(postReadRepository).findByUserIdOrderByReadAtAsc(eq(user.getId()), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(1);
        verify(postReadRepository).deleteAllInBatch(List.of(oldRead));
        verify(postReadRepository).save(any(PostRead.class));
    }

    private static User user(Long userId, String name) {
        User user = User.createLocalUser(name);
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }

    private static Post post(Long postId, User author, String title) {
        Post post = Post.create(author, "Learning", title, title + " content");
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
