package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.read.PostRead;
import com.jungle_choi.namanmu.domain.read.PostReadRepository;
import com.jungle_choi.namanmu.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostReadService {

    private final PostReadRepository postReadRepository;

    public PostReadService(PostReadRepository postReadRepository) {
        this.postReadRepository = postReadRepository;
    }

    @Transactional
    public void markRead(User user, Post post) {
        if (user == null || post == null) {
            return;
        }

        postReadRepository.findByUserIdAndPostId(user.getId(), post.getId())
                .ifPresentOrElse(
                        PostRead::markAgain,
                        () -> postReadRepository.save(PostRead.create(user, post)));
    }
}
