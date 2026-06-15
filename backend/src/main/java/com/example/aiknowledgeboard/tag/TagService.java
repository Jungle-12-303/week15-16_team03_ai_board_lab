package com.example.aiknowledgeboard.tag;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;



@Service
public class TagService {
    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Set<Tag> getOrCreateTags(List<String> rawTags) {
        LinkedHashSet<String> names = normalize(rawTags);
        LinkedHashSet<Tag> result = new LinkedHashSet<>();
        for (String name : names) {
            result.add(tagRepository.findByName(name).orElseGet(() -> tagRepository.save(new Tag(name))));
        }
        return result;
    }

    public LinkedHashSet<String> normalize(List<String> rawTags) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (rawTags == null) {
            return names;
        }
        for (String rawTag : rawTags) {
            if (rawTag == null) {
                continue;
            }
            String name = rawTag.trim().replace("#", "").toLowerCase(Locale.ROOT);
            if (!name.isBlank()) {
                names.add(name);
            }
            if (names.size() >= 5) {
                break;
            }
        }
        return names;
    }

    public List<String> toNames(Set<Tag> tags) {
        List<String> names = new ArrayList<>();
        for (Tag tag : tags) {
            names.add(tag.getName());
        }
        return names;
    }
}
