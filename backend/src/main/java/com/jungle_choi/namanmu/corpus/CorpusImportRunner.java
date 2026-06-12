package com.jungle_choi.namanmu.corpus;

import com.jungle_choi.namanmu.config.CorpusImportProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.corpus-import", name = "enabled", havingValue = "true")
public class CorpusImportRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CorpusImportRunner.class);

    private final CorpusImportProperties properties;
    private final CorpusSourceLoader corpusSourceLoader;
    private final CorpusWebCrawler corpusWebCrawler;
    private final CorpusPostImportService corpusPostImportService;
    private final ConfigurableApplicationContext applicationContext;

    public CorpusImportRunner(
            CorpusImportProperties properties,
            CorpusSourceLoader corpusSourceLoader,
            CorpusWebCrawler corpusWebCrawler,
            CorpusPostImportService corpusPostImportService,
            ConfigurableApplicationContext applicationContext) {
        this.properties = properties;
        this.corpusSourceLoader = corpusSourceLoader;
        this.corpusWebCrawler = corpusWebCrawler;
        this.corpusPostImportService = corpusPostImportService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<CorpusSource> sources = corpusSourceLoader.load(properties.normalizedSourceFile());
        int requestedLimit = Math.min(properties.normalizedMaxItems(), sources.size());
        int importedCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        log.info(
                "Corpus import started. sourceFile={}, maxItems={}, totalSources={}",
                properties.normalizedSourceFile(),
                requestedLimit,
                sources.size());

        for (CorpusSource source : sources.stream().limit(requestedLimit).toList()) {
            try {
                CrawledDocument document = corpusWebCrawler.crawl(source, properties);
                CorpusPostImportService.ImportResult result =
                        corpusPostImportService.importDocument(source, document, properties);

                if (result.imported()) {
                    importedCount++;
                    log.info(
                            "Corpus imported. category={}, tags={}, title={}",
                            result.category(),
                            result.tags(),
                            result.title());
                } else {
                    skippedCount++;
                    log.info(
                            "Corpus skipped. reason={}, title={}, url={}",
                            result.message(),
                            result.title(),
                            source.url());
                }
            } catch (Exception exception) {
                failedCount++;
                log.warn(
                        "Corpus import failed. url={}, reason={}",
                        source.url(),
                        exception.getMessage());
            } finally {
                corpusWebCrawler.pause(properties);
            }
        }

        log.info(
                "Corpus import finished. imported={}, skipped={}, failed={}",
                importedCount,
                skippedCount,
                failedCount);

        if (properties.exitAfterRun()) {
            int exitCode = SpringApplication.exit(applicationContext);
            System.exit(exitCode);
        }
    }
}
