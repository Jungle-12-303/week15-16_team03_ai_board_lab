package com.jungle_choi.namanmu.api;

import com.jungle_choi.namanmu.service.RagEvaluationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/evaluation")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173"})
public class RagEvaluationController {

    private final RagEvaluationService ragEvaluationService;

    public RagEvaluationController(RagEvaluationService ragEvaluationService) {
        this.ragEvaluationService = ragEvaluationService;
    }

    @PostMapping("/retrieval")
    public RagEvaluationService.RagEvaluationReport evaluateRetrieval(
            @RequestParam(defaultValue = "5") int k) {
        return ragEvaluationService.evaluateRetrieval(k);
    }
}
