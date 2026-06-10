package com.jungle_choi.namanmu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jungle_choi.namanmu.domain.embedding.PostEmbedding;
import com.jungle_choi.namanmu.domain.embedding.PostEmbeddingRepository;
import com.jungle_choi.namanmu.domain.post.Post;
import com.jungle_choi.namanmu.service.OpenAiEmbeddingClient.EmbeddingResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostEmbeddingService {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final PostEmbeddingRepository postEmbeddingRepository;
    private final ObjectMapper objectMapper;

    public PostEmbeddingService(
            PostEmbeddingRepository postEmbeddingRepository,
            ObjectMapper objectMapper) {
        this.postEmbeddingRepository = postEmbeddingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveOrReplace(Post post, String sourceText, EmbeddingResult embeddingResult) {
        String embeddingJson = toJson(embeddingResult);
        String sourceHash = hash(sourceText);

        postEmbeddingRepository.findByPost_Id(post.getId())
                .ifPresentOrElse(
                        (postEmbedding) -> postEmbedding.replace(
                                embeddingResult.model(),
                                embeddingResult.dimensions(),
                                embeddingJson,
                                sourceHash),
                        () -> postEmbeddingRepository.save(PostEmbedding.create(
                                post,
                                embeddingResult.model(),
                                embeddingResult.dimensions(),
                                embeddingJson,
                                sourceHash)));
    }

    public String sourceHash(String sourceText) {
        return hash(sourceText);
    }

    private String toJson(EmbeddingResult embeddingResult) {
        try {
            return objectMapper.writeValueAsString(embeddingResult.embedding());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Embedding vector could not be serialized.", exception);
        }
    }

    private static String hash(String sourceText) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] digest = messageDigest.digest(normalize(sourceText).getBytes(StandardCharsets.UTF_8));

            return toHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(HASH_ALGORITHM + " is not available.", exception);
        }
    }

    private static String normalize(String sourceText) {
        if (sourceText == null) {
            return "";
        }

        return sourceText.trim();
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }

        return hex.toString();
    }
}
