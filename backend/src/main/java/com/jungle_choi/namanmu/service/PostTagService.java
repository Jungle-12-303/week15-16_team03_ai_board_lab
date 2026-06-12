package com.jungle_choi.namanmu.service;

import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.domain.post.PostTag;
import com.jungle_choi.namanmu.domain.post.PostTagRepository;
import com.jungle_choi.namanmu.domain.tag.Tag;
import com.jungle_choi.namanmu.domain.tag.TagRepository;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PostTagService {

    private final PostTagRepository postTagRepository;
    private final TagRepository tagRepository;

    public PostTagService(
            PostTagRepository postTagRepository,
            TagRepository tagRepository) {
        this.postTagRepository = postTagRepository;
        this.tagRepository = tagRepository;
    }

    public void updatePostTags(Post post, List<String> tagNames) {
        postTagRepository.deleteByPostId(post.getId());
        postTagRepository.flush();

        normalizeTags(tagNames).forEach((tagName) -> {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(Tag.create(tagName)));
            postTagRepository.save(PostTag.create(post, tag));
        });
    }

    private static Set<String> normalizeTags(List<String> tagNames) {
        Set<String> normalizedTags = new LinkedHashSet<>();
        Set<String> normalizedTagKeys = new HashSet<>();

        if (tagNames == null) {
            return normalizedTags;
        }

        tagNames.stream()
                .map(String::trim)
                .filter((tagName) -> !tagName.isBlank())
                .forEach((tagName) -> {
                    String tagKey = tagName.toLowerCase(Locale.ROOT);
                    if (normalizedTagKeys.add(tagKey)) {
                        normalizedTags.add(tagName);
                    }
                });

        return normalizedTags;
    }
}
