package com.jungle_choi.namanmu.corpus;

import com.jungle_choi.namanmu.config.CorpusImportProperties;
import java.io.IOException;
import java.time.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class CorpusWebCrawler {

    private static final String CONTENT_SELECTOR = String.join(", ",
            "article",
            "main",
            "[role=main]",
            "#content",
            "#contents",
            ".content",
            ".contents",
            ".article",
            ".post",
            ".entry-content");

    public CrawledDocument crawl(CorpusSource source, CorpusImportProperties properties) {
        try {
            Document document = Jsoup.connect(source.url())
                    .userAgent(properties.normalizedUserAgent())
                    .timeout((int) properties.requestTimeout().toMillis())
                    .followRedirects(true)
                    .get();
            String title = normalize(document.title());
            String text = extractText(document);

            return new CrawledDocument(title, text, source.url());
        } catch (IOException exception) {
            throw new IllegalStateException("Corpus source could not be crawled: " + source.url(), exception);
        }
    }

    public void pause(CorpusImportProperties properties) {
        Duration delay = properties.requestDelay();

        if (delay.isZero()) {
            return;
        }

        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Corpus import was interrupted.", exception);
        }
    }

    private static String extractText(Document document) {
        document.select(String.join(", ",
                "script",
                "style",
                "noscript",
                "svg",
                "canvas",
                "iframe",
                "form",
                "nav",
                "header",
                "footer",
                "aside",
                "button",
                "select",
                "input")).remove();

        Element content = document.selectFirst(CONTENT_SELECTOR);
        Element selectedContent = content == null ? document.body() : content;

        if (selectedContent == null) {
            return "";
        }

        return normalize(selectedContent.text());
    }

    private static String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }
}
