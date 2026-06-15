package com.example.aiknowledgeboard.ai.common;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiLogService {
    private final AiLogRepository aiLogRepository;

    public AiLogService(AiLogRepository aiLogRepository) {
        this.aiLogRepository = aiLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String featureType, String input, String output, boolean success, String errorMessage) {
        aiLogRepository.save(new AiLog(featureType, trim(input), trim(output), success, trim(errorMessage)));
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
