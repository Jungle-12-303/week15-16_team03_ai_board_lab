package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.service.rag.RagAblationEvaluationService;
import com.jungle_choi.namanmu.service.rag.RagEvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/evaluation")
public class RagEvaluationController {

    private final RagEvaluationService ragEvaluationService;
    private final RagAblationEvaluationService ragAblationEvaluationService;

    public RagEvaluationController(
            RagEvaluationService ragEvaluationService,
            RagAblationEvaluationService ragAblationEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
        this.ragAblationEvaluationService = ragAblationEvaluationService;
    }

    @PostMapping("/retrieval")
    public RagEvaluationService.RagEvaluationReport evaluateRetrieval(
            @RequestParam(defaultValue = "5") int k) {
        return ragEvaluationService.evaluateRetrieval(k);
    }

    @PostMapping("/retrieval/variants")
    public RagAblationEvaluationService.AblationEvaluationReport evaluateRetrievalVariants(
            @RequestParam(defaultValue = "5") int k) {
        return ragAblationEvaluationService.evaluateVariants(k);
    }
}
