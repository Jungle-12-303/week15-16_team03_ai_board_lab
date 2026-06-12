package com.jungle_choi.namanmu.corpus;

import com.jungle_choi.namanmu.config.CorpusImportProperties;
import com.jungle_choi.namanmu.domain.post.PostRepository;
import com.jungle_choi.namanmu.domain.user.User;
import com.jungle_choi.namanmu.domain.user.UserRepository;
import com.jungle_choi.namanmu.service.PostService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorpusPostImportService {

    private static final int MAX_TITLE_LENGTH = 120;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final CorpusClassifier corpusClassifier;

    public CorpusPostImportService(
            UserRepository userRepository,
            PostRepository postRepository,
            PostService postService,
            CorpusClassifier corpusClassifier) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.postService = postService;
        this.corpusClassifier = corpusClassifier;
    }

    @Transactional
    public ImportResult importDocument(
            CorpusSource source,
            CrawledDocument document,
            CorpusImportProperties properties) {
        String title = normalizeTitle(document.title(), document.url());
        String sourceMarker = sourceMarker(document.url());
        User author = findOrCreateAuthor(properties.normalizedAuthorName());

        if (postRepository.existsImportedSource(author.getEmail(), sourceMarker)) {
            return ImportResult.skipped(title, "Source URL already exists.");
        }

        if (postRepository.existsByAuthor_EmailAndTitle(author.getEmail(), title)) {
            return ImportResult.skipped(title, "Title already exists for crawler author.");
        }

        String content = buildContent(document, properties);
        if (content.length() < properties.normalizedMinContentLength()) {
            return ImportResult.skipped(title, "Content is shorter than minContentLength.");
        }

        String category = corpusClassifier.normalizeCategory(
                source.category(),
                title,
                content);
        List<String> tags = corpusClassifier.buildTags(
                category,
                source.tags(),
                title,
                content);

        postService.createPost(
                author,
                new PostService.SavePostCommand(
                        category,
                        title,
                        content,
                        tags));

        return ImportResult.imported(title, category, tags);
    }

    private User findOrCreateAuthor(String authorName) {
        User newAuthor = User.createLocalUser(authorName);

        return userRepository.findByEmail(newAuthor.getEmail())
                .orElseGet(() -> userRepository.save(newAuthor));
    }

    private static String normalizeTitle(String title, String url) {
        String normalizedTitle = normalize(title);

        if (normalizedTitle.isBlank()) {
            normalizedTitle = url;
        }

        if (normalizedTitle.length() <= MAX_TITLE_LENGTH) {
            return normalizedTitle;
        }

        return normalizedTitle.substring(0, MAX_TITLE_LENGTH);
    }

    private static String buildContent(
            CrawledDocument document,
            CorpusImportProperties properties) {
        String text = normalize(document.text());
        int maxLength = properties.normalizedMaxContentLength();

        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }

        return text + "\n\n---\n" + sourceMarker(document.url());
    }

    private static String sourceMarker(String url) {
        return "Source: " + url;
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }

    public record ImportResult(
            boolean imported,
            String title,
            String category,
            List<String> tags,
            String message) {

        static ImportResult imported(String title, String category, List<String> tags) {
            return new ImportResult(
                    true,
                    title,
                    category,
                    tags,
                    "Imported.");
        }

        static ImportResult skipped(String title, String message) {
            return new ImportResult(
                    false,
                    title,
                    "",
                    List.of(),
                    message);
        }
    }
}
