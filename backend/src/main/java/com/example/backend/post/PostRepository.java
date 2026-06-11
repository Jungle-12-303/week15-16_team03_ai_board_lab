package com.example.backend.post;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long>{
    List<Post> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);
}