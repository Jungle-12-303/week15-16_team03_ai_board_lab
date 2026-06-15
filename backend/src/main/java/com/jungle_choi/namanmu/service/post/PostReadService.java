package com.jungle_choi.namanmu.service.post;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.read.PostRead;
import com.jungle_choi.namanmu.domain.read.PostReadRepository;
import com.jungle_choi.namanmu.domain.user.User;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostReadService {

    private static final int READ_HISTORY_KEEP_LIMIT = 50;

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

        pruneOldReadHistory(user.getId());
    }

    private void pruneOldReadHistory(Long userId) {
        long excessCount = postReadRepository.countByUserId(userId) - READ_HISTORY_KEEP_LIMIT;
        if (excessCount <= 0) {
            return;
        }

        List<PostRead> oldReads = postReadRepository.findByUserIdOrderByReadAtAsc(
                userId,
                PageRequest.of(0, Math.toIntExact(excessCount)));
        postReadRepository.deleteAllInBatch(oldReads);
    }
}
