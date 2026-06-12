package com.jungle_choi.namanmu.corpus;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CorpusClassifier {

    private static final List<String> CATEGORIES = List.of(
            "Development",
            "Learning",
            "Project",
            "Daily",
            "Review",
            "Briefing");

    private static final List<TagRule> TAG_RULES = List.of(
            new TagRule("React", List.of("react", "리액트", "jsx", "component", "컴포넌트")),
            new TagRule("Spring", List.of("spring", "스프링", "spring boot", "spring security")),
            new TagRule("Java", List.of("java", "자바", "jvm")),
            new TagRule("JavaScript", List.of("javascript", "자바스크립트", "js", "typescript", "타입스크립트")),
            new TagRule("Backend", List.of("backend", "백엔드", "server", "서버", "api")),
            new TagRule("Frontend", List.of("frontend", "프론트엔드", "ui", "ux", "browser", "브라우저")),
            new TagRule("Database", List.of("database", "데이터베이스", "mysql", "postgres", "sql", "jpa")),
            new TagRule("Security", List.of("security", "보안", "auth", "인증", "jwt", "token", "토큰")),
            new TagRule("AWS", List.of("aws", "amazon", "ec2", "s3", "vpc", "cloud", "클라우드")),
            new TagRule("Docker", List.of("docker", "도커", "container", "컨테이너")),
            new TagRule("GitHub", List.of("github", "git", "pull request", "issue")),
            new TagRule("AI", List.of("ai", "인공지능", "llm", "gpt", "openai", "rag", "mcp", "agent")),
            new TagRule("Data", List.of("data", "데이터", "공공데이터", "통계", "분석")),
            new TagRule("Weather", List.of("weather", "날씨", "기상", "기온", "비", "눈", "바람")),
            new TagRule("Policy", List.of("정책", "정부", "보도자료", "공공", "기관")),
            new TagRule("Learning", List.of("learn", "학습", "공부", "튜토리얼", "guide", "가이드")),
            new TagRule("Review", List.of("review", "리뷰", "후기", "비교", "장단점"))
    );

    public String normalizeCategory(String category, String title, String content) {
        if (category != null && CATEGORIES.contains(category.trim())) {
            return category.trim();
        }

        String text = searchableText(title, content);

        if (containsAny(text, "정책", "보도자료", "공공데이터", "날씨", "기상")) {
            return "Briefing";
        }

        if (containsAny(text, "리뷰", "후기", "비교", "장단점")) {
            return "Review";
        }

        if (containsAny(text, "프로젝트", "github", "배포", "운영")) {
            return "Project";
        }

        if (containsAny(text, "학습", "튜토리얼", "guide", "가이드", "공부")) {
            return "Learning";
        }

        return "Development";
    }

    public List<String> buildTags(String category, List<String> baseTags, String title, String content) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(category);

        if (baseTags != null) {
            baseTags.stream()
                    .map(String::trim)
                    .filter((tag) -> !tag.isBlank())
                    .forEach(tags::add);
        }

        String text = searchableText(title, content);
        TAG_RULES.stream()
                .filter((rule) -> rule.matches(text))
                .map(TagRule::name)
                .forEach(tags::add);

        return tags.stream()
                .filter((tag) -> tag.length() <= 30)
                .limit(6)
                .toList();
    }

    private static String searchableText(String title, String content) {
        return ((title == null ? "" : title) + " " + (content == null ? "" : content))
                .toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private record TagRule(String name, List<String> keywords) {

        boolean matches(String text) {
            return keywords.stream()
                    .map((keyword) -> keyword.toLowerCase(Locale.ROOT))
                    .anyMatch(text::contains);
        }
    }
}
