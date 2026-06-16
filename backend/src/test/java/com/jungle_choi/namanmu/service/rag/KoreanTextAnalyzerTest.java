package com.jungle_choi.namanmu.service.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KoreanTextAnalyzerTest {

    private final KoreanTextAnalyzer analyzer = new KoreanTextAnalyzer();

    @Test
    void tokenizeNormalizesKoreanAndTechnicalTerms() {
        assertThat(analyzer.tokenize("깃허브 액션으로 배포 실패와 시크릿 설정을 정리했습니다."))
                .contains("github", "actions", "deploy", "secrets")
                .doesNotContain("으로", "했습니다");
    }

    @Test
    void tokenizeKeepsHardwareTermsAsSubjectSignals() {
        assertThat(analyzer.tokenize("7800X3D에서 9800X3D로 CPU 업그레이드할지 고민 중입니다."))
                .contains("7800x3d", "9800x3d", "cpu", "upgrade");
    }
}
