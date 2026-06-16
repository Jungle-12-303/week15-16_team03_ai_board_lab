package com.jungle_choi.namanmu.service.rag;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ko.KoreanAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.springframework.stereotype.Component;

@Component
public class KoreanTextAnalyzer {

    private final KoreanAnalyzer analyzer = new KoreanAnalyzer();

    public List<String> tokenize(String text) {
        String normalizedText = normalizeSearchText(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        addNoriTokens(tokens, normalizedText);
        addFallbackTokens(tokens, normalizedText);

        return tokens.stream()
                .map(KoreanTextAnalyzer::normalizeSearchToken)
                .filter(KoreanTextAnalyzer::isMeaningfulTerm)
                .toList();
    }

    private void addNoriTokens(Set<String> tokens, String text) {
        try (TokenStream tokenStream = analyzer.tokenStream("content", text)) {
            CharTermAttribute termAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                tokens.add(termAttribute.toString());
            }

            tokenStream.end();
        } catch (IOException exception) {
            throw new IllegalStateException("Korean text analysis failed.", exception);
        }
    }

    private static void addFallbackTokens(Set<String> tokens, String text) {
        Arrays.stream(text.split("[^\\p{L}\\p{N}]+"))
                .filter((token) -> !token.isBlank())
                .filter((token) -> token.matches(".*[a-z0-9].*"))
                .forEach(tokens::add);
    }

    static String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace("깃허브 액션", " github actions ")
                .replace("깃헙 액션", " github actions ")
                .replace("깃허브", " github ")
                .replace("깃헙", " github ")
                .replace("깃랩", " gitlab ")
                .replace("깃 액션", " github actions ")
                .replace("github action", " github actions ")
                .replace("워크플로우", " workflow ")
                .replace("워크플로", " workflow ")
                .replace("시크릿", " secrets ")
                .replace("비밀값", " secrets ")
                .replace("배포", " deploy ")
                .replace("시피유", " cpu ")
                .replace("씨피유", " cpu ")
                .replace("프로세서", " cpu ")
                .replace("그래픽 카드", " gpu ")
                .replace("그래픽카드", " gpu ")
                .replace("팬 소음", " fan noise ")
                .replace("소음", " noise ")
                .replace("언더볼팅", " undervolt ")
                .replace("발열", " temperature ")
                .replace("온도", " temperature ")
                .replace("날씨", " weather ")
                .replace("기상", " weather ")
                .replace("기온", " temperature ")
                .replace("브리핑", " briefing ")
                .replace("리액트", " react ")
                .replace("유즈 스테이트", " usestate ")
                .replace("유즈스테이트", " usestate ")
                .replace("상태 관리", " state ")
                .replace("상태관리", " state ")
                .replace("상태", " state ")
                .replace("훅", " hook ")
                .replace("예보", " forecast ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeSearchToken(String token) {
        String particleStrippedToken = stripKoreanParticle(token);

        if (particleStrippedToken.startsWith("업그레이드")) {
            return "upgrade";
        }

        if (particleStrippedToken.startsWith("언더볼팅")) {
            return "undervolt";
        }

        if (particleStrippedToken.startsWith("워크플로")) {
            return "workflow";
        }

        if (particleStrippedToken.startsWith("배포")) {
            return "deploy";
        }

        if (particleStrippedToken.startsWith("소음")) {
            return "noise";
        }

        if (particleStrippedToken.startsWith("하드웨어")) {
            return "hardware";
        }

        return particleStrippedToken;
    }

    private static String stripKoreanParticle(String token) {
        if (token == null) {
            return "";
        }

        for (String suffix : List.of("으로", "에서", "에게", "부터", "까지", "처럼", "보다")) {
            if (token.endsWith(suffix) && token.length() > suffix.length() + 1) {
                return token.substring(0, token.length() - suffix.length());
            }
        }

        for (String suffix : List.of("은", "는", "이", "가", "을", "를", "에", "와", "과", "로", "도", "만", "의")) {
            if (token.endsWith(suffix) && token.length() > suffix.length() + 1) {
                return token.substring(0, token.length() - suffix.length());
            }
        }

        return token;
    }

    private static boolean isMeaningfulTerm(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        if (token.length() < 2) {
            return false;
        }

        return !Set.of(
                "this",
                "that",
                "with",
                "from",
                "about",
                "daily",
                "learning",
                "project",
                "development",
                "review",
                "briefing",
                "current",
                "하고",
                "싶다",
                "정리",
                "정리하고",
                "사용",
                "사용법",
                "내용",
                "관련",
                "게시글",
                "작성",
                "찾고",
                "오늘",
                "그냥",
                "지역",
                "짧은",
                "하려고",
                "합니다",
                "현재",
                "다음",
                "위해",
                "위해서",
                "대한",
                "대해",
                "있는",
                "없는",
                "있고",
                "중입니다",
                "했습니다",
                "고민",
                "기록",
                "메모",
                "확인",
                "간단히",
                "짧게",
                "기분",
                "피곤",
                "피곤해서",
                "일상",
                "생각",
                "느낌",
                "이번",
                "정도",
                "부분",
                "때문",
                "통해")
                .contains(token);
    }

    static boolean isGenericGuardTerm(String token) {
        return Set.of(
                "오늘",
                "어제",
                "내일",
                "정보",
                "상황",
                "실시간",
                "방법",
                "기반",
                "자동",
                "좋은",
                "나쁜")
                .contains(token);
    }

    @PreDestroy
    void close() {
        analyzer.close();
    }
}
